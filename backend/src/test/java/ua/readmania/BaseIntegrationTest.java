package ua.readmania;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for Spring Boot integration tests (*IT or @SpringBootTest).
 *
 * Configures:
 * - Singleton PostgreSQL 17 Testcontainer via @ServiceConnection.
 * - Active 'test' profile for environment isolation.
 * - @Transactional for test data rollback isolation.
 * - Real Flyway migrations and Hibernate ddl-auto: validate.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {
}
