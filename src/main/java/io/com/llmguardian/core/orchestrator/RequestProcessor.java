package io.com.llmguardian.core.orchestrator;

import io.com.llmguardian.cache.CacheManager;
import io.com.llmguardian.optimization.OptimizationResult;
import io.com.llmguardian.optimization.PromptOptimizer;
import io.com.llmguardian.provider.LLMProvider;
import io.com.llmguardian.provider.ProviderException;
import io.com.llmguardian.provider.ProviderResponse;
import io.com.llmguardian.routing.ComplexityAnalyzer;
import io.com.llmguardian.routing.ComplexityScore;
import io.com.llmguardian.routing.ModelDecision;
import io.com.llmguardian.routing.ModelRouter;
import io.com.llmguardian.security.AuditLogger;
import io.com.llmguardian.security.PIIContext;
import io.com.llmguardian.security.PIIDetector;
import io.com.llmguardian.security.PIIRedactor;
import io.com.llmguardian.security.PIIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Main request processor - orchestrates the complete LLMGuardian pipeline.
 * 
 * Pipeline Flow:
 * 1. PII Detection & Redaction (Security)
 * 2. Prompt Optimization (Token reduction)
 * 3. Complexity Analysis (Routing)
 * 4. Model Selection (Routing)
 * 5. Cache Check (Cache)
 * 6. LLM API Call (Provider) [if cache miss]
 * 7. Cache Store (Cache)
 * 8. PII Restoration (Security)
 * 9. Audit Logging (Security)
 * 10. Response Assembly
 * 
 * Features:
 * - Transaction management
 * - Error handling at each stage
 * - Performance tracking
 * - Comprehensive logging
 * - Cost tracking
 * 
 * @author LLMGuardian Team
 */
