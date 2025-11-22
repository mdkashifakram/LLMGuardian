package io.com.llmguardian.provider;

import java.time.Instant;

/**
 * Response from LLM provider.
 * 
 * Contains:
 * - Generated text
 * - Token usage (input/output)
 * - Model used
 * - Completion metadata
 * - Timing information
 * 
 * @author LLMGuardian Team
 */
public class ProviderResponse {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final String text;                  // Generated text
    private final String modelId;               // Model used
    private final int inputTokens;              // Tokens in prompt
    private final int outputTokens;             // Tokens in response
    private final long latencyMs;               // Time taken (ms)
    private final Instant timestamp;            // When completed
    private final String finishReason;          // Why completion stopped
    private final Double estimatedCost;         // Estimated cost (USD)
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private ProviderResponse(Builder builder) {
        this.text = builder.text;
        this.modelId = builder.modelId;
        this.inputTokens = builder.inputTokens;
        this.outputTokens = builder.outputTokens;
        this.latencyMs = builder.latencyMs;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.finishReason = builder.finishReason;
        this.estimatedCost = builder.estimatedCost;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getText() {
        return text;
    }
    
    public String getModelId() {
        return modelId;
    }
    
    public int getInputTokens() {
        return inputTokens;
    }
    
    public int getOutputTokens() {
        return outputTokens;
    }
    
    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
    
    public long getLatencyMs() {
        return latencyMs;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public Double getEstimatedCost() {
        return estimatedCost;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    /**
     * Check if completion finished successfully.
     */
    public boolean isComplete() {
        return "stop".equalsIgnoreCase(finishReason);
    }
    
    /**
     * Check if completion hit length limit.
     */
    public boolean isLengthLimited() {
        return "length".equalsIgnoreCase(finishReason);
    }
    
    /**
     * Check if completion was stopped by content filter.
     */
    public boolean isFiltered() {
        return "content_filter".equalsIgnoreCase(finishReason);
    }
    
    @Override
    public String toString() {
        return String.format(
            "ProviderResponse{model=%s, tokens=%d→%d, latency=%dms, cost=$%.6f, reason=%s}",
            modelId,
            inputTokens,
            outputTokens,
            latencyMs,
            estimatedCost != null ? estimatedCost : 0.0,
            finishReason
        );
    }
    
    /**
     * Get detailed summary.
     */
    public String getSummary() {
        return String.format(
            """
            === Provider Response ===
            Model: %s
            Input Tokens: %,d
            Output Tokens: %,d
            Total Tokens: %,d
            Latency: %,d ms
            Cost: $%.6f
            Finish Reason: %s
            Text Length: %d chars
            """,
            modelId,
            inputTokens,
            outputTokens,
            getTotalTokens(),
            latencyMs,
            estimatedCost != null ? estimatedCost : 0.0,
            finishReason,
            text != null ? text.length() : 0
        );
    }
    
    // ========================================
    // BUILDER PATTERN
    // ========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String text;
        private String modelId;
        private int inputTokens;
        private int outputTokens;
        private long latencyMs;
        private Instant timestamp;
        private String finishReason = "stop";
        private Double estimatedCost;
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }
        
        public Builder inputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }
        
        public Builder outputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }
        
        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }
        
        public Builder estimatedCost(Double estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }
        
        public ProviderResponse build() {
            return new ProviderResponse(this);
        }
    }
}
