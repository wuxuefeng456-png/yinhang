package com.bankops.repository;

import com.bankops.model.WorkOrder;
import com.bankops.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByUpdatedAtDesc();
    List<WorkOrder> findTop6ByOrderByUpdatedAtDesc();
    long countByStatus(WorkOrderStatus status);
}
