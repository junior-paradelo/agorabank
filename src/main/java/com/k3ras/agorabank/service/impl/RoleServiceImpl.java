package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import com.k3ras.agorabank.repository.RoleRepository;
import com.k3ras.agorabank.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(RoleName name) {
        return roleRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getByName(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(RoleName name) {
        return roleRepository.existsByName(name);
    }

    @Override
    public Role ensureRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    @Override
    public void ensureDefaultRoles() {
        for (RoleName name : RoleName.values()) {
            ensureRole(name);
        }
    }
}
