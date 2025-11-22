package io.com.llmguardian.optimization;

import io.com.llmguardian.optimization.dto.Intent;
import io.com.llmguardian.optimization.dto.Intent.IntentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for extracting user intent from prompts.
 * 
 * Uses keyword/pattern matching to determine what the user wants to do.
 * This helps optimize the prompt by understanding its purpose.
 * 
 * Process:
 * 1. Normalize prompt (lowercase, remove punctuation)
 * 2. Match against keyword patterns
 * 3. Calculate confidence based on matches
 * 4. Return most likely intent
 * 
 * @author LLMGuardian Team
 */
@Service
public class IntentExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(IntentExtractor.class);
    
    // ========================================
    // INTENT PATTERNS (Keyword-based)
    // ========================================
    
    private static final Map<IntentType, List<Pattern>> INTENT_PATTERNS = new HashMap<>();
    
    static {
        // GENERATE_TEXT
        INTENT_PATTERNS.put(IntentType.GENERATE_TEXT, Arrays.asList(
            Pattern.compile("\\b(write|create|generate|compose|draft)\\b.*\\b(email|letter|message|document|article|post)\\b"),
            Pattern.compile("\\b(write me|create a|generate a|compose a)\\b"),
            Pattern.compile("\\bhelp me write\\b")
        ));
        
        // EXPLAIN_CONCEPT
        INTENT_PATTERNS.put(IntentType.EXPLAIN_CONCEPT, Arrays.asList(
            Pattern.compile("\\b(explain|describe|tell me about|what is|what are)\\b"),
            Pattern.compile("\\bhow does .* work\\b"),
            Pattern.compile("\\bcan you explain\\b")
        ));
        
        // SUMMARIZE
        INTENT_PATTERNS.put(IntentType.SUMMARIZE, Arrays.asList(
            Pattern.compile("\\b(summarize|summary|tldr|key points|main ideas)\\b"),
            Pattern.compile("\\b(condense|shorten|brief|briefly)\\b"),
            Pattern.compile("\\bgive me (a|the) (summary|overview)\\b")
        ));
        
        // TRANSLATE
        INTENT_PATTERNS.put(IntentType.TRANSLATE, Arrays.asList(
            Pattern.compile("\\btranslate\\b.*\\b(to|into|in)\\b"),
            Pattern.compile("\\b(spanish|french|german|hindi|chinese|japanese) translation\\b"),
            Pattern.compile("\\bconvert.*to (spanish|french|german)\\b")
        ));
        
        // ANALYZE
        INTENT_PATTERNS.put(IntentType.ANALYZE, Arrays.asList(
            Pattern.compile("\\b(analyze|analysis|examine|evaluate|assess)\\b"),
            Pattern.compile("\\b(what does this mean|interpret|review)\\b"),
            Pattern.compile("\\b(compare|contrast|difference between)\\b")
        ));
        
        // QUESTION_ANSWER
        INTENT_PATTERNS.put(IntentType.QUESTION_ANSWER, Arrays.asList(
            Pattern.compile("^(what|who|when|where|why|how|which)\\b"),
            Pattern.compile("\\b(can you tell me|do you know|is it true that)\\b"),
            Pattern.compile("\\?$")  // Ends with question mark
        ));
        
        // CODE_GENERATION
        INTENT_PATTERNS.put(IntentType.CODE_GENERATION, Arrays.asList(
            Pattern.compile("\\b(write|create|generate) .* (code|function|class|script|program)\\b"),
            Pattern.compile("\\b(implement|build|develop) .* (in|using) (python|java|javascript|react)\\b"),
            Pattern.compile("\\bhow to code\\b")
        ));
        
        // CODE_EXPLANATION
        INTENT_PATTERNS.put(IntentType.CODE_EXPLANATION, Arrays.asList(
            Pattern.compile("\\bexplain (this|the) code\\b"),
            Pattern.compile("\\bwhat does this code do\\b"),
            Pattern.compile("\\bhow does this .* work\\b.*\\bcode\\b")
        ));
        
        // CREATIVE_WRITING
        INTENT_PATTERNS.put(IntentType.CREATIVE_WRITING, Arrays.asList(
            Pattern.compile("\\b(write|create) .* (story|poem|song|lyrics)\\b"),
            Pattern.compile("\\b(creative|fiction|narrative|tale)\\b"),
            Pattern.compile("\\bonce upon a time\\b")
        ));
        
        // DATA_EXTRACTION
        INTENT_PATTERNS.put(IntentType.DATA_EXTRACTION, Arrays.asList(
            Pattern.compile("\\b(extract|find|get|retrieve|pull out)\\b.*\\b(data|information|details)\\b"),
            Pattern.compile("\\b(list all|show me all|give me all)\\b"),
            Pattern.compile("\\bwhat are the (names|dates|numbers|values)\\b")
        ));
        
        // COMPARISON
        INTENT_PATTERNS.put(IntentType.COMPARISON, Arrays.asList(
            Pattern.compile("\\b(compare|comparison|versus|vs|difference between)\\b"),
            Pattern.compile("\\b(better|worse|pros and cons|advantages|disadvantages)\\b"),
            Pattern.compile("\\bwhich is (better|best|faster|cheaper)\\b")
        ));
        
        // CLASSIFICATION
        INTENT_PATTERNS.put(IntentType.CLASSIFICATION, Arrays.asList(
            Pattern.compile("\\b(classify|categorize|label|tag|identify type)\\b"),
            Pattern.compile("\\bwhat (type|kind|category) (is|are)\\b"),
            Pattern.compile("\\bis this .* (positive|negative|neutral)\\b")
        ));
    }
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Extract intent from a prompt.
     * 
     * @param prompt The user's prompt
     * @return Extracted intent with confidence
     */
    public Intent extractIntent(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.debug("Empty prompt, returning UNKNOWN intent");
            return Intent.unknown();
        }
        
        log.debug("Extracting intent from prompt: {}", 
            prompt.substring(0, Math.min(50, prompt.length())) + "...");
        
        // Normalize prompt
        String normalized = normalizePrompt(prompt);
        
        // Score each intent type
        Map<IntentType, Double> scores = new HashMap<>();
        for (Map.Entry<IntentType, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            IntentType type = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            double score = scoreIntent(normalized, patterns);
            if (score > 0) {
                scores.put(type, score);
            }
        }
        
        // No matches found
        if (scores.isEmpty()) {
            log.debug("No intent patterns matched, returning UNKNOWN");
            return Intent.unknown();
        }
        
        // Get highest scoring intent
        IntentType bestIntent = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(IntentType.UNKNOWN);
        
        double confidence = scores.get(bestIntent);
        
        log.info("Extracted intent: {} (confidence: {:.2f})", bestIntent, confidence);
        
        return Intent.of(bestIntent, confidence);
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Normalize prompt for pattern matching.
     * 
     * - Convert to lowercase
     * - Remove extra whitespace
     * - Preserve question marks (important for QUESTION_ANSWER)
     */
    private String normalizePrompt(String prompt) {
        return prompt.toLowerCase()
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Score how well the prompt matches an intent type.
     * 
     * Returns confidence score 0.0 - 1.0:
     * - 0 matches = 0.0
     * - 1 match = 0.6
     * - 2+ matches = 0.9
     * 
     * @param normalizedPrompt The normalized prompt
     * @param patterns Patterns for this intent type
     * @return Confidence score
     */
    private double scoreIntent(String normalizedPrompt, List<Pattern> patterns) {
        int matchCount = 0;
        
        for (Pattern pattern : patterns) {
            if (pattern.matcher(normalizedPrompt).find()) {
                matchCount++;
            }
        }
        
        // Convert match count to confidence
        if (matchCount == 0) return 0.0;
        if (matchCount == 1) return 0.6;
        if (matchCount == 2) return 0.8;
        return 0.9;  // 3+ matches = very confident
    }
    
    // ========================================
    // OPTIONAL: BATCH EXTRACTION
    // ========================================
    
    /**
     * Extract intents from multiple prompts.
     * 
     * @param prompts List of prompts
     * @return List of intents (same order)
     */
    public List<Intent> extractBatch(List<String> prompts) {
        List<Intent> results = new ArrayList<>();
        for (String prompt : prompts) {
            results.add(extractIntent(prompt));
        }
        return results;
    }
}
