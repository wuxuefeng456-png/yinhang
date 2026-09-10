package com.bankops.service;

import com.bankops.dto.Requests;
import com.bankops.model.Role;
import com.bankops.model.WorkOrder;
import com.bankops.model.WorkOrderStatus;
import com.bankops.repository.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WorkOrderService {
    private static final String LEGACY_MEMBER_ASSIGNEE = "普通成员";
    private final WorkOrderRepository repository;

    public WorkOrderService(WorkOrderRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<WorkOrder> findAll() { return repository.findAllByOrderByUpdatedAtDesc(); }

    @Transactional(readOnly = true)
    public List<WorkOrder> findVisible(Role role, String username) {
        return role == Role.ADMIN ? findAll() : findMyWork(username);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findMyWork(String username) {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(order -> isAvailablePending(order, username)
                        || (order.getStatus() != WorkOrderStatus.CLOSED && isOwnedBy(order, username)))
                .toList();
    }

    @Transactional
    public WorkOrder create(Requests.WorkOrderRequest request, String creator) {
        WorkOrder order = new WorkOrder();
        order.setTicketNo("OPS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(100, 1000));
        apply(order, request);
        order.setCreatedBy(creator);
        return repository.save(order);
    }

    @Transactional
    public WorkOrder update(Long id, Requests.WorkOrderRequest request) {
        WorkOrder order = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "工单不存在"));
        apply(order, request);
        return repository.save(order);
    }

    @Transactional
    public WorkOrder start(Long id, String username) {
        WorkOrder order = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "工单不存在"));
        if (order.getStatus() != WorkOrderStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只有待处理工单可以开始处理");
        }
        if (!isAvailablePending(order, username)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "该工单已分配给其他成员");
        }
        order.setStatus(WorkOrderStatus.PROCESSING);
        order.setAssignee(username);
        return repository.save(order);
    }

    @Transactional
    public WorkOrder complete(Long id, String username, Role role) {
        WorkOrder order = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "工单不存在"));
        if (order.getStatus() == WorkOrderStatus.CLOSED || order.getStatus() == WorkOrderStatus.RESOLVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该工单已经结束，不能再次处理");
        }
        if (role == Role.MEMBER) {
            if (order.getStatus() != WorkOrderStatus.PROCESSING) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请先领取工单并开始处理");
            }
            if (!isOwnedBy(order, username)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "只能完成自己领取的工单");
            }
        }
        order.setStatus(WorkOrderStatus.RESOLVED);
        if (isUnassigned(order.getAssignee())) order.setAssignee(username);
        return repository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new BusinessException(HttpStatus.NOT_FOUND, "工单不存在");
        repository.deleteById(id);
    }

    private void apply(WorkOrder order, Requests.WorkOrderRequest request) {
        order.setTitle(request.title().trim());
        order.setCategory(request.category().trim());
        order.setPriority(request.priority().trim());
        order.setStatus(request.status());
        order.setSystemName(request.systemName().trim());
        order.setAssignee(clean(request.assignee()));
        order.setDescription(clean(request.description()));
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private boolean isAvailablePending(WorkOrder order, String username) {
        return order.getStatus() == WorkOrderStatus.PENDING
                && (isUnassigned(order.getAssignee()) || username.equalsIgnoreCase(order.getAssignee()));
    }

    private boolean isOwnedBy(WorkOrder order, String username) {
        return username.equalsIgnoreCase(clean(order.getAssignee()));
    }

    private boolean isUnassigned(String assignee) {
        String value = clean(assignee);
        return value.isEmpty() || LEGACY_MEMBER_ASSIGNEE.equals(value);
    }
}
