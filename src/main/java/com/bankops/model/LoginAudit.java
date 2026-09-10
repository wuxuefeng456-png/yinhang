package com.bankops.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_audit", indexes = {
        @Index(name = "idx_audit_time", columnList = "loginTime"),
        @Index(name = "idx_audit_username", columnList = "username")
})
public class LoginAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @PrePersist
    public void prePersist() {
        if (loginTime == null) loginTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
