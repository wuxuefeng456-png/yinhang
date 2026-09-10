package com.bankops.controller;

import com.bankops.config.AuthInterceptor;
import com.bankops.dto.Views;
import com.bankops.model.AssetStatus;
import com.bankops.model.Role;
import com.bankops.model.WorkOrder;
import com.bankops.model.WorkOrderStatus;
import com.bankops.repository.AssetRepository;
import com.bankops.repository.LoginAuditRepository;
import com.bankops.repository.UserAccountRepository;
import com.bankops.repository.WorkOrderRepository;
import com.bankops.service.WorkOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final WorkOrderRepository orders;
    private final AssetRepository assets;
    private final UserAccountRepository users;
    private final LoginAuditRepository audits;
    private final WorkOrderService workOrderService;

    public DashboardController(WorkOrderRepository orders, AssetRepository assets,
                               UserAccountRepository users, LoginAuditRepository audits,
                               WorkOrderService workOrderService) {
        this.orders = orders;
        this.assets = assets;
        this.users = users;
        this.audits = audits;
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public Map<String, Object> dashboard(HttpServletRequest request) {
        Views.CurrentUser user = (Views.CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
        Map<String, Object> result = new LinkedHashMap<>();
        List<WorkOrder> visibleOrders = user.role() == Role.ADMIN
                ? orders.findAllByOrderByUpdatedAtDesc()
                : workOrderService.findMyWork(user.username());
        long pending = countStatus(visibleOrders, WorkOrderStatus.PENDING);
        long processing = countStatus(visibleOrders, WorkOrderStatus.PROCESSING);
        long resolved = countStatus(visibleOrders, WorkOrderStatus.RESOLVED);
        long closed = countStatus(visibleOrders, WorkOrderStatus.CLOSED);
        result.put("pendingOrders", pending + processing);
        result.put("onlineAssets", assets.countByStatus(AssetStatus.ONLINE));
        result.put("maintenanceAssets", assets.countByStatus(AssetStatus.MAINTENANCE));
        result.put("failedLogins24h", user.role() == Role.ADMIN
                ? audits.countBySuccessFalseAndLoginTimeAfter(LocalDateTime.now().minusHours(24)) : 0);
        result.put("totalUsers", user.role() == Role.ADMIN ? users.count() : 0);
        result.put("orderStatus", Map.of(
                "pending", pending,
                "processing", processing,
                "resolved", resolved,
                "closed", closed));
        result.put("assetStatus", Map.of(
                "online", assets.countByStatus(AssetStatus.ONLINE),
                "maintenance", assets.countByStatus(AssetStatus.MAINTENANCE),
                "offline", assets.countByStatus(AssetStatus.OFFLINE)));
        result.put("recentOrders", visibleOrders.stream().limit(6).toList());
        return result;
    }

    private long countStatus(List<WorkOrder> workOrders, WorkOrderStatus status) {
        return workOrders.stream().filter(order -> order.getStatus() == status).count();
    }
}
