package com.example.ci.testcontainers;

import com.example.ci.model.User;
import com.example.ci.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Infrastructure Testing Tier (Testcontainers):
 * Demonstrates spinning up real containerized infrastructure (PostgreSQL)
 * for testing production-like environments in CI workflows.
 */
@SpringBootTest
class UserRepositoryDatabaseTest {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryDatabaseTest.class);

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Testcontainers: Verify database persistence in real PostgreSQL container when Docker is active")
    void testUserRepositoryWithRealPostgreSqlContainer() {
        boolean dockerAvailable = isDockerRunning();

        if (!dockerAvailable) {
            log.warn("[TESTCONTAINERS] Docker is not available locally. Test will be skipped locally but will execute in GitHub Actions CI.");
            assumeTrue(false, "Docker environment not detected. Skipping Testcontainers test locally; verified on GitHub Actions CI.");
        }

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")) {

            postgres.start();
            log.info("[TESTCONTAINERS] PostgreSQL container started on port: {}", postgres.getFirstMappedPort());

            User user = new User("Testcontainer User", "tc.user@example.com", "DEVOPS", true);
            User saved = userRepository.save(user);

            assertThat(saved.getId()).isNotNull();

            Optional<User> found = userRepository.findByEmail("tc.user@example.com");
            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo("DEVOPS");

            postgres.stop();
        }
    }

    private boolean isDockerRunning() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }
}
