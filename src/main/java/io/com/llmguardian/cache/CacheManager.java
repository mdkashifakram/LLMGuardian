package io.com.llmguardian.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Cache Manager - Orchestrates L1 (Caffeine) and L2 (Redis) caches.
 * 
 * Caching Strategy:
 * 
 * READ (get):
 * 1. Check L1 (fast) → HIT: return immediately
 * 2. Check L2 (slower) → HIT: store in L1 for next time, return
 * 3. MISS: return empty
 * 
 * WRITE (put):
 * 1. Store in L1 (fast local access)
 * 2. Store in L2 (cross-instance sharing)
 * 
 * Benefits:
 * - Ultra-fast reads (L1 hit: <1ms)
 * - Cross-instance sharing (L2)
 * - Automatic promotion to L1 on L2 hit
 * - Graceful degradation if Redis is down
 * 
 * @author LLMGuardian Team
 */
@Service
public class CacheManager {
    
    private static final Logger log = LoggerFactory.getLogger(CacheManager.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private L1CacheService l1Cache;
    
    @Autowired
    private L2CacheService l2Cache;
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private CacheConfig config;
    
    // ========================================
    // PUBLIC API - GET
    // ========================================
    
    /**
     * Get value from cache (checks L1 → L2).
     * 
     * @param key Cache key
     * @return Optional containing cached value
     */
    public Optional<String> get(String key) {
        long startTime = System.currentTimeMillis();
        
        // Step 1: Check L1 (fast)
        Optional<String> l1Result = l1Cache.get(key);
        if (l1Result.isPresent()) {
            long latency = System.currentTimeMillis() - startTime;
            log.debug("Cache HIT (L1): {} ({}ms)", key, latency);
            return l1Result;
        }
        
        // Step 2: Check L2 (slower)
        Optional<String> l2Result = l2Cache.get(key);
        if (l2Result.isPresent()) {
            // Promote to L1 for faster future access
            l1Cache.put(key, l2Result.get());
            
            long latency = System.currentTimeMillis() - startTime;
            log.debug("Cache HIT (L2→L1): {} ({}ms)", key, latency);
            return l2Result;
        }
        
        // Step 3: Complete miss
        long latency = System.currentTimeMillis() - startTime;
        log.debug("Cache MISS: {} ({}ms)", key, latency);
        return Optional.empty();
    }
    
    /**
     * Get value from cache using prompt and model.
     * 
     * Convenience method that generates key automatically.
     * 
     * @param prompt The prompt text
     * @param modelId The model identifier
     * @return Optional containing cached value
     */
    public Optional<String> get(String prompt, String modelId) {
        String key = keyGenerator.generateKey(prompt, modelId);
        return get(key);
    }
    
    // ========================================
    // PUBLIC API - PUT
    // ========================================
    
    /**
     * Put value into cache (both L1 and L2).
     * 
     * @param key Cache key
     * @param value Value to cache
     */
    public void put(String key, String value) {
        try {
            // Store in both caches
            l1Cache.put(key, value);
            l2Cache.put(key, value);
            
            log.debug("Cache PUT (L1+L2): {}", key);
            
        } catch (Exception e) {
            log.error("Error putting into cache: {}", key, e);
        }
    }
    
    /**
     * Put value into cache using prompt and model.
     * 
     * Convenience method that generates key automatically.
     * 
     * @param prompt The prompt text
     * @param modelId The model identifier
     * @param value Value to cache
     */
    public void put(String prompt, String modelId, String value) {
        String key = keyGenerator.generateKey(prompt, modelId);
        put(key, value);
    }
    
    /**
     * Put value with custom L2 TTL.
     * 
     * @param key Cache key
     * @param value Value to cache
     * @param l2TtlSeconds Custom TTL for L2 (L1 uses default)
     */
    public void putWithTtl(String key, String value, long l2TtlSeconds) {
        try {
            l1Cache.put(key, value);
            l2Cache.put(key, value, l2TtlSeconds);
            
            log.debug("Cache PUT (L1+L2): {} (L2 TTL: {}s)", key, l2TtlSeconds);
            
        } catch (Exception e) {
            log.error("Error putting into cache with TTL: {}", key, e);
        }
    }
    
    // ========================================
    // PUBLIC API - DELETE
    // ========================================
    
    /**
     * Evict entry from both caches.
     * 
     * @param key Cache key
     */
    public void evict(String key) {
        try {
            l1Cache.evict(key);
            l2Cache.evict(key);
            
            log.debug("Cache EVICT (L1+L2): {}", key);
            
        } catch (Exception e) {
            log.error("Error evicting from cache: {}", key, e);
        }
    }
    
    /**
     * Clear both caches completely.
     * 
     * CAUTION: This removes ALL cached data.
     */
    public void clear() {
        try {
            l1Cache.clear();
            l2Cache.clear();
            
            log.info("Cache CLEARED (L1+L2)");
            
        } catch (Exception e) {
            log.error("Error clearing cache", e);
        }
    }
    
    // ========================================
    // PUBLIC API - STATS
    // ========================================
    
    /**
     * Get L1 cache statistics.
     */
    public CacheStats getL1Stats() {
        return l1Cache.getStats();
    }
    
    /**
     * Get L2 cache statistics.
     */
    public CacheStats getL2Stats() {
        return l2Cache.getStats();
    }
    
    /**
     * Get combined cache statistics.
     */
    public CombinedStats getCombinedStats() {
        CacheStats l1 = l1Cache.getStats();
        CacheStats l2 = l2Cache.getStats();
        
        return new CombinedStats(l1, l2);
    }
    
    /**
     * Combined statistics for both cache tiers.
     */
    public static class CombinedStats {
        private final CacheStats l1;
        private final CacheStats l2;
        
        public CombinedStats(CacheStats l1, CacheStats l2) {
            this.l1 = l1;
            this.l2 = l2;
        }
        
        public CacheStats getL1() {
            return l1;
        }
        
        public CacheStats getL2() {
            return l2;
        }
        
        public long getTotalHits() {
            return l1.getHits() + l2.getHits();
        }
        
        public long getTotalMisses() {
            return l1.getMisses() + l2.getMisses();
        }
        
        public double getOverallHitRate() {
            long totalRequests = l1.getTotalRequests() + l2.getMisses();
            if (totalRequests == 0) return 0.0;
            return (getTotalHits() * 100.0) / totalRequests;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CombinedStats{L1: hits=%d, L2: hits=%d, overall=%.2f%%}",
                l1.getHits(),
                l2.getHits(),
                getOverallHitRate()
            );
        }
    }
    
