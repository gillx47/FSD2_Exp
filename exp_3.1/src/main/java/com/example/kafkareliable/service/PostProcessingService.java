package com.example.kafkareliable.service;

import com.example.kafkareliable.model.PostStatus;
import com.example.kafkareliable.model.ScheduledPostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service simulating the business processing and publishing of scheduled posts.
 * Includes controlled failure simulation to demonstrate retries and DLQ routing.
 */
@Service
public class PostProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PostProcessingService.class);

    // Track simulated attempts per event to allow transient recovery
    private final Map<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();
    private final Map<String, PostStatus> postStatuses = new ConcurrentHashMap<>();

    public static class TransientProcessingException extends RuntimeException {
        public TransientProcessingException(String message) {
            super(message);
        }
    }

    public static class FatalProcessingException extends RuntimeException {
        public FatalProcessingException(String message) {
            super(message);
        }
    }

    public void processScheduledPost(ScheduledPostEvent event) {
        log.info("[BUSINESS-SERVICE] Processing post: postId={}, userId={}, content='{}'",
                event.getPostId(), event.getUserId(), event.getContent());

        int currentAttempt = attemptCounters
                .computeIfAbsent(event.getEventId(), k -> new AtomicInteger(0))
                .incrementAndGet();

        log.info("[BUSINESS-SERVICE] Attempt #{} for eventId={}", currentAttempt, event.getEventId());

        if ("TRANSIENT_FAILURE".equalsIgnoreCase(event.getSimulationMode())) {
            // Fails on attempt 1, succeeds on attempt 2 (demonstrating exponential backoff retry success)
            if (currentAttempt < 2) {
                log.warn("[BUSINESS-SERVICE] Simulated transient timeout on attempt {} for postId={}",
                        currentAttempt, event.getPostId());
                postStatuses.put(event.getPostId(), PostStatus.FAILED_RETRYING);
                throw new TransientProcessingException(
                        "Simulated downstream network timeout for post: " + event.getPostId());
            } else {
                log.info("[BUSINESS-SERVICE] Transient issue recovered on attempt {} for postId={}",
                        currentAttempt, event.getPostId());
            }
        } else if ("FATAL_FAILURE".equalsIgnoreCase(event.getSimulationMode())) {
            // Always fails, exhausting max retries and causing DeadLetterPublishingRecoverer to send to DLQ
            log.error("[BUSINESS-SERVICE] Simulated fatal error on attempt {} for postId={}",
                    currentAttempt, event.getPostId());
            postStatuses.put(event.getPostId(), PostStatus.FAILED_SENT_TO_DLQ);
            throw new FatalProcessingException(
                    "Simulated unrecoverable fatal failure for post: " + event.getPostId());
        }

        // Successful processing
        postStatuses.put(event.getPostId(), PostStatus.PUBLISHED);
        log.info("[BUSINESS-SERVICE] Post successfully published! postId={}, userId={}",
                event.getPostId(), event.getUserId());
    }

    public PostStatus getPostStatus(String postId) {
        return postStatuses.getOrDefault(postId, PostStatus.PENDING);
    }

    public Map<String, PostStatus> getAllPostStatuses() {
        return Map.copyOf(postStatuses);
    }

    public void reset() {
        attemptCounters.clear();
        postStatuses.clear();
    }
}
