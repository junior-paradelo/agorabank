package com.k3ras.agorabank.repository;


import com.k3ras.agorabank.model.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuthUserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthUserRepository authUserRepository;

    private void persistUser(String username, String email) {
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setEnabled(true);
        entityManager.persistAndFlush(user);
    }

    @Test
    void findByUsername_returnsUser_whenExists() {
        // given
        persistUser("john", "john@example.com");

        // when
        Optional<AuthUser> found = authUserRepository.findByUsername("john");

        // then
        assertThat(found).isPresent();
        assertThat(found)
                .isPresent()
                .get()
                .extracting(AuthUser::getUsername)
                .isEqualTo("john");
    }

    @Test
    void findByUsername_returnsEmpty_whenNotExists() {
        // given
        persistUser("john", "john@example.com");

        // when
        Optional<AuthUser> found = authUserRepository.findByUsername("mike");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_returnsUser_whenExists() {
        // given
        persistUser("anna", "anna@example.com");

        // when
        Optional<AuthUser> found = authUserRepository.findByEmail("anna@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found)
                .isPresent()
                .get()
                .extracting(AuthUser::getEmail)
                .isEqualTo("anna@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotExists() {
        // given
        persistUser("anna", "anna@example.com");

        // when
        Optional<AuthUser> found = authUserRepository.findByEmail("other@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void findByUsernameOrEmail_returnsUser_whenUsernameMatches() {
        // given
        persistUser("lucas", "lucas@example.com");

        // when
        Optional<AuthUser> found = authUserRepository
                .findByUsernameOrEmail("lucas", "wrong@example.com");

        // then
        assertThat(found).isPresent();
    }

    @Test
    void findByUsernameOrEmail_returnsUser_whenEmailMatches() {
        // given
        persistUser("lucas", "lucas@example.com");

        // when
        Optional<AuthUser> found = authUserRepository
                .findByUsernameOrEmail("wrong", "lucas@example.com");

        // then
        assertThat(found).isPresent();
    }

    @Test
    void findByUsernameOrEmail_returnsEmpty_whenNoMatch() {
        // given
        persistUser("lucas", "lucas@example.com");

        // when
        Optional<AuthUser> found = authUserRepository
                .findByUsernameOrEmail("nope", "nope@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_returnsTrue_whenExists() {
        // given
        persistUser("mark", "mark@example.com");

        // when
        boolean exists = authUserRepository.existsByUsername("mark");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenNotExists() {
        // given
        persistUser("mark", "mark@example.com");

        // when
        boolean exists = authUserRepository.existsByUsername("other");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_returnsTrue_whenExists() {
        // given
        persistUser("eva", "eva@example.com");

        // when
        boolean exists = authUserRepository.existsByEmail("eva@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenNotExists() {
        // given
        persistUser("eva", "eva@example.com");

        // when
        boolean exists = authUserRepository.existsByEmail("other@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void findByEnabled_returnsUsers_whenEnabledTrue() {
        // given
        persistUser("enabledUser", "enabled@example.com");

        AuthUser disabledUser = new AuthUser();
        disabledUser.setUsername("disabledUser");
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setPasswordHash("hashed-password");
        disabledUser.setEnabled(false);
        entityManager.persistAndFlush(disabledUser);

        // when
        var results = authUserRepository.findByEnabled(true);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getUsername()).isEqualTo("enabledUser");
    }

    @Test
    void findByUsernameAndEnabled_returnsUser_whenMatchesBoth() {
        // given
        persistUser("comboUser", "combo@example.com");

        // when
        Optional<AuthUser> found = authUserRepository
                .findByUsernameAndEnabled("comboUser", true);

        // then
        assertThat(found).isPresent();
        assertThat(found)
                .isPresent()
                .get()
                .extracting(AuthUser::getUsername)
                .isEqualTo("comboUser");
    }


    @Test
    void findByUsernameAndEnabled_returnsEmpty_whenEnabledDoesNotMatch() {
        // given
        persistUser("comboUser2", "combo3@example.com"); // enabled = true by default

        // when
        Optional<AuthUser> found = authUserRepository
                .findByUsernameAndEnabled("comboUser2", false);

        // then
        assertThat(found).isEmpty();
    }

}
