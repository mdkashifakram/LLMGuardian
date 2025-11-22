package io.com.llmguardian.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * L1 Cache Service - In-memory caching with Caffeine.
 * 
 * Characteristics:
 * - Ultra-fast (< 1ms access)
 * - Limited size (1000 entries default)
 * - Process-local (not shared across instances)
 * - Eviction: LRU (Least Recently Used)
 * - TTL: 60 minutes default
 * 
 * Use Case:
 * - Hot data (frequently accessed)
 * - First line of defense before L2
 * - Minimize Redis calls
 * 
 * @author LLMGuardian Team
 */
@Service
public class L1CacheService {
    
    private static final Logger log = LoggerFactory.getLogger(L1CacheService.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private CaffeineCache caffeineCache;
    
    @Autowired
    private CacheConfig config;
    
    private final CacheStats stats = CacheStats.forL1();
    
    // ========================================
    // PUBLIC API - GET
    // ========================================
    
    /**
     * Get value from L1 cache.
     * 
     * @param key Cache key
     * @return Optional containing value if found
     */
    public Optional<String> get(String key) {
        try {
            String value = caffeineCache.get(key, String.class);
            
            if (value != null) {
                stats.recordHit();
                log.trace("L1 cache HIT: {}", key);
                return Optional.of(value);
            } else {
                stats.recordMiss();
                log.trace("L1 cache MISS: {}", key);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error reading from L1 cache: {}", key, e);
            stats.recordMiss();
            return Optional.empty();
        }
    }
    
    // ========================================
    // PUBLIC API - PUT
    // ========================================
    
    /**
     * Put value into L1 cache.
     * 
     * @param key Cache key
     * @param value Value to cache
     */
    public void put(String key, String value) {
        try {
            caffeineCache.put(key, value);
            log.trace("L1 cache PUT: {}", key);
            
            // Update size
            updateSize();
            
        } catch (Exception e) {
            log.error("Error writing to L1 cache: {}", key, e);
        }
    }
    
    // ========================================
    // PUBLIC API - DELETE
    // ========================================
    
    /**
     * Remove entry from L1 cache.
     * 
     * @param key Cache key
     */
    public void evict(String key) {
        try {
            caffeineCache.evict(key);
            stats.recordEviction();
            log.trace("L1 cache EVICT: {}", key);
            
            updateSize();
            
        } catch (Exception e) {
            log.error("Error evicting from L1 cache: {}", key, e);
        }
    }
    
    /**
     * Clear entire L1 cache.
     */
    public void clear() {
        try {
            caffeineCache.clear();
            log.info("L1 cache cleared");
            
            stats.setCurrentSize(0);
            
        } catch (Exception e) {
            log.error("Error clearing L1 cache", e);
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
     * Get Caffeine native stats (if enabled).
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getCaffeineStats() {
        return caffeineCache.getNativeCache().stats();
    }
    
    /**
     * Get current cache size.
     */
    public long getSize() {
        return caffeineCache.getNativeCache().estimatedSize();
    }
    
    /**
     * Get cache hit rate from Caffeine stats.
     */
    public double getHitRate() {
        if (config.getL1().isRecordStats()) {
            return getCaffeineStats().hitRate() * 100.0;
        } else {
            return stats.getHitRate();
        }
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Update size in stats.
     */
    private void updateSize() {
        long size = getSize();
        stats.setCurrentSize(size);
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    /**
     * Check if key exists in cache.
     */
    public boolean contains(String key) {
        return get(key).isPresent();
    }
    
    /**
     * Get cache summary.
     */
    public String getSummary() {
        updateSize();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== L1 CACHE (Caffeine) ===\n");
        sb.append(String.format("Size: %d / %d\n", getSize(), config.getL1().getMaxSize()));
        sb.append(String.format("Hit Rate: %.2f%%\n", getHitRate()));
        sb.append(String.format("TTL: %d minutes\n", config.getL1().getTtlMinutes()));
        
        if (config.getL1().isRecordStats()) {
            com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = getCaffeineStats();
            sb.append(String.format("Hits: %,d\n", caffeineStats.hitCount()));
            sb.append(String.format("Misses: %,d\n", caffeineStats.missCount()));
            sb.append(String.format("Evictions: %,d\n", caffeineStats.evictionCount()));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format(
            "L1CacheService{size=%d/%d, hitRate=%.2f%%}",
            getSize(),
            config.getL1().getMaxSize(),
            getHitRate()
        );
    }
}
