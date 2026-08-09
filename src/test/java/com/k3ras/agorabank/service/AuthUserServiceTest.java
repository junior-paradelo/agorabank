package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.DuplicatedResourceException;
import com.k3ras.agorabank.exception.InvalidCredentialsException;
import com.k3ras.agorabank.model.AuthUser;
import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import com.k3ras.agorabank.repository.AuthUserRepository;
import com.k3ras.agorabank.service.impl.AuthUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthUserServiceImpl authUserService;

    @BeforeEach
    void setUp() {
        authUserService = new AuthUserServiceImpl(authUserRepository, roleService, passwordEncoder);
    }
    private AuthUser user(String username, String email, String rawPassword) {
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(rawPassword);
        return user;
    }

    @Test
    void register_hashesPassword_assignsUserRole_andEnables() {
        // given
        AuthUser user = user("new.user", "new@example.com", "raw-password");
        Role userRole = new Role(RoleName.USER);
        when(authUserRepository.existsByUsername("new.user")).thenReturn(false);
        when(authUserRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(roleService.getByName(RoleName.USER)).thenReturn(userRole);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthUser saved = authUserService.register(user);

        // then
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getRoles()).contains(userRole);
        verify(authUserRepository).save(user);
    }

    @Test
    void register_throwsDuplicatedResource_whenUsernameExists() {
        // given
        AuthUser user = user("existing", "new@example.com", "raw-password");
        when(authUserRepository.existsByUsername("existing")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> authUserService.register(user))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("existing");
    }

    @Test
    void register_throwsDuplicatedResource_whenEmailExists() {
        // given
        AuthUser user = user("new.user", "existing@example.com", "raw-password");
        when(authUserRepository.existsByUsername("new.user")).thenReturn(false);
        when(authUserRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> authUserService.register(user))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void authenticate_returnsUser_whenCredentialsValid() {
        // given
        AuthUser user = user("johndoe", "john@example.com", "encoded-password");
        user.setEnabled(true);
        when(authUserRepository.findByUsernameOrEmail("johndoe", "johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);

        // when
        AuthUser authenticated = authUserService.authenticate("johndoe", "raw-password");

        // then
        assertThat(authenticated).isEqualTo(user);
    }

    @Test
    void authenticate_throwsInvalidCredentials_whenPasswordWrong() {
        // given
        AuthUser user = user("johndoe", "john@example.com", "encoded-password");
        user.setEnabled(true);
        when(authUserRepository.findByUsernameOrEmail("johndoe", "johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        // when-then
        assertThatThrownBy(() -> authUserService.authenticate("johndoe", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_throwsInvalidCredentials_whenUserDisabled() {
        // given
        AuthUser user = user("johndoe", "john@example.com", "encoded-password");
        user.setEnabled(false);
        when(authUserRepository.findByUsernameOrEmail("johndoe", "johndoe")).thenReturn(Optional.of(user));

        // when-then
        assertThatThrownBy(() -> authUserService.authenticate("johndoe", "raw-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void addRole_addsRoleToUser() {
        // given
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser();
        Role adminRole = new Role(RoleName.ADMIN);
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleService.getByName(RoleName.ADMIN)).thenReturn(adminRole);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthUser result = authUserService.addRole(userId, RoleName.ADMIN);

        // then
        assertThat(result.getRoles()).contains(adminRole);
        verify(authUserRepository).save(user);
    }

    @Test
    void removeRole_removesRoleFromUser() {
        // given
        UUID userId = UUID.randomUUID();
        Role userRole = new Role(RoleName.USER);
        AuthUser user = new AuthUser();
        user.addRole(userRole);
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleService.getByName(RoleName.USER)).thenReturn(userRole);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthUser result = authUserService.removeRole(userId, RoleName.USER);

        // then
        assertThat(result.getRoles()).doesNotContain(userRole);
        verify(authUserRepository).save(user);
    }

    @Test
    void enable_setsEnabledTrue() {
        // given
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser();
        user.setEnabled(false);
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthUser enabled = authUserService.enable(userId);

        // then
        assertThat(enabled.getEnabled()).isTrue();
    }

    @Test
    void disable_setsEnabledFalse() {
        // given
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser();
        user.setEnabled(true);
        when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AuthUser disabled = authUserService.disable(userId);

        // then
        assertThat(disabled.getEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_returnsUserDetailsWithRoles() {
        // given
        Role userRole = new Role(RoleName.USER);
        Role adminRole = new Role(RoleName.ADMIN);
        AuthUser user = user("johndoe", "john@example.com", "encoded-password");
        user.setEnabled(true);
        user.addRole(userRole);
        user.addRole(adminRole);
        when(authUserRepository.findByUsernameOrEmail("johndoe", "johndoe")).thenReturn(Optional.of(user));

        // when
        UserDetails details = authUserService.loadUserByUsername("johndoe");

        // then
        assertThat(details.getUsername()).isEqualTo("johndoe");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFound_whenMissing() {
        // given
        when(authUserRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> authUserService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
