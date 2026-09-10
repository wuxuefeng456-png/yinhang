package com.bankops.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "governance_record", indexes = {
        @Index(name = "idx_governance_module", columnList = "module"),
        @Index(name = "idx_governance_status", columnList = "status"),
        @Index(name = "idx_governance_department", columnList = "department"),
        @Index(name = "idx_governance_updated", columnList = "updatedAt")
})
public class GovernanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ManagementModule module;

    @Column(nullable = false, unique = true, length = 40)
    private String recordNo;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 40)
    private String recordType;

    @Column(nullable = false, length = 40)
    private String department;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 40)
    private String owner;

    private LocalDate plannedDate;

    @Column(length = 20)
    private String priority;

    @Column(length = 20)
    private String riskLevel;

    @Column(length = 30)
    private String versionNo;

    private Integer score;

    @Column(nullable = false)
    private Boolean compliant = Boolean.TRUE;

    @Column(length = 1500)
    private String details;

    @Column(nullable = false, length = 30)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (compliant == null) compliant = Boolean.TRUE;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public ManagementModule getModule() { return module; }
    public void setModule(ManagementModule module) { this.module = module; }
    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getCompliant() { return compliant; }
    public void setCompliant(Boolean compliant) { this.compliant = compliant; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
