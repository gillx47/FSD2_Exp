package com.example.kafkareliable.consumer;

import com.example.kafkareliable.model.ScheduledPostEvent;
import com.example.kafkareliable.service.IdempotencyService;
import com.example.kafkareliable.service.PostProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer component that processes scheduled posts.
 * Implements:
 * 1. Idempotency verification using event tracking store
 * 2. Business processing of posts
 * 3. Exception propagation to DefaultErrorHandler for exponential backoff retries
 */
@Component
public class ScheduledPostConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostConsumer.class);

    private final IdempotencyService idempotencyService;
    private final PostProcessingService postProcessingService;

    public ScheduledPostConsumer(IdempotencyService idempotencyService,
                                 PostProcessingService postProcessingService) {
        this.idempotencyService = idempotencyService;
        this.postProcessingService = postProcessingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.scheduled-posts:scheduled-posts-topic}",
            groupId = "${spring.kafka.consumer.group-id:scheduled-posts-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeScheduledPost(
            @Payload ScheduledPostEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[CONSUMER] Received event from topic '{}' [partition={}, offset={}]: eventId={}, postId={}",
                topic, partition, offset, event.getEventId(), event.getPostId());

        // 1. Idempotency Check: Verify if event has already been processed
        if (idempotencyService.isProcessed(event.getEventId())) {
            log.warn("[CONSUMER-IDEMPOTENCY] DUPLICATE DETECTED! eventId={} was already processed. Discarding duplicate to ensure at-most-once execution.",
                    event.getEventId());
            return;
        }

        // 2. Business Execution: Process the post (may throw transient or fatal error)
        postProcessingService.processScheduledPost(event);

        // 3. Mark as Processed in Idempotency Store after successful execution
        idempotencyService.markAsProcessed(event.getEventId(), event.getPostId());

        log.info("[CONSUMER] Successfully completed processing and committed eventId={}", event.getEventId());
    }
}
