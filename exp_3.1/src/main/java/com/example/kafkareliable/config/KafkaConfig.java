package com.example.kafkareliable.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${app.kafka.topics.scheduled-posts:scheduled-posts-topic}")
    private String scheduledPostsTopic;

    @Value("${app.kafka.topics.dlq:scheduled-posts-topic.DLT}")
    private String dlqTopic;

    @Value("${app.kafka.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.kafka.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.kafka.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    /**
     * Topic declarations for automatic creation by KafkaAdmin
     */
    @Bean
    public NewTopic scheduledPostsTopic() {
        return TopicBuilder.name(scheduledPostsTopic)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(2)
                .replicas(1)
                .build();
    }

    /**
     * DeadLetterPublishingRecoverer routes exhausted records to the DLQ topic.
     * By default, it publishes to <original-topic>.DLT, or we explicitly route to dlqTopic.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                (KafkaOperations<Object, Object>) kafkaTemplate,
                (record, exception) -> {
                    log.error("[DLQ-ROUTER] Routing failed record [key={}, offset={}] to DLQ topic: {}. Cause: {}",
                            record.key(), record.offset(), dlqTopic, exception.getMessage());
                    return new TopicPartition(dlqTopic, record.partition());
                }
        );
    }

    /**
     * DefaultErrorHandler configured with Exponential Backoff and DLQ routing.
     * Max retries = maxAttempts - 1 (since 1 initial attempt + retries = maxAttempts)
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // maxRetries count in ExponentialBackOffWithMaxRetries
        int retries = Math.max(1, maxAttempts - 1);
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retries);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxIntervalMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Add retry listener to log each retry attempt with backoff progress
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("[RETRY-MECHANISM] Retry attempt #{} for record [key={}, partition={}, offset={}]. Exception: {}",
                    deliveryAttempt, record.key(), record.partition(), record.offset(), ex.getMessage());
        });

        return errorHandler;
    }

    /**
     * Container Factory registering the DefaultErrorHandler
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler defaultErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }
}
