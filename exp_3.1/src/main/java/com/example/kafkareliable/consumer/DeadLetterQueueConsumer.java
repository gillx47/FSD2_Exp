package com.example.kafkareliable.consumer;

import com.example.kafkareliable.model.ScheduledPostEvent;
import com.example.kafkareliable.service.DlqAuditService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Dead Letter Queue (DLQ) Consumer.
 * Listens on the dead letter topic and handles poisoned / persistently failed messages.
 */
@Component
public class DeadLetterQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    private final DlqAuditService dlqAuditService;

    public DeadLetterQueueConsumer(DlqAuditService dlqAuditService) {
        this.dlqAuditService = dlqAuditService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.dlq:scheduled-posts-topic.DLT}",
            groupId = "${spring.kafka.consumer.group-id:scheduled-posts-group}-dlq"
    )
    public void consumeDlqMessage(ConsumerRecord<String, ScheduledPostEvent> record) {
        ScheduledPostEvent event = record.value();

        String originalTopic = getHeaderAsString(record, KafkaHeaders.DLT_ORIGINAL_TOPIC, "unknown");
        String exceptionMessage = getHeaderAsString(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE, "unknown");
        int originalPartition = getHeaderAsInt(record, KafkaHeaders.DLT_ORIGINAL_PARTITION, record.partition());
        long originalOffset = getHeaderAsLong(record, KafkaHeaders.DLT_ORIGINAL_OFFSET, record.offset());

        log.error("[DLQ-CONSUMER] ALARM: Poisoned / Failed message received on DLQ! " +
                        "eventId={}, postId={}, originalTopic={}, partition={}, offset={}, cause='{}'",
                event.getEventId(), event.getPostId(), originalTopic, originalPartition, originalOffset, exceptionMessage);

        dlqAuditService.recordFailure(
                event.getEventId(),
                originalTopic,
                originalPartition,
                originalOffset,
                exceptionMessage,
                event
        );
    }

    private String getHeaderAsString(ConsumerRecord<?, ?> record, String headerKey, String defaultValue) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return defaultValue;
    }

    private int getHeaderAsInt(ConsumerRecord<?, ?> record, String headerKey, int defaultValue) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            byte[] val = header.value();
            if (val.length >= 4) {
                return ByteBuffer.wrap(val).getInt();
            }
            try {
                return Integer.parseInt(new String(val, StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private long getHeaderAsLong(ConsumerRecord<?, ?> record, String headerKey, long defaultValue) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            byte[] val = header.value();
            if (val.length >= 8) {
                return ByteBuffer.wrap(val).getLong();
            }
            try {
                return Long.parseLong(new String(val, StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }
}
