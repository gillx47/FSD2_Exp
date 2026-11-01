package com.example.kafkareliable.service;

import com.example.kafkareliable.model.ScheduledPostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Audit repository for recording and inspecting Dead Letter Queue (DLQ) messages.
 * Allows administrators and operations teams to analyze failed messages.
 */
@Service
public class DlqAuditService {

    private static final Logger log = LoggerFactory.getLogger(DlqAuditService.class);

    public record DlqRecord(
            String eventId,
            String originalTopic,
            int originalPartition,
            long originalOffset,
            String exceptionMessage,
            LocalDateTime timestamp,
            ScheduledPostEvent payload
    ) {}

    private final List<DlqRecord> dlqRecords = Collections.synchronizedList(new ArrayList<>());

    public void recordFailure(String eventId, String originalTopic, int originalPartition,
                              long originalOffset, String exceptionMessage, ScheduledPostEvent payload) {
        DlqRecord record = new DlqRecord(
                eventId,
                originalTopic,
                originalPartition,
                originalOffset,
                exceptionMessage,
                LocalDateTime.now(),
                payload
        );
        dlqRecords.add(record);
        log.error("[DLQ-AUDIT] Recorded failed message in DLQ audit log: eventId={}, exception={}",
                eventId, exceptionMessage);
    }

    public List<DlqRecord> getDlqRecords() {
        return new ArrayList<>(dlqRecords);
    }

    public void clear() {
        dlqRecords.clear();
    }
}
