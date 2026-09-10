package com.bankops.service;

import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.model.AccountStatus;
import com.bankops.model.Role;
import com.bankops.model.ManagementModule;
import com.bankops.model.SpecialistDuty;
import com.bankops.model.UserAccount;
import com.bankops.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;
    private final PasswordService passwordService;
    private final AuthService authService;

    public UserAccountService(UserAccountRepository repository, PasswordService passwordService, AuthService authService) {
        this.repository = repository;
        this.passwordService = passwordService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Views.UserView> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(Views.UserView::from).toList();
    }

    @Transactional
    public Views.UserView create(Requests.UserCreateRequest request) {
        validateAssignment(request.role(), request.specialtyModule(), request.specialistDuty());
        return createAccount(request.username(), request.displayName(), request.password(), request.role(),
                request.specialtyModule(), request.specialistDuty());
    }

    private Views.UserView createAccount(String rawUsername, String rawDisplayName, String rawPassword, Role role,
                                         ManagementModule specialtyModule, SpecialistDuty specialistDuty) {
        String username = rawUsername.trim();
        if (authService.isIllegalUsername(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "账号格式非法：仅允许4-20位字母、数字和下划线，且必须以字母开头");
        }
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "账号已存在");
        }
        PasswordService.PasswordValue password = passwordService.create(rawPassword);
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(rawDisplayName.trim());
        user.setRole(role);
        user.setSpecialtyModule(specialtyModule);
        user.setSpecialistDuty(specialistDuty);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(password.hash());
        user.setPasswordSalt(password.salt());
        return Views.UserView.from(repository.save(user));
    }

    @Transactional
    public Views.UserView changeAssignment(Long id, ManagementModule specialtyModule, SpecialistDuty specialistDuty) {
        UserAccount user = get(id);
        validateAssignment(user.getRole(), specialtyModule, specialistDuty);
        user.setSpecialtyModule(specialtyModule);
        user.setSpecialistDuty(specialistDuty);
        UserAccount saved = repository.save(user);
        authService.invalidateUser(id);
        return Views.UserView.from(saved);
    }

    @Transactional
    public Views.UserView changeStatus(Long id, AccountStatus status, Long currentUserId) {
        UserAccount user = get(id);
        if (id.equals(currentUserId) && status != AccountStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能停用或锁定当前登录账号");
        }
        if (user.getRole() == Role.ADMIN
                && user.getStatus() == AccountStatus.ACTIVE
                && status != AccountStatus.ACTIVE
                && repository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE) <= 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "系统必须至少保留一个可用管理员");
        }
        user.setStatus(status);
        if (status == AccountStatus.ACTIVE) user.setFailedLoginAttempts(0);
        UserAccount saved = repository.save(user);
        if (status != AccountStatus.ACTIVE) authService.invalidateUser(id);
        return Views.UserView.from(saved);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserAccount user = get(id);
        PasswordService.PasswordValue password = passwordService.create(newPassword);
        user.setPasswordHash(password.hash());
        user.setPasswordSalt(password.salt());
        user.setFailedLoginAttempts(0);
        user.setStatus(AccountStatus.ACTIVE);
        repository.save(user);
        authService.invalidateUser(id);
    }

    private UserAccount get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "账号不存在"));
    }

    private void validateAssignment(Role role, ManagementModule specialtyModule, SpecialistDuty specialistDuty) {
        if (role == Role.ADMIN && (specialtyModule != null || specialistDuty != null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "管理员无需绑定专员岗位");
        }
        if ((specialtyModule == null) != (specialistDuty == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "专员模块和岗位职责必须同时设置");
        }
    }
}
