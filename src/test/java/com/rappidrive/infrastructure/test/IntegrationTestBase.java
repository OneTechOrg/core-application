package com.rappidrive.infrastructure.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests.
 * Manually uses a local PostgreSQL container to avoid Testcontainers discovery issues on some macOS setups.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Points to the container manually started via Docker
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/rappidrive_test");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

}
