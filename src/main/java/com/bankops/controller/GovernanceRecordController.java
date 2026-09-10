package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Requests;
import com.bankops.dto.Views;
import com.bankops.model.GovernanceRecord;
import com.bankops.model.ManagementModule;
import com.bankops.model.Role;
import com.bankops.model.SpecialistDuty;
import com.bankops.service.BusinessException;
import com.bankops.service.GovernanceRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
public class GovernanceRecordController {
    private final GovernanceRecordService service;

    public GovernanceRecordController(GovernanceRecordService service) { this.service = service; }

    @GetMapping("/{module}")
    public List<GovernanceRecord> list(@PathVariable String module,
                                       @RequestParam(defaultValue = "") String department,
                                       @RequestParam(defaultValue = "") String status,
                                       @RequestParam(defaultValue = "") String category,
                                       @RequestParam(defaultValue = "") String keyword,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                       HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireModuleAccess(request, managementModule);
        return service.find(managementModule, department, status, category, keyword, fromDate, toDate);
    }

    @GetMapping("/{module}/workflow")
    public Map<String, GovernanceRecordService.WorkflowStep> workflow(@PathVariable String module,
                                                                      HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireModuleAccess(request, managementModule);
        return service.workflow(managementModule);
    }

    @PostMapping("/{module}")
    public GovernanceRecord create(@PathVariable String module,
                                   @Valid @RequestBody Requests.GovernanceRecordRequest body,
                                   HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        Views.CurrentUser user = requireDuty(request, managementModule, SpecialistDuty.REGISTER);
        return service.create(managementModule, body, user.username());
    }

    @PutMapping("/{module}/{id}")
    public GovernanceRecord update(@PathVariable String module, @PathVariable Long id,
                                   @Valid @RequestBody Requests.GovernanceRecordRequest body,
                                   HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireDuty(request, managementModule, SpecialistDuty.REGISTER);
        return service.update(managementModule, id, body);
    }

    @PutMapping("/{module}/{id}/status")
    public GovernanceRecord updateStatus(@PathVariable String module, @PathVariable Long id,
                                         @Valid @RequestBody Requests.GovernanceStatusRequest body,
                                         HttpServletRequest request) {
        requireAdmin(request);
        return service.updateStatus(ManagementModule.fromPath(module), id, body.status());
    }

    @PutMapping("/{module}/{id}/advance")
    public GovernanceRecord advance(@PathVariable String module, @PathVariable Long id,
                                    HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireDuty(request, managementModule, service.requiredDuty(managementModule, id));
        return service.advance(managementModule, id);
    }

    @PutMapping("/{module}/{id}/review")
    public GovernanceRecord review(@PathVariable String module, @PathVariable Long id,
                                   @Valid @RequestBody Requests.GovernanceReviewRequest body,
                                   HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireDuty(request, managementModule, SpecialistDuty.REVIEW);
        return service.review(managementModule, id, body);
    }

    @PutMapping("/{module}/{id}/reject")
    public GovernanceRecord reject(@PathVariable String module, @PathVariable Long id,
                                   @Valid @RequestBody Requests.GovernanceDecisionRequest body,
                                   HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        SpecialistDuty required = service.requiredDuty(managementModule, id);
        if (required != SpecialistDuty.REVIEW && required != SpecialistDuty.APPROVE) {
            requireAdmin(request);
        } else {
            requireDuty(request, managementModule, required);
        }
        return service.reject(managementModule, id, body.reason());
    }

    @DeleteMapping("/{module}/{id}")
    public Map<String, String> delete(@PathVariable String module, @PathVariable Long id,
                                      HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireDuty(request, managementModule, SpecialistDuty.REGISTER);
        service.delete(managementModule, id);
        return Map.of("message", "业务记录已删除");
    }

    @GetMapping("/{module}/export")
    public ResponseEntity<byte[]> export(@PathVariable String module,
                                         @RequestParam(defaultValue = "") String department,
                                         @RequestParam(defaultValue = "") String status,
                                         @RequestParam(defaultValue = "") String category,
                                         @RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                         HttpServletRequest request) {
        ManagementModule managementModule = ManagementModule.fromPath(module);
        requireModuleAccess(request, managementModule);
        byte[] body = ("\uFEFF" + service.exportCsv(managementModule, department, status, category, keyword, fromDate, toDate))
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + managementModule.name().toLowerCase() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    private Views.CurrentUser requireAdmin(HttpServletRequest request) {
        Views.CurrentUser user = currentUser(request);
        if (user.role() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "该管理模块仅管理员可用");
        }
        return user;
    }

    private Views.CurrentUser requireModuleAccess(HttpServletRequest request, ManagementModule module) {
        Views.CurrentUser user = currentUser(request);
        if (user.role() == Role.ADMIN) return user;
        if (user.specialtyModule() != module || user.specialistDuty() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "没有该管理模块的岗位权限");
        }
        return user;
    }

    private Views.CurrentUser requireDuty(HttpServletRequest request, ManagementModule module, SpecialistDuty duty) {
        Views.CurrentUser user = requireModuleAccess(request, module);
        if (user.role() != Role.ADMIN && user.specialistDuty() != duty) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前操作需要“" + dutyLabel(duty) + "”岗位");
        }
        return user;
    }

    private Views.CurrentUser currentUser(HttpServletRequest request) {
        return (Views.CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    private String dutyLabel(SpecialistDuty duty) {
        return switch (duty) {
            case REGISTER -> "台账录入";
            case REVIEW -> "评审确认";
            case APPROVE -> "审批决策";
            case OPERATE -> "执行跟踪";
            case CLOSE -> "验收关闭";
        };
    }
}