    // ========================================
    // PUBLIC API - UTILITY
    // ========================================
    
    /**
     * Check if key exists in any cache.
     */
    public boolean contains(String key) {
        return l1Cache.contains(key) || l2Cache.contains(key);
    }
    
    /**
     * Warm up cache with multiple entries.
     * 
     * Useful for preloading common queries.
     * 
     * @param entries Map of key → value to preload
     */
    public void warmUp(java.util.Map<String, String> entries) {
        log.info("Warming up cache with {} entries", entries.size());
        
        entries.forEach((key, value) -> {
            try {
                put(key, value);
            } catch (Exception e) {
                log.error("Error warming up cache for key: {}", key, e);
            }
        });
        
        log.info("Cache warm-up complete");
    }
    
    /**
     * Get comprehensive cache summary.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("========================================\n");
        sb.append("         CACHE SYSTEM SUMMARY          \n");
        sb.append("========================================\n\n");
        
        sb.append(l1Cache.getSummary());
        sb.append("\n");
        sb.append(l2Cache.getSummary());
        sb.append("\n");
        
        CombinedStats combined = getCombinedStats();
        sb.append("=== COMBINED STATISTICS ===\n");
        sb.append(String.format("Total Hits: %,d (L1: %,d, L2: %,d)\n", 
            combined.getTotalHits(),
            combined.getL1().getHits(),
            combined.getL2().getHits()));
        sb.append(String.format("Total Misses: %,d\n", combined.getTotalMisses()));
        sb.append(String.format("Overall Hit Rate: %.2f%%\n", combined.getOverallHitRate()));
        
        sb.append("========================================\n");
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        CombinedStats stats = getCombinedStats();
        return String.format(
            "CacheManager{L1: %d hits, L2: %d hits, overall: %.2f%%}",
            stats.getL1().getHits(),
            stats.getL2().getHits(),
            stats.getOverallHitRate()
        );
    }
    
    // ========================================
    // HEALTH CHECK
    // ========================================
    
    /**
     * Check cache health.
     * 
     * @return true if both caches are operational
     */
    public boolean isHealthy() {
        try {
            // Test L1
            String testKey = "health_check_" + System.currentTimeMillis();
            l1Cache.put(testKey, "test");
            boolean l1Healthy = l1Cache.get(testKey).isPresent();
            l1Cache.evict(testKey);
            
            // Test L2 (if enabled)
            boolean l2Healthy = true;
            if (config.isL2Enabled()) {
                l2Cache.put(testKey, "test", 60);
                l2Healthy = l2Cache.get(testKey).isPresent();
                l2Cache.evict(testKey);
            }
            
            return l1Healthy && l2Healthy;
            
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return false;
        }
    }
}
