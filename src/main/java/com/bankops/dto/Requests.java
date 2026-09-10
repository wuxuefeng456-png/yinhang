package com.bankops.dto;

import com.bankops.model.AccountStatus;
import com.bankops.model.AssetStatus;
import com.bankops.model.Role;
import com.bankops.model.ManagementModule;
import com.bankops.model.SpecialistDuty;
import com.bankops.model.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class Requests {
    private Requests() {}

    public record LoginRequest(
            @NotBlank @Size(max = 30) String username,
            @NotBlank @Size(max = 64) String password) {}

    public record UserCreateRequest(
            @NotBlank @Size(min = 4, max = 20) String username,
            @NotBlank @Size(min = 2, max = 40) String displayName,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotNull Role role,
            ManagementModule specialtyModule,
            SpecialistDuty specialistDuty) {}

    public record UserStatusRequest(@NotNull AccountStatus status) {}

    public record PasswordResetRequest(
            @NotBlank @Size(min = 8, max = 64) String newPassword) {}

    public record SpecialistAssignmentRequest(
            ManagementModule specialtyModule,
            SpecialistDuty specialistDuty) {}

    public record WorkOrderRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 30) String category,
            @NotBlank @Size(max = 20) String priority,
            @NotNull WorkOrderStatus status,
            @NotBlank @Size(max = 60) String systemName,
            @Size(max = 40) String assignee,
            @Size(max = 1000) String description) {}

    public record AssetRequest(
            @NotBlank @Size(max = 30) String assetCode,
            @NotBlank @Size(max = 80) String assetName,
            @NotBlank @Size(max = 30) String assetType,
            @NotBlank @Size(max = 45) String ipAddress,
            @NotBlank @Size(max = 20) String environment,
            @NotNull AssetStatus status,
            @NotBlank @Size(max = 40) String owner,
            @Size(max = 500) String description) {}

    public record GovernanceRecordRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 40) String recordType,
            @NotBlank @Size(max = 40) String department,
            @Size(max = 100) String category,
            @NotBlank @Size(max = 30) String status,
            @NotBlank @Size(max = 40) String owner,
            LocalDate plannedDate,
            @Size(max = 20) String priority,
            @Size(max = 20) String riskLevel,
            @Size(max = 30) String versionNo,
            @Min(0) @Max(100) Integer score,
            Boolean compliant,
            @Size(max = 1500) String details) {}

    public record GovernanceStatusRequest(@NotBlank @Size(max = 30) String status) {}

    public record GovernanceReviewRequest(
            @Min(0) @Max(100) Integer score,
            Boolean compliant,
            @NotBlank @Size(max = 1500) String details) {}

    public record GovernanceDecisionRequest(@NotBlank @Size(max = 500) String reason) {}
}
