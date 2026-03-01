package com.matjazt.netmon2.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * Spring Cache configuration using Caffeine as the cache provider.
 *
 * <p>Configures in-memory caching for frequently accessed data with automatic expiration and
 * eviction policies.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Value("${netmon.cache.network-config.maximum-size:100}")
    private long networkMaxSize;

    @Value("${netmon.cache.network-config.expire-after-write:30s}")
    private Duration networkExpireAfterWrite;

    @Value("${netmon.cache.security.maximum-size:100}")
    private long securityMaxSize;

    @Value("${netmon.cache.security.expire-after-write:30s}")
    private Duration securityExpireAfterWrite;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(
                Arrays.asList(
                        buildCache("networkConfigCache", networkMaxSize, networkExpireAfterWrite),
                        buildCache("userDetailsCache", securityMaxSize, securityExpireAfterWrite),
                        buildCache(
                                "networkAccessCache", securityMaxSize, securityExpireAfterWrite)));
        return manager;
    }

    private CaffeineCache buildCache(String name, long maxSize, Duration ttl) {
        logger.debug("Creating cache '{}' with maxSize={} and TTL={}", name, maxSize, ttl);
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttl)
                        .recordStats()
                        .build());
    }
}
