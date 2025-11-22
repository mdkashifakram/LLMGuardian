package io.com.llmguardian.optimization;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Configuration for prompt optimization.
 * 
 * Loads settings from application.yml under "llmguardian.optimization" prefix.
 * 
 * Example configuration:
 * <pre>
 * llmguardian:
 *   optimization:
 *     enabled: true
 *     target-reduction: 30
 *     min-prompt-length: 50
 *     strategies:
 *       remove-redundancy: true
 *       compress-whitespace: true
 *       simplify-language: true
 *       preserve-technical-terms: true
 *     stopwords:
 *       enabled: true
 *       custom-words:
 *         - basically
 *         - actually
 * </pre>
 * 
 * @author LLMGuardian Team
 */
@Configuration
@ConfigurationProperties(prefix = "llmguardian.optimization")
public class OptimizationConfig {
    
    // ========================================
    // NESTED CONFIGURATION CLASSES
    // ========================================
    
    private boolean enabled = true;
    private int targetReduction = 30;          // Target % reduction (20-40%)
    private int minPromptLength = 50;          // Don't optimize if shorter than this
    private Strategies strategies = new Strategies();
    private Stopwords stopwords = new Stopwords();
    
    // ========================================
    // STRATEGIES CONFIG
    // ========================================
    
    public static class Strategies {
        private boolean removeRedundancy = true;
        private boolean compressWhitespace = true;
        private boolean simplifyLanguage = true;
        private boolean preserveTechnicalTerms = true;
        private boolean removeFillerWords = true;
        
        // Getters and setters
        public boolean isRemoveRedundancy() {
            return removeRedundancy;
        }
        
        public void setRemoveRedundancy(boolean removeRedundancy) {
            this.removeRedundancy = removeRedundancy;
        }
        
        public boolean isCompressWhitespace() {
            return compressWhitespace;
        }
        
        public void setCompressWhitespace(boolean compressWhitespace) {
            this.compressWhitespace = compressWhitespace;
        }
        
        public boolean isSimplifyLanguage() {
            return simplifyLanguage;
        }
        
        public void setSimplifyLanguage(boolean simplifyLanguage) {
            this.simplifyLanguage = simplifyLanguage;
        }
        
        public boolean isPreserveTechnicalTerms() {
            return preserveTechnicalTerms;
        }
        
        public void setPreserveTechnicalTerms(boolean preserveTechnicalTerms) {
            this.preserveTechnicalTerms = preserveTechnicalTerms;
        }
        
        public boolean isRemoveFillerWords() {
            return removeFillerWords;
        }
        
        public void setRemoveFillerWords(boolean removeFillerWords) {
            this.removeFillerWords = removeFillerWords;
        }
    }
    
    // ========================================
    // STOPWORDS CONFIG
    // ========================================
    
    public static class Stopwords {
        private boolean enabled = true;
        private List<String> customWords = new ArrayList<>();
        
        // Common filler words that can be removed
        private static final Set<String> DEFAULT_STOPWORDS = Set.of(
            "basically", "actually", "literally", "honestly", "frankly",
            "really", "very", "quite", "just", "simply", "merely",
            "perhaps", "maybe", "possibly", "probably", "essentially",
            "practically", "virtually", "effectively"
        );
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<String> getCustomWords() {
            return customWords;
        }
        
        public void setCustomWords(List<String> customWords) {
            this.customWords = customWords;
        }
        
        /**
         * Get all stopwords (default + custom).
         */
        public Set<String> getAllStopwords() {
            Set<String> all = new HashSet<>(DEFAULT_STOPWORDS);
            all.addAll(customWords);
            return all;
        }
    }
    
    // ========================================
    // MAIN GETTERS/SETTERS
    // ========================================
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getTargetReduction() {
        return targetReduction;
    }
    
    public void setTargetReduction(int targetReduction) {
        this.targetReduction = targetReduction;
    }
    
    public int getMinPromptLength() {
        return minPromptLength;
    }
    
    public void setMinPromptLength(int minPromptLength) {
        this.minPromptLength = minPromptLength;
    }
    
    public Strategies getStrategies() {
        return strategies;
    }
    
    public void setStrategies(Strategies strategies) {
        this.strategies = strategies;
    }
    
    public Stopwords getStopwords() {
        return stopwords;
    }
    
    public void setStopwords(Stopwords stopwords) {
        this.stopwords = stopwords;
    }
    
    // ========================================
    // CONVENIENCE METHODS
    // ========================================
    
    /**
     * Check if optimization should be applied to a prompt.
     * 
     * @param promptLength Length of the prompt
     * @return true if should optimize
     */
    public boolean shouldOptimize(int promptLength) {
        return enabled && promptLength >= minPromptLength;
    }
    
    /**
     * Get all enabled optimization strategies.
     * 
     * @return Set of strategy names
     */
    public Set<String> getEnabledStrategies() {
        Set<String> enabled = new HashSet<>();
        if (strategies.removeRedundancy) enabled.add("REMOVE_REDUNDANCY");
        if (strategies.compressWhitespace) enabled.add("COMPRESS_WHITESPACE");
        if (strategies.simplifyLanguage) enabled.add("SIMPLIFY_LANGUAGE");
        if (strategies.preserveTechnicalTerms) enabled.add("PRESERVE_TECHNICAL");
        if (strategies.removeFillerWords) enabled.add("REMOVE_FILLER");
        return enabled;
    }
}
