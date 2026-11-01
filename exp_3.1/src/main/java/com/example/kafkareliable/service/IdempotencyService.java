package com.example.kafkareliable.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the Idempotency Pattern via Event Tracking / Deduplication Store.
 * In a distributed architecture, this would typically be backed by Redis or an RDBMS table
 * with unique constraints on (event_id).
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    public record ProcessedRecord(String eventId, String postId, LocalDateTime processedAt) {}

    private final Map<String, ProcessedRecord> processedEvents = new ConcurrentHashMap<>();

    /**
     * Checks if the event has already been processed.
     */
    public boolean isProcessed(String eventId) {
        return processedEvents.containsKey(eventId);
    }

    /**
     * Atomically records the event as processed.
     * Returns true if newly marked, false if it was already processed (duplicate detected).
     */
    public boolean markAsProcessed(String eventId, String postId) {
        ProcessedRecord existing = processedEvents.putIfAbsent(
                eventId,
                new ProcessedRecord(eventId, postId, LocalDateTime.now())
        );

        if (existing != null) {
            log.warn("[IDEMPOTENCY] Duplicate event detected for eventId: {}. Already processed at: {}",
                    eventId, existing.processedAt());
            return false;
        }

        log.info("[IDEMPOTENCY] Event marked as processed successfully: eventId={}, postId={}", eventId, postId);
        return true;
    }

    public Map<String, ProcessedRecord> getProcessedEvents() {
        return Map.copyOf(processedEvents);
    }

    public void clear() {
        processedEvents.clear();
    }
}
