package io.com.llmguardian.routing;

/**
 * Strategy for routing requests to LLM models.
 * 
 * Determines which factors to consider when selecting a model:
 * - COMPLEXITY_BASED: Route based on prompt complexity score
 * - COST_OPTIMIZED: Always choose cheapest model
 * - PERFORMANCE_OPTIMIZED: Always choose fastest/best model
 * - BALANCED: Balance between cost and quality
 * 
 * @author LLMGuardian Team
 */
public enum RoutingStrategy {
    
    /**
     * Route based on complexity analysis (RECOMMENDED).
     * - Simple queries (0-30) → gpt-4o-mini
     * - Medium queries (31-60) → gpt-4o-mini
     * - Complex queries (61-100) → gpt-4o
     */
    COMPLEXITY_BASED,
    
    /**
     * Always use cheapest model regardless of complexity.
     * Use case: Cost-sensitive applications
     */
    COST_OPTIMIZED,
    
    /**
     * Always use best model regardless of cost.
     * Use case: Quality-critical applications
     */
    PERFORMANCE_OPTIMIZED,
    
    /**
     * Balance cost and quality based on complexity.
     * More aggressive cost optimization than COMPLEXITY_BASED.
     */
    BALANCED;
    
    /**
     * Get default strategy.
     */
    public static RoutingStrategy getDefault() {
        return COMPLEXITY_BASED;
    }
}
