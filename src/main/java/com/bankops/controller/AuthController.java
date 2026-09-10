package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.service.BusinessException;
import com.bankops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Views.LoginResponse login(@Valid @RequestBody Requests.LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request.username(), request.password(), clientIp(servletRequest));
    }

    @PostMapping("/register")
    public void register() {
        throw new BusinessException(HttpStatus.FORBIDDEN, "公开注册已关闭，请联系管理员创建账号");
    }

    @GetMapping("/me")
    public Views.CurrentUser me(HttpServletRequest request) {
        return (Views.CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.logout(token);
        return Map.of("message", "已安全退出");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
