package io.com.llmguardian.api.controller;

import io.com.llmguardian.cache.CacheManager;
import io.com.llmguardian.cache.CacheStats;
import io.com.llmguardian.routing.ModelRegistry;
import io.com.llmguardian.security.AuditLogger;
import io.com.llmguardian.security.PIIPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for analytics and monitoring.
 * 
 * Endpoints:
 * - GET /api/v1/analytics/cache - Cache statistics
 * - GET /api/v1/analytics/pii - PII detection statistics
 * - GET /api/v1/analytics/models - Available models
 * - GET /api/v1/analytics/summary - Overall system summary
 * 
 * These endpoints provide insights into system performance,
 * security, and configuration.
 * 
 * @author LLMGuardian Team
 */
@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private AuditLogger auditLogger;
    
    @Autowired
    private ModelRegistry modelRegistry;
    
    // ========================================
    // CACHE ANALYTICS
    // ========================================
    
    /**
     * Get cache statistics.
     * 
     * GET /api/v1/analytics/cache
     * 
     * Returns L1 and L2 cache performance metrics.
     */
    @GetMapping("/cache")
    public ResponseEntity<CacheAnalytics> getCacheAnalytics() {
        log.debug("Cache analytics requested");
        
        CacheStats l1Stats = cacheManager.getL1Stats();
        CacheStats l2Stats = cacheManager.getL2Stats();
        CacheManager.CombinedStats combined = cacheManager.getCombinedStats();
        
        CacheAnalytics analytics = new CacheAnalytics();
        
        // L1 stats
        analytics.l1Hits = l1Stats.getHits();
        analytics.l1Misses = l1Stats.getMisses();
        analytics.l1HitRate = l1Stats.getHitRate();
        analytics.l1Size = l1Stats.getCurrentSize();
        
        // L2 stats
        analytics.l2Hits = l2Stats.getHits();
        analytics.l2Misses = l2Stats.getMisses();
        analytics.l2HitRate = l2Stats.getHitRate();
        
        // Combined stats
        analytics.totalHits = combined.getTotalHits();
        analytics.totalMisses = combined.getTotalMisses();
        analytics.overallHitRate = combined.getOverallHitRate();
        
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Clear cache (admin operation).
     * 
     * POST /api/v1/analytics/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        log.warn("Cache clear requested");
        
        cacheManager.clear();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }
    
    // ========================================
    // PII ANALYTICS
    // ========================================
    
    /**
     * Get PII detection statistics.
     * 
     * GET /api/v1/analytics/pii
     * 
     * Returns counts of PII types detected.
     */
    @GetMapping("/pii")
    public ResponseEntity<PIIAnalytics> getPIIAnalytics(
        @RequestParam(required = false, defaultValue = "30") int days
    ) {
        log.debug("PII analytics requested for last {} days", days);
        
        PIIAnalytics analytics = new PIIAnalytics();
        
        // Get detection counts by type
        Map<PIIPattern, Long> countsByType = auditLogger.getDetectionCountsByType();
        analytics.detectionsByType = new HashMap<>();
        countsByType.forEach((type, count) -> 
            analytics.detectionsByType.put(type.getName(), count)
        );
        
        // Get total detections in time period
        analytics.totalDetections = auditLogger.getDetectionCountLastNDays(days);
        analytics.periodDays = days;
        
        return ResponseEntity.ok(analytics);
    }
    
    // ========================================
    // MODEL ANALYTICS
    // ========================================
    
    /**
     * Get available models information.
     * 
     * GET /api/v1/analytics/models
     */
    @GetMapping("/models")
    public ResponseEntity<ModelAnalytics> getModelAnalytics() {
        log.debug("Model analytics requested");
        
        ModelAnalytics analytics = new ModelAnalytics();
        
        analytics.totalModels = modelRegistry.getModelCount();
        analytics.enabledModels = modelRegistry.getEnabledModelCount();
        
        // Build model details
        analytics.models = new HashMap<>();
        modelRegistry.getAllModels().forEach(model -> {
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("displayName", model.getDisplayName());
            modelInfo.put("provider", model.getProvider());
            modelInfo.put("capability", model.getCapability().name());
            modelInfo.put("inputCost", model.getCostPer1kInputTokens());
            modelInfo.put("outputCost", model.getCostPer1kOutputTokens());
            modelInfo.put("maxTokens", model.getMaxTokens());
            modelInfo.put("enabled", model.isEnabled());
            
            analytics.models.put(model.getModelId(), modelInfo);
        });
        
        return ResponseEntity.ok(analytics);
    }
    
    // ========================================
    // SYSTEM SUMMARY
    // ========================================
    
    /**
     * Get overall system summary.
     * 
     * GET /api/v1/analytics/summary
     * 
     * Returns comprehensive system statistics.
     */
    @GetMapping("/summary")
    public ResponseEntity<SystemSummary> getSystemSummary() {
        log.debug("System summary requested");
        
        SystemSummary summary = new SystemSummary();
        
        // Cache metrics
        CacheManager.CombinedStats cacheStats = cacheManager.getCombinedStats();
        summary.cacheHitRate = cacheStats.getOverallHitRate();
        summary.totalCacheHits = cacheStats.getTotalHits();
        
        // PII metrics
        summary.totalPIIDetections = auditLogger.getDetectionCountLastNDays(30);
        
        // Model metrics
        summary.availableModels = modelRegistry.getEnabledModelCount();
        
        // System info
        summary.status = "HEALTHY";
        summary.version = "1.0.0";
        
        return ResponseEntity.ok(summary);
    }
    
    // ========================================
    // HEALTH CHECK (Detailed)
    // ========================================
    
    /**
     * Detailed health check.
     * 
     * GET /api/v1/analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        log.debug("Detailed health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "LLMGuardian Analytics");
        
        // Component health
        Map<String, String> components = new HashMap<>();
        components.put("cache", cacheManager.isHealthy() ? "UP" : "DOWN");
        components.put("models", modelRegistry.getEnabledModelCount() > 0 ? "UP" : "DOWN");
        
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }
    
    // ========================================
    // RESPONSE CLASSES
    // ========================================
    
    public static class CacheAnalytics {
        // L1 Cache
        public long l1Hits;
        public long l1Misses;
        public double l1HitRate;
        public long l1Size;
        
        // L2 Cache
        public long l2Hits;
        public long l2Misses;
        public double l2HitRate;
        
        // Combined
        public long totalHits;
        public long totalMisses;
        public double overallHitRate;
    }
    
    public static class PIIAnalytics {
        public long totalDetections;
        public int periodDays;
        public Map<String, Long> detectionsByType;
    }
    
    public static class ModelAnalytics {
        public int totalModels;
        public int enabledModels;
        public Map<String, Map<String, Object>> models;
    }
    
    public static class SystemSummary {
        public String status;
        public String version;
        public double cacheHitRate;
        public long totalCacheHits;
        public long totalPIIDetections;
        public int availableModels;
    }
}
