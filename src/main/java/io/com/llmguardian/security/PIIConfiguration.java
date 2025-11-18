package io.com.llmguardian.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration for PII detection and redaction.
 * 
 * Loads settings from application.yml under "llmguardian.pii" prefix.
 * 
 * Example configuration:
 * <pre>
 * llmguardian:
 *   pii:
 *     detection:
 *       enabled: true
 *       patterns:
 *         EMAIL: true
 *         PHONE: true
 *         SSN: false
 *         AADHAAR: true
 *     redaction:
 *       token-generation: RANDOM
 *       token-length: 6
 *     audit:
 *       enabled: true
 *       level: STANDARD
 *       retention-days: 90
 * </pre>
 * 
 * @author LLMGuardian Team
 */
@Configuration
@ConfigurationProperties(prefix = "llmguardian.pii")
public class PIIConfiguration {
    
    // ========================================
    // NESTED CONFIGURATION CLASSES
    // ========================================
    
    private Detection detection = new Detection();
    private Redaction redaction = new Redaction();
    private Audit audit = new Audit();
    
    // ========================================
    // DETECTION CONFIG
    // ========================================
    
    public static class Detection {
        private boolean enabled = true;
        private Map<String, Boolean> patterns = new HashMap<>();
        
        // Custom patterns (added at runtime)
        private List<CustomPattern> customPatterns = new ArrayList<>();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Map<String, Boolean> getPatterns() {
            return patterns;
        }
        
        public void setPatterns(Map<String, Boolean> patterns) {
            this.patterns = patterns;
        }
        
        public List<CustomPattern> getCustomPatterns() {
            return customPatterns;
        }
        
        public void setCustomPatterns(List<CustomPattern> customPatterns) {
            this.customPatterns = customPatterns;
        }
    }
    
    /**
     * Custom pattern definition (from YAML).
     */
    public static class CustomPattern {
        private String name;
        private String regex;
        private String region = "Custom";
        private boolean enabled = true;
        
        // Getters and setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getRegex() {
            return regex;
        }
        
        public void setRegex(String regex) {
            this.regex = regex;
        }
        
        public String getRegion() {
            return region;
        }
        
        public void setRegion(String region) {
            this.region = region;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    // ========================================
    // REDACTION CONFIG
    // ========================================
    
    public static class Redaction {
        private TokenGenerationStrategy tokenGeneration = TokenGenerationStrategy.RANDOM;
        private int tokenLength = 6;
        
        public TokenGenerationStrategy getTokenGeneration() {
            return tokenGeneration;
        }
        
        public void setTokenGeneration(TokenGenerationStrategy tokenGeneration) {
            this.tokenGeneration = tokenGeneration;
        }
        
        public int getTokenLength() {
            return tokenLength;
        }
        
        public void setTokenLength(int tokenLength) {
            this.tokenLength = tokenLength;
        }
    }
    
    // ========================================
    // AUDIT CONFIG
    // ========================================
    
    public static class Audit {
        private boolean enabled = true;
        private AuditLevel level = AuditLevel.STANDARD;
        private int retentionDays = 90;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public AuditLevel getLevel() {
            return level;
        }
        
        public void setLevel(AuditLevel level) {
            this.level = level;
        }
        
        public int getRetentionDays() {
            return retentionDays;
        }
        
        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
    
    // ========================================
    // ENUMS
    // ========================================
    
    public enum TokenGenerationStrategy {
        RANDOM,      // UUID-based (recommended)
        SEQUENTIAL   // Counter-based (debugging)
    }
    
    public enum AuditLevel {
        NONE,        // No audit logging
        MINIMAL,     // Just count of detections
        STANDARD,    // Type and count (recommended)
        DETAILED     // Full metadata (debugging)
    }
    
    // ========================================
    // MAIN GETTERS/SETTERS
    // ========================================
    
    public Detection getDetection() {
        return detection;
    }
    
    public void setDetection(Detection detection) {
        this.detection = detection;
    }
    
    public Redaction getRedaction() {
        return redaction;
    }
    
    public void setRedaction(Redaction redaction) {
        this.redaction = redaction;
    }
    
    public Audit getAudit() {
        return audit;
    }
    
    public void setAudit(Audit audit) {
        this.audit = audit;
    }
    
    // ========================================
    // CONVENIENCE METHODS
    // ========================================
    
    /**
     * Get set of enabled PII patterns.
     * 
     * Combines:
     * 1. Built-in patterns (from PIIPattern enum)
     * 2. Configuration overrides (from YAML)
     * 
     * @return Set of enabled patterns
     */
    public Set<PIIPattern> getEnabledPatterns() {
        return Arrays.stream(PIIPattern.values())
            .filter(pattern -> {
                // Check if explicitly configured
                String patternName = pattern.getName();
                if (detection.patterns.containsKey(patternName)) {
                    return detection.patterns.get(patternName);
                }
                // Otherwise use default
                return pattern.isDefaultEnabled();
            })
            .collect(Collectors.toSet());
    }
    
    /**
     * Check if a specific pattern is enabled.
     * 
     * @param pattern The pattern to check
     * @return true if enabled
     */
    public boolean isPatternEnabled(PIIPattern pattern) {
        String patternName = pattern.getName();
        if (detection.patterns.containsKey(patternName)) {
            return detection.patterns.get(patternName);
        }
        return pattern.isDefaultEnabled();
    }
    
    /**
     * Get token generation strategy.
     * 
     * @return RANDOM or SEQUENTIAL
     */
    public TokenGenerationStrategy getTokenGeneration() {
        return redaction.tokenGeneration;
    }
    
    /**
     * Get token length (for RANDOM generation).
     * 
     * @return Number of hex characters (default: 6)
     */
    public int getTokenLength() {
        return redaction.tokenLength;
    }
    
    /**
     * Check if audit logging is enabled.
     * 
     * @return true if audit is enabled
     */
    public boolean isAuditEnabled() {
        return audit.enabled;
    }
    
    /**
     * Get audit level.
     * 
     * @return NONE, MINIMAL, STANDARD, or DETAILED
     */
    public AuditLevel getAuditLevel() {
        return audit.level;
    }
    
    /**
     * Get audit retention period.
     * 
     * @return Days to keep audit logs
     */
    public int getAuditRetentionDays() {
        return audit.retentionDays;
    }
}