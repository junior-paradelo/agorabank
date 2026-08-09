package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    Optional<Role> findByName(RoleName name);

    Role getByName(RoleName name);

    List<Role> getAll();

    boolean existsByName(RoleName name);

    Role ensureRole(RoleName name);

    void ensureDefaultRoles();
}
