package com.bankops.config;

import com.bankops.dto.Views;
import com.bankops.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER = "bankops.currentUser";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String token = request.getHeader("X-Auth-Token");
        Optional<Views.CurrentUser> user = authService.authenticate(token);
        if (user.isEmpty()) {
            writeError(response, 401, "登录状态已失效，请重新登录");
            return false;
        }
        if (request.getRequestURI().startsWith("/api/admin") && user.get().role() != com.bankops.model.Role.ADMIN) {
            writeError(response, 403, "该功能仅管理员可用");
            return false;
        }
        request.setAttribute(CURRENT_USER, user.get());
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("message", message, "status", status)));
    }
}
