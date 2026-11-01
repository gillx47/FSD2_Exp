package com.example.kafkareliable.producer;

import com.example.kafkareliable.model.ScheduledPostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer component for publishing scheduled post events into Apache Kafka.
 */
@Component
public class ScheduledPostProducer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String scheduledPostsTopic;

    public ScheduledPostProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.scheduled-posts:scheduled-posts-topic}") String scheduledPostsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.scheduledPostsTopic = scheduledPostsTopic;
    }

    public CompletableFuture<SendResult<String, Object>> publishPostEvent(ScheduledPostEvent event) {
        String key = event.getPostId() != null ? event.getPostId() : event.getEventId();
        log.info("[PRODUCER] Publishing event to topic '{}': eventId={}, postId={}, simulationMode={}",
                scheduledPostsTopic, event.getEventId(), event.getPostId(), event.getSimulationMode());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(scheduledPostsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[PRODUCER] Successfully published event: eventId={} -> topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[PRODUCER] Failed to publish event: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });

        return future;
    }
}
