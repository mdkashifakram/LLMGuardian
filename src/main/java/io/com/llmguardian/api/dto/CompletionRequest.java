package io.com.llmguardian.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API request for LLM completion.
 * 
 * This is the external API contract - what clients send to our service.
 * 
 * Example JSON:
 * <pre>
 * {
 *   "query": "Write an email to john@example.com about the project",
 *   "maxTokens": 500,
 *   "temperature": 0.7,
 *   "model": "gpt-4o-mini"
 * }
 * </pre>
 * 
 * @author LLMGuardian Team
 */
public class CompletionRequest {
    
    // ========================================
    // REQUIRED FIELDS
    // ========================================
    
    @NotBlank(message = "Query cannot be empty")
    private String query;
    
    // ========================================
    // OPTIONAL FIELDS
    // ========================================
    
    @Min(value = 1, message = "maxTokens must be at least 1")
    @Max(value = 4096, message = "maxTokens cannot exceed 4096")
    private Integer maxTokens = 1000;  // Default: 1000 tokens
    
    @Min(value = 0, message = "temperature must be between 0 and 2")
    @Max(value = 2, message = "temperature must be between 0 and 2")
    private Double temperature;  // Optional: model creativity (0.0-2.0)
    
    @Min(value = 0, message = "topP must be between 0 and 1")
    @Max(value = 1, message = "topP must be between 0 and 1")
    private Double topP;  // Optional: nucleus sampling
    
    private String model;  // Optional: override model selection
    
    private String routingStrategy;  // Optional: COMPLEXITY_BASED, COST_OPTIMIZED, etc.
    
    private Boolean enableOptimization = true;  // Optional: enable/disable prompt optimization
    
    private Boolean enableCache = true;  // Optional: enable/disable caching
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    public CompletionRequest() {
    }
    
    public CompletionRequest(String query) {
        this.query = query;
    }
    
    public CompletionRequest(String query, Integer maxTokens) {
        this.query = query;
        this.maxTokens = maxTokens;
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getRoutingStrategy() {
        return routingStrategy;
    }
    
    public void setRoutingStrategy(String routingStrategy) {
        this.routingStrategy = routingStrategy;
    }
    
    public Boolean getEnableOptimization() {
        return enableOptimization;
    }
    
    public void setEnableOptimization(Boolean enableOptimization) {
        this.enableOptimization = enableOptimization;
    }
    
    public Boolean getEnableCache() {
        return enableCache;
    }
    
    public void setEnableCache(Boolean enableCache) {
        this.enableCache = enableCache;
    }
    
    // ========================================
    // VALIDATION HELPERS
    // ========================================
    
    /**
     * Check if request has valid parameters.
     */
    public boolean isValid() {
        return query != null && 
               !query.trim().isEmpty() && 
               maxTokens > 0 && 
               maxTokens <= 4096;
    }
    
    /**
     * Get effective max tokens (with fallback).
     */
    public int getEffectiveMaxTokens() {
        return maxTokens != null ? maxTokens : 1000;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "CompletionRequest{query='%s...', maxTokens=%d, model=%s, optimization=%s, cache=%s}",
            query != null && query.length() > 50 ? query.substring(0, 50) : query,
            maxTokens,
            model,
            enableOptimization,
            enableCache
        );
    }
}
