package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByEmail(String email);
    Customer findByDocumentNumber(String documentNumber);
    boolean existsByEmail(String email);
    boolean existsByDocumentNumber(String documentNumber);
}
