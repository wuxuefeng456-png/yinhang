package com.bankops.dto;

import com.bankops.model.AccountStatus;
import com.bankops.model.Role;
import com.bankops.model.ManagementModule;
import com.bankops.model.SpecialistDuty;
import com.bankops.model.UserAccount;

import java.time.LocalDateTime;

public final class Views {
    private Views() {}

    public record CurrentUser(Long id, String username, String displayName, Role role, AccountStatus status,
                              ManagementModule specialtyModule, SpecialistDuty specialistDuty) {
        public static CurrentUser from(UserAccount user) {
            return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getStatus(),
                    user.getSpecialtyModule(), user.getSpecialistDuty());
        }
    }

    public record UserView(
            Long id,
            String username,
            String displayName,
            Role role,
            ManagementModule specialtyModule,
            SpecialistDuty specialistDuty,
            AccountStatus status,
            int failedLoginAttempts,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt) {
        public static UserView from(UserAccount user) {
            return new UserView(
                    user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                    user.getSpecialtyModule(), user.getSpecialistDuty(), user.getStatus(),
                    user.getFailedLoginAttempts(), user.getLastLoginAt(), user.getCreatedAt());
        }
    }

    public record LoginResponse(String token, CurrentUser user, long expiresInSeconds) {}
}
