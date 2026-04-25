package com.rappidrive.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String FARE_CONFIG_CACHE = "fareConfigByTenant";
    public static final String TENANT_EXISTS_CACHE = "tenantExists";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            FARE_CONFIG_CACHE,
            TENANT_EXISTS_CACHE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(500));
        return cacheManager;
    }
}
