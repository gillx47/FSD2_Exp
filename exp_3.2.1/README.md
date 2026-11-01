# Experiment 3.2.1: Continuous Integration Pipeline with Multi-Tier Testing

This project demonstrates a production-grade Continuous Integration (CI) pipeline implementing the testing pyramid:
1. **Unit Testing:** JUnit 5 & Mockito
2. **API Integration Testing:** Spring MockMvc
3. **Consumer-Driven Contract Testing:** Pact (Consumer generation & Provider verification)
4. **Containerized Infrastructure Testing:** Testcontainers (PostgreSQL)
5. **CI Automation:** GitHub Actions (`.github/workflows/ci.yml`)

---

## Project Structure
```
exp_3.2.1/
├── pom.xml
├── ci-workflow.yml
├── README.md
├── LAB_REPORT.md
├── src/main/java/com/example/ci/
│   ├── CiTestingApplication.java
│   ├── controller/UserController.java
│   ├── dto/UserDto.java
│   ├── model/User.java
│   ├── repository/UserRepository.java
│   └── service/UserService.java
└── src/test/java/com/example/ci/
    ├── unit/UserServiceUnitTest.java
    ├── integration/UserControllerIntegrationTest.java
    ├── contract/
    │   ├── UserConsumerPactTest.java
    │   └── UserProviderPactVerificationTest.java
    └── testcontainers/UserRepositoryDatabaseTest.java
```

---

## Running Tests Locally

Run the complete test suite:
```powershell
mvn clean test
```

Inspect generated Pact contract files:
```powershell
cat target/pacts/FrontendConsumer-UserService.json
```

---

## GitHub Actions CI Workflow

The CI workflow is configured in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).
Whenever code is pushed to GitHub:
1. The pipeline checks out the repository.
2. Sets up JDK 17 with Maven caching.
3. Automatically runs all test tiers.
4. Archives the generated Pact contracts and test reports as pipeline artifacts.
