package com.matjazt.netmon2.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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

    /**
     * Configure cache manager with Caffeine.
     *
     * <p>Cache policies:
     *
     * <ul>
     *   <li>Maximum size: 10,000 entries per cache
     *   <li>Expiration: 10 minutes after write
     * </ul>
     *
     * @return configured CacheManager
     */
    @Bean
    public CacheManager networkConfigurationCacheManager(
            @Value("${netmon.cache.network-config.maximum-size:10000}") long maximumSize,
            @Value("${netmon.cache.network-config.expire-after-write:10m}")
                    Duration expireAfterWrite) {
        logger.debug(
                "Configuring networkConfigurationCacheManager with maxSize={} and"
                        + " expireAfterWrite={}",
                maximumSize,
                expireAfterWrite);
        CaffeineCacheManager manager = new CaffeineCacheManager("networkConfigById");
        manager.setCaffeine(
                Caffeine.newBuilder().maximumSize(maximumSize).expireAfterWrite(expireAfterWrite));
        return manager;
    }
}
