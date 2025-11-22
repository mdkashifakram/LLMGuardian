package io.com.llmguardian.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry of available LLM models.
 * 
 * Responsibilities:
 * - Store all configured models
 * - Provide lookup by ID
 * - Filter models by capability, cost, etc.
 * - Manage model availability
 * 
 * @author LLMGuardian Team
 */
@Component
public class ModelRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final Map<String, ModelConfig> models;  // modelId → config
    private final String defaultModelId;            // Fallback model
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public ModelRegistry() {
        this.models = new HashMap<>();
        this.defaultModelId = "gpt-4o-mini";
        
        // Register default models
        registerDefaultModels();
        
        log.info("ModelRegistry initialized with {} models", models.size());
    }
    
    /**
     * Register default OpenAI models.
     */
    private void registerDefaultModels() {
        register(ModelConfig.gpt4oMini());      // Primary: cost-effective
        register(ModelConfig.gpt4o());          // Premium: complex tasks
        register(ModelConfig.gpt35Turbo());     // Legacy: very cheap
        
        log.debug("Registered default models: gpt-4o-mini, gpt-4o, gpt-3.5-turbo");
    }
    
    // ========================================
    // PUBLIC API - REGISTRATION
    // ========================================
    
    /**
     * Register a model in the registry.
     * 
     * @param model Model configuration
     */
    public void register(ModelConfig model) {
        models.put(model.getModelId(), model);
        log.debug("Registered model: {}", model.getModelId());
    }
    
    /**
     * Register multiple models.
     */
    public void registerAll(List<ModelConfig> modelList) {
        modelList.forEach(this::register);
    }
    
    /**
     * Unregister a model.
     */
    public void unregister(String modelId) {
        models.remove(modelId);
        log.debug("Unregistered model: {}", modelId);
    }
    
    // ========================================
    // PUBLIC API - LOOKUP
    // ========================================
    
    /**
     * Get model by ID.
     * 
     * @param modelId Model identifier
     * @return Optional containing model config
     */
    public Optional<ModelConfig> getModel(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }
    
    /**
     * Get model by ID or throw exception.
     * 
     * @param modelId Model identifier
     * @return Model config
     * @throws IllegalArgumentException if model not found
     */
    public ModelConfig getModelOrThrow(String modelId) {
        return getModel(modelId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Model not found: " + modelId
            ));
    }
    
    /**
     * Get default model (fallback).
     */
    public ModelConfig getDefaultModel() {
        return getModelOrThrow(defaultModelId);
    }
    
    /**
     * Get all registered models.
     */
    public List<ModelConfig> getAllModels() {
        return new ArrayList<>(models.values());
    }
    
    /**
     * Get all enabled models.
     */
    public List<ModelConfig> getEnabledModels() {
        return models.values().stream()
            .filter(ModelConfig::isEnabled)
            .collect(Collectors.toList());
    }
    
    // ========================================
    // PUBLIC API - FILTERING
    // ========================================
    
    /**
     * Get models by capability level.
     * 
     * @param capability Minimum capability required
     * @return List of models with at least this capability
     */
    public List<ModelConfig> getModelsByCapability(ModelConfig.ModelCapability capability) {
        return getEnabledModels().stream()
            .filter(model -> isCapabilitySufficient(model.getCapability(), capability))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if model capability is sufficient for requirement.
     */
    private boolean isCapabilitySufficient(
        ModelConfig.ModelCapability modelCap,
        ModelConfig.ModelCapability requiredCap
    ) {
        int modelLevel = getCapabilityLevel(modelCap);
        int requiredLevel = getCapabilityLevel(requiredCap);
        return modelLevel >= requiredLevel;
    }
    
    /**
     * Convert capability to numeric level (for comparison).
     */
    private int getCapabilityLevel(ModelConfig.ModelCapability capability) {
        switch (capability) {
            case BASIC: return 1;
            case STANDARD: return 2;
            case ADVANCED: return 3;
            default: return 0;
        }
    }
    
    /**
     * Get models that can handle a complexity level.
     */
    public List<ModelConfig> getModelsForComplexity(ComplexityScore.ComplexityLevel level) {
        return getEnabledModels().stream()
            .filter(model -> model.canHandle(level))
            .collect(Collectors.toList());
    }
    
    /**
     * Get cheapest model.
     */
    public ModelConfig getCheapestModel() {
        return getEnabledModels().stream()
            .min(Comparator.comparingDouble(ModelConfig::getCostPer1kInputTokens))
            .orElse(getDefaultModel());
    }
    
    /**
     * Get most capable model.
     */
    public ModelConfig getMostCapableModel() {
        return getEnabledModels().stream()
            .max(Comparator.comparing(model -> getCapabilityLevel(model.getCapability())))
            .orElse(getDefaultModel());
    }
    
    /**
     * Get cheapest model that can handle complexity level.
     */
    public ModelConfig getCheapestForComplexity(ComplexityScore.ComplexityLevel level) {
        return getModelsForComplexity(level).stream()
            .min(Comparator.comparingDouble(ModelConfig::getCostPer1kInputTokens))
            .orElse(getDefaultModel());
    }
    
    /**
     * Get cost-effective models (< $0.001 per 1k tokens).
     */
    public List<ModelConfig> getCostEffectiveModels() {
        return getEnabledModels().stream()
            .filter(ModelConfig::isCostEffective)
            .collect(Collectors.toList());
    }
    
    // ========================================
    // PUBLIC API - VALIDATION
    // ========================================
    
    /**
     * Check if a model exists.
     */
    public boolean hasModel(String modelId) {
        return models.containsKey(modelId);
    }
    
    /**
     * Check if a model is enabled.
     */
    public boolean isModelEnabled(String modelId) {
        return getModel(modelId)
            .map(ModelConfig::isEnabled)
            .orElse(false);
    }
    
    /**
     * Get count of registered models.
     */
    public int getModelCount() {
        return models.size();
    }
    
    /**
     * Get count of enabled models.
     */
    public int getEnabledModelCount() {
        return (int) models.values().stream()
            .filter(ModelConfig::isEnabled)
            .count();
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "ModelRegistry{total=%d, enabled=%d, default=%s}",
            getModelCount(),
            getEnabledModelCount(),
            defaultModelId
        );
    }
    
    /**
     * Get summary of all models.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MODEL REGISTRY ===\n");
        sb.append(String.format("Total Models: %d\n", getModelCount()));
        sb.append(String.format("Enabled Models: %d\n", getEnabledModelCount()));
        sb.append(String.format("Default Model: %s\n\n", defaultModelId));
        
        sb.append("Available Models:\n");
        getAllModels().forEach(model -> {
            sb.append(String.format("  - %s: %s (%.6f/%.6f) [%s]\n",
                model.getModelId(),
                model.getDisplayName(),
                model.getCostPer1kInputTokens(),
                model.getCostPer1kOutputTokens(),
                model.isEnabled() ? "ENABLED" : "DISABLED"
            ));
        });
        
        return sb.toString();
    }
}
