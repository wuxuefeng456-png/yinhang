package com.bankops.service;

import com.bankops.dto.Views;
import com.bankops.model.AccountStatus;
import com.bankops.model.LoginAudit;
import com.bankops.model.UserAccount;
import com.bankops.repository.LoginAuditRepository;
import com.bankops.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final String[] DANGEROUS_MARKERS = {"'", "\"", ";", "--", "/*", "<script", " union ", " select ", " drop "};

    private final UserAccountRepository userRepository;
    private final LoginAuditRepository auditRepository;
    private final PasswordService passwordService;
    private final ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();
    private final int sessionHours;
    private final int maxFailedAttempts;

    public AuthService(UserAccountRepository userRepository,
                       LoginAuditRepository auditRepository,
                       PasswordService passwordService,
                       @Value("${bankops.auth.session-hours:8}") int sessionHours,
                       @Value("${bankops.auth.max-failed-attempts:5}") int maxFailedAttempts) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.passwordService = passwordService;
        this.sessionHours = sessionHours;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Views.LoginResponse login(String rawUsername, String password, String ipAddress) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (isIllegalUsername(username)) {
            writeAudit(username, false, "检测到非法账号格式", ipAddress);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "检测到非法账号：仅允许4-20位字母、数字和下划线，且必须以字母开头");
        }

        Optional<UserAccount> optional = userRepository.findByUsernameIgnoreCase(username);
        if (optional.isEmpty()) {
            writeAudit(username, false, "账号不存在", ipAddress);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        UserAccount user = optional.get();
        if (user.getStatus() == AccountStatus.DISABLED) {
            writeAudit(username, false, "账号已停用", ipAddress);
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已停用，请联系管理员");
        }
        if (user.getStatus() == AccountStatus.LOCKED) {
            writeAudit(username, false, "账号已锁定", ipAddress);
            throw new BusinessException(HttpStatus.LOCKED, "账号已锁定，请联系管理员解锁");
        }

        if (!passwordService.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            int failed = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failed);
            if (failed >= maxFailedAttempts) user.setStatus(AccountStatus.LOCKED);
            userRepository.save(user);
            writeAudit(username, false, failed >= maxFailedAttempts ? "密码错误，账号已锁定" : "密码错误", ipAddress);
            if (failed >= maxFailedAttempts) {
                throw new BusinessException(HttpStatus.LOCKED, "密码连续错误5次，账号已自动锁定");
            }
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误，剩余尝试次数：" + (maxFailedAttempts - failed));
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(sessionHours);
        sessions.put(token, new SessionData(user.getId(), expiresAt));
        writeAudit(username, true, "登录成功", ipAddress);
        return new Views.LoginResponse(token, Views.CurrentUser.from(user), Duration.ofHours(sessionHours).toSeconds());
    }

    @Transactional(readOnly = true)
    public Optional<Views.CurrentUser> authenticate(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        SessionData session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return userRepository.findById(session.userId())
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .map(Views.CurrentUser::from);
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public void invalidateUser(Long userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }

    public boolean isIllegalUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) return true;
        String lower = " " + username.toLowerCase(Locale.ROOT) + " ";
        for (String marker : DANGEROUS_MARKERS) {
            if (lower.contains(marker)) return true;
        }
        return false;
    }

    private void writeAudit(String username, boolean success, String reason, String ipAddress) {
        LoginAudit audit = new LoginAudit();
        audit.setUsername(username.isBlank() ? "<empty>" : username.substring(0, Math.min(username.length(), 30)));
        audit.setSuccess(success);
        audit.setReason(reason);
        audit.setIpAddress(ipAddress);
        auditRepository.save(audit);
    }

    private record SessionData(Long userId, LocalDateTime expiresAt) {}
}
