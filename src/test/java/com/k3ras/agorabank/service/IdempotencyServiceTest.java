package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.DuplicateIdempotentRequestException;
import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.enums.IdempotencyRecordScope;
import com.k3ras.agorabank.model.enums.IdempotencyRecordStatus;
import com.k3ras.agorabank.repository.IdempotencyRecordRepository;
import com.k3ras.agorabank.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final Duration TTL = Duration.ofHours(1);

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyServiceImpl(idempotencyRecordRepository, TTL);
    }

    @Test
    void startRecord_createsInProgressRecord_withExpiryAndHash() {
        // given
        Account account = new Account();
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IdempotencyRecord record = idempotencyService.startRecord(
                account, IdempotencyRecordScope.DEPOSIT, "key-1", "hash-1");

        // then
        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(captor.capture());
        IdempotencyRecord saved = captor.getValue();
        assertThat(saved.getAccount()).isEqualTo(account);
        assertThat(saved.getScope()).isEqualTo(IdempotencyRecordScope.DEPOSIT);
        assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(saved.getRequestHash()).isEqualTo("hash-1");
        assertThat(saved.getStatus()).isEqualTo(IdempotencyRecordStatus.IN_PROGRESS);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void findExisting_returnsRecord_whenNotExpired() {
        // given
        UUID accountId = UUID.randomUUID();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(idempotencyRecordRepository.findByAccountIdAndScopeAndIdempotencyKey(
                accountId, IdempotencyRecordScope.DEPOSIT, "key-1")).thenReturn(Optional.of(record));

        // when
        Optional<IdempotencyRecord> found =
                idempotencyService.findExisting(accountId, IdempotencyRecordScope.DEPOSIT, "key-1");

        // then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(record);
        verify(idempotencyRecordRepository, never()).delete(any(IdempotencyRecord.class));
    }

    @Test
    void findExisting_deletesAndReturnsEmpty_whenExpired() {
        // given
        UUID accountId = UUID.randomUUID();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(idempotencyRecordRepository.findByAccountIdAndScopeAndIdempotencyKey(
                accountId, IdempotencyRecordScope.DEPOSIT, "key-1")).thenReturn(Optional.of(record));

        // when
        Optional<IdempotencyRecord> found =
                idempotencyService.findExisting(accountId, IdempotencyRecordScope.DEPOSIT, "key-1");

        // then
        assertThat(found).isEmpty();
        verify(idempotencyRecordRepository).delete(record);
    }

    @Test
    void isDuplicate_delegatesToRepository() {
        // given
        UUID accountId = UUID.randomUUID();
        when(idempotencyRecordRepository.existsByAccountIdAndScopeAndIdempotencyKeyAndStatus(
                accountId, IdempotencyRecordScope.WITHDRAW, "key-1", IdempotencyRecordStatus.IN_PROGRESS))
                .thenReturn(true);

        // when
        boolean duplicate = idempotencyService.isDuplicate(
                accountId, IdempotencyRecordScope.WITHDRAW, "key-1", IdempotencyRecordStatus.IN_PROGRESS);

        // then
        assertThat(duplicate).isTrue();
    }

    @Test
    void verifyRequestHash_doesNotThrow_whenHashMatches() {
        // given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestHash("expected-hash");

        // when-then
        idempotencyService.verifyRequestHash(record, "expected-hash");
    }

    @Test
    void verifyRequestHash_throws_whenHashDiffers() {
        // given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestHash("expected-hash");

        // when-then
        assertThatThrownBy(() -> idempotencyService.verifyRequestHash(record, "different-hash"))
                .isInstanceOf(DuplicateIdempotentRequestException.class);
    }

    @Test
    void completeRecord_setsCompletedStatusAndResponse() {
        // given
        IdempotencyRecord record = new IdempotencyRecord();
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IdempotencyRecord completed = idempotencyService.completeRecord(record, 200, "body");

        // then
        assertThat(completed.getStatus()).isEqualTo(IdempotencyRecordStatus.COMPLETED);
        assertThat(completed.getResponseCode()).isEqualTo(200);
        assertThat(completed.getResponseBody()).isEqualTo("body");
    }

    @Test
    void failRecord_setsFailedStatusAndResponse() {
        // given
        IdempotencyRecord record = new IdempotencyRecord();
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        IdempotencyRecord failed = idempotencyService.failRecord(record, 400, "error");

        // then
        assertThat(failed.getStatus()).isEqualTo(IdempotencyRecordStatus.FAILED);
        assertThat(failed.getResponseCode()).isEqualTo(400);
        assertThat(failed.getResponseBody()).isEqualTo("error");
    }

    @Test
    void cleanupExpired_deletesExpiredRecords() {
        // given
        when(idempotencyRecordRepository.deleteExpiredRecords(any(LocalDateTime.class))).thenReturn(3);

        // when
        int deleted = idempotencyService.cleanupExpired();

        // then
        assertThat(deleted).isEqualTo(3);
    }

    @Test
    void hashRequest_returnsDeterministic64HexChars() {
        // when
        String hash1 = idempotencyService.hashRequest("payload");
        String hash2 = idempotencyService.hashRequest("payload");

        // then
        assertThat(hash1).hasSize(64).isEqualTo(hash2);
    }

    @Test
    void hashRequest_differsForDifferentPayloads() {
        // when
        String hash1 = idempotencyService.hashRequest("payload-a");
        String hash2 = idempotencyService.hashRequest("payload-b");

        // then
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
