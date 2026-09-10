package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.model.LoginAudit;
import com.bankops.repository.LoginAuditRepository;
import com.bankops.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserAccountService userService;
    private final LoginAuditRepository auditRepository;

    public AdminController(UserAccountService userService, LoginAuditRepository auditRepository) {
        this.userService = userService;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/users")
    public List<Views.UserView> users() { return userService.findAll(); }

    @PostMapping("/users")
    public Views.UserView createUser(@Valid @RequestBody Requests.UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/users/{id}/status")
    public Views.UserView changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody Requests.UserStatusRequest request,
                                       HttpServletRequest servletRequest) {
        Views.CurrentUser current = (Views.CurrentUser) servletRequest.getAttribute(AuthInterceptor.CURRENT_USER);
        return userService.changeStatus(id, request.status(), current.id());
    }

    @PutMapping("/users/{id}/password")
    public Map<String, String> resetPassword(@PathVariable Long id,
                                             @Valid @RequestBody Requests.PasswordResetRequest request) {
        userService.resetPassword(id, request.newPassword());
        return Map.of("message", "密码已重置，账号已解锁");
    }

    @PutMapping("/users/{id}/assignment")
    public Views.UserView changeAssignment(@PathVariable Long id,
                                           @RequestBody Requests.SpecialistAssignmentRequest request) {
        return userService.changeAssignment(id, request.specialtyModule(), request.specialistDuty());
    }

    @GetMapping("/login-audits")
    public List<LoginAudit> audits() { return auditRepository.findTop100ByOrderByLoginTimeDesc(); }
}
