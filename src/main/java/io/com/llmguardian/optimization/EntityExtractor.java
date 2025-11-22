package io.com.llmguardian.optimization;

import io.com.llmguardian.optimization.dto.Entity;
import io.com.llmguardian.optimization.dto.Entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting important entities from prompts.
 * 
 * Entities are critical pieces of information that MUST be preserved
 * during optimization (names, dates, numbers, technical terms, etc.)
 * 
 * Process:
 * 1. Scan prompt with regex patterns
 * 2. Identify and classify entities
 * 3. Mark positions (so we don't remove them)
 * 4. Return all found entities
 * 
 * @author LLMGuardian Team
 */
@Service
public class EntityExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(EntityExtractor.class);
    
    // ========================================
    // ENTITY PATTERNS
    // ========================================
    
    // PERSON - Capitalized names
    private static final Pattern PERSON_PATTERN = Pattern.compile(
        "\\b([A-Z][a-z]+ [A-Z][a-z]+)\\b"  // "John Smith"
    );
    
    // ORGANIZATION - Capitalized with Inc/Corp/Ltd
    private static final Pattern ORG_PATTERN = Pattern.compile(
        "\\b([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*(?: Inc\\.?| Corp\\.?| Ltd\\.?| LLC)?)\\b"
    );
    
    // DATE - Various formats
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +  // 12/31/2024 or 12-31-24
        "[A-Z][a-z]+ \\d{1,2},? \\d{4}|" +          // January 1, 2024
        "\\d{4}-\\d{2}-\\d{2})\\b"                  // 2024-01-01
    );
    
    // NUMBER - Pure numbers
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
        "\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\b"  // 1,234.56
    );
    
    // AMOUNT - Money amounts
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "\\$\\d+(?:,\\d{3})*(?:\\.\\d{2})?|" +      // $1,234.56
        "\\d+(?:,\\d{3})* (?:USD|EUR|GBP|INR)|" +   // 1234 USD
        "Rs\\.? ?\\d+(?:,\\d{3})*"                  // Rs. 1,234
    );
    
    // TECHNOLOGY - Common tech terms
    private static final Pattern TECH_PATTERN = Pattern.compile(
        "\\b(Python|Java|JavaScript|React|Angular|Node\\.?js|" +
        "Spring|Django|Flask|PostgreSQL|MongoDB|Redis|" +
        "AWS|Azure|GCP|Docker|Kubernetes|Git|GitHub|" +
        "API|REST|GraphQL|SQL|NoSQL|HTML|CSS|TypeScript|" +
        "Machine Learning|AI|TensorFlow|PyTorch)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // REQUIREMENT - Explicit requirements/constraints
    private static final Pattern REQUIREMENT_PATTERN = Pattern.compile(
        "\\b(must|required|need|necessary|should|have to|cannot|can't|must not)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // CONSTRAINT - Limitations
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile(
        "\\b(within \\d+|less than \\d+|more than \\d+|" +
        "maximum|minimum|at least|at most|no more than)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Extract all entities from a prompt.
     * 
     * @param prompt The prompt to analyze
     * @return List of extracted entities
     */
    public List<Entity> extractEntities(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.debug("Empty prompt, no entities to extract");
            return Collections.emptyList();
        }
        
        log.debug("Extracting entities from prompt: {}", 
            prompt.substring(0, Math.min(50, prompt.length())) + "...");
        
        List<Entity> entities = new ArrayList<>();
        
        // Extract each entity type
        entities.addAll(extractPattern(prompt, AMOUNT_PATTERN, EntityType.AMOUNT));
        entities.addAll(extractPattern(prompt, DATE_PATTERN, EntityType.DATE));
        entities.addAll(extractPattern(prompt, TECH_PATTERN, EntityType.TECHNOLOGY));
        entities.addAll(extractPattern(prompt, PERSON_PATTERN, EntityType.PERSON));
        entities.addAll(extractPattern(prompt, ORG_PATTERN, EntityType.ORGANIZATION));
        entities.addAll(extractPattern(prompt, NUMBER_PATTERN, EntityType.NUMBER));
        
        // Extract concepts (requirement/constraint keywords)
        entities.addAll(extractKeywords(prompt, REQUIREMENT_PATTERN, EntityType.REQUIREMENT));
        entities.addAll(extractKeywords(prompt, CONSTRAINT_PATTERN, EntityType.CONSTRAINT));
        
        // Remove duplicates and overlaps
        List<Entity> filtered = removeDuplicates(entities);
        
        log.info("Extracted {} entities from prompt", filtered.size());
        if (log.isDebugEnabled()) {
            filtered.forEach(e -> log.debug("  - {}", e));
        }
        
        return filtered;
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Extract entities matching a pattern.
     * 
     * @param text The text to search
     * @param pattern The regex pattern
     * @param type The entity type
     * @return List of found entities
     */
    private List<Entity> extractPattern(String text, Pattern pattern, EntityType type) {
        List<Entity> entities = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String value = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            
            // Additional validation for some types
            if (shouldInclude(value, type)) {
                entities.add(new Entity(type, value, start, end));
            }
        }
        
        return entities;
    }
    
    /**
     * Extract keyword-based entities (for REQUIREMENT/CONSTRAINT).
     * 
     * These capture the surrounding context, not just the keyword.
     */
    private List<Entity> extractKeywords(String text, Pattern pattern, EntityType type) {
        List<Entity> entities = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            // Capture the keyword + surrounding words
            String keyword = matcher.group();
            int keywordStart = matcher.start();
            int keywordEnd = matcher.end();
            
            // Expand to capture full phrase (up to next punctuation or 10 words)
            String phrase = extractPhrase(text, keywordStart, keywordEnd);
            
            entities.add(new Entity(type, phrase, keywordStart, keywordStart + phrase.length()));
        }
        
        return entities;
    }
    
    /**
     * Extract phrase around a keyword.
     * 
     * Example: "must be completed within 24 hours"
     * Not just: "must"
     */
    private String extractPhrase(String text, int keywordStart, int keywordEnd) {
        // Find start of phrase (previous punctuation or beginning)
        int phraseStart = keywordStart;
        while (phraseStart > 0 && !isPunctuation(text.charAt(phraseStart - 1))) {
            phraseStart--;
        }
        
        // Find end of phrase (next punctuation or end, max 10 words)
        int phraseEnd = keywordEnd;
        int wordCount = 0;
        while (phraseEnd < text.length() && wordCount < 10) {
            char c = text.charAt(phraseEnd);
            if (isPunctuation(c)) break;
            if (Character.isWhitespace(c)) wordCount++;
            phraseEnd++;
        }
        
        return text.substring(phraseStart, phraseEnd).trim();
    }
    
    /**
     * Check if character is sentence-ending punctuation.
     */
    private boolean isPunctuation(char c) {
        return c == '.' || c == '!' || c == '?' || c == ';' || c == '\n';
    }
    
    /**
     * Validate if entity should be included.
     * 
     * Filters out false positives.
     */
    private boolean shouldInclude(String value, EntityType type) {
        switch (type) {
            case NUMBER:
                // Skip single-digit numbers (likely not important)
                return value.length() > 1;
            
            case PERSON:
                // Skip common false positives
                String lower = value.toLowerCase();
                return !lower.equals("the") && 
                       !lower.equals("and") &&
                       value.length() > 3;
            
            case ORGANIZATION:
                // Skip short matches
                return value.length() > 2;
            
            default:
                return true;
        }
    }
    
    /**
     * Remove duplicate and overlapping entities.
     * 
     * Strategy:
     * 1. Sort by position
     * 2. If entities overlap, keep the one with higher priority
     * 3. Priority: AMOUNT > TECHNOLOGY > PERSON > others
     */
    private List<Entity> removeDuplicates(List<Entity> entities) {
        if (entities.size() <= 1) {
            return entities;
        }
        
        // Sort by start position
        List<Entity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingInt(Entity::getStartPosition));
        
        // Remove overlaps
        List<Entity> filtered = new ArrayList<>();
        Entity previous = null;
        
        for (Entity current : sorted) {
            if (previous == null) {
                filtered.add(current);
                previous = current;
            } else {
                // Check for overlap
                if (current.getStartPosition() >= previous.getEndPosition()) {
                    // No overlap
                    filtered.add(current);
                    previous = current;
                } else {
                    // Overlap - keep higher priority entity
                    if (getPriority(current.getType()) > getPriority(previous.getType())) {
                        // Replace previous with current
                        filtered.remove(filtered.size() - 1);
                        filtered.add(current);
                        previous = current;
                    }
                    // Otherwise keep previous (skip current)
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * Get priority for entity type (higher = more important).
     */
    private int getPriority(EntityType type) {
        switch (type) {
            case AMOUNT: return 100;
            case TECHNOLOGY: return 90;
            case PERSON: return 80;
            case ORGANIZATION: return 70;
            case DATE: return 60;
            case REQUIREMENT: return 50;
            case CONSTRAINT: return 50;
            case NUMBER: return 40;
            case CONCEPT: return 30;
            default: return 10;
        }
    }
    
    // ========================================
    // OPTIONAL: BATCH EXTRACTION
    // ========================================
    
    /**
     * Extract entities from multiple prompts.
     * 
     * @param prompts List of prompts
     * @return List of entity lists (same order)
     */
    public List<List<Entity>> extractBatch(List<String> prompts) {
        List<List<Entity>> results = new ArrayList<>();
        for (String prompt : prompts) {
            results.add(extractEntities(prompt));
        }
        return results;
    }
}
