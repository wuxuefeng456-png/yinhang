package com.bankops.repository;

import com.bankops.model.GovernanceRecord;
import com.bankops.model.ManagementModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernanceRecordRepository extends JpaRepository<GovernanceRecord, Long> {
    List<GovernanceRecord> findAllByModuleOrderByUpdatedAtDesc(ManagementModule module);
}
