# Lab Report: Experiment 3.2.1
## Continuous Integration (CI) Pipeline with Multi-Tier Testing (Unit, Integration, Contract & Testcontainers)

**Course Outcome Mapped:**
- **CO6 - BT6 (Bloom's Taxonomy Level 6 - Create):** Design, configure, and automate a comprehensive Continuous Integration pipeline incorporating unit testing, API integration testing, consumer-driven contract testing, and containerized infrastructure verification.

---

## 1. Aim & Objectives

- **Aim:** To design and implement a Continuous Integration pipeline that automates building and testing of applications across multiple testing tiers.
- **Objectives:**
  1. Understand core Continuous Integration (CI) and DevOps concepts.
  2. Implement unit testing using JUnit 5 and Mockito.
  3. Implement REST API integration testing using Spring MockMvc.
  4. Perform Consumer-Driven Contract (CDC) testing using Pact.
  5. Integrate containerized infrastructure testing with Testcontainers.
  6. Automate test execution and artifact archiving with GitHub Actions.

---

## 2. Testing Pyramid & System Architecture

```
                      / \
                     /   \
                    / Pact\        <-- Consumer-Driven Contract Testing
                   /Contract\          (Pact Consumer & Provider Verification)
                  /-----------\
                 / Integration \   <-- REST API & Database Integration Testing
                /   (MockMvc)   \      (Spring Boot Test & Testcontainers)
               /-----------------\
              /    Unit Tests     \ <-- Method & Business Logic Isolation
             / (JUnit 5 + Mockito) \    (Fast, In-Memory, Mocked Dependencies)
            /-----------------------\
```

### CI Pipeline Workflow

```
   Git Push / Pull Request
             |
             v
+-----------------------------+
|    GitHub Actions Runner    |
|       (ubuntu-latest)       |
+--------------+--------------+
               |
               v
  1. Setup JDK 17 & Maven Cache
               |
               v
  2. Tier 1: Unit Tests (JUnit 5 + Mockito)
               |
               v
  3. Tier 2: Integration Tests (MockMvc)
               |
               v
  4. Tier 3: Contract Tests (Pact Consumer -> target/pacts -> Pact Provider)
               |
               v
  5. Tier 4: Real Container Test (Testcontainers PostgreSQL)
               |
               v
  6. Artifact Archiving (Surefire Reports & Pact Contracts)
               |
               v
        Pipeline Success
```

---

## 3. Implementation Details

### Tier 1: Unit Testing (JUnit 5 + Mockito)
- **File:** `src/test/java/com/example/ci/unit/UserServiceUnitTest.java`
- **Scope:** Tests business methods of `UserService` in isolation.
- **Techniques:**
  - `@ExtendWith(MockitoExtension.class)`
  - `@Mock private UserRepository userRepository`
  - `@InjectMocks private UserService userService`
  - Verifies exception handling (`DuplicateEmailException`, `UserNotFoundException`) and repository interaction counts.

### Tier 2: API Integration Testing (MockMvc)
- **File:** `src/test/java/com/example/ci/integration/UserControllerIntegrationTest.java`
- **Scope:** Tests HTTP endpoint routing, JSON serialization/deserialization, HTTP status codes, and input validation.
- **Techniques:**
  - `@SpringBootTest`, `@AutoConfigureMockMvc`
  - `mockMvc.perform(post("/api/users")...).andExpect(status().isCreated())`
  - Validates `@Valid` bean validation failure returns `400 Bad Request`.

### Tier 3: Consumer-Driven Contract Testing (Pact)
- **Consumer Test:** `src/test/java/com/example/ci/contract/UserConsumerPactTest.java`
  - Uses `@ExtendWith(PactConsumerTestExt.class)`
  - Generates `target/pacts/FrontendConsumer-UserService.json` contract defining expected interactions on `GET /api/users/1`.
- **Provider Test:** `src/test/java/com/example/ci/contract/UserProviderPactVerificationTest.java`
  - Uses `@Provider("UserService")` and `@PactFolder("target/pacts")`
  - Verifies against a live Spring Boot `@SpringBootTest(webEnvironment = RANDOM_PORT)` server.
  - Sets up state with `@State("user 1 exists")`.

### Tier 4: Containerized Testing (Testcontainers)
- **File:** `src/test/java/com/example/ci/testcontainers/UserRepositoryDatabaseTest.java`
- **Scope:** Spins up a real `postgres:15-alpine` container on-the-fly during CI execution to validate database transactions against production-identical infrastructure.

### CI Automation (GitHub Actions)
- **File:** `.github/workflows/ci.yml`
- Automates checkout, JDK setup, Maven caching, test execution, and contract artifact publishing on every commit.

---

## 4. Verification & Results

All 4 testing tiers execute systematically as part of the automated build lifecycle.

| Tier | Component Tested | Technology | Verification Outcome |
|---|---|---|---|
| **Tier 1: Unit** | `UserService` business logic & exceptions | JUnit 5, Mockito | **PASSED** |
| **Tier 2: Integration** | `UserController` REST APIs & validation | MockMvc, SpringBootTest | **PASSED** |
| **Tier 3: Contract** | Consumer expectation & Provider adherence | Pact JVM JUnit 5 | **PASSED** |
| **Tier 4: Container** | Real PostgreSQL database persistence | Testcontainers | **PASSED** |

---

## 5. Conclusion
Experiment 3.2.1 successfully demonstrates an automated, multi-tiered Continuous Integration pipeline satisfying **CO6 - BT6**. Code changes are automatically checked for unit correctness, API fidelity, cross-service contract validity, and production-like database behavior, ensuring early defect detection and continuous delivery readiness.
