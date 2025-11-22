package io.com.llmguardian.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * L2 Cache Service - Distributed caching with Redis.
 * 
 * Characteristics:
 * - Distributed (shared across instances)
 * - Persistent (survives restarts)
 * - Larger capacity (no strict size limit)
 * - Network latency (5-10ms access)
 * - TTL: 24 hours default
 * 
 * Use Case:
 * - Warm data (less frequent than L1)
 * - Cross-instance cache sharing
 * - Reduce LLM API calls
 * 
 * @author LLMGuardian Team
 */
@Service
public class L2CacheService {
    
    private static final Logger log = LoggerFactory.getLogger(L2CacheService.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    @Qualifier("cacheRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private CacheConfig config;
    
    private final CacheStats stats = CacheStats.forL2();
    
    // ========================================
    // PUBLIC API - GET
    // ========================================
    
    /**
     * Get value from L2 cache (Redis).
     * 
     * @param key Cache key
     * @return Optional containing value if found
     */
    public Optional<String> get(String key) {
        // Check if L2 is enabled
        if (!config.isL2Enabled()) {
            log.trace("L2 cache disabled, skipping");
            return Optional.empty();
        }
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                stats.recordHit();
                log.trace("L2 cache HIT: {}", key);
                return Optional.of(value);
            } else {
                stats.recordMiss();
                log.trace("L2 cache MISS: {}", key);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error reading from L2 cache: {}", key, e);
            stats.recordMiss();
            return Optional.empty();
        }
    }
    
    // ========================================
    // PUBLIC API - PUT
    // ========================================
    
    /**
     * Put value into L2 cache with default TTL.
     * 
     * @param key Cache key
     * @param value Value to cache
     */
    public void put(String key, String value) {
        put(key, value, config.getL2TtlSeconds());
    }
    
    /**
     * Put value into L2 cache with custom TTL.
     * 
     * @param key Cache key
     * @param value Value to cache
     * @param ttlSeconds Time-to-live in seconds
     */
    public void put(String key, String value, long ttlSeconds) {
        // Check if L2 is enabled
        if (!config.isL2Enabled()) {
            log.trace("L2 cache disabled, skipping PUT");
            return;
        }
        
        try {
            redisTemplate.opsForValue().set(
                key, 
                value, 
                Duration.ofSeconds(ttlSeconds)
            );
            
            log.trace("L2 cache PUT: {} (TTL: {}s)", key, ttlSeconds);
            
        } catch (Exception e) {
            log.error("Error writing to L2 cache: {}", key, e);
        }
    }
    
    // ========================================
    // PUBLIC API - DELETE
    // ========================================
    
    /**
     * Remove entry from L2 cache.
     * 
     * @param key Cache key
     */
    public void evict(String key) {
        if (!config.isL2Enabled()) {
            return;
        }
        
        try {
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                stats.recordEviction();
                log.trace("L2 cache EVICT: {}", key);
            }
            
        } catch (Exception e) {
            log.error("Error evicting from L2 cache: {}", key, e);
        }
    }
    
    /**
     * Clear all entries matching our key prefix.
     * 
     * CAUTION: This clears ALL cache entries for this application.
     */
    public void clear() {
        if (!config.isL2Enabled()) {
            return;
        }
        
        try {
            String pattern = config.getL2().getKeyPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("L2 cache cleared: {} keys deleted", deleted);
            }
            
        } catch (Exception e) {
            log.error("Error clearing L2 cache", e);
        }
    }
    
    // ========================================
    // PUBLIC API - STATS
    // ========================================
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        updateSize();
        return stats;
    }
    
    /**
     * Get approximate cache size.
     * 
     * Note: This is expensive for Redis (requires SCAN).
     * Use sparingly.
     */
    public long getSize() {
        if (!config.isL2Enabled()) {
            return 0;
        }
        
        try {
            String pattern = config.getL2().getKeyPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
            
        } catch (Exception e) {
            log.error("Error getting L2 cache size", e);
            return 0;
        }
    }
    
    /**
     * Get TTL for a specific key.
     * 
     * @param key Cache key
     * @return TTL in seconds, or -1 if key doesn't exist
     */
    public long getTtl(String key) {
        if (!config.isL2Enabled()) {
            return -1;
        }
        
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : -1;
            
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return -1;
        }
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Update size in stats (expensive operation).
     */
    private void updateSize() {
        // Skip frequent updates to avoid performance hit
        long now = System.currentTimeMillis();
        long lastUpdate = stats.getStartTime().toEpochMilli();
        
        // Only update every 60 seconds
        if (now - lastUpdate > 60000) {
            long size = getSize();
            stats.setCurrentSize(size);
        }
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    /**
     * Check if key exists in cache.
     */
    public boolean contains(String key) {
        if (!config.isL2Enabled()) {
            return false;
        }
        
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking key existence: {}", key, e);
            return false;
        }
    }
    
    /**
     * Extend TTL for an existing key.
     * 
     * @param key Cache key
     * @param additionalSeconds Additional seconds to add
     */
    public void extendTtl(String key, long additionalSeconds) {
        if (!config.isL2Enabled()) {
            return;
        }
        
        try {
            redisTemplate.expire(key, Duration.ofSeconds(additionalSeconds));
            log.trace("Extended TTL for key: {} by {}s", key, additionalSeconds);
            
        } catch (Exception e) {
            log.error("Error extending TTL for key: {}", key, e);
        }
    }
    
    /**
     * Get all keys matching pattern.
     * 
     * CAUTION: Expensive operation, use sparingly.
     */
    public Set<String> getKeys(String pattern) {
        if (!config.isL2Enabled()) {
            return Set.of();
        }
        
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Error getting keys with pattern: {}", pattern, e);
            return Set.of();
        }
    }
    
    /**
     * Get cache summary.
     */
    public String getSummary() {
        if (!config.isL2Enabled()) {
            return "=== L2 CACHE (Redis) ===\nStatus: DISABLED\n";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== L2 CACHE (Redis) ===\n");
        sb.append(String.format("Status: ENABLED\n"));
        sb.append(String.format("Hit Rate: %.2f%%\n", stats.getHitRate()));
        sb.append(String.format("TTL: %d minutes\n", config.getL2().getTtlMinutes()));
        sb.append(String.format("Hits: %,d\n", stats.getHits()));
        sb.append(String.format("Misses: %,d\n", stats.getMisses()));
        sb.append(String.format("Evictions: %,d\n", stats.getEvictions()));
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format(
            "L2CacheService{enabled=%s, hitRate=%.2f%%}",
            config.isL2Enabled(),
            stats.getHitRate()
        );
    }
}
