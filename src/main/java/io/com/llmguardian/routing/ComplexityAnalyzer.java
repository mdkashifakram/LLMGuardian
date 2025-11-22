package io.com.llmguardian.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for analyzing prompt complexity.
 * 
 * Analyzes prompts to determine their complexity (0-100 score):
 * - 0-30: SIMPLE (basic queries, greetings, simple facts)
 * - 31-60: MEDIUM (multi-step, explanations, simple code)
 * - 61-100: COMPLEX (deep analysis, creative tasks, complex reasoning)
 * 
 * Analysis Factors:
 * 1. Token Count (30 points max)
 * 2. Reasoning Complexity (40 points max)
 * 3. Technical Complexity (30 points max)
 * 
 * @author LLMGuardian Team
 */
@Service
public class ComplexityAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(ComplexityAnalyzer.class);
    
    // ========================================
    // COMPLEXITY PATTERNS
    // ========================================
    
    // Reasoning keywords (indicate multi-step thinking)
    private static final Pattern REASONING_PATTERN = Pattern.compile(
        "\\b(analyze|compare|evaluate|explain|describe|why|how|" +
        "consider|reasoning|logic|conclusion|therefore|because|" +
        "pros and cons|advantages|disadvantages|trade-off)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // Technical keywords (indicate specialized domain)
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile(
        "\\b(algorithm|implementation|architecture|database|api|" +
        "framework|optimization|debugging|testing|deployment|" +
        "machine learning|neural network|regression|classification|" +
        "concurrent|asynchronous|thread|process|memory leak)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // Code indicators
    private static final Pattern CODE_PATTERN = Pattern.compile(
        "(```|function|class|def |import |public |private |" +
        "void |int |string |return |if\\(|for\\(|while\\()",
        Pattern.CASE_INSENSITIVE
    );
    
    // Creative/complex task indicators
    private static final Pattern CREATIVE_PATTERN = Pattern.compile(
        "\\b(write|create|design|compose|generate|build|develop|" +
        "story|poem|essay|article|script|plan|strategy)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // Multi-step indicators
    private static final Pattern MULTISTEP_PATTERN = Pattern.compile(
        "\\b(first|second|third|then|next|finally|step|phase|" +
        "and then|after that|following that)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Analyze prompt complexity.
     * 
     * @param prompt The prompt to analyze
     * @return ComplexityScore with detailed breakdown
     */
    public ComplexityScore analyze(String prompt) {
        long startTime = System.currentTimeMillis();
        
        // Validate input
        if (prompt == null || prompt.trim().isEmpty()) {
            log.warn("Empty prompt provided, returning minimum complexity");
            return ComplexityScore.simple("Empty prompt");
        }
        
        log.debug("Analyzing complexity for prompt of length: {}", prompt.length());
        
        // Calculate individual factor scores
        Map<String, Integer> factors = new HashMap<>();
        
        int tokenScore = analyzeTokenCount(prompt);
        factors.put("Token Count", tokenScore);
        
        int reasoningScore = analyzeReasoning(prompt);
        factors.put("Reasoning", reasoningScore);
        
        int technicalScore = analyzeTechnical(prompt);
        factors.put("Technical", technicalScore);
        
        // Total score (0-100)
        int totalScore = tokenScore + reasoningScore + technicalScore;
        
        // Build reasoning text
        String reasoning = buildReasoning(totalScore, factors);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        ComplexityScore result = ComplexityScore.builder()
            .score(totalScore)
            .factors(factors)
            .reasoning(reasoning)
            .analysisTimeMs(analysisTime)
            .build();
        
        log.info("Complexity analysis complete: {} ({}ms)", result.getLevel(), analysisTime);
        
        return result;
    }
    
    // ========================================
    // FACTOR 1: TOKEN COUNT (0-30 points)
    // ========================================
    
    /**
     * Analyze token count.
     * 
     * Scoring:
     * - < 50 tokens: 5 points (very short)
     * - 50-100 tokens: 10 points (short)
     * - 100-200 tokens: 15 points (medium)
     * - 200-400 tokens: 20 points (long)
     * - > 400 tokens: 30 points (very long)
     */
    private int analyzeTokenCount(String prompt) {
        int tokens = estimateTokens(prompt);
        
        if (tokens < 50) return 5;
        if (tokens < 100) return 10;
        if (tokens < 200) return 15;
        if (tokens < 400) return 20;
        return 30;
    }
    
    /**
     * Estimate token count (rough approximation).
     * Rule: ~4 characters per token on average.
     */
    private int estimateTokens(String text) {
        return text.length() / 4;
    }
    
    // ========================================
    // FACTOR 2: REASONING COMPLEXITY (0-40 points)
    // ========================================
    
    /**
     * Analyze reasoning complexity.
     * 
     * Checks for:
     * - Reasoning keywords (10 points)
     * - Multi-step thinking (10 points)
     * - Creative/complex tasks (10 points)
     * - Multiple questions (10 points)
     */
    private int analyzeReasoning(String prompt) {
        int score = 0;
        
        // 1. Check for reasoning keywords
        int reasoningMatches = countMatches(prompt, REASONING_PATTERN);
        if (reasoningMatches > 0) {
            score += Math.min(10, reasoningMatches * 3);
        }
        
        // 2. Check for multi-step indicators
        int multiStepMatches = countMatches(prompt, MULTISTEP_PATTERN);
        if (multiStepMatches > 0) {
            score += Math.min(10, multiStepMatches * 4);
        }
        
        // 3. Check for creative tasks
        int creativeMatches = countMatches(prompt, CREATIVE_PATTERN);
        if (creativeMatches > 0) {
            score += Math.min(10, creativeMatches * 5);
        }
        
        // 4. Check for multiple questions (complexity indicator)
        int questionCount = countChar(prompt, '?');
        if (questionCount > 1) {
            score += Math.min(10, questionCount * 3);
        }
        
        return Math.min(40, score); // Cap at 40
    }
    
    // ========================================
    // FACTOR 3: TECHNICAL COMPLEXITY (0-30 points)
    // ========================================
    
    /**
     * Analyze technical complexity.
     * 
     * Checks for:
     * - Technical keywords (15 points)
     * - Code blocks/snippets (15 points)
     */
    private int analyzeTechnical(String prompt) {
        int score = 0;
        
        // 1. Check for technical keywords
        int technicalMatches = countMatches(prompt, TECHNICAL_PATTERN);
        if (technicalMatches > 0) {
            score += Math.min(15, technicalMatches * 4);
        }
        
        // 2. Check for code
        int codeMatches = countMatches(prompt, CODE_PATTERN);
        if (codeMatches > 0) {
            score += Math.min(15, codeMatches * 5);
        }
        
        return Math.min(30, score); // Cap at 30
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Count pattern matches in text.
     */
    private int countMatches(String text, Pattern pattern) {
        var matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * Count occurrences of a character.
     */
    private int countChar(String text, char c) {
        return (int) text.chars().filter(ch -> ch == c).count();
    }
    
    /**
     * Build human-readable reasoning text.
     */
    private String buildReasoning(int totalScore, Map<String, Integer> factors) {
        StringBuilder reasoning = new StringBuilder();
        
        // Overall assessment
        if (totalScore <= 30) {
            reasoning.append("Simple query - ");
        } else if (totalScore <= 60) {
            reasoning.append("Medium complexity - ");
        } else {
            reasoning.append("Complex query - ");
        }
        
        // Top contributing factors
        String topFactor = factors.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
        
        reasoning.append("primarily driven by ").append(topFactor.toLowerCase());
        
        return reasoning.toString();
    }
    
    // ========================================
    // OPTIONAL: BATCH ANALYSIS
    // ========================================
    
    /**
     * Analyze multiple prompts efficiently.
     * 
     * @param prompts List of prompts to analyze
     * @return List of complexity scores
     */
    public Map<String, ComplexityScore> analyzeBatch(Map<String, String> prompts) {
        Map<String, ComplexityScore> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : prompts.entrySet()) {
            results.put(entry.getKey(), analyze(entry.getValue()));
        }
        
        return results;
    }
}
