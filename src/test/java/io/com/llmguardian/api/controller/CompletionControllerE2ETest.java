package io.com.llmguardian.api.controller;

import io.com.llmguardian.BaseIntegrationTest;
import io.com.llmguardian.api.dto.CompletionRequest;
import io.com.llmguardian.provider.ProviderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CompletionControllerE2ETest extends BaseIntegrationTest {

    @Test
    public void testSuccessfulCompletion() throws Exception {
        // Mock Provider Response
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("This is a generated response.")
                .modelId("gpt-4o-mini")
                .inputTokens(10)
                .outputTokens(5)
                .latencyMs(100)
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);

        CompletionRequest request = new CompletionRequest("Hello, world!", 100);

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.text").value("This is a generated response."))
                .andExpect(jsonPath("$.metadata.modelUsed").value("gpt-4o-mini"));
    }

    @Test
    public void testPiiDetectionAndRedaction() throws Exception {
        // Mock Provider Response (Provider receives redacted text)
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("Response to redacted query.")
                .modelId("gpt-4o-mini")
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);

        // Input with PII (Email)
        CompletionRequest request = new CompletionRequest("Contact me at test@example.com", 100);

        mockMvc.perform(post("/api/v1/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.piiDetected").value(true))
                .andExpect(jsonPath("$.metadata.piiCount").value(1));
    }

    @Test
    public void testPromptOptimization() throws Exception {
        // Mock Provider Response
        ProviderResponse mockResponse = ProviderResponse.builder()
                .text("Optimized response.")
                .modelId("gpt-4o-mini")
                .build();

        when(llmProvider.complete(anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);

        // Input with filler words
        CompletionRequest request = new CompletionRequest("Basically, I just want to say hello actually.", 100);

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
