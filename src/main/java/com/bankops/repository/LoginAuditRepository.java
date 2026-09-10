package com.bankops.repository;

import com.bankops.model.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
    List<LoginAudit> findTop100ByOrderByLoginTimeDesc();
    long countBySuccessFalseAndLoginTimeAfter(LocalDateTime time);
}
