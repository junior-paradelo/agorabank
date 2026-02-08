package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    
    // Basic searches
    Optional<IdempotencyRecord> findByAccountIdAndScopeAndKey(UUID accountId, String scope, String key);
    Optional<IdempotencyRecord> findByKey(String key);
    
    // Operations by account
    List<IdempotencyRecord> findByAccountId(UUID accountId);
    List<IdempotencyRecord> findByAccountIdAndScope(UUID accountId, String scope);
    
    // Operations by status
    List<IdempotencyRecord> findByStatus(String status);
    List<IdempotencyRecord> findByAccountIdAndStatus(UUID accountId, String status);
    
    // Cleanup expired records
    @Query("SELECT i FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    List<IdempotencyRecord> findExpiredRecords(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);
    
    // Check for in-progress duplicates
    boolean existsByAccountIdAndScopeAndKeyAndStatus(UUID accountId, String scope, String key, String status);
}
