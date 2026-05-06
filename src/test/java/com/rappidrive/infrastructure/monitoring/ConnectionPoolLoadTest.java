package com.rappidrive.infrastructure.monitoring;

import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load test for HikariCP connection pool with virtual threads.
 * Validates that the pool can handle high concurrency without becoming a bottleneck.
 */
@SpringBootTest
@ActiveProfiles("dev")
class ConnectionPoolLoadTest {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPoolLoadTest.class);
    
    @Autowired
    private DriverRepositoryPort driverRepository;
    
    @Autowired
    private ConnectionPoolMonitor poolMonitor;
    
    private static final int CONCURRENT_REQUESTS = 100;
    private static final int ACCEPTABLE_FAILURE_RATE_PERCENT = 5; // Allow 5% failures
    
    @Test
    void shouldHandleConcurrentDatabaseOperations() throws Exception {
        log.info("Starting connection pool load test with {} concurrent requests", CONCURRENT_REQUESTS);
        
        // Use virtual thread executor for maximum concurrency
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        try {
            // Create 100 concurrent database operations
            List<CompletableFuture<Void>> futures = IntStream.range(0, CONCURRENT_REQUESTS)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate real workload - query database
                        performDatabaseOperation(i);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        log.warn("Request {} failed: {}", i, e.getMessage());
                    }
                }, executor))
                .toList();
            
            // Wait for all requests to complete (max 30 seconds)
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allOf.get(30, TimeUnit.SECONDS);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Log results
        log.info("Load test completed in {}ms", duration);
        log.info("Success: {}, Failures: {}, Success Rate: {:.1f}%", 
                successCount.get(), failureCount.get(), 
                (successCount.get() * 100.0 / CONCURRENT_REQUESTS));
        log.info("Pool utilization: {:.1f}%", poolMonitor.getPoolUtilization() * 100);
        log.info("Waiting threads: {}", poolMonitor.getWaitingThreads());
        
        // Assertions
        int failureRate = (failureCount.get() * 100) / CONCURRENT_REQUESTS;
        assertThat(failureRate)
            .as("Failure rate should be below %d%%", ACCEPTABLE_FAILURE_RATE_PERCENT)
            .isLessThanOrEqualTo(ACCEPTABLE_FAILURE_RATE_PERCENT);
        
        assertThat(duration)
            .as("Total duration should be reasonable for %d concurrent requests", CONCURRENT_REQUESTS)
            .isLessThan(30000); // 30 seconds max
        
        // Verify no threads are stuck waiting
        assertThat(poolMonitor.getWaitingThreads())
            .as("No threads should be waiting for connections after test completes")
            .isEqualTo(0);
    }
    
    @Test
    void shouldNotExhaustConnectionPool() {
        // Execute multiple sequential batches to ensure connections are properly released
        for (int batch = 0; batch < 5; batch++) {
            log.info("Executing batch {}", batch + 1);
            
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, 20)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    performDatabaseOperation(i);
                }, executor))
                .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            executor.shutdown();
            
            // Verify pool returns to low utilization between batches
            double utilization = poolMonitor.getPoolUtilization();
            log.info("Pool utilization after batch {}: {:.1f}%", batch + 1, utilization * 100);
        }
        
        // After all batches, pool should not be exhausted
        assertThat(poolMonitor.getPoolUtilization())
            .as("Pool should release connections between batches")
            .isLessThan(0.5); // Less than 50% utilization
    }
    
    private void performDatabaseOperation(int requestId) {
        try {
            // Simulate realistic database query (read-only to avoid FK violations)
            TenantId tenantId = new TenantId(UUID.randomUUID());
            
            // Query for drivers by tenant (triggers connection acquisition)
            // This is a realistic read operation that doesn't require pre-existing data
            driverRepository.findByTenantId(tenantId);
            
            // Small delay to simulate processing time
            Thread.sleep(50);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
    }
    
    private Driver createTestDriver(TenantId tenantId, int id) {
        // Use a pool of valid CPFs to avoid validation errors
        String[] validCpfs = {
            "111.444.777-35", "123.456.789-09", "987.654.321-00",
            "234.567.890-12", "345.678.901-23", "456.789.012-34",
            "567.890.123-45", "678.901.234-56", "789.012.345-67",
            "890.123.456-78"
        };
        
        return new Driver(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            tenantId,
            "Load Test Driver " + id,
            new Email("driver" + id + "@loadtest.com"),
            new CPF(validCpfs[id % validCpfs.length]),
            new Phone("+55119999" + String.format("%04d", id)),
            new DriverLicense(
                "CNH" + String.format("%011d", id),
                "B",
                LocalDate.now().minusYears(2),
                LocalDate.now().plusYears(5),
                true
            )
        );
    }
}
