package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.DuplicatedResourceException;
import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.CustomerDocumentType;
import com.k3ras.agorabank.model.enums.CustomerStatus;
import com.k3ras.agorabank.repository.CustomerRepository;
import com.k3ras.agorabank.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository);
    }

    private Customer customer(String email, String documentNumber) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setDocumentNumber(documentNumber);
        customer.setDocumentType(CustomerDocumentType.DNI);
        customer.setStatus(CustomerStatus.ENABLED);
        return customer;
    }

    @Test
    void create_savesCustomer_whenUnique() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByDocumentNumber("12345678A")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Customer saved = customerService.create(customer);

        // then
        assertThat(saved).isEqualTo(customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void create_throwsDuplicatedResource_whenEmailExists() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> customerService.create(customer))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("email");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void create_throwsDuplicatedResource_whenDocumentNumberExists() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByDocumentNumber("12345678A")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> customerService.create(customer))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("document number");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void getById_returnsCustomer_whenExists() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // when
        Customer found = customerService.getById(1L);

        // then
        assertThat(found).isEqualTo(customer);
    }

    @Test
    void getById_throwsResourceNotFound_whenMissing() {
        // given
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> customerService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findByEmail_returnsCustomer() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        when(customerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));

        // when
        Optional<Customer> found = customerService.findByEmail("john@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(customer);
    }

    @Test
    void findByDocumentNumber_returnsCustomer() {
        // given
        Customer customer = customer("john@example.com", "12345678A");
        when(customerRepository.findByDocumentNumber("12345678A")).thenReturn(Optional.of(customer));

        // when
        Optional<Customer> found = customerService.findByDocumentNumber("12345678A");

        // then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(customer);
    }

    @Test
    void update_updatesFields_whenUnique() {
        // given
        Customer existing = customer("john@example.com", "12345678A");
        existing.setId(1L);
        Customer updated = customer("new@example.com", "87654321B");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(customerRepository.existsByDocumentNumber("87654321B")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Customer result = customerService.update(1L, updated);

        // then
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getDocumentNumber()).isEqualTo("87654321B");
        verify(customerRepository).save(existing);
    }

    @Test
    void update_throwsDuplicatedResource_whenEmailTakenByAnother() {
        // given
        Customer existing = customer("john@example.com", "12345678A");
        existing.setId(1L);
        Customer updated = customer("taken@example.com", "87654321B");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> customerService.update(1L, updated))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("email");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_throwsDuplicatedResource_whenDocumentNumberTaken() {
        // given
        Customer existing = customer("john@example.com", "12345678A");
        existing.setId(1L);
        Customer updated = customer("john@example.com", "taken-doc");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByDocumentNumber("taken-doc")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> customerService.update(1L, updated))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("document number");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void delete_setsStatusDisabled() {
        // given
        Customer existing = customer("john@example.com", "12345678A");
        existing.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        customerService.delete(1L);

        // then
        assertThat(existing.getStatus()).isEqualTo(CustomerStatus.DISABLED);
        verify(customerRepository).save(existing);
    }

    @Test
    void delete_throwsResourceNotFound_whenMissing() {
        // given
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> customerService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void existsByEmail_returnsTrue() {
        // given
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        // when-then
        assertThat(customerService.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    void existsByDocumentNumber_returnsFalse() {
        // given
        when(customerRepository.existsByDocumentNumber("nope")).thenReturn(false);

        // when-then
        assertThat(customerService.existsByDocumentNumber("nope")).isFalse();
    }
}
