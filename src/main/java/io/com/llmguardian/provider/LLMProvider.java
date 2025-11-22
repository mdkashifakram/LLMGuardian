package io.com.llmguardian.provider;

/**
 * Interface for LLM providers (OpenAI, Anthropic, etc.).
 * 
 * Abstracts the LLM API to allow multiple providers.
 * Implementations must handle:
 * - API authentication
 * - Request formatting
 * - Response parsing
 * - Error handling
 * - Retry logic
 * 
 * @author LLMGuardian Team
 */
public interface LLMProvider {
    
    /**
     * Generate completion for a prompt.
     * 
     * @param modelId Model identifier (e.g., "gpt-4o-mini")
     * @param prompt The prompt text
     * @param maxTokens Maximum tokens to generate
     * @return Provider response with generated text
     * @throws ProviderException if completion fails
     */
    ProviderResponse complete(String modelId, String prompt, int maxTokens) 
        throws ProviderException;
    
    /**
     * Generate completion with additional parameters.
     * 
     * @param request Completion request with all parameters
     * @return Provider response with generated text
     * @throws ProviderException if completion fails
     */
    ProviderResponse complete(CompletionRequest request) 
        throws ProviderException;
    
    /**
     * Check if provider is available/healthy.
     * 
     * @return true if provider can handle requests
     */
    boolean isAvailable();
    
    /**
     * Get provider name.
     * 
     * @return Provider identifier (e.g., "OpenAI")
     */
    String getProviderName();
    
    /**
     * Check if model is supported by this provider.
     * 
     * @param modelId Model identifier
     * @return true if model is supported
     */
    boolean supportsModel(String modelId);
    
    // ========================================
    // COMPLETION REQUEST (Nested Class)
    // ========================================
    
    /**
     * Request object for completion with additional parameters.
     */
    class CompletionRequest {
        private final String modelId;
        private final String prompt;
        private final int maxTokens;
        private final Double temperature;
        private final Double topP;
        private final Integer n;
        private final String[] stopSequences;
        
        private CompletionRequest(Builder builder) {
            this.modelId = builder.modelId;
            this.prompt = builder.prompt;
            this.maxTokens = builder.maxTokens;
            this.temperature = builder.temperature;
            this.topP = builder.topP;
            this.n = builder.n;
            this.stopSequences = builder.stopSequences;
        }
        
        // Getters
        public String getModelId() { return modelId; }
        public String getPrompt() { return prompt; }
        public int getMaxTokens() { return maxTokens; }
        public Double getTemperature() { return temperature; }
        public Double getTopP() { return topP; }
        public Integer getN() { return n; }
        public String[] getStopSequences() { return stopSequences; }
        
        // Builder
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String modelId;
            private String prompt;
            private int maxTokens = 1000;
            private Double temperature;
            private Double topP;
            private Integer n;
            private String[] stopSequences;
            
            public Builder modelId(String modelId) {
                this.modelId = modelId;
                return this;
            }
            
            public Builder prompt(String prompt) {
                this.prompt = prompt;
                return this;
            }
            
            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }
            
            public Builder temperature(Double temperature) {
                this.temperature = temperature;
                return this;
            }
            
            public Builder topP(Double topP) {
                this.topP = topP;
                return this;
            }
            
            public Builder n(Integer n) {
                this.n = n;
                return this;
            }
            
            public Builder stopSequences(String... stopSequences) {
                this.stopSequences = stopSequences;
                return this;
            }
            
            public CompletionRequest build() {
                if (modelId == null || prompt == null) {
                    throw new IllegalArgumentException("modelId and prompt are required");
                }
                return new CompletionRequest(this);
            }
        }
    }
}
