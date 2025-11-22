package io.com.llmguardian.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for routing requests to appropriate LLM models.
 * 
 * Makes intelligent decisions about which model to use based on:
 * - Prompt complexity
 * - Routing strategy
 * - Model availability
 * - Cost constraints
 * 
 * Routing Strategies:
 * 1. COMPLEXITY_BASED (default): Route based on complexity score
 * 2. COST_OPTIMIZED: Always use cheapest model
 * 3. PERFORMANCE_OPTIMIZED: Always use best model
 * 4. BALANCED: Balance cost and quality
 * 
 * @author LLMGuardian Team
 */
@Service
public class ModelRouter {
    
    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private ModelRegistry modelRegistry;
    
    // Default strategy (can be configured)
    private RoutingStrategy defaultStrategy = RoutingStrategy.COMPLEXITY_BASED;
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Route a request to the appropriate model.
     * Uses default routing strategy.
     * 
     * @param complexity The complexity score
     * @return Model decision with selected model
     */
    public ModelDecision route(ComplexityScore complexity) {
        return route(complexity, defaultStrategy);
    }
    
    /**
     * Route a request with explicit strategy.
     * 
     * @param complexity The complexity score
     * @param strategy Routing strategy to use
     * @return Model decision with selected model
     */
    public ModelDecision route(ComplexityScore complexity, RoutingStrategy strategy) {
        long startTime = System.currentTimeMillis();
        
        log.debug("Routing request with strategy: {}, complexity: {}", 
            strategy, 
            complexity.getLevel());
        
        // Select model based on strategy
        ModelConfig selectedModel = selectModel(complexity, strategy);
        
        // Build reasoning
        String reasoning = buildReasoning(complexity, strategy, selectedModel);
        
        long routingTime = System.currentTimeMillis() - startTime;
        
        ModelDecision decision = ModelDecision.builder()
            .modelId(selectedModel.getModelId())
            .modelName(selectedModel.getDisplayName())
            .reasoning(reasoning)
            .estimatedCostPer1kTokens(selectedModel.getCostPer1kInputTokens())
            .strategyUsed(strategy)
            .complexity(complexity)
            .routingTimeMs(routingTime)
            .build();
        
        log.info("Routing decision: {} ({}ms)", selectedModel.getModelId(), routingTime);
        
        return decision;
    }
    
    // ========================================
    // MODEL SELECTION LOGIC
    // ========================================
    
    /**
     * Select model based on complexity and strategy.
     */
    private ModelConfig selectModel(ComplexityScore complexity, RoutingStrategy strategy) {
        switch (strategy) {
            case COMPLEXITY_BASED:
                return selectByComplexity(complexity);
            
            case COST_OPTIMIZED:
                return selectCheapest();
            
            case PERFORMANCE_OPTIMIZED:
                return selectBest();
            
            case BALANCED:
                return selectBalanced(complexity);
            
            default:
                log.warn("Unknown strategy: {}, using COMPLEXITY_BASED", strategy);
                return selectByComplexity(complexity);
        }
    }
    
    /**
     * STRATEGY 1: Complexity-Based Routing (Default).
     * 
     * Rules:
     * - SIMPLE (0-30): gpt-4o-mini (fast, cheap)
     * - MEDIUM (31-60): gpt-4o-mini (can handle it)
     * - COMPLEX (61-100): gpt-4o (need power)
     */
    private ModelConfig selectByComplexity(ComplexityScore complexity) {
        ComplexityScore.ComplexityLevel level = complexity.getLevel();
        
        switch (level) {
            case SIMPLE:
            case MEDIUM:
                // Use cost-effective model
                return modelRegistry.getModel("gpt-4o-mini")
                    .orElse(modelRegistry.getDefaultModel());
            
            case COMPLEX:
                // Use powerful model
                return modelRegistry.getModel("gpt-4o")
                    .orElse(modelRegistry.getDefaultModel());
            
            default:
                return modelRegistry.getDefaultModel();
        }
    }
    
    /**
     * STRATEGY 2: Cost-Optimized.
     * Always use cheapest available model.
     */
    private ModelConfig selectCheapest() {
        return modelRegistry.getCheapestModel();
    }
    
    /**
     * STRATEGY 3: Performance-Optimized.
     * Always use most capable model.
     */
    private ModelConfig selectBest() {
        return modelRegistry.getMostCapableModel();
    }
    
