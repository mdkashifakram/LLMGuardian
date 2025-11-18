package io.com.llmguardian.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of PII detection on a text.
 * 
 * Contains:
 * - List of all matches found
 * - Summary statistics
 * - Detection metadata
 * 
 * @author LLMGuardian Team
 */
public class PIIResult {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final boolean detected;              // Was any PII found?
    private final List<PIIMatch> matches;        // All matches found
    private final long detectionTimeMs;          // How long detection took
    private final int totalMatches;              // Total count
    
    // ========================================
    // CONSTRUCTOR (Private - use Builder)
    // ========================================
    
    private PIIResult(Builder builder) {
        this.detected = !builder.matches.isEmpty();
        this.matches = Collections.unmodifiableList(new ArrayList<>(builder.matches));
        this.detectionTimeMs = builder.detectionTimeMs;
        this.totalMatches = matches.size();
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public boolean isDetected() {
        return detected;
    }
    
    public List<PIIMatch> getMatches() {
        return matches;  // Already unmodifiable
    }
    
    public long getDetectionTimeMs() {
        return detectionTimeMs;
    }
    
    public int getTotalMatches() {
        return totalMatches;
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Get count of each PII type detected
     * Example: {EMAIL=2, PHONE=1}
     */
    public Map<PIIPattern, Long> getCountByType() {
        return matches.stream()
            .collect(Collectors.groupingBy(
                PIIMatch::getType,
                Collectors.counting()
            ));
    }
    
    /**
     * Get all matches of a specific type
     */
    public List<PIIMatch> getMatchesByType(PIIPattern type) {
        return matches.stream()
            .filter(match -> match.getType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a specific type was detected
     */
    public boolean hasType(PIIPattern type) {
        return matches.stream()
            .anyMatch(match -> match.getType() == type);
    }
    
    @Override
    public String toString() {
        return String.format(
            "PIIResult{detected=%s, totalMatches=%d, types=%s, detectionTimeMs=%d}",
            detected,
            totalMatches,
            getCountByType(),
            detectionTimeMs
        );
    }
    
    // ========================================
    // BUILDER PATTERN
    // ========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<PIIMatch> matches = new ArrayList<>();
        private long detectionTimeMs = 0;
        
        public Builder addMatch(PIIMatch match) {
            this.matches.add(match);
            return this;
        }
        
        public Builder matches(List<PIIMatch> matches) {
            this.matches = new ArrayList<>(matches);
            return this;
        }
        
        public Builder detectionTimeMs(long detectionTimeMs) {
            this.detectionTimeMs = detectionTimeMs;
            return this;
        }
        
        public PIIResult build() {
            return new PIIResult(this);
        }
    }
    
    // ========================================
    // FACTORY METHODS (Convenience)
    // ========================================
    
    /**
     * Create empty result (no PII detected)
     */
    public static PIIResult empty() {
        return builder().build();
    }
}