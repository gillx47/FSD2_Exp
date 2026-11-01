# Laboratory Experiment: Event-Driven Communication with Apache Kafka
## Reliability Mechanisms: Exponential Backoff Retries, Dead-Letter Queues (DLQ), and Idempotency Patterns

**Course Outcomes Mapped:**
- **CO2 - BT2 (Bloom's Taxonomy Level 2 - Understand):** Understand failure handling and asynchronous message delivery in distributed event-driven systems.
- **CO3 - BT3 (Bloom's Taxonomy Level 3 - Apply):** Apply retry strategies with exponential backoff and route unrecoverable events to Dead-Letter Queues.
- **CO5 - BT5 (Bloom's Taxonomy Level 5 - Evaluate):** Evaluate and enforce message deduplication and system consistency using the Idempotency Pattern.

---

## 1. System Architecture

```
                    +------------------------------------+
                    |       Producer Component           |
                    |   (POST /api/posts/publish...)     |
                    +-----------------+------------------+
                                      |
                                      v
                +---------------------------------------------+
                | Kafka Topic: [scheduled-posts-topic]        |
                +---------------------+-----------------------+
                                      |
                                      v
                       +-----------------------------+
                       |    Idempotency Verifier     |
                       |  (Event Tracking Registry)  |
                       +--------------+--------------+
                                      |
                    +-----------------+-----------------+
                    |                                   |
         [Already Processed]                   [New Event]
                    |                                   |
                    v                                   v
          Duplicate Ignored                  Business Processing
          (Clean Commit)                     (Publish Scheduled Post)
                                                        |
                                       +----------------+----------------+
                                       |                                 |
                                   [Success]                         [Failure]
                                       |                                 |
                                       v                                 v
                             Mark Processed in Store           DefaultErrorHandler
                             Commit Offset                     (Exponential BackOff)
                                                                         |
                                                   +---------------------+---------------------+
                                                   |                                           |
                                           [Retry Attempt <= Max]                     [Max Retries Exhausted]
                                                   |                                           |
                                                   v                                           v
                                        Backoff Delay (1s -> 2s -> 4s)             DeadLetterPublishingRecoverer
                                        Retry Processing                                       |
                                                                                               v
                                                                             +-----------------------------------+
                                                                             | Kafka DLQ Topic:                  |
                                                                             | [scheduled-posts-topic.DLT]       |
                                                                             +-----------------+-----------------+
                                                                                               |
                                                                                               v
                                                                                   +-----------------------+
                                                                                   | DLQ Consumer & Audit  |
                                                                                   | (Alerts & Diagnostics)|
                                                                                   +-----------------------+
```

---

## 2. Key Components Implemented

1. **ScheduledPostEvent (`model/ScheduledPostEvent.java`)**:
   - Encapsulates domain payload: `eventId` (UUID for deduplication), `postId`, `userId`, `content`, `scheduledTime`, and `simulationMode`.

2. **Idempotency Service (`service/IdempotencyService.java`)**:
   - Maintains an atomic tracking store using `ConcurrentHashMap` with atomic `putIfAbsent()`.
   - Before executing downstream operations, the consumer verifies if `eventId` exists.
   - If present: logs warning and cleanly acknowledges the message without double execution.
   - If absent: proceeds with business logic and records `eventId` upon success.

3. **Exponential Backoff & Error Handler (`config/KafkaConfig.java`)**:
   - Configures Spring Kafka's `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries`.
   - Initial interval: `1000ms`, multiplier: `2.0`, max retries: `2` (3 total attempts).
   - Dynamically logs each delivery attempt with backoff progress.

4. **Dead-Letter Queue (DLQ) & Recoverer (`consumer/DeadLetterQueueConsumer.java` & `config/KafkaConfig.java`)**:
   - `DeadLetterPublishingRecoverer` automatically catches exhausted failures and publishes them to `scheduled-posts-topic.DLT`.
   - Adds diagnostic headers: `dlt-original-topic`, `dlt-original-partition`, `dlt-original-offset`, `dlt-exception-message`, `dlt-exception-stacktrace`.
   - `DeadLetterQueueConsumer` listens on the DLT topic, extracts metadata, logs alarms, and archives records in `DlqAuditService`.

5. **REST API Controller (`controller/PostController.java`)**:
   - Provides ready-to-test endpoints to demonstrate normal processing, retry recovery, DLQ routing, and duplicate elimination.

---

## 3. How to Run & Verify

### Option A: Run Automated Integration Tests (Embedded Kafka)
Embedded Kafka runs inside the JVM without requiring any external Kafka or Docker setup:
```powershell
cd C:\Users\harno\.gemini\antigravity\scratch\kafka-reliable-posts
mvn test
```

### Option B: Run via Docker Compose & Spring Boot Application
1. Start Kafka broker:
   ```powershell
   docker-compose up -d
   ```
2. Start the Spring Boot application:
   ```powershell
   mvn spring-boot:run
   ```
3. Test failure and reliability scenarios using cURL or Postman:

#### Scenario 1: Normal Scheduled Post Processing
```bash
curl -X POST http://localhost:8080/api/posts/publish \
  -H "Content-Type: application/json" \
  -d '{"content": "Happy New Year 2027!"}'
```

#### Scenario 2: Exponential Backoff Retry (Transient Failure)
```bash
curl -X POST http://localhost:8080/api/posts/simulate-retry
```
*Console output:*
- Attempt #1 fails with `TransientProcessingException`.
- `DefaultErrorHandler` triggers backoff delay.
- Attempt #2 succeeds, and status transitions to `PUBLISHED`.

#### Scenario 3: Dead-Letter Queue Routing (Fatal Failure)
```bash
curl -X POST http://localhost:8080/api/posts/simulate-dlq
```
*Console output:*
- Attempt #1, #2, #3 fail with `FatalProcessingException`.
- `DeadLetterPublishingRecoverer` routes the message to `scheduled-posts-topic.DLT`.
- `DeadLetterQueueConsumer` captures the failed payload and audit headers.
- Inspect DLQ records:
  ```bash
  curl http://localhost:8080/api/posts/dlq-records
  ```

#### Scenario 4: Idempotent Deduplication (At-Least-Once Delivery Protection)
```bash
curl -X POST http://localhost:8080/api/posts/simulate-duplicate
```
*Console output:*
- Two messages with the identical `eventId` are produced.
- Message 1 is processed and marked in the idempotency registry.
- Message 2 is intercepted by `IdempotencyService`, detected as duplicate, and skipped.
- Check processed store:
  ```bash
  curl http://localhost:8080/api/posts/idempotency-store
  ```
