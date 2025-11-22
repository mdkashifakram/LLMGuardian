package io.com.llmguardian.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration for two-tier caching system.
 * 
 * Tier 1 (L1): Caffeine (in-memory, ultra-fast)
 * Tier 2 (L2): Redis (distributed, persistent)
 * 
 * Configuration from application.yml:
 * <pre>
 * llmguardian:
 *   cache:
 *     l1:
 *       max-size: 1000
 *       ttl-minutes: 60
 *       stats-enabled: true
 *     l2:
 *       ttl-minutes: 1440  # 24 hours
 *       enabled: true
 * </pre>
 * 
 * @author LLMGuardian Team
 */
@Configuration
@ConfigurationProperties(prefix = "llmguardian.cache")
public class CacheConfig {
    
    // ========================================
    // NESTED CONFIGURATION CLASSES
    // ========================================
    
    private L1Config l1 = new L1Config();
    private L2Config l2 = new L2Config();
    
    /**
     * L1 Cache Configuration (Caffeine).
     */
    public static class L1Config {
        private int maxSize = 1000;              // Max entries
        private int ttlMinutes = 60;             // Time-to-live
        private boolean statsEnabled = true;     // Track stats
        private boolean recordStats = true;      // Enable Caffeine stats
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public int getTtlMinutes() {
            return ttlMinutes;
        }
        
        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
        
        public boolean isStatsEnabled() {
            return statsEnabled;
        }
        
        public void setStatsEnabled(boolean statsEnabled) {
            this.statsEnabled = statsEnabled;
        }
        
        public boolean isRecordStats() {
            return recordStats;
        }
        
        public void setRecordStats(boolean recordStats) {
            this.recordStats = recordStats;
        }
    }
    
    /**
     * L2 Cache Configuration (Redis).
     */
    public static class L2Config {
        private int ttlMinutes = 1440;           // 24 hours default
        private boolean enabled = true;          // Can disable for dev
        private String keyPrefix = "llm:";       // Key namespace
        
        public int getTtlMinutes() {
            return ttlMinutes;
        }
        
        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getKeyPrefix() {
            return keyPrefix;
        }
        
        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
    
    // ========================================
    // GETTERS/SETTERS
    // ========================================
    
    public L1Config getL1() {
        return l1;
    }
    
    public void setL1(L1Config l1) {
        this.l1 = l1;
    }
    
    public L2Config getL2() {
        return l2;
    }
    
    public void setL2(L2Config l2) {
        this.l2 = l2;
    }
    
    // ========================================
    // BEAN DEFINITIONS
    // ========================================
    
    /**
     * Create Caffeine cache bean (L1).
     */
    @Bean(name = "l1Cache")
    public CaffeineCache caffeineCache() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(l1.getMaxSize())
            .expireAfterWrite(Duration.ofMinutes(l1.getTtlMinutes()));
        
        // Enable stats recording if configured
        if (l1.isRecordStats()) {
            caffeine.recordStats();
        }
        
        return new CaffeineCache("l1Cache", caffeine.build());
    }
    
    /**
     * Create Redis template bean (L2).
     * 
     * Configured for String keys and String values.
     */
    @Bean(name = "cacheRedisTemplate")
    public RedisTemplate<String, String> redisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializers for both key and value
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        
        return template;
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Get L1 TTL in seconds.
     */
    public long getL1TtlSeconds() {
        return l1.getTtlMinutes() * 60L;
    }
    
    /**
     * Get L2 TTL in seconds.
     */
    public long getL2TtlSeconds() {
        return l2.getTtlMinutes() * 60L;
    }
    
    /**
     * Check if L2 cache is enabled.
     */
    public boolean isL2Enabled() {
        return l2.isEnabled();
    }
    
    /**
     * Check if stats are enabled.
     */
    public boolean isStatsEnabled() {
        return l1.isStatsEnabled();
    }
    
    @Override
    public String toString() {
        return String.format(
            "CacheConfig{L1: maxSize=%d, ttl=%dmin, stats=%s | L2: enabled=%s, ttl=%dmin}",
            l1.getMaxSize(),
            l1.getTtlMinutes(),
            l1.isStatsEnabled(),
            l2.isEnabled(),
            l2.getTtlMinutes()
        );
    }
}