@Service
public class RequestProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestProcessor.class);
    
    // ========================================
    // DEPENDENCIES - ALL MODULES
    // ========================================
    
    // Security Module
    @Autowired
    private PIIDetector piiDetector;
    
    @Autowired
    private PIIRedactor piiRedactor;
    
    @Autowired
    private PIIContext piiContext;
    
    @Autowired
    private AuditLogger auditLogger;
    
    // Optimization Module
    @Autowired
    private PromptOptimizer promptOptimizer;
    
    // Routing Module
    @Autowired
    private ComplexityAnalyzer complexityAnalyzer;
    
    @Autowired
    private ModelRouter modelRouter;
    
    // Cache Module
    @Autowired
    private CacheManager cacheManager;
    
    // Provider Module
    @Autowired
    private LLMProvider llmProvider;
    
    // ========================================
    // PUBLIC API - MAIN PROCESSING METHOD
    // ========================================
    
    /**
     * Process a completion request through the full pipeline.
     * 
     * @param request The completion request
     * @return Complete processing result
     */
    @Transactional
    public ProcessingResult process(CompletionRequest request) {
        long startTime = System.currentTimeMillis();
        UUID requestId = UUID.randomUUID();
        
        log.info("=== Starting request processing: {} ===", requestId);
        
        ProcessingResult.Builder resultBuilder = ProcessingResult.builder()
            .requestId(requestId)
            .originalQuery(request.getQuery());
        
        try {
            // ========================================
            // STAGE 1: PII DETECTION & REDACTION
            // ========================================
            long stage1Start = System.currentTimeMillis();
            
            log.debug("[Stage 1] PII Detection & Redaction");
            PIIResult piiResult = piiDetector.detect(request.getQuery());
            
            String redactedQuery = request.getQuery();
            if (piiResult.isDetected()) {
                log.info("PII detected: {} matches", piiResult.getTotalMatches());
                redactedQuery = piiRedactor.redact(request.getQuery(), piiResult);
                resultBuilder.piiDetected(true)
                    .piiCount(piiResult.getTotalMatches());
            } else {
                log.debug("No PII detected");
                resultBuilder.piiDetected(false);
            }
            
            long stage1Time = System.currentTimeMillis() - stage1Start;
            log.debug("[Stage 1] Complete: {}ms", stage1Time);
            
            // ========================================
            // STAGE 2: PROMPT OPTIMIZATION
            // ========================================
            long stage2Start = System.currentTimeMillis();
            
            log.debug("[Stage 2] Prompt Optimization");
            OptimizationResult optimization = promptOptimizer.optimize(redactedQuery);
            
            String optimizedPrompt = optimization.getOptimizedPrompt();
            log.info("Optimization: {} → {} tokens ({:.1f}% reduction)",
                optimization.getOriginalTokens(),
                optimization.getOptimizedTokens(),
                optimization.getReductionPercentage());
            
            resultBuilder.optimization(optimization);
            
            long stage2Time = System.currentTimeMillis() - stage2Start;
            log.debug("[Stage 2] Complete: {}ms", stage2Time);
            
            // ========================================
            // STAGE 3: COMPLEXITY ANALYSIS
            // ========================================
            long stage3Start = System.currentTimeMillis();
            
            log.debug("[Stage 3] Complexity Analysis");
            ComplexityScore complexity = complexityAnalyzer.analyze(optimizedPrompt);
            
            log.info("Complexity: {} (score: {})",
                complexity.getLevel(),
                complexity.getScore());
            
            resultBuilder.complexity(complexity);
            
            long stage3Time = System.currentTimeMillis() - stage3Start;
            log.debug("[Stage 3] Complete: {}ms", stage3Time);
            
            // ========================================
            // STAGE 4: MODEL ROUTING
            // ========================================
            long stage4Start = System.currentTimeMillis();
            
            log.debug("[Stage 4] Model Routing");
            ModelDecision routing = modelRouter.route(complexity);
            
            log.info("Routing: selected {} ({})",
                routing.getModelId(),
                routing.getReasoning());
            
            resultBuilder.routing(routing);
            
            long stage4Time = System.currentTimeMillis() - stage4Start;
            log.debug("[Stage 4] Complete: {}ms", stage4Time);
            
            // ========================================
            // STAGE 5: CACHE CHECK
            // ========================================
            long stage5Start = System.currentTimeMillis();
            
            log.debug("[Stage 5] Cache Check");
            Optional<String> cachedResponse = cacheManager.get(
                optimizedPrompt,
                routing.getModelId()
            );
            
            String responseText;
            boolean fromCache;
            ProviderResponse providerResponse = null;
            
            if (cachedResponse.isPresent()) {
                // CACHE HIT
                log.info("Cache HIT - returning cached response");
                responseText = cachedResponse.get();
                fromCache = true;
                
                long stage5Time = System.currentTimeMillis() - stage5Start;
                log.debug("[Stage 5] Complete (cache hit): {}ms", stage5Time);
                
            } else {
                // CACHE MISS - Call LLM
                log.info("Cache MISS - calling LLM provider");
                fromCache = false;
                
                long stage5Time = System.currentTimeMillis() - stage5Start;
                log.debug("[Stage 5] Complete (cache miss): {}ms", stage5Time);
                
                // ========================================
                // STAGE 6: LLM PROVIDER CALL
                // ========================================
                long stage6Start = System.currentTimeMillis();
                
                log.debug("[Stage 6] LLM Provider Call");
                providerResponse = llmProvider.complete(
                    routing.getModelId(),
                    optimizedPrompt,
                    request.getMaxTokens()
                );
                
                responseText = providerResponse.getText();
                
                log.info("LLM response: {} tokens in {}ms, cost: ${}",
                    providerResponse.getTotalTokens(),
                    providerResponse.getLatencyMs(),
                    providerResponse.getEstimatedCost());
                
                resultBuilder.providerResponse(providerResponse);
                
                long stage6Time = System.currentTimeMillis() - stage6Start;
                log.debug("[Stage 6] Complete: {}ms", stage6Time);
                
                // ========================================
                // STAGE 7: CACHE STORE
                // ========================================
                long stage7Start = System.currentTimeMillis();
                
                log.debug("[Stage 7] Cache Store");
                cacheManager.put(optimizedPrompt, routing.getModelId(), responseText);
                
                long stage7Time = System.currentTimeMillis() - stage7Start;
                log.debug("[Stage 7] Complete: {}ms", stage7Time);
            }
            
            resultBuilder.fromCache(fromCache);
            
            // ========================================
            // STAGE 8: PII RESTORATION
            // ========================================
            long stage8Start = System.currentTimeMillis();
            
            log.debug("[Stage 8] PII Restoration");
            if (piiResult.isDetected()) {
                responseText = piiRedactor.restore(responseText);
                log.debug("PII restored in response");
            }
            
            long stage8Time = System.currentTimeMillis() - stage8Start;
            log.debug("[Stage 8] Complete: {}ms", stage8Time);
            
            // ========================================
            // STAGE 9: AUDIT LOGGING
            // ========================================
            long stage9Start = System.currentTimeMillis();
            
            log.debug("[Stage 9] Audit Logging");
            if (piiResult.isDetected()) {
                auditLogger.logDetections(piiContext);
            }
            
            long stage9Time = System.currentTimeMillis() - stage9Start;
            log.debug("[Stage 9] Complete: {}ms", stage9Time);
            
            // ========================================
            // STAGE 10: RESPONSE ASSEMBLY
            // ========================================
            long totalTime = System.currentTimeMillis() - startTime;
            
            ProcessingResult result = resultBuilder
                .responseText(responseText)
                .totalLatencyMs(totalTime)
                .success(true)
                .build();
            
            log.info("=== Request processing complete: {} ({} ms) ===",
                requestId,
                totalTime);
            log.info("Summary: cached={}, pii={}, optimized={}→{} tokens, complexity={}, model={}",
                fromCache,
                piiResult.isDetected(),
                optimization.getOriginalTokens(),
                optimization.getOptimizedTokens(),
                complexity.getLevel(),
                routing.getModelId());
            
            return result;
            
        } catch (ProviderException e) {
            // Provider-specific error
            log.error("Provider error during request processing: {}", e.getMessage(), e);
            
            return resultBuilder
                .success(false)
                .error(e.getMessage())
                .errorType("PROVIDER_ERROR")
                .totalLatencyMs(System.currentTimeMillis() - startTime)
                .build();
            
        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error during request processing: {}", e.getMessage(), e);
            
            return resultBuilder
                .success(false)
                .error(e.getMessage())
                .errorType("INTERNAL_ERROR")
                .totalLatencyMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    // ========================================
    // NESTED CLASS: COMPLETION REQUEST
    // ========================================
    
    /**
     * Request object for processing.
     */
    public static class CompletionRequest {
        private final String query;
        private final int maxTokens;
        
        public CompletionRequest(String query, int maxTokens) {
            this.query = query;
            this.maxTokens = maxTokens;
        }
        
        public String getQuery() {
            return query;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public static CompletionRequest of(String query, int maxTokens) {
            return new CompletionRequest(query, maxTokens);
        }
        
        public static CompletionRequest of(String query) {
            return new CompletionRequest(query, 1000);
        }
    }
    
    // ========================================
    // NESTED CLASS: PROCESSING RESULT
    // ========================================
    
    /**
     * Complete processing result with all metadata.
     */
    public static class ProcessingResult {
        private final UUID requestId;
        private final String originalQuery;
        private final String responseText;
        private final boolean success;
        private final String error;
        private final String errorType;
        
        // Metadata
        private final boolean piiDetected;
        private final int piiCount;
        private final OptimizationResult optimization;
        private final ComplexityScore complexity;
        private final ModelDecision routing;
        private final ProviderResponse providerResponse;
        private final boolean fromCache;
        private final long totalLatencyMs;
        
        private ProcessingResult(Builder builder) {
            this.requestId = builder.requestId;
            this.originalQuery = builder.originalQuery;
            this.responseText = builder.responseText;
            this.success = builder.success;
            this.error = builder.error;
            this.errorType = builder.errorType;
            this.piiDetected = builder.piiDetected;
            this.piiCount = builder.piiCount;
            this.optimization = builder.optimization;
            this.complexity = builder.complexity;
            this.routing = builder.routing;
            this.providerResponse = builder.providerResponse;
            this.fromCache = builder.fromCache;
            this.totalLatencyMs = builder.totalLatencyMs;
        }
        
        // Getters
        public UUID getRequestId() { return requestId; }
        public String getOriginalQuery() { return originalQuery; }
        public String getResponseText() { return responseText; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public String getErrorType() { return errorType; }
        public boolean isPiiDetected() { return piiDetected; }
        public int getPiiCount() { return piiCount; }
        public OptimizationResult getOptimization() { return optimization; }
        public ComplexityScore getComplexity() { return complexity; }
        public ModelDecision getRouting() { return routing; }
        public ProviderResponse getProviderResponse() { return providerResponse; }
        public boolean isFromCache() { return fromCache; }
        public long getTotalLatencyMs() { return totalLatencyMs; }
        
        // Builder
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private UUID requestId;
            private String originalQuery;
            private String responseText;
            private boolean success;
            private String error;
            private String errorType;
            private boolean piiDetected;
            private int piiCount;
            private OptimizationResult optimization;
            private ComplexityScore complexity;
            private ModelDecision routing;
            private ProviderResponse providerResponse;
            private boolean fromCache;
            private long totalLatencyMs;
            
            public Builder requestId(UUID requestId) {
                this.requestId = requestId;
                return this;
            }
            
            public Builder originalQuery(String originalQuery) {
                this.originalQuery = originalQuery;
                return this;
            }
            
            public Builder responseText(String responseText) {
                this.responseText = responseText;
                return this;
            }
            
            public Builder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public Builder error(String error) {
                this.error = error;
                return this;
            }
            
            public Builder errorType(String errorType) {
                this.errorType = errorType;
                return this;
            }
            
            public Builder piiDetected(boolean piiDetected) {
                this.piiDetected = piiDetected;
                return this;
            }
            
            public Builder piiCount(int piiCount) {
                this.piiCount = piiCount;
                return this;
            }
            
            public Builder optimization(OptimizationResult optimization) {
                this.optimization = optimization;
                return this;
            }
            
            public Builder complexity(ComplexityScore complexity) {
                this.complexity = complexity;
                return this;
            }
            
            public Builder routing(ModelDecision routing) {
                this.routing = routing;
                return this;
            }
            
            public Builder providerResponse(ProviderResponse providerResponse) {
                this.providerResponse = providerResponse;
                return this;
            }
            
            public Builder fromCache(boolean fromCache) {
                this.fromCache = fromCache;
                return this;
            }
            
            public Builder totalLatencyMs(long totalLatencyMs) {
                this.totalLatencyMs = totalLatencyMs;
                return this;
            }
            
            public ProcessingResult build() {
                return new ProcessingResult(this);
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "ProcessingResult{id=%s, success=%s, cached=%s, latency=%dms}",
                requestId,
                success,
                fromCache,
                totalLatencyMs
            );
        }
    }
}