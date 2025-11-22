package io.com.llmguardian.routing;

/**
 * Configuration for a single LLM model.
 * 
 * Defines:
 * - Model identifier (e.g., "gpt-4o-mini")
 * - Display name
 * - Pricing
 * - Capabilities
 * - Performance characteristics
 * 
 * @author LLMGuardian Team
 */
public class ModelConfig {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final String modelId;               // API identifier (e.g., "gpt-4o-mini")
    private final String displayName;           // Human-readable name
    private final String provider;              // Provider (OpenAI, Anthropic, etc.)
    private final double costPer1kInputTokens;  // Input cost
    private final double costPer1kOutputTokens; // Output cost
    private final int maxTokens;                // Max context window
    private final ModelCapability capability;   // Capability level
    private final boolean enabled;              // Is this model available?
    
    // ========================================
    // MODEL CAPABILITIES
    // ========================================
    
    public enum ModelCapability {
        BASIC,      // Simple tasks, Q&A
        STANDARD,   // Most general tasks
        ADVANCED    // Complex reasoning, coding, analysis
    }
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private ModelConfig(Builder builder) {
        this.modelId = builder.modelId;
        this.displayName = builder.displayName;
        this.provider = builder.provider;
        this.costPer1kInputTokens = builder.costPer1kInputTokens;
        this.costPer1kOutputTokens = builder.costPer1kOutputTokens;
        this.maxTokens = builder.maxTokens;
        this.capability = builder.capability;
        this.enabled = builder.enabled;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getModelId() {
        return modelId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public double getCostPer1kInputTokens() {
        return costPer1kInputTokens;
    }
    
    public double getCostPer1kOutputTokens() {
        return costPer1kOutputTokens;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public ModelCapability getCapability() {
        return capability;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Calculate estimated cost for a request.
     * 
     * @param inputTokens Estimated input tokens
     * @param outputTokens Estimated output tokens
     * @return Total estimated cost
     */
    public double estimateCost(int inputTokens, int outputTokens) {
        double inputCost = (inputTokens / 1000.0) * costPer1kInputTokens;
        double outputCost = (outputTokens / 1000.0) * costPer1kOutputTokens;
        return inputCost + outputCost;
    }
    
    /**
     * Check if this model can handle the given complexity.
     */
    public boolean canHandle(ComplexityScore.ComplexityLevel level) {
        switch (capability) {
            case BASIC:
                return level == ComplexityScore.ComplexityLevel.SIMPLE;
            case STANDARD:
                return level != ComplexityScore.ComplexityLevel.COMPLEX;
            case ADVANCED:
                return true; // Can handle all
            default:
                return false;
        }
    }
    
    /**
     * Check if model is cost-effective (< $0.001 per 1k tokens).
     */
    public boolean isCostEffective() {
        return costPer1kInputTokens < 0.001;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ModelConfig{id=%s, name=%s, capability=%s, cost=$%.6f/$%.6f, enabled=%s}",
            modelId,
            displayName,
            capability,
            costPer1kInputTokens,
            costPer1kOutputTokens,
            enabled
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
        private String displayName;
        private String provider = "OpenAI";
        private double costPer1kInputTokens;
        private double costPer1kOutputTokens;
        private int maxTokens = 4096;
        private ModelCapability capability = ModelCapability.STANDARD;
        private boolean enabled = true;
        
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder costPer1kInputTokens(double cost) {
            this.costPer1kInputTokens = cost;
            return this;
        }
        
        public Builder costPer1kOutputTokens(double cost) {
            this.costPer1kOutputTokens = cost;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder capability(ModelCapability capability) {
            this.capability = capability;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public ModelConfig build() {
            return new ModelConfig(this);
        }
    }
    
    // ========================================
    // PREDEFINED MODELS (Factory Methods)
    // ========================================
    
    /**
     * GPT-4o-mini - Cost-effective, fast, good for most tasks.
     */
    public static ModelConfig gpt4oMini() {
        return builder()
            .modelId("gpt-4o-mini")
            .displayName("GPT-4o Mini")
            .provider("OpenAI")
            .costPer1kInputTokens(0.00015)    // $0.15 per 1M input tokens
            .costPer1kOutputTokens(0.0006)    // $0.60 per 1M output tokens
            .maxTokens(128000)
            .capability(ModelCapability.STANDARD)
            .enabled(true)
            .build();
    }
    
    /**
     * GPT-4o - Most capable, higher cost.
     */
    public static ModelConfig gpt4o() {
        return builder()
            .modelId("gpt-4o")
            .displayName("GPT-4o")
            .provider("OpenAI")
            .costPer1kInputTokens(0.0025)     // $2.50 per 1M input tokens
            .costPer1kOutputTokens(0.01)      // $10 per 1M output tokens
            .maxTokens(128000)
            .capability(ModelCapability.ADVANCED)
            .enabled(true)
            .build();
    }
    
    /**
     * GPT-3.5-turbo - Legacy, very cost-effective.
     */
    public static ModelConfig gpt35Turbo() {
        return builder()
            .modelId("gpt-3.5-turbo")
            .displayName("GPT-3.5 Turbo")
            .provider("OpenAI")
            .costPer1kInputTokens(0.0005)     // $0.50 per 1M input tokens
            .costPer1kOutputTokens(0.0015)    // $1.50 per 1M output tokens
            .maxTokens(16385)
            .capability(ModelCapability.BASIC)
            .enabled(true)
            .build();
    }
}
