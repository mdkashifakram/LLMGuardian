package io.com.llmguardian.api.controller;

import io.com.llmguardian.api.dto.CompletionRequest;
import io.com.llmguardian.api.dto.CompletionResponse;
import io.com.llmguardian.core.orchestrator.RequestProcessor;
import io.com.llmguardian.core.orchestrator.RequestProcessor.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST controller for LLM completion requests.
 * 
 * Endpoints:
 * - POST /api/v1/completions - Main completion endpoint
 * - GET /api/v1/health - Health check
 * 
 * This is the main entry point for external clients.
 * 
 * @author LLMGuardian Team
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@CrossOrigin(origins = "*")  // Configure appropriately for production
public class CompletionController {
    
    private static final Logger log = LoggerFactory.getLogger(CompletionController.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private RequestProcessor requestProcessor;
    
    // ========================================
    // ENDPOINTS
    // ========================================
    
    /**
     * Main completion endpoint.
     * 
     * POST /api/v1/completions
     * 
     * Example request:
     * <pre>
     * {
     *   "query": "Write an email about the project",
     *   "maxTokens": 500
     * }
     * </pre>
     * 
     * @param request Completion request
     * @return Completion response with generated text
     */
    @PostMapping("/completions")
    public ResponseEntity<CompletionResponse> complete(
        @Valid @RequestBody CompletionRequest request
    ) {
        log.info("Received completion request: {}", request);
        
        try {
            // Convert API request to internal request
            RequestProcessor.CompletionRequest internalRequest = 
                RequestProcessor.CompletionRequest.of(
                    request.getQuery(),
                    request.getEffectiveMaxTokens()
                );
            
            // Process through pipeline
            ProcessingResult result = requestProcessor.process(internalRequest);
            
            // Convert internal result to API response
            CompletionResponse response = convertToApiResponse(result);
            
            // Return appropriate HTTP status
            if (result.isSuccess()) {
                log.info("Completion successful: requestId={}", result.getRequestId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Completion failed: requestId={}, error={}", 
                    result.getRequestId(), 
                    result.getError());
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
            }
            
        } catch (IllegalArgumentException e) {
            // Validation error
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CompletionResponse.error(null, "Invalid request: " + e.getMessage()));
            
        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error processing completion", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CompletionResponse.error(null, "Internal server error"));
        }
    }
    
    /**
     * Health check endpoint.
     * 
     * GET /api/v1/health
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        log.debug("Health check requested");
        
        HealthResponse health = new HealthResponse();
        health.setStatus("UP");
        health.setService("LLMGuardian");
        health.setVersion("1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    // ========================================
    // CONVERSION METHODS
    // ========================================
    
    /**
     * Convert internal ProcessingResult to API CompletionResponse.
     */
    private CompletionResponse convertToApiResponse(ProcessingResult result) {
        if (!result.isSuccess()) {
            return CompletionResponse.error(
                result.getRequestId(),
                result.getError()
            );
        }
        
        // Build metadata
        CompletionResponse.ResponseMetadata metadata = new CompletionResponse.ResponseMetadata();
        
        // Model information
        if (result.getRouting() != null) {
            metadata.setModelUsed(result.getRouting().getModelId());
        }
        
        if (result.getComplexity() != null) {
            metadata.setComplexityLevel(result.getComplexity().getLevel().name());
        }
        
        // Token usage
        if (result.getProviderResponse() != null) {
            metadata.setInputTokens(result.getProviderResponse().getInputTokens());
            metadata.setOutputTokens(result.getProviderResponse().getOutputTokens());
            metadata.setTotalTokens(result.getProviderResponse().getTotalTokens());
            metadata.setEstimatedCost(result.getProviderResponse().getEstimatedCost());
        }
        
        // Performance
        metadata.setLatencyMs(result.getTotalLatencyMs());
        metadata.setFromCache(result.isFromCache());
        
        // Optimization
        if (result.getOptimization() != null) {
            metadata.setOptimizationApplied(result.getOptimization().wasOptimized());
            metadata.setTokensSaved(result.getOptimization().getTokensSaved());
            metadata.setReductionPercentage(result.getOptimization().getReductionPercentage());
        }
        
        // Security
        metadata.setPiiDetected(result.isPiiDetected());
        metadata.setPiiCount(result.getPiiCount());
        
        // Build response
        return CompletionResponse.success(
            result.getRequestId(),
            result.getResponseText(),
            metadata
        );
    }
    
    // ========================================
    // EXCEPTION HANDLERS
    // ========================================
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<CompletionResponse> handleValidationError(
        org.springframework.web.bind.MethodArgumentNotValidException ex
    ) {
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        
        log.warn("Validation error: {}", errorMessage);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(CompletionResponse.error(null, errorMessage));
    }
    
    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CompletionResponse> handleGeneralError(Exception ex) {
        log.error("Unexpected error in controller", ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(CompletionResponse.error(null, "Internal server error"));
    }
    
    // ========================================
    // NESTED CLASS: HEALTH RESPONSE
    // ========================================
    
    public static class HealthResponse {
        private String status;
        private String service;
        private String version;
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getService() {
            return service;
        }
        
        public void setService(String service) {
            this.service = service;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
    }
}
