package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.exception.DuplicatedResourceException;
import com.k3ras.agorabank.exception.InvalidCredentialsException;
import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.AuthUser;
import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import com.k3ras.agorabank.repository.AuthUserRepository;
import com.k3ras.agorabank.service.AuthUserService;
import com.k3ras.agorabank.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthUserServiceImpl implements AuthUserService, UserDetailsService {

    private final AuthUserRepository authUserRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthUserServiceImpl(AuthUserRepository authUserRepository,
                               RoleService roleService,
                               PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthUser register(AuthUser user) {
        if (authUserRepository.existsByUsername(user.getUsername())) {
            throw new DuplicatedResourceException("Username already exists: " + user.getUsername());
        }
        if (authUserRepository.existsByEmail(user.getEmail())) {
            throw new DuplicatedResourceException("Email already exists: " + user.getEmail());
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setEnabled(true);
        user.addRole(roleService.getByName(RoleName.USER));
        return authUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUser authenticate(String usernameOrEmail, String rawPassword) {
        AuthUser user = authUserRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new InvalidCredentialsException("User is disabled");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUser getById(UUID id) {
        return authUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> getByUsername(String username) {
        return authUserRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> getByEmail(String email) {
        return authUserRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthUser> getByEnabled(Boolean enabled) {
        return authUserRepository.findByEnabled(enabled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthUser> getByRole(RoleName roleName) {
        return authUserRepository.findByRoleName(roleName.name());
    }

    @Override
    public AuthUser addRole(UUID userId, RoleName roleName) {
        AuthUser user = getById(userId);
        Role role = roleService.getByName(roleName);
        user.addRole(role);
        return authUserRepository.save(user);
    }

    @Override
    public AuthUser removeRole(UUID userId, RoleName roleName) {
        AuthUser user = getById(userId);
        Role role = roleService.getByName(roleName);
        user.removeRole(role);
        return authUserRepository.save(user);
    }

    @Override
    public AuthUser enable(UUID userId) {
        AuthUser user = getById(userId);
        user.setEnabled(true);
        return authUserRepository.save(user);
    }

    @Override
    public AuthUser disable(UUID userId) {
        AuthUser user = getById(userId);
        user.setEnabled(false);
        return authUserRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return authUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authUserRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                true,
                true,
                true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                        .collect(Collectors.toSet()));
    }
}
