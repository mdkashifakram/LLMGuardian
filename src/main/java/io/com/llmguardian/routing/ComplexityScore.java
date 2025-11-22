package io.com.llmguardian.routing;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of complexity analysis on a prompt.
 * 
 * Contains:
 * - Overall complexity score (0-100)
 * - Complexity level (SIMPLE, MEDIUM, COMPLEX)
 * - Individual factor scores
 * - Reasoning for the score
 * 
 * Scoring Breakdown:
 * - 0-30: SIMPLE (basic queries, greetings, simple facts)
 * - 31-60: MEDIUM (multi-step reasoning, code explanation)
 * - 61-100: COMPLEX (deep analysis, multi-domain reasoning, creative tasks)
 * 
 * @author LLMGuardian Team
 */
public class ComplexityScore {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final int score;                    // 0-100
    private final ComplexityLevel level;        // SIMPLE, MEDIUM, COMPLEX
    private final Map<String, Integer> factors; // Individual factor scores
    private final String reasoning;             // Why this score?
    private final long analysisTimeMs;          // Time taken to analyze
    
    // ========================================
    // COMPLEXITY LEVELS
    // ========================================
    
    public enum ComplexityLevel {
        SIMPLE,     // 0-30: Use cheap model
        MEDIUM,     // 31-60: Use cheap model (can handle it)
        COMPLEX     // 61-100: Use powerful model
    }
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private ComplexityScore(Builder builder) {
        this.score = builder.score;
        this.level = determineLevel(builder.score);
        this.factors = new HashMap<>(builder.factors);
        this.reasoning = builder.reasoning;
        this.analysisTimeMs = builder.analysisTimeMs;
    }
    
    /**
     * Determine complexity level from score.
     */
    private ComplexityLevel determineLevel(int score) {
        if (score <= 30) return ComplexityLevel.SIMPLE;
        if (score <= 60) return ComplexityLevel.MEDIUM;
        return ComplexityLevel.COMPLEX;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public int getScore() {
        return score;
    }
    
    public ComplexityLevel getLevel() {
        return level;
    }
    
    public Map<String, Integer> getFactors() {
        return new HashMap<>(factors);
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }
    
    /**
     * Check if this is a simple query.
     */
    public boolean isSimple() {
        return level == ComplexityLevel.SIMPLE;
    }
    
    /**
     * Check if this is a complex query.
     */
    public boolean isComplex() {
        return level == ComplexityLevel.COMPLEX;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "ComplexityScore{score=%d, level=%s, factors=%s, time=%dms}",
            score,
            level,
            factors,
            analysisTimeMs
        );
    }
    
    /**
     * Get human-readable summary.
     */
    public String getSummary() {
        return String.format(
            "Complexity: %s (score: %d/100) - %s",
            level,
            score,
            reasoning
        );
    }
    
    // ========================================
    // BUILDER PATTERN
    // ========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int score;
        private Map<String, Integer> factors = new HashMap<>();
        private String reasoning = "";
        private long analysisTimeMs;
        
        public Builder score(int score) {
            this.score = Math.max(0, Math.min(100, score)); // Clamp 0-100
            return this;
        }
        
        public Builder addFactor(String name, int score) {
            this.factors.put(name, score);
            return this;
        }
        
        public Builder factors(Map<String, Integer> factors) {
            this.factors = new HashMap<>(factors);
            return this;
        }
        
        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }
        
        public Builder analysisTimeMs(long analysisTimeMs) {
            this.analysisTimeMs = analysisTimeMs;
            return this;
        }
        
        public ComplexityScore build() {
            return new ComplexityScore(this);
        }
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    /**
     * Create simple complexity score.
     */
    public static ComplexityScore simple(String reasoning) {
        return builder()
            .score(15)
            .reasoning(reasoning)
            .build();
    }
    
    /**
     * Create medium complexity score.
     */
    public static ComplexityScore medium(String reasoning) {
        return builder()
            .score(45)
            .reasoning(reasoning)
            .build();
    }
    
    /**
     * Create complex complexity score.
     */
    public static ComplexityScore complex(String reasoning) {
        return builder()
            .score(75)
            .reasoning(reasoning)
            .build();
    }
}
