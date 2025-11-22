package io.com.llmguardian.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * API response for LLM completion.
 * 
 * This is the external API contract - what clients receive from our service.
 * 
 * Example JSON:
 * <pre>
 * {
 *   "requestId": "550e8400-e29b-41d4-a716-446655440000",
 *   "text": "Here's the email you requested...",
 *   "success": true,
 *   "metadata": {
 *     "modelUsed": "gpt-4o-mini",
 *     "tokensUsed": 245,
 *     "fromCache": false,
 *     "latencyMs": 1234,
 *     "optimizationApplied": true,
 *     "tokensSaved": 50,
 *     "estimatedCost": 0.000123
 *   },
 *   "timestamp": "2025-01-15T10:30:00Z"
 * }
 * </pre>
 * 
 * @author LLMGuardian Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
public class CompletionResponse {
    
    // ========================================
    // CORE FIELDS
    // ========================================
    
    private UUID requestId;
    private String text;
    private boolean success;
    private String error;
    private Instant timestamp;
    
    // ========================================
    // METADATA (Optional)
    // ========================================
    
    private ResponseMetadata metadata;
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    public CompletionResponse() {
        this.timestamp = Instant.now();
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    /**
     * Create successful response.
     */
    public static CompletionResponse success(
        UUID requestId, 
        String text, 
        ResponseMetadata metadata
    ) {
        CompletionResponse response = new CompletionResponse();
        response.setRequestId(requestId);
        response.setText(text);
        response.setSuccess(true);
        response.setMetadata(metadata);
        return response;
    }
    
    /**
     * Create error response.
     */
    public static CompletionResponse error(UUID requestId, String errorMessage) {
        CompletionResponse response = new CompletionResponse();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public UUID getRequestId() {
        return requestId;
    }
    
    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public ResponseMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }
    
    // ========================================
    // NESTED CLASS: RESPONSE METADATA
    // ========================================
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseMetadata {
        
        // Model information
        private String modelUsed;
        private String complexityLevel;
        
        // Token usage
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        
        // Performance
        private Long latencyMs;
        private Boolean fromCache;
        
        // Optimization
        private Boolean optimizationApplied;
        private Integer tokensSaved;
        private Double reductionPercentage;
        
        // Security
        private Boolean piiDetected;
        private Integer piiCount;
        
        // Cost
        private Double estimatedCost;
        
        // ========================================
        // CONSTRUCTORS
        // ========================================
        
        public ResponseMetadata() {
        }
        
        // ========================================
        // GETTERS AND SETTERS
        // ========================================
        
        public String getModelUsed() {
            return modelUsed;
        }
        
        public void setModelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
        }
        
        public String getComplexityLevel() {
            return complexityLevel;
        }
        
        public void setComplexityLevel(String complexityLevel) {
            this.complexityLevel = complexityLevel;
        }
        
        public Integer getInputTokens() {
            return inputTokens;
        }
        
        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }
        
        public Integer getOutputTokens() {
            return outputTokens;
        }
        
        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }
        
        public Integer getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
        
        public Long getLatencyMs() {
            return latencyMs;
        }
        
        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }
        
        public Boolean getFromCache() {
            return fromCache;
        }
        
        public void setFromCache(Boolean fromCache) {
            this.fromCache = fromCache;
        }
        
        public Boolean getOptimizationApplied() {
            return optimizationApplied;
        }
        
        public void setOptimizationApplied(Boolean optimizationApplied) {
            this.optimizationApplied = optimizationApplied;
        }
        
        public Integer getTokensSaved() {
            return tokensSaved;
        }
        
        public void setTokensSaved(Integer tokensSaved) {
            this.tokensSaved = tokensSaved;
        }
        
        public Double getReductionPercentage() {
            return reductionPercentage;
        }
        
        public void setReductionPercentage(Double reductionPercentage) {
            this.reductionPercentage = reductionPercentage;
        }
        
        public Boolean getPiiDetected() {
            return piiDetected;
        }
        
        public void setPiiDetected(Boolean piiDetected) {
            this.piiDetected = piiDetected;
        }
        
        public Integer getPiiCount() {
            return piiCount;
        }
        
        public void setPiiCount(Integer piiCount) {
            this.piiCount = piiCount;
        }
        
        public Double getEstimatedCost() {
            return estimatedCost;
        }
        
        public void setEstimatedCost(Double estimatedCost) {
            this.estimatedCost = estimatedCost;
        }
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        if (success) {
            return String.format(
                "CompletionResponse{requestId=%s, success=true, textLength=%d, latency=%dms}",
                requestId,
                text != null ? text.length() : 0,
                metadata != null ? metadata.getLatencyMs() : 0
            );
        } else {
            return String.format(
                "CompletionResponse{requestId=%s, success=false, error='%s'}",
                requestId,
                error
            );
        }
    }
}
