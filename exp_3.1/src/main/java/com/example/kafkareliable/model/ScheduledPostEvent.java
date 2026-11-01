package com.example.kafkareliable.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

public class ScheduledPostEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String postId;
    private String userId;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledTime;

    /**
     * Simulation modes for educational testing:
     * - NORMAL: Process successfully
     * - TRANSIENT_FAILURE: Fails on first attempt, succeeds on retry
     * - FATAL_FAILURE: Always fails, exhausts retries, routed to DLQ
     */
    private String simulationMode;

    private int attemptCount;

    public ScheduledPostEvent() {
    }

    public ScheduledPostEvent(String eventId, String postId, String userId, String content,
                              LocalDateTime scheduledTime, String simulationMode) {
        this.eventId = eventId;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.scheduledTime = scheduledTime;
        this.simulationMode = simulationMode != null ? simulationMode : "NORMAL";
        this.attemptCount = 0;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getSimulationMode() {
        return simulationMode;
    }

    public void setSimulationMode(String simulationMode) {
        this.simulationMode = simulationMode;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    @Override
    public String toString() {
        return "ScheduledPostEvent{" +
                "eventId='" + eventId + '\'' +
                ", postId='" + postId + '\'' +
                ", userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", simulationMode='" + simulationMode + '\'' +
                ", attemptCount=" + attemptCount +
                '}';
    }
}
