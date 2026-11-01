package com.example.kafkareliable.controller;

import com.example.kafkareliable.model.PostStatus;
import com.example.kafkareliable.model.ScheduledPostEvent;
import com.example.kafkareliable.producer.ScheduledPostProducer;
import com.example.kafkareliable.service.DlqAuditService;
import com.example.kafkareliable.service.IdempotencyService;
import com.example.kafkareliable.service.PostProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final ScheduledPostProducer producer;
    private final IdempotencyService idempotencyService;
    private final DlqAuditService dlqAuditService;
    private final PostProcessingService postProcessingService;

    public PostController(ScheduledPostProducer producer,
                          IdempotencyService idempotencyService,
                          DlqAuditService dlqAuditService,
                          PostProcessingService postProcessingService) {
        this.producer = producer;
        this.idempotencyService = idempotencyService;
        this.dlqAuditService = dlqAuditService;
        this.postProcessingService = postProcessingService;
    }

    /**
     * 1. Normal Scheduled Post: Processes successfully and records in Idempotency store.
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishNormalPost(@RequestBody(required = false) ScheduledPostEvent request) {
        String eventId = request != null && request.getEventId() != null ? request.getEventId() : UUID.randomUUID().toString();
        String postId = request != null && request.getPostId() != null ? request.getPostId() : "post-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = request != null && request.getUserId() != null ? request.getUserId() : "user-101";
        String content = request != null && request.getContent() != null ? request.getContent() : "Welcome to event-driven Kafka architectures!";

        ScheduledPostEvent event = new ScheduledPostEvent(eventId, postId, userId, content, LocalDateTime.now(), "NORMAL");
        producer.publishPostEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "SUBMITTED",
                "scenario", "NORMAL_PROCESSING",
                "eventId", eventId,
                "postId", postId,
                "message", "Post event published to Kafka topic."
        ));
    }

    /**
     * 2. Transient Failure Simulation: Fails on attempt 1, retries with exponential backoff, succeeds on attempt 2.
     */
    @PostMapping("/simulate-retry")
    public ResponseEntity<Map<String, Object>> simulateTransientRetry() {
        String eventId = UUID.randomUUID().toString();
        String postId = "post-retry-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event = new ScheduledPostEvent(
                eventId,
                postId,
                "user-retry",
                "Testing exponential backoff retry recovery",
                LocalDateTime.now(),
                "TRANSIENT_FAILURE"
        );

        producer.publishPostEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "SUBMITTED",
                "scenario", "EXPONENTIAL_BACKOFF_RETRY",
                "eventId", eventId,
                "postId", postId,
                "message", "Transient failure event published. Consumer will fail once, apply backoff, retry, and succeed."
        ));
    }

    /**
     * 3. Fatal Failure Simulation: Fails all retries, routes to DLQ topic.
     */
    @PostMapping("/simulate-dlq")
    public ResponseEntity<Map<String, Object>> simulateDlqRouting() {
        String eventId = UUID.randomUUID().toString();
        String postId = "post-dlq-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event = new ScheduledPostEvent(
                eventId,
                postId,
                "user-dlq",
                "Poison message simulating unrecoverable error",
                LocalDateTime.now(),
                "FATAL_FAILURE"
        );

        producer.publishPostEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "SUBMITTED",
                "scenario", "DLQ_ROUTING",
                "eventId", eventId,
                "postId", postId,
                "message", "Fatal error event published. After retries are exhausted, it will route to DLQ topic."
        ));
    }

    /**
     * 4. Idempotency Simulation: Sends the exact same event twice to verify deduplication.
     */
    @PostMapping("/simulate-duplicate")
    public ResponseEntity<Map<String, Object>> simulateDuplicateProcessing() {
        String commonEventId = "fixed-idempotency-" + UUID.randomUUID().toString().substring(0, 8);
        String postId = "post-duplicate-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event1 = new ScheduledPostEvent(
                commonEventId,
                postId,
                "user-dedup",
                "First copy of scheduled post",
                LocalDateTime.now(),
                "NORMAL"
        );

        ScheduledPostEvent event2 = new ScheduledPostEvent(
                commonEventId,
                postId,
                "user-dedup",
                "Duplicate copy of scheduled post with SAME eventId",
                LocalDateTime.now(),
                "NORMAL"
        );

        // Send both messages with the same eventId
        producer.publishPostEvent(event1);
        producer.publishPostEvent(event2);

        return ResponseEntity.ok(Map.of(
                "status", "SUBMITTED",
                "scenario", "IDEMPOTENT_DEDUPLICATION",
                "sharedEventId", commonEventId,
                "postId", postId,
                "message", "Sent 2 identical events. First will process, second will be detected and skipped."
        ));
    }

    /**
     * Inspection: Processed Events (Idempotency Store)
     */
    @GetMapping("/idempotency-store")
    public ResponseEntity<Map<String, IdempotencyService.ProcessedRecord>> getIdempotencyStore() {
        return ResponseEntity.ok(idempotencyService.getProcessedEvents());
    }

    /**
     * Inspection: DLQ Records (Audit Log)
     */
    @GetMapping("/dlq-records")
    public ResponseEntity<List<DlqAuditService.DlqRecord>> getDlqRecords() {
        return ResponseEntity.ok(dlqAuditService.getDlqRecords());
    }

    /**
     * Inspection: Post Statuses
     */
    @GetMapping("/statuses")
    public ResponseEntity<Map<String, PostStatus>> getStatuses() {
        return ResponseEntity.ok(postProcessingService.getAllPostStatuses());
    }
}
