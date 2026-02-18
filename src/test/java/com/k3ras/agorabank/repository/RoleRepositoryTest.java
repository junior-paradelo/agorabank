package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Role;
import com.k3ras.agorabank.model.enums.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    private void persistRole(RoleName name) {
        Role role = new Role();
        role.setName(name);
        entityManager.persistAndFlush(role);
    }

    @Test
    void findByName_returnsRole_whenExists() {
        // given
        persistRole(RoleName.USER);

        // when
        Optional<Role> found = roleRepository.findByName(RoleName.USER);

        // then
        assertThat(found)
                .isPresent()
                .get()
                .extracting(Role::getName)
                .isEqualTo(RoleName.USER);
    }

    @Test
    void findByName_returnsEmpty_whenNotExists() {
        // given
        persistRole(RoleName.USER);

        // when
        Optional<Role> found = roleRepository.findByName(RoleName.ADMIN);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByName_returnsTrue_whenExists() {
        // given
        persistRole(RoleName.ADMIN);

        // when
        boolean exists = roleRepository.existsByName(RoleName.ADMIN);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_returnsFalse_whenNotExists() {
        // given
        persistRole(RoleName.USER);

        // when
        boolean exists = roleRepository.existsByName(RoleName.ADMIN);

        // then
        assertThat(exists).isFalse();
    }

}
