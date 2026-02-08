package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    
    // Basic searches
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByUsernameOrEmail(String username, String email);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Operations by status
    List<AuthUser> findByEnabled(Boolean enabled);
    Optional<AuthUser> findByUsernameAndEnabled(String username, Boolean enabled);
    
    // Operations by role
    @Query("SELECT u FROM AuthUser u JOIN u.roles r WHERE r.name = :roleName")
    List<AuthUser> findByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM AuthUser u JOIN u.roles r WHERE r.name = :roleName AND u.enabled = :enabled")
    List<AuthUser> findByRoleNameAndEnabled(@Param("roleName") String roleName, @Param("enabled") Boolean enabled);
}
