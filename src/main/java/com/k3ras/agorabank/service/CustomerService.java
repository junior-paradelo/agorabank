package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.Customer;

import java.util.Optional;

public interface CustomerService {

    Customer create(Customer customer);

    Customer getById(Long id);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByDocumentNumber(String documentNumber);

    Customer update(Long id, Customer customer);

    void delete(Long id);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

}
