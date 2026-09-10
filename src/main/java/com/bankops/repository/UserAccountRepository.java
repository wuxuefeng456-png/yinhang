package com.bankops.repository;

import com.bankops.model.AccountStatus;
import com.bankops.model.Role;
import com.bankops.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRole(Role role);
    long countByRoleAndStatus(Role role, AccountStatus status);
    long countByStatus(AccountStatus status);
    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
