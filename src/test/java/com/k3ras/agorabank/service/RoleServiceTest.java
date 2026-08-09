package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import com.k3ras.agorabank.repository.RoleRepository;
import com.k3ras.agorabank.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository);
    }

    @Test
    void getByName_returnsRole_whenExists() {
        // given
        Role role = new Role(RoleName.USER);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));

        // when
        Role found = roleService.getByName(RoleName.USER);

        // then
        assertThat(found).isEqualTo(role);
        assertThat(found.getName()).isEqualTo(RoleName.USER);
    }

    @Test
    void getByName_throwsResourceNotFound_whenMissing() {
        // given
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> roleService.getByName(RoleName.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void findByName_returnsRole_whenExists() {
        // given
        Role role = new Role(RoleName.USER);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));

        // when
        Optional<Role> found = roleService.findByName(RoleName.USER);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(RoleName.USER);
    }

    @Test
    void findByName_returnsEmpty_whenMissing() {
        // given
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        // when
        Optional<Role> found = roleService.findByName(RoleName.USER);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByName_returnsTrue_whenExists() {
        // given
        when(roleRepository.existsByName(RoleName.USER)).thenReturn(true);

        // when
        boolean exists = roleService.existsByName(RoleName.USER);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_returnsFalse_whenMissing() {
        // given
        when(roleRepository.existsByName(RoleName.ADMIN)).thenReturn(false);

        // when
        boolean exists = roleService.existsByName(RoleName.ADMIN);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void ensureRole_createsRole_whenMissing() {
        // given
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Role ensured = roleService.ensureRole(RoleName.USER);

        // then
        assertThat(ensured.getName()).isEqualTo(RoleName.USER);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void ensureRole_returnsExisting_whenPresent() {
        // given
        Role existing = new Role(RoleName.ADMIN);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(existing));

        // when
        Role ensured = roleService.ensureRole(RoleName.ADMIN);

        // then
        assertThat(ensured).isEqualTo(existing);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void ensureDefaultRoles_ensuresUserAndAdmin() {
        // given
        when(roleRepository.findByName(any(RoleName.class))).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        roleService.ensureDefaultRoles();

        // then
        verify(roleRepository, times(RoleName.values().length)).findByName(any(RoleName.class));
        verify(roleRepository, times(RoleName.values().length)).save(any(Role.class));
        verify(roleRepository).save(argThat(role -> role.getName() == RoleName.USER));
        verify(roleRepository).save(argThat(role -> role.getName() == RoleName.ADMIN));
    }
}