    /**
     * STRATEGY 4: Balanced.
     * More aggressive cost optimization than COMPLEXITY_BASED.
     * 
     * Rules:
     * - SIMPLE (0-30): cheapest model
     * - MEDIUM (31-60): gpt-4o-mini
     * - COMPLEX (61-100): gpt-4o (only when really needed)
     */
    private ModelConfig selectBalanced(ComplexityScore complexity) {
        ComplexityScore.ComplexityLevel level = complexity.getLevel();
        
        switch (level) {
            case SIMPLE:
                // Use absolute cheapest
                return modelRegistry.getCheapestModel();
            
            case MEDIUM:
                // Use standard cost-effective model
                return modelRegistry.getModel("gpt-4o-mini")
                    .orElse(modelRegistry.getDefaultModel());
            
            case COMPLEX:
                // Only use premium for very high scores (75+)
                if (complexity.getScore() >= 75) {
                    return modelRegistry.getModel("gpt-4o")
                        .orElse(modelRegistry.getDefaultModel());
                } else {
                    // Try with gpt-4o-mini first
                    return modelRegistry.getModel("gpt-4o-mini")
                        .orElse(modelRegistry.getDefaultModel());
                }
            
            default:
                return modelRegistry.getDefaultModel();
        }
    }
    
    // ========================================
    // REASONING BUILDER
    // ========================================
    
    /**
     * Build human-readable reasoning for model selection.
     */
    private String buildReasoning(
        ComplexityScore complexity, 
        RoutingStrategy strategy, 
        ModelConfig selected
    ) {
        StringBuilder reasoning = new StringBuilder();
        
        // Strategy-specific reasoning
        switch (strategy) {
            case COMPLEXITY_BASED:
                reasoning.append(String.format(
                    "Complexity score %d (%s) → selected %s for optimal cost/quality balance",
                    complexity.getScore(),
                    complexity.getLevel(),
                    selected.getDisplayName()
                ));
                break;
            
            case COST_OPTIMIZED:
                reasoning.append(String.format(
                    "Cost optimization strategy → selected cheapest model: %s ($%.6f/1k)",
                    selected.getDisplayName(),
                    selected.getCostPer1kInputTokens()
                ));
                break;
            
            case PERFORMANCE_OPTIMIZED:
                reasoning.append(String.format(
                    "Performance optimization → selected most capable model: %s",
                    selected.getDisplayName()
                ));
                break;
            
            case BALANCED:
                reasoning.append(String.format(
                    "Balanced strategy with score %d → selected %s",
                    complexity.getScore(),
                    selected.getDisplayName()
                ));
                break;
        }
        
        return reasoning.toString();
    }
    
    // ========================================
    // CONFIGURATION
    // ========================================
    
    /**
     * Set default routing strategy.
     */
    public void setDefaultStrategy(RoutingStrategy strategy) {
        this.defaultStrategy = strategy;
        log.info("Default routing strategy set to: {}", strategy);
    }
    
    /**
     * Get current default strategy.
     */
    public RoutingStrategy getDefaultStrategy() {
        return defaultStrategy;
    }
    
    // ========================================
    // OPTIONAL: COST ESTIMATION
    // ========================================
    
    /**
     * Estimate cost for a routing decision.
     * 
     * @param decision The routing decision
     * @param estimatedInputTokens Estimated input tokens
     * @param estimatedOutputTokens Estimated output tokens
     * @return Estimated cost in USD
     */
    public double estimateCost(
        ModelDecision decision, 
        int estimatedInputTokens, 
        int estimatedOutputTokens
    ) {
        ModelConfig model = modelRegistry.getModelOrThrow(decision.getModelId());
        return model.estimateCost(estimatedInputTokens, estimatedOutputTokens);
    }
    
    /**
     * Compare cost between two routing strategies.
     * 
     * @param complexity The complexity score
     * @param strategy1 First strategy
     * @param strategy2 Second strategy
     * @param inputTokens Estimated input tokens
     * @param outputTokens Estimated output tokens
     * @return Cost difference (strategy1 - strategy2)
     */
    public double compareCost(
        ComplexityScore complexity,
        RoutingStrategy strategy1,
        RoutingStrategy strategy2,
        int inputTokens,
        int outputTokens
    ) {
        ModelDecision decision1 = route(complexity, strategy1);
        ModelDecision decision2 = route(complexity, strategy2);
        
        double cost1 = estimateCost(decision1, inputTokens, outputTokens);
        double cost2 = estimateCost(decision2, inputTokens, outputTokens);
        
        return cost1 - cost2;
    }
    
    // ========================================
    // OPTIONAL: VALIDATION
    // ========================================
    
    /**
     * Validate that selected model can handle complexity.
     * 
     * @param decision Routing decision
     * @return true if model is appropriate
     */
    public boolean validateDecision(ModelDecision decision) {
        ModelConfig model = modelRegistry.getModelOrThrow(decision.getModelId());
        ComplexityScore.ComplexityLevel level = decision.getComplexity().getLevel();
        
        return model.canHandle(level);
    }
}
