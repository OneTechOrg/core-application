package com.rappidrive.infrastructure.messaging;

import com.rappidrive.application.ports.output.EventDispatcherPort;
import com.rappidrive.application.ports.output.OutboxRepositoryPort;
import com.rappidrive.domain.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Background worker that publishes pending outbox events to an external transport.
 */
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepositoryPort outboxRepository;
    private final EventDispatcherPort dispatcher;

    // Configurable values; for simplicity are constants here
    private final int batchSize = 20;
    private final int maxAttempts = 5;
    private final Duration baseBackoff = Duration.ofSeconds(5);

    @Scheduled(fixedDelayString = "${outbox.publisher.delay.ms:5000}")
    public void publishPendingScheduled() {
        publishPending();
    }

    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> batch = outboxRepository.findPendingBatch(now, batchSize);
        if (batch.isEmpty()) return;
        log.debug("Publishing {} outbox events", batch.size());

        for (OutboxEvent e : batch) {
            try {
                dispatcher.dispatch(e.getId(), e.getEventType(), e.getPayload());
                outboxRepository.markSent(e.getId());
                log.info("Event {} dispatched", e.getId());
            } catch (Exception ex) {
                log.warn("Failed to dispatch event {}: {}", e.getId(), ex.getMessage());
                int attempts = e.getAttempts() + 1;
                if (attempts >= maxAttempts) {
                    outboxRepository.markFailed(e.getId());
                    log.error("Event {} marked as FAILED after {} attempts", e.getId(), attempts);
                } else {
                    LocalDateTime nextAttempt = computeNextAttempt(attempts);
                    outboxRepository.incrementAttempts(e.getId(), nextAttempt);
                }
            }
        }
    }

    private LocalDateTime computeNextAttempt(int attempts) {
        // simple exponential-ish backoff
        long seconds = baseBackoff.getSeconds() * attempts;
        return LocalDateTime.now().plusSeconds(seconds);
    }
}
