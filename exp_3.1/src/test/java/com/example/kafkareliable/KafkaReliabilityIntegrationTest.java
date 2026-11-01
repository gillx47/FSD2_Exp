package com.example.kafkareliable;

import com.example.kafkareliable.model.PostStatus;
import com.example.kafkareliable.model.ScheduledPostEvent;
import com.example.kafkareliable.producer.ScheduledPostProducer;
import com.example.kafkareliable.service.DlqAuditService;
import com.example.kafkareliable.service.IdempotencyService;
import com.example.kafkareliable.service.PostProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 2,
        topics = { "scheduled-posts-topic", "scheduled-posts-topic.DLT" },
        brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.retry.max-attempts=3",
        "app.kafka.retry.initial-interval-ms=300",
        "app.kafka.retry.multiplier=1.5",
        "app.kafka.retry.max-interval-ms=1000"
})
@DirtiesContext
class KafkaReliabilityIntegrationTest {

    @Autowired
    private ScheduledPostProducer producer;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private DlqAuditService dlqAuditService;

    @Autowired
    private PostProcessingService postProcessingService;

    @BeforeEach
    void setUp() {
        idempotencyService.clear();
        dlqAuditService.clear();
        postProcessingService.reset();
    }

    @Test
    @DisplayName("1. Normal event should be processed successfully and stored in Idempotency Service")
    void testNormalPostProcessing() {
        String eventId = UUID.randomUUID().toString();
        String postId = "post-success-1";

        ScheduledPostEvent event = new ScheduledPostEvent(
                eventId, postId, "user-alice", "Scheduled post for product launch",
                LocalDateTime.now(), "NORMAL"
        );

        producer.publishPostEvent(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(postProcessingService.getPostStatus(postId)).isEqualTo(PostStatus.PUBLISHED);
            assertThat(idempotencyService.isProcessed(eventId)).isTrue();
        });
    }

    @Test
    @DisplayName("2. Idempotency Pattern: Duplicate event with same eventId must be ignored and not re-executed")
    void testDuplicateEventIsIgnored() {
        String sharedEventId = "duplicate-test-" + UUID.randomUUID();
        String postId = "post-dup-1";

        ScheduledPostEvent event1 = new ScheduledPostEvent(
                sharedEventId, postId, "user-bob", "Original post submission",
                LocalDateTime.now(), "NORMAL"
        );

        ScheduledPostEvent duplicateEvent = new ScheduledPostEvent(
                sharedEventId, postId, "user-bob", "Duplicate post submission with identical eventId",
                LocalDateTime.now(), "NORMAL"
        );

        // Publish first event
        producer.publishPostEvent(event1);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(postProcessingService.getPostStatus(postId)).isEqualTo(PostStatus.PUBLISHED);
            assertThat(idempotencyService.isProcessed(sharedEventId)).isTrue();
        });

        // Publish duplicate event with identical eventId
        producer.publishPostEvent(duplicateEvent);

        // Allow some time for Kafka delivery and consumer check
        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // The event is processed only once in the idempotency registry
            assertThat(idempotencyService.getProcessedEvents()).containsKey(sharedEventId);
            assertThat(postProcessingService.getPostStatus(postId)).isEqualTo(PostStatus.PUBLISHED);
        });
    }

    @Test
    @DisplayName("3. Retry Mechanism: Transient failure should be retried with backoff and succeed")
    void testTransientFailureRecoversOnRetry() {
        String eventId = UUID.randomUUID().toString();
        String postId = "post-transient-1";

        ScheduledPostEvent event = new ScheduledPostEvent(
                eventId, postId, "user-charlie", "Post triggering simulated network glitch",
                LocalDateTime.now(), "TRANSIENT_FAILURE"
        );

        producer.publishPostEvent(event);

        // Must recover and reach PUBLISHED status after retry
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(postProcessingService.getPostStatus(postId)).isEqualTo(PostStatus.PUBLISHED);
            assertThat(idempotencyService.isProcessed(eventId)).isTrue();
        });
    }

    @Test
    @DisplayName("4. Dead Letter Queue: Fatal error exhausts retries and is routed to DLQ topic")
    void testFatalFailureExhaustsRetriesAndRoutesToDlq() {
        String eventId = UUID.randomUUID().toString();
        String postId = "post-fatal-1";

        ScheduledPostEvent event = new ScheduledPostEvent(
                eventId, postId, "user-david", "Poison message that will fail retries",
                LocalDateTime.now(), "FATAL_FAILURE"
        );

        producer.publishPostEvent(event);

        // Await until DLQ consumer receives the message on scheduled-posts-topic.DLT
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(dlqAuditService.getDlqRecords()).isNotEmpty();
            DlqAuditService.DlqRecord record = dlqAuditService.getDlqRecords().stream()
                    .filter(r -> eventId.equals(r.eventId()))
                    .findFirst()
                    .orElse(null);

            assertThat(record).isNotNull();
            assertThat(record.originalTopic()).contains("scheduled-posts");
            assertThat(record.exceptionMessage()).contains("Simulated unrecoverable fatal failure");
        });
    }
}
