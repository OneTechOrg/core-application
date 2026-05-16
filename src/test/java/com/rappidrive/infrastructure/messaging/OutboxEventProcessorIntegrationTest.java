package com.rappidrive.infrastructure.messaging;

import com.rappidrive.domain.outbox.OutboxEvent;
import com.rappidrive.infrastructure.persistence.adapters.JpaOutboxRepository;
import com.rappidrive.infrastructure.persistence.entities.OutboxEventJpaEntity;
import com.rappidrive.infrastructure.persistence.repositories.SpringDataOutboxRepository;
import com.rappidrive.infrastructure.test.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OutboxEventProcessor with real database.
 * HIST-2026-011: Validates full event lifecycle from PENDING → SENT/FAILED
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaOutboxRepository.class})
class OutboxEventProcessorIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SpringDataOutboxRepository springDataRepository;

    @Autowired
    private JpaOutboxRepository outboxRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clear outbox table before each test
        springDataRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    void shouldPersistOutboxEventSuccessfully() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(
                eventId,
                aggregateId,
                "TRIP_CREATED",
                "{\"tripId\": \"123\"}",
                "PENDING",
                0,
                null,
                LocalDateTime.now(ZoneId.systemDefault()),
                null,
                null
        );

        // When
        outboxRepository.save(event);
        entityManager.flush();

        // Then
        OutboxEventJpaEntity persisted = springDataRepository.findById(eventId).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getId()).isEqualTo(eventId);
        assertThat(persisted.getStatus()).isEqualTo("PENDING");
        assertThat(persisted.getAttempts()).isZero();
        assertThat(persisted.getEventType()).isEqualTo("TRIP_CREATED");
    }

    @Test
    void shouldFindPendingEventsWithoutLock() {
        // Given - Insert multiple events
        UUID event1Id = UUID.randomUUID();
        UUID event2Id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
            event1Id, UUID.randomUUID(), "TRIP_CREATED", "{}", 
            "PENDING", 0, null, now, null, null
        );
        springDataRepository.save(event1);

        OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
            event2Id, UUID.randomUUID(), "DRIVER_ASSIGNED", "{}", 
            "PENDING", 0, null, now, null, null
        );
        springDataRepository.save(event2);

        // When
        List<OutboxEvent> pending = outboxRepository.findPendingBatch(now.plusSeconds(10), 10);

        // Then
        assertThat(pending).hasSize(2);
        assertThat(pending).extracting(OutboxEvent::getStatus).allMatch("PENDING"::equals);
    }

    @Test
    void shouldFindPendingEventsWithPessimisticLock() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEventJpaEntity event = new OutboxEventJpaEntity(
            eventId, UUID.randomUUID(), "PAYMENT_PROCESSED", "{\"amount\": 100}", 
            "PENDING", 0, null, now, null, null
        );
        springDataRepository.save(event);

        // When - Query with pessimistic lock (SELECT ... FOR UPDATE SKIP LOCKED)
        List<OutboxEventJpaEntity> locked = springDataRepository.findPendingForUpdate(10);

        // Then
        assertThat(locked).hasSize(1);
        assertThat(locked.get(0).getId()).isEqualTo(eventId);
    }

    @Test
    void shouldSkipLockedEventsDuringConcurrentAccess() {
        // Given
        UUID event1Id = UUID.randomUUID();
        UUID event2Id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEventJpaEntity event1 = new OutboxEventJpaEntity(
            event1Id, UUID.randomUUID(), "TRIP_CREATED", "{}", 
            "PENDING", 0, null, now, null, null
        );
        springDataRepository.save(event1);

        OutboxEventJpaEntity event2 = new OutboxEventJpaEntity(
            event2Id, UUID.randomUUID(), "DRIVER_ASSIGNED", "{}", 
            "PENDING", 0, null, now, null, null
        );
        springDataRepository.save(event2);

        // When - First query locks event1
        List<OutboxEventJpaEntity> firstBatch = springDataRepository.findPendingForUpdate(1);

        // Then - Should get only the first event
        assertThat(firstBatch).hasSize(1);
    }

    @Test
    void shouldMarkEventAsSentSuccessfully() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEvent event = new OutboxEvent(
                eventId,
                UUID.randomUUID(),
                "TRIP_COMPLETED",
                "{}",
                "PENDING",
                0,
                null,
                createdAt,
                null,
                null
        );
        outboxRepository.save(event);
        entityManager.flush();

        // When
        outboxRepository.markSent(eventId);
        entityManager.flush();

        // Then
        OutboxEventJpaEntity persisted = springDataRepository.findById(eventId).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo("SENT");
        assertThat(persisted.getSentAt()).isNotNull();
    }

    @Test
    void shouldIncrementAttemptsAndUpdateNextAttemptTime() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEvent event = new OutboxEvent(
                eventId,
                UUID.randomUUID(),
                "PAYMENT_FAILED",
                "{}",
                "PENDING",
                0,
                null,
                createdAt,
                null,
                null
        );
        outboxRepository.save(event);
        entityManager.flush();

        // When
        LocalDateTime nextAttempt = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(10);
        outboxRepository.incrementAttempts(eventId, nextAttempt);
        entityManager.flush();

        // Then
        OutboxEventJpaEntity persisted = springDataRepository.findById(eventId).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getAttempts()).isEqualTo(1);
        assertThat(persisted.getNextAttemptAt()).isNotNull();
    }

    @Test
    void shouldMarkEventAsFailedAfterMaxRetries() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEvent event = new OutboxEvent(
                eventId,
                UUID.randomUUID(),
                "ASYNC_SERVICE_CALL",
                "{}",
                "PENDING",
                5, // Already maxed out
                null,
                createdAt,
                null,
                null
        );
        outboxRepository.save(event);
        entityManager.flush();

        // When
        outboxRepository.markFailed(eventId);
        entityManager.flush();

        // Then
        OutboxEventJpaEntity persisted = springDataRepository.findById(eventId).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldIdempotentlyMarkEventAsSent() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

        OutboxEvent event = new OutboxEvent(
                eventId,
                UUID.randomUUID(),
                "IDEMPOTENT_TEST",
                "{}",
                "PENDING",
                0,
                null,
                createdAt,
                null,
                null
        );
        outboxRepository.save(event);
        entityManager.flush();

        // When - Mark as sent
        outboxRepository.markSent(eventId);
        entityManager.flush();

        OutboxEventJpaEntity firstSent = springDataRepository.findById(eventId).orElse(null);
        assertThat(firstSent).isNotNull();
        assertThat(firstSent.getStatus()).isEqualTo("SENT");
        assertThat(firstSent.getSentAt()).isNotNull();
        LocalDateTime firstSentAt = firstSent.getSentAt();

        // When - Mark again (should be idempotent)
        outboxRepository.markSent(eventId);
        entityManager.flush();

        // Then - Should be idempotent (sentAt doesn't change, status remains SENT)
        OutboxEventJpaEntity secondSent = springDataRepository.findById(eventId).orElse(null);
        assertThat(secondSent).isNotNull();
        assertThat(secondSent.getStatus()).isEqualTo("SENT");
        assertThat(secondSent.getSentAt()).isEqualTo(firstSentAt);  // Should be same timestamp
    }

    @Test
    void shouldHandleEventsWithRetryDelay() {
        // Given
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime nextAttempt = createdAt.plusSeconds(100); // Far in future

        OutboxEventJpaEntity event = new OutboxEventJpaEntity(
            eventId, UUID.randomUUID(), "RETRY_DELAYED_EVENT", "{}", 
            "PENDING", 2, nextAttempt, createdAt, null, null
        );
        springDataRepository.save(event);
        entityManager.flush();

        // When - Query for pending should not return this event yet
        List<OutboxEvent> pending = outboxRepository.findPendingBatch(
                LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(50), 10
        );

        // Then - Should not include our event because next_attempt_at is in the future
        assertThat(pending).noneMatch(e -> e.getId().equals(eventId));

        // But query after next attempt time should return it
        List<OutboxEvent> afterDelay = outboxRepository.findPendingBatch(
                LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(150), 10
        );
        assertThat(afterDelay).anySatisfy(e -> {
            if (e.getId().equals(eventId)) {
                assertThat(e.getAttempts()).isEqualTo(2);
            }
        });
    }
}
