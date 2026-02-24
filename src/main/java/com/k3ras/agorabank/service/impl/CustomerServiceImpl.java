package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.CustomerStatus;
import com.k3ras.agorabank.repository.CustomerRepository;
import com.k3ras.agorabank.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer create(Customer customer) {
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Customer with this email already exists");
        }

        if (customer.getDocumentNumber() != null &&
                customerRepository.existsByDocumentNumber(customer.getDocumentNumber())) {
            throw new IllegalArgumentException("Customer with this document number already exists");
        }

        return customerRepository.save(customer);
    }

    @Override
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    public Optional<Customer> findByDocumentNumber(String documentNumber) {
        return customerRepository.findByDocumentNumber(documentNumber);
    }

    @Override
    public Customer update(Long id, Customer customer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));

        if (customer.getEmail() != null && !customer.getEmail().equals(existing.getEmail())) {
            if (customerRepository.existsByEmail(customer.getEmail())) {
                throw new IllegalArgumentException("Customer with this email already exists");
            }
            existing.setEmail(customer.getEmail());
        }

        if (customer.getDocumentNumber() != null &&
                !customer.getDocumentNumber().equals(existing.getDocumentNumber())) {
            if (customerRepository.existsByDocumentNumber(customer.getDocumentNumber())) {
                throw new IllegalArgumentException("Customer with this document number already exists");
            }
            existing.setDocumentNumber(customer.getDocumentNumber());
        }

        existing.setFirstName(customer.getFirstName());
        existing.setLastName(customer.getLastName());
        existing.setPhoneNumber(customer.getPhoneNumber());
        existing.setDocumentType(customer.getDocumentType());
        existing.setStatus(customer.getStatus());

        return customerRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
        customer.setStatus(CustomerStatus.DISABLED);
        customerRepository.save(customer);
    }

    @Override
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocumentNumber(String documentNumber) {
        return customerRepository.existsByDocumentNumber(documentNumber);
    }
}
