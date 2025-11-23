package io.com.llmguardian.api.controller;

import io.com.llmguardian.BaseIntegrationTest;
import io.com.llmguardian.api.dto.CompletionRequest;
import io.com.llmguardian.provider.ProviderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CompletionControllerE2ETest extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
    }

    @Test
    public void testSuccessfulCompletion_EndToEnd() throws Exception {
        // 1. Mock Provider Response
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("This is a generated response.")
                .modelId("gpt-4o-mini")
                .inputTokens(10)
                .outputTokens(5)
                .latencyMs(100)
                .estimatedCost(0.0001)
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);
        
        // Mock Cache Miss
        when(l2CacheService.get(anyString())).thenReturn(Optional.empty());

        // 2. Perform Request
        CompletionRequest request = new CompletionRequest("Hello, world!", 100);

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.text").value("This is a generated response."))
                .andExpect(jsonPath("$.metadata.modelUsed").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.metadata.totalTokens").value(15))
                .andExpect(jsonPath("$.metadata.estimatedCost").value(0.0001));
        
        // 3. Verify Provider Called
        verify(llmProvider, times(1)).complete(anyString(), anyString(), anyInt());
    }

    @Test
    public void testPiiDetectionAndAuditLogging() throws Exception {
        // 1. Mock Provider Response (Provider receives redacted text)
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("Response to redacted query.")
                .modelId("gpt-4o-mini")
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);
        
        // Mock Cache Miss
        when(l2CacheService.get(anyString())).thenReturn(Optional.empty());

        // 2. Input with PII (Valid Email)
        CompletionRequest request = new CompletionRequest("Contact me at john.doe@example.com regarding the project.", 100);

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.piiDetected").value(true))
                .andExpect(jsonPath("$.metadata.piiCount").value(1));

        // 3. Verify Audit Log Created
        assertThat(auditRepository.count()).isEqualTo(1);
    }

    @Test
    public void testCacheHitScenario() throws Exception {
        // 1. Mock Provider Response for first call
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("Original response.")
                .modelId("gpt-4o-mini")
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);

        CompletionRequest request = new CompletionRequest("Repeat this query.", 100);

        // 2. First Call (Cache Miss)
        when(l2CacheService.get(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.fromCache").value(false));

        // 3. Second Call (Cache Hit)
        // Prepare cached response JSON
        ProviderResponse cachedResponse = ProviderResponse.builder()
                .text("Original response.")
                .modelId("gpt-4o-mini")
                .build();
        String cachedJson = objectMapper.writeValueAsString(cachedResponse);
        
        when(l2CacheService.get(anyString())).thenReturn(Optional.of(cachedJson));

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.fromCache").value(true))
                .andExpect(jsonPath("$.text").value("Original response."));

        // 4. Verify Provider Called ONLY ONCE (for the first request)
        verify(llmProvider, times(1)).complete(anyString(), anyString(), anyInt());
    }

    @Test
    public void testPromptOptimization() throws Exception {
        // 1. Mock Provider Response
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("Optimized response.")
                .modelId("gpt-4o-mini")
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);
        
        // Mock Cache Miss
        when(l2CacheService.get(anyString())).thenReturn(Optional.empty());

        // 2. Input with filler words (Must be > 50 chars)
        String longPrompt = "So basically, I was wondering if you could possibly help me write an email actually about the project status.";
        CompletionRequest request = new CompletionRequest(longPrompt, 100);

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.optimizationApplied").value(true));
    }

    @Test
    public void testInvalidRequest() throws Exception {
        // Empty query
        CompletionRequest request = new CompletionRequest("");

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
