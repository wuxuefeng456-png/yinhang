package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.model.Role;
import com.bankops.model.WorkOrder;
import com.bankops.service.BusinessException;
import com.bankops.service.WorkOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) { this.service = service; }

    @GetMapping
    public List<WorkOrder> list(HttpServletRequest request) {
        Views.CurrentUser user = currentUser(request);
        return service.findVisible(user.role(), user.username());
    }

    @GetMapping("/my")
    public List<WorkOrder> myWork(HttpServletRequest request) {
        requireMember(request);
        return service.findMyWork(currentUser(request).username());
    }

    @PostMapping
    public WorkOrder create(@Valid @RequestBody Requests.WorkOrderRequest request, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        Views.CurrentUser user = currentUser(servletRequest);
        return service.create(request, user.username());
    }

    @PutMapping("/{id}")
    public WorkOrder update(@PathVariable Long id, @Valid @RequestBody Requests.WorkOrderRequest request,
                            HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return service.update(id, request);
    }

    @PutMapping("/{id}/start")
    public WorkOrder start(@PathVariable Long id, HttpServletRequest request) {
        requireMember(request);
        return service.start(id, currentUser(request).username());
    }

    @PutMapping("/{id}/complete")
    public WorkOrder complete(@PathVariable Long id, HttpServletRequest request) {
        Views.CurrentUser user = currentUser(request);
        return service.complete(id, user.username(), user.role());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.delete(id);
        return Map.of("message", "工单已删除");
    }

    private Views.CurrentUser currentUser(HttpServletRequest request) {
        return (Views.CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    private void requireAdmin(HttpServletRequest request) {
        if (currentUser(request).role() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有管理员可以维护工单");
        }
    }

    private void requireMember(HttpServletRequest request) {
        if (currentUser(request).role() != Role.MEMBER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "该功能仅普通成员可用");
        }
    }
}
