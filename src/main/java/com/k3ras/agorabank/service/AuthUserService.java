package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.AuthUser;
import com.k3ras.agorabank.model.enums.RoleName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserService {

    AuthUser register(AuthUser user);

    AuthUser authenticate(String usernameOrEmail, String rawPassword);

    AuthUser getById(UUID id);

    Optional<AuthUser> getByUsername(String username);

    Optional<AuthUser> getByEmail(String email);

    List<AuthUser> getByEnabled(Boolean enabled);

    List<AuthUser> getByRole(RoleName roleName);

    AuthUser addRole(UUID userId, RoleName roleName);

    AuthUser removeRole(UUID userId, RoleName roleName);

    AuthUser enable(UUID userId);

    AuthUser disable(UUID userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
