package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void whenFindByEmail_thenReturnCustomer() {
        // given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setDocumentNumber("12345678");
        entityManager.persist(customer);
        entityManager.flush();

        // when
        Customer found = customerRepository.findByEmail("test@example.com");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        // given
        Customer customer = new Customer();
        customer.setEmail("exists@example.com");
        customer.setDocumentNumber("87654321");
        entityManager.persist(customer);
        entityManager.flush();

        // when
        boolean exists = customerRepository.existsByEmail("exists@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void whenFindByDocumentNumber_thenReturnCustomer() {
        // given
        Customer customer = new Customer();
        customer.setEmail("doc@example.com");
        customer.setDocumentNumber("DOC123456");
        entityManager.persist(customer);
        entityManager.flush();

        // when
        Customer found = customerRepository.findByDocumentNumber("DOC123456");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getDocumentNumber()).isEqualTo("DOC123456");
    }

    @Test
    void whenExistsByDocumentNumber_thenReturnTrue() {
        // given
        Customer customer = new Customer();
        customer.setEmail("exists@example.com");
        customer.setDocumentNumber("87654321");
        entityManager.persist(customer);
        entityManager.flush();

        // when
        boolean exists = customerRepository.existsByDocumentNumber("87654321");

        // then
        assertThat(exists).isTrue();
    }

}
