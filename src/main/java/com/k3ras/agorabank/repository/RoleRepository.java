package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Basic searches
    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
