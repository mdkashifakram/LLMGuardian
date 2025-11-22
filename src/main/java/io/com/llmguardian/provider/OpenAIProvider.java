package io.com.llmguardian.provider;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * OpenAI provider implementation.
 * 
 * Features:
 * - Chat completion API integration
 * - Automatic retry with exponential backoff
 * - Error handling and classification
 * - Token usage tracking
 * - Cost estimation
 * - Timeout configuration
 * 
 * Supported Models:
 * - gpt-4o
 * - gpt-4o-mini
 * - gpt-3.5-turbo
 * 
 * @author LLMGuardian Team
 */
@Service
public class OpenAIProvider implements LLMProvider {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private ProviderConfig config;
    
    private OpenAiService openAiService;
    
    // ========================================
    // SUPPORTED MODELS
    // ========================================
    
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "gpt-4o",
        "gpt-4o-mini",
        "gpt-3.5-turbo",
        "gpt-4-turbo",
        "gpt-4"
    );
    
    // ========================================
    // INITIALIZATION
    // ========================================
    
    /**
     * Initialize OpenAI service lazily.
     */
    private OpenAiService getService() {
        if (openAiService == null) {
            synchronized (this) {
                if (openAiService == null) {
                    if (!config.getOpenai().isValid()) {
                        throw new IllegalStateException(
                            "OpenAI configuration is invalid: " + 
                            config.getValidationError()
                        );
                    }
                    
                    Duration timeout = config.getOpenai().getTimeout();
                    openAiService = new OpenAiService(
                        config.getOpenai().getApiKey(),
                        timeout
                    );
                    
                    log.info("OpenAI service initialized (timeout: {}s)", 
                        config.getOpenai().getTimeoutSeconds());
                }
            }
        }
        return openAiService;
    }
    
    // ========================================
    // PUBLIC API - COMPLETION
    // ========================================
    
    @Override
    public ProviderResponse complete(String modelId, String prompt, int maxTokens) 
        throws ProviderException {
        
        CompletionRequest request = CompletionRequest.builder()
            .modelId(modelId)
            .prompt(prompt)
            .maxTokens(maxTokens)
            .build();
        
        return complete(request);
    }
    
    @Override
    public ProviderResponse complete(CompletionRequest request) 
        throws ProviderException {
        
        long startTime = System.currentTimeMillis();
        
        // Validate
        validateRequest(request);
        
        log.debug("Calling OpenAI: model={}, maxTokens={}", 
            request.getModelId(), 
            request.getMaxTokens());
        
        // Retry logic
        int attempt = 0;
        int maxRetries = config.getOpenai().getMaxRetries();
        ProviderException lastException = null;
        
        while (attempt <= maxRetries) {
            try {
                // Make API call
                ChatCompletionResult result = callOpenAI(request);
                
                // Parse response
                ProviderResponse response = parseResponse(
                    result, 
                    request.getModelId(),
                    System.currentTimeMillis() - startTime
                );
                
                log.info("OpenAI completion successful: {} tokens in {}ms",
                    response.getTotalTokens(),
                    response.getLatencyMs());
                
                return response;
                
            } catch (ProviderException e) {
                lastException = e;
                
                // Check if retryable
                if (!e.isRetryable() || attempt >= maxRetries) {
                    log.error("OpenAI call failed (non-retryable or max retries): {}", 
                        e.getMessage());
                    throw e;
                }
                
                // Exponential backoff
                long delay = calculateBackoff(attempt);
                log.warn("OpenAI call failed (attempt {}/{}), retrying in {}ms: {}", 
                    attempt + 1, 
                    maxRetries,
                    delay,
                    e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                
                attempt++;
            }
        }
        
        // All retries exhausted
        throw lastException;
    }
    
    // ========================================
    // PRIVATE - API CALL
    // ========================================
    
    /**
     * Call OpenAI API.
     */
    private ChatCompletionResult callOpenAI(CompletionRequest request) 
        throws ProviderException {
        
        try {
            // Build OpenAI request
            ChatCompletionRequest.ChatCompletionRequestBuilder builder = 
                ChatCompletionRequest.builder()
                    .model(request.getModelId())
                    .messages(List.of(
                        new ChatMessage("user", request.getPrompt())
                    ))
                    .maxTokens(request.getMaxTokens());
            
            // Add optional parameters
            if (request.getTemperature() != null) {
                builder.temperature(request.getTemperature());
            }
            if (request.getTopP() != null) {
                builder.topP(request.getTopP());
            }
            if (request.getN() != null) {
                builder.n(request.getN());
            }
            if (request.getStopSequences() != null) {
                builder.stop(List.of(request.getStopSequences()));
            }
            
            ChatCompletionRequest openAiRequest = builder.build();
            
            // Make API call
            ChatCompletionResult result = getService()
                .createChatCompletion(openAiRequest);
            
            return result;
            
        } catch (com.theokanning.openai.OpenAiHttpException e) {
            // HTTP errors
            throw ProviderException.fromHttpStatus(
                e.statusCode,
                "OpenAI API error: " + e.getMessage()
            );
            
        } catch (Exception e) {
            // Unknown error
            throw new ProviderException(
                "Unexpected error calling OpenAI: " + e.getMessage(),
                e
            );
        }
    }
    
    // ========================================
    // PRIVATE - RESPONSE PARSING
    // ========================================
    
    /**
     * Parse OpenAI response into our ProviderResponse.
     */
    private ProviderResponse parseResponse(
        ChatCompletionResult result,
        String modelId,
        long latencyMs
    ) {
        // Extract text
        String text = result.getChoices().get(0).getMessage().getContent();
        
        // Extract token usage
        long inputTokens = result.getUsage().getPromptTokens();
        long outputTokens = result.getUsage().getCompletionTokens();
        
        // Extract finish reason
        String finishReason = result.getChoices().get(0).getFinishReason();
        
        // Estimate cost (simplified - you'd get actual pricing from ModelConfig)
        double estimatedCost = estimateCost(modelId, (int)inputTokens, (int)outputTokens);
        
        return ProviderResponse.builder()
            .text(text)
            .modelId(modelId)
            .inputTokens((int)inputTokens)
            .outputTokens((int)outputTokens)
            .latencyMs(latencyMs)
            .finishReason(finishReason)
            .estimatedCost(estimatedCost)
            .build();
    }
    
    /**
     * Estimate cost based on model and tokens.
     * 
     * Pricing (as of 2024):
     * - gpt-4o: $2.50/$10 per 1M tokens (input/output)
     * - gpt-4o-mini: $0.15/$0.60 per 1M tokens
     * - gpt-3.5-turbo: $0.50/$1.50 per 1M tokens
     */
    private double estimateCost(String modelId, int inputTokens, int outputTokens) {
        double inputCostPer1k, outputCostPer1k;
        
        switch (modelId) {
            case "gpt-4o":
                inputCostPer1k = 0.0025;
                outputCostPer1k = 0.01;
                break;
            
            case "gpt-4o-mini":
                inputCostPer1k = 0.00015;
                outputCostPer1k = 0.0006;
                break;
            
            case "gpt-3.5-turbo":
                inputCostPer1k = 0.0005;
                outputCostPer1k = 0.0015;
                break;
            
            default:
                // Unknown model, use gpt-4o-mini pricing as default
                inputCostPer1k = 0.00015;
                outputCostPer1k = 0.0006;
        }
        
        double inputCost = (inputTokens / 1000.0) * inputCostPer1k;
        double outputCost = (outputTokens / 1000.0) * outputCostPer1k;
        
        return inputCost + outputCost;
    }
    
    // ========================================
    // PRIVATE - VALIDATION
    // ========================================
    
    /**
     * Validate completion request.
     */
    private void validateRequest(CompletionRequest request) throws ProviderException {
        if (request.getPrompt() == null || request.getPrompt().isEmpty()) {
            throw ProviderException.invalidRequest("Prompt cannot be empty");
        }
        
        if (request.getMaxTokens() <= 0) {
            throw ProviderException.invalidRequest("maxTokens must be positive");
        }
        
        if (!supportsModel(request.getModelId())) {
            throw ProviderException.invalidRequest(
                "Model not supported: " + request.getModelId()
            );
        }
    }
    
    // ========================================
    // PRIVATE - RETRY LOGIC
    // ========================================
    
    /**
     * Calculate backoff delay with exponential backoff + jitter.
     * 
     * Formula: baseDelay * (2 ^ attempt) + random jitter
     */
    private long calculateBackoff(int attempt) {
        long baseDelay = config.getOpenai().getRetryDelayMs();
        long exponentialDelay = baseDelay * (1L << attempt); // 2^attempt
        long jitter = (long) (Math.random() * baseDelay);
        
        return exponentialDelay + jitter;
    }
    
    // ========================================
    // PUBLIC API - PROVIDER INFO
    // ========================================
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple health check - just verify we can initialize
            getService();
            return true;
        } catch (Exception e) {
            log.error("OpenAI provider not available", e);
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    @Override
    public boolean supportsModel(String modelId) {
        return SUPPORTED_MODELS.contains(modelId);
    }
    
    /**
     * Get list of supported models.
     */
    public Set<String> getSupportedModels() {
        return Set.copyOf(SUPPORTED_MODELS);
    }
}
