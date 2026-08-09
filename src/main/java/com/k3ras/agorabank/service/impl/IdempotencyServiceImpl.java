package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.exception.DuplicateIdempotentRequestException;
import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.enums.IdempotencyRecordScope;
import com.k3ras.agorabank.model.enums.IdempotencyRecordStatus;
import com.k3ras.agorabank.repository.IdempotencyRecordRepository;
import com.k3ras.agorabank.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final Duration ttl;

    @Autowired
    public IdempotencyServiceImpl(IdempotencyRecordRepository idempotencyRecordRepository,
                                  @Value("${agorabank.idempotency.ttl:PT30M}") Duration ttl) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.ttl = ttl;
    }

    @Override
    public Optional<IdempotencyRecord> findExisting(UUID accountId, IdempotencyRecordScope scope,
                                                    String idempotencyKey) {
        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByAccountIdAndScopeAndIdempotencyKey(accountId, scope, idempotencyKey);
        if (existing.isPresent() && existing.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            idempotencyRecordRepository.delete(existing.get());
            return Optional.empty();
        }
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicate(UUID accountId, IdempotencyRecordScope scope, String idempotencyKey,
                               IdempotencyRecordStatus status) {
        return idempotencyRecordRepository
                .existsByAccountIdAndScopeAndIdempotencyKeyAndStatus(accountId, scope, idempotencyKey, status);
    }

    @Override
    public IdempotencyRecord startRecord(Account account, IdempotencyRecordScope scope, String idempotencyKey,
                                         String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setAccount(account);
        record.setScope(scope);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        record.setExpiresAt(LocalDateTime.now().plus(ttl));
        return idempotencyRecordRepository.save(record);
    }

    @Override
    public void verifyRequestHash(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new DuplicateIdempotentRequestException(
                    "Idempotency key reused with a different request payload");
        }
    }

    @Override
    public IdempotencyRecord completeRecord(IdempotencyRecord record, Integer responseCode, String responseBody) {
        record.setStatus(IdempotencyRecordStatus.COMPLETED);
        record.setResponseCode(responseCode);
        record.setResponseBody(responseBody);
        return idempotencyRecordRepository.save(record);
    }

    @Override
    public IdempotencyRecord failRecord(IdempotencyRecord record, Integer responseCode, String responseBody) {
        record.setStatus(IdempotencyRecordStatus.FAILED);
        record.setResponseCode(responseCode);
        record.setResponseBody(responseBody);
        return idempotencyRecordRepository.save(record);
    }

    @Override
    public int cleanupExpired() {
        return idempotencyRecordRepository.deleteExpiredRecords(LocalDateTime.now());
    }

    @Override
    public String hashRequest(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
