package io.com.llmguardian.optimization;

import io.com.llmguardian.optimization.dto.Entity;
import io.com.llmguardian.optimization.dto.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of prompt optimization.
 * 
 * Contains:
 * - Original and optimized text
 * - Token reduction statistics
 * - Extracted metadata (intent, entities)
 * - Performance metrics
 * 
 * @author LLMGuardian Team
 */
public class OptimizationResult {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final String originalPrompt;
    private final String optimizedPrompt;
    private final int originalTokens;
    private final int optimizedTokens;
    private final double reductionPercentage;
    private final Intent intent;
    private final List<Entity> entities;
    private final long optimizationTimeMs;
    private final boolean wasOptimized;
    private final String skipReason;  // If not optimized, why?
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private OptimizationResult(Builder builder) {
        this.originalPrompt = builder.originalPrompt;
        this.optimizedPrompt = builder.optimizedPrompt;
        this.originalTokens = builder.originalTokens;
        this.optimizedTokens = builder.optimizedTokens;
        this.reductionPercentage = calculateReduction(originalTokens, optimizedTokens);
        this.intent = builder.intent;
        this.entities = Collections.unmodifiableList(new ArrayList<>(builder.entities));
        this.optimizationTimeMs = builder.optimizationTimeMs;
        this.wasOptimized = builder.wasOptimized;
        this.skipReason = builder.skipReason;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getOriginalPrompt() {
        return originalPrompt;
    }
    
    public String getOptimizedPrompt() {
        return optimizedPrompt;
    }
    
    public int getOriginalTokens() {
        return originalTokens;
    }
    
    public int getOptimizedTokens() {
        return optimizedTokens;
    }
    
    public int getTokensSaved() {
        return originalTokens - optimizedTokens;
    }
    
    public double getReductionPercentage() {
        return reductionPercentage;
    }
    
    public Intent getIntent() {
        return intent;
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public long getOptimizationTimeMs() {
        return optimizationTimeMs;
    }
    
    public boolean wasOptimized() {
        return wasOptimized;
    }
    
    public String getSkipReason() {
        return skipReason;
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Calculate reduction percentage.
     */
    private double calculateReduction(int original, int optimized) {
        if (original == 0) return 0.0;
        return ((double) (original - optimized) / original) * 100.0;
    }
    
    /**
     * Check if optimization was effective.
     * Effective = saved at least 10% tokens
     */
    public boolean isEffective() {
        return wasOptimized && reductionPercentage >= 10.0;
    }
    
    /**
     * Get human-readable summary.
     */
    public String getSummary() {
        if (!wasOptimized) {
            return String.format("Optimization skipped: %s", skipReason);
        }
        return String.format(
            "Optimized: %d → %d tokens (%.1f%% reduction) in %dms",
            originalTokens,
            optimizedTokens,
            reductionPercentage,
            optimizationTimeMs
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "OptimizationResult{wasOptimized=%s, tokens=%d→%d, reduction=%.1f%%, entities=%d, time=%dms}",
            wasOptimized,
            originalTokens,
            optimizedTokens,
            reductionPercentage,
            entities.size(),
            optimizationTimeMs
        );
    }
    
    // ========================================
    // BUILDER PATTERN
    // ========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String originalPrompt;
        private String optimizedPrompt;
        private int originalTokens;
        private int optimizedTokens;
        private Intent intent;
        private List<Entity> entities = new ArrayList<>();
        private long optimizationTimeMs;
        private boolean wasOptimized = true;
        private String skipReason;
        
        public Builder originalPrompt(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            return this;
        }
        
        public Builder optimizedPrompt(String optimizedPrompt) {
            this.optimizedPrompt = optimizedPrompt;
            return this;
        }
        
        public Builder originalTokens(int originalTokens) {
            this.originalTokens = originalTokens;
            return this;
        }
        
        public Builder optimizedTokens(int optimizedTokens) {
            this.optimizedTokens = optimizedTokens;
            return this;
        }
        
        public Builder intent(Intent intent) {
            this.intent = intent;
            return this;
        }
        
        public Builder entities(List<Entity> entities) {
            this.entities = new ArrayList<>(entities);
            return this;
        }
        
        public Builder addEntity(Entity entity) {
            this.entities.add(entity);
            return this;
        }
        
        public Builder optimizationTimeMs(long optimizationTimeMs) {
            this.optimizationTimeMs = optimizationTimeMs;
            return this;
        }
        
        public Builder wasOptimized(boolean wasOptimized) {
            this.wasOptimized = wasOptimized;
            return this;
        }
        
        public Builder skipReason(String skipReason) {
            this.skipReason = skipReason;
            this.wasOptimized = false;
            return this;
        }
        
        public OptimizationResult build() {
            return new OptimizationResult(this);
        }
    }
    
    // ========================================
    // FACTORY METHODS (Convenience)
    // ========================================
    
    /**
     * Create result for skipped optimization.
     */
    public static OptimizationResult skipped(String originalPrompt, String reason) {
        return builder()
            .originalPrompt(originalPrompt)
            .optimizedPrompt(originalPrompt)
            .originalTokens(estimateTokens(originalPrompt))
            .optimizedTokens(estimateTokens(originalPrompt))
            .skipReason(reason)
            .wasOptimized(false)
            .build();
    }
    
    /**
     * Simple token estimation (4 chars ≈ 1 token).
     */
    private static int estimateTokens(String text) {
        return text.length() / 4;
    }
}
