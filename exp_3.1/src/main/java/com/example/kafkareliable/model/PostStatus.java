package com.example.kafkareliable.model;

public enum PostStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED_RETRYING,
    FAILED_SENT_TO_DLQ,
    DUPLICATE_IGNORED
}
