package io.com.llmguardian.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for LLM providers.
 * 
 * Loads settings from application.yml under "llmguardian.provider" prefix.
 * 
 * Example configuration:
 * <pre>
 * llmguardian:
 *   provider:
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       organization-id: ${OPENAI_ORG_ID:}
 *       timeout-seconds: 30
 *       max-retries: 3
 *       retry-delay-ms: 1000
 *       default-model: gpt-4o-mini
 * </pre>
 * 
 * @author LLMGuardian Team
 */
@Configuration
@ConfigurationProperties(prefix = "llmguardian.provider")
public class ProviderConfig {
    
    // ========================================
    // NESTED CONFIGURATION
    // ========================================
    
    private OpenAIConfig openai = new OpenAIConfig();
    
    /**
     * OpenAI-specific configuration.
     */
    public static class OpenAIConfig {
        private String apiKey;
        private String organizationId;
        private int timeoutSeconds = 30;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private String defaultModel = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com";
        
        // Connection pool settings
        private int maxConnectionsTotal = 50;
        private int maxConnectionsPerRoute = 20;
        
        // Rate limiting (optional)
        private Integer maxRequestsPerMinute;
        
        // Getters and setters
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getOrganizationId() {
            return organizationId;
        }
        
        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }
        
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
        
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        public long getRetryDelayMs() {
            return retryDelayMs;
        }
        
        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
        
        public String getDefaultModel() {
            return defaultModel;
        }
        
        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getMaxConnectionsTotal() {
            return maxConnectionsTotal;
        }
        
        public void setMaxConnectionsTotal(int maxConnectionsTotal) {
            this.maxConnectionsTotal = maxConnectionsTotal;
        }
        
        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }
        
        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }
        
        public Integer getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }
        
        public void setMaxRequestsPerMinute(Integer maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }
        
        /**
         * Get timeout as Duration.
         */
        public Duration getTimeout() {
            return Duration.ofSeconds(timeoutSeconds);
        }
        
        /**
         * Validate configuration.
         */
        public boolean isValid() {
            return apiKey != null && !apiKey.isEmpty();
        }
    }
    
    // ========================================
    // MAIN GETTERS/SETTERS
    // ========================================
    
    public OpenAIConfig getOpenai() {
        return openai;
    }
    
    public void setOpenai(OpenAIConfig openai) {
        this.openai = openai;
    }
    
    // ========================================
    // VALIDATION
    // ========================================
    
    /**
     * Validate all provider configurations.
     */
    public boolean isValid() {
        return openai.isValid();
    }
    
    /**
     * Get error message if configuration is invalid.
     */
    public String getValidationError() {
        if (!openai.isValid()) {
            return "OpenAI API key is required. Set OPENAI_API_KEY environment variable.";
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ProviderConfig{OpenAI: model=%s, timeout=%ds, retries=%d, hasKey=%s}",
            openai.getDefaultModel(),
            openai.getTimeoutSeconds(),
            openai.getMaxRetries(),
            openai.getApiKey() != null ? "yes" : "no"
        );
    }
}
