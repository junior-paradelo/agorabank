package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.enums.IdempotencyRecordScope;
import com.k3ras.agorabank.model.enums.IdempotencyRecordStatus;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyService {

    Optional<IdempotencyRecord> findExisting(UUID accountId, IdempotencyRecordScope scope, String idempotencyKey);

    boolean isDuplicate(UUID accountId, IdempotencyRecordScope scope, String idempotencyKey,
                        IdempotencyRecordStatus status);

    IdempotencyRecord startRecord(Account account, IdempotencyRecordScope scope, String idempotencyKey,
                                  String requestHash);

    void verifyRequestHash(IdempotencyRecord record, String requestHash);

    IdempotencyRecord completeRecord(IdempotencyRecord record, Integer responseCode, String responseBody);

    IdempotencyRecord failRecord(IdempotencyRecord record, Integer responseCode, String responseBody);

    int cleanupExpired();

    String hashRequest(String payload);
}
