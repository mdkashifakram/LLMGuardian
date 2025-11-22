package io.com.llmguardian.routing;

/**
 * Result of model routing decision.
 * 
 * Contains:
 * - Selected model ID (e.g., "gpt-4o-mini")
 * - Reasoning for selection
 * - Estimated cost
 * - Alternative models considered
 * 
 * @author LLMGuardian Team
 */
public class ModelDecision {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final String modelId;               // Selected model (e.g., "gpt-4o-mini")
    private final String modelName;             // Human-readable name
    private final String reasoning;             // Why this model?
    private final double estimatedCostPer1kTokens; // Cost estimate
    private final RoutingStrategy strategyUsed; // Strategy that made decision
    private final ComplexityScore complexity;   // Input complexity
    private final long routingTimeMs;           // Time to make decision
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private ModelDecision(Builder builder) {
        this.modelId = builder.modelId;
        this.modelName = builder.modelName;
        this.reasoning = builder.reasoning;
        this.estimatedCostPer1kTokens = builder.estimatedCostPer1kTokens;
        this.strategyUsed = builder.strategyUsed;
        this.complexity = builder.complexity;
        this.routingTimeMs = builder.routingTimeMs;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getModelId() {
        return modelId;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public double getEstimatedCostPer1kTokens() {
        return estimatedCostPer1kTokens;
    }
    
    public RoutingStrategy getStrategyUsed() {
        return strategyUsed;
    }
    
    public ComplexityScore getComplexity() {
        return complexity;
    }
    
    public long getRoutingTimeMs() {
        return routingTimeMs;
    }
    
    /**
     * Check if this is a cost-effective model.
     */
    public boolean isCostEffective() {
        return estimatedCostPer1kTokens < 0.001; // Less than $0.001 per 1k tokens
    }
    
    /**
     * Check if this is a premium model.
     */
    public boolean isPremiumModel() {
        return modelId.contains("gpt-4") && !modelId.contains("mini");
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "ModelDecision{model=%s, strategy=%s, cost=$%.6f/1k, time=%dms}",
            modelId,
            strategyUsed,
            estimatedCostPer1kTokens,
            routingTimeMs
        );
    }
    
    /**
     * Get human-readable summary.
     */
    public String getSummary() {
        return String.format(
            "Selected: %s (cost: $%.6f/1k tokens) - %s",
            modelName,
            estimatedCostPer1kTokens,
            reasoning
        );
    }
    
    // ========================================
    // BUILDER PATTERN
    // ========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String modelId;
        private String modelName;
        private String reasoning;
        private double estimatedCostPer1kTokens;
        private RoutingStrategy strategyUsed = RoutingStrategy.COMPLEXITY_BASED;
        private ComplexityScore complexity;
        private long routingTimeMs;
        
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }
        
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }
        
        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }
        
        public Builder estimatedCostPer1kTokens(double cost) {
            this.estimatedCostPer1kTokens = cost;
            return this;
        }
        
        public Builder strategyUsed(RoutingStrategy strategy) {
            this.strategyUsed = strategy;
            return this;
        }
        
        public Builder complexity(ComplexityScore complexity) {
            this.complexity = complexity;
            return this;
        }
        
        public Builder routingTimeMs(long routingTimeMs) {
            this.routingTimeMs = routingTimeMs;
            return this;
        }
        
        public ModelDecision build() {
            return new ModelDecision(this);
        }
    }
}
