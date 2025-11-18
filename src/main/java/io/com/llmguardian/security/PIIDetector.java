package io.com.llmguardian.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Service responsible for detecting PII in text.
 * 
 * Process:
 * 1. Load enabled patterns from configuration
 * 2. Run regex matching for each pattern
 * 3. Validate matches to reduce false positives
 * 4. Return all confirmed matches
 * 
 * Thread-safe and stateless.
 * 
 * @author LLMGuardian Team
 */
@Service
public class PIIDetector {
    
    private static final Logger log = LoggerFactory.getLogger(PIIDetector.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private PIIConfiguration config;  // Loads from application.yml
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Detect all PII in the given text.
     * 
     * This is the main entry point for PII detection.
     * 
     * @param text The text to scan for PII
     * @return PIIResult containing all matches found
     * 
     * Example:
     * <pre>
     * String text = "Email me at john@example.com or call 555-1234";
     * PIIResult result = detector.detect(text);
     * // result.getMatches() contains EMAIL and PHONE matches
     * </pre>
     */
    public PIIResult detect(String text) {
        long startTime = System.currentTimeMillis();
        
        // Validate input
        if (text == null || text.trim().isEmpty()) {
            log.debug("Empty text provided, returning empty result");
            return PIIResult.empty();
        }
        
        log.debug("Starting PII detection on text of length: {}", text.length());
        
        // Collect all matches
        List<PIIMatch> allMatches = new ArrayList<>();
        
        // Get enabled patterns from configuration
        Set<PIIPattern> enabledPatterns = config.getEnabledPatterns();
        
        log.debug("Scanning with {} enabled patterns: {}", 
            enabledPatterns.size(), 
            enabledPatterns);
        
        // Scan with each enabled pattern
        for (PIIPattern pattern : enabledPatterns) {
            List<PIIMatch> patternMatches = detectPattern(text, pattern);
            allMatches.addAll(patternMatches);
            
            if (!patternMatches.isEmpty()) {
                log.debug("Pattern {} found {} matches", 
                    pattern.getName(), 
                    patternMatches.size());
            }
        }
        
        // Remove overlapping matches (keep most specific)
        List<PIIMatch> filteredMatches = removeOverlaps(allMatches);
        
        long detectionTime = System.currentTimeMillis() - startTime;
        
        log.info("PII detection completed: found {} matches in {}ms", 
            filteredMatches.size(), 
            detectionTime);
        
        return PIIResult.builder()
            .matches(filteredMatches)
            .detectionTimeMs(detectionTime)
            .build();
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Detect a specific pattern in text.
     * 
     * Process:
     * 1. Run regex matcher
     * 2. For each match, validate it
     * 3. Create PIIMatch object if valid
     * 
     * @param text The text to scan
     * @param pattern The pattern to look for
     * @return List of validated matches
     */
    private List<PIIMatch> detectPattern(String text, PIIPattern pattern) {
        List<PIIMatch> matches = new ArrayList<>();
        
        // Run regex matching
        Matcher matcher = pattern.getPattern().matcher(text);
        
        while (matcher.find()) {
            String matchedValue = matcher.group();
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            
            log.trace("Found potential {} at position {}-{}: ***", 
                pattern.getName(), 
                startIndex, 
                endIndex);
            
            // Validate to reduce false positives
            if (pattern.validate(matchedValue)) {
                PIIMatch match = new PIIMatch(
                    pattern,
                    matchedValue,
                    startIndex,
                    endIndex
                );
                matches.add(match);
                
                log.trace("Validated {} match", pattern.getName());
            } else {
                log.trace("Rejected {} match (failed validation)", pattern.getName());
            }
        }
        
        return matches;
    }
    
    /**
     * Remove overlapping matches, keeping the most specific one.
     * 
     * Example problem:
     * Text: "john@example.com"
     * - EMAIL pattern matches: "john@example.com"
     * - Hypothetical USERNAME pattern matches: "john"
     * 
     * We keep EMAIL (more specific) and discard USERNAME.
     * 
     * Strategy:
     * 1. Sort by start position
     * 2. If matches overlap, keep the longer one
     * 3. If same length, keep based on priority (EMAIL > PHONE > etc.)
     * 
     * @param matches All matches found
     * @return Filtered list with no overlaps
     */
    private List<PIIMatch> removeOverlaps(List<PIIMatch> matches) {
        if (matches.size() <= 1) {
            return matches;
        }
        
        // Sort by start position
        List<PIIMatch> sorted = new ArrayList<>(matches);
        sorted.sort((m1, m2) -> {
            int posCompare = Integer.compare(m1.getStartIndex(), m2.getStartIndex());
            if (posCompare != 0) {
                return posCompare;
            }
            // If same start, longer match first
            return Integer.compare(m2.getLength(), m1.getLength());
        });
        
        List<PIIMatch> filtered = new ArrayList<>();
        PIIMatch previous = null;
        
        for (PIIMatch current : sorted) {
            if (previous == null) {
                // First match
                filtered.add(current);
                previous = current;
            } else {
                // Check for overlap
                if (current.getStartIndex() >= previous.getEndIndex()) {
                    // No overlap
                    filtered.add(current);
                    previous = current;
                } else {
                    // Overlap detected
                    log.trace("Overlap detected: {} overlaps with {}, keeping first", 
                        current.getType().getName(), 
                        previous.getType().getName());
                    // Skip current (we already added previous)
                }
            }
        }
        
        return filtered;
    }
    
    // ========================================
    // OPTIONAL: BATCH DETECTION
    // ========================================
    
    /**
     * Detect PII in multiple texts efficiently.
     * 
     * Useful for batch processing (e.g., analyzing logs).
     * 
     * @param texts List of texts to scan
     * @return List of results (same order as input)
     */
    public List<PIIResult> detectBatch(List<String> texts) {
        log.info("Starting batch PII detection on {} texts", texts.size());
        
        List<PIIResult> results = new ArrayList<>();
        
        for (String text : texts) {
            results.add(detect(text));
        }
        
        log.info("Batch detection completed");
        return results;
    }
    
    // ========================================
    // OPTIONAL: PATTERN TESTING
    // ========================================
    
    /**
     * Test if text contains a specific PII type.
     * 
     * Convenience method for simple checks.
     * 
     * @param text Text to check
     * @param pattern Pattern to look for
     * @return true if pattern found, false otherwise
     */
    public boolean contains(String text, PIIPattern pattern) {
        PIIResult result = detect(text);
        return result.hasType(pattern);
    }
}