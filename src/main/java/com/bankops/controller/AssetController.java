package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.model.Asset;
import com.bankops.model.Role;
import com.bankops.service.AssetService;
import com.bankops.service.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService service;

    public AssetController(AssetService service) { this.service = service; }

    @GetMapping
    public List<Asset> list() { return service.findAll(); }

    @PostMapping
    public Asset create(@Valid @RequestBody Requests.AssetRequest request, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Asset update(@PathVariable Long id, @Valid @RequestBody Requests.AssetRequest request,
                        HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.delete(id);
        return Map.of("message", "资产已删除");
    }

    private void requireAdmin(HttpServletRequest request) {
        Views.CurrentUser user = (Views.CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (user.role() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有管理员可以维护资产");
        }
    }
}
