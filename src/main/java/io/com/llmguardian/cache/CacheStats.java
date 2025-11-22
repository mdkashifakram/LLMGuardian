package io.com.llmguardian.cache;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics and metrics for cache performance.
 * 
 * Tracks:
 * - Hit/miss counts
 * - Hit rate percentage
 * - Total requests
 * - Cache size
 * - Eviction counts
 * 
 * Thread-safe using atomic counters.
 * 
 * @author LLMGuardian Team
 */
public class CacheStats {
    
    // ========================================
    // FIELDS (Thread-safe counters)
    // ========================================
    
    private final String cacheName;              // L1 or L2
    private final AtomicLong hits;               // Cache hits
    private final AtomicLong misses;             // Cache misses
    private final AtomicLong evictions;          // Entries evicted
    private final AtomicLong totalRequests;      // Total get operations
    private final Instant startTime;             // When stats started
    private volatile long currentSize;           // Current entries (updated externally)
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public CacheStats(String cacheName) {
        this.cacheName = cacheName;
        this.hits = new AtomicLong(0);
        this.misses = new AtomicLong(0);
        this.evictions = new AtomicLong(0);
        this.totalRequests = new AtomicLong(0);
        this.startTime = Instant.now();
        this.currentSize = 0;
    }
    
    // ========================================
    // PUBLIC API - INCREMENT COUNTERS
    // ========================================
    
    /**
     * Record a cache hit.
     */
    public void recordHit() {
        hits.incrementAndGet();
        totalRequests.incrementAndGet();
    }
    
    /**
     * Record a cache miss.
     */
    public void recordMiss() {
        misses.incrementAndGet();
        totalRequests.incrementAndGet();
    }
    
    /**
     * Record an eviction.
     */
    public void recordEviction() {
        evictions.incrementAndGet();
    }
    
    /**
     * Update current cache size.
     */
    public void setCurrentSize(long size) {
        this.currentSize = size;
    }
    
    // ========================================
    // PUBLIC API - GETTERS
    // ========================================
    
    public String getCacheName() {
        return cacheName;
    }
    
    public long getHits() {
        return hits.get();
    }
    
    public long getMisses() {
        return misses.get();
    }
    
    public long getEvictions() {
        return evictions.get();
    }
    
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    public long getCurrentSize() {
        return currentSize;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    // ========================================
    // CALCULATED METRICS
    // ========================================
    
    /**
     * Calculate hit rate percentage.
     * 
     * @return Hit rate (0.0 to 100.0)
     */
    public double getHitRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (hits.get() * 100.0) / total;
    }
    
    /**
     * Calculate miss rate percentage.
     * 
     * @return Miss rate (0.0 to 100.0)
     */
    public double getMissRate() {
        return 100.0 - getHitRate();
    }
    
    /**
     * Get uptime in seconds.
     */
    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    /**
     * Get average requests per second.
     */
    public double getRequestsPerSecond() {
        long uptime = getUptimeSeconds();
        if (uptime == 0) {
            return 0.0;
        }
        return totalRequests.get() / (double) uptime;
    }
    
    // ========================================
    // RESET
    // ========================================
    
    /**
     * Reset all statistics.
     */
    public void reset() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        totalRequests.set(0);
        currentSize = 0;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "CacheStats{name=%s, hits=%d, misses=%d, hitRate=%.2f%%, size=%d, evictions=%d}",
            cacheName,
            hits.get(),
            misses.get(),
            getHitRate(),
            currentSize,
            evictions.get()
        );
    }
    
    /**
     * Get detailed summary.
     */
    public String getSummary() {
        return String.format(
            """
            === %s Cache Statistics ===
            Total Requests: %,d
            Hits: %,d (%.2f%%)
            Misses: %,d (%.2f%%)
            Current Size: %,d entries
            Evictions: %,d
            Uptime: %,d seconds
            Requests/Second: %.2f
            """,
            cacheName,
            totalRequests.get(),
            hits.get(), getHitRate(),
            misses.get(), getMissRate(),
            currentSize,
            evictions.get(),
            getUptimeSeconds(),
            getRequestsPerSecond()
        );
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    public static CacheStats forL1() {
        return new CacheStats("L1");
    }
    
    public static CacheStats forL2() {
        return new CacheStats("L2");
    }
}
