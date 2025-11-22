package io.com.llmguardian.optimization.dto;

/**
 * Represents the extracted intent from a prompt.
 * 
 * Intent = What the user is trying to accomplish
 * 
 * Examples:
 * - "Write an email to..." → GENERATE_TEXT
 * - "Explain how quantum computing works" → EXPLAIN_CONCEPT
 * - "Summarize this article" → SUMMARIZE
 * - "Translate to Spanish" → TRANSLATE
 * 
 * @author LLMGuardian Team
 */
public class Intent {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final IntentType type;
    private final double confidence;      // 0.0 to 1.0
    private final String description;     // Human-readable description
    
    // ========================================
    // INTENT TYPES
    // ========================================
    
    public enum IntentType {
        GENERATE_TEXT,      // Create new content
        EXPLAIN_CONCEPT,    // Explain something
        SUMMARIZE,          // Condense information
        TRANSLATE,          // Language translation
        ANALYZE,            // Analyze data/text
        QUESTION_ANSWER,    // Answer a question
        CODE_GENERATION,    // Write code
        CODE_EXPLANATION,   // Explain code
        CREATIVE_WRITING,   // Stories, poems, etc.
        DATA_EXTRACTION,    // Extract specific info
        COMPARISON,         // Compare things
        CLASSIFICATION,     // Categorize/classify
        UNKNOWN             // Can't determine
    }
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public Intent(IntentType type, double confidence, String description) {
        this.type = type;
        this.confidence = confidence;
        this.description = description;
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    public static Intent of(IntentType type, double confidence) {
        return new Intent(type, confidence, type.name());
    }
    
    public static Intent unknown() {
        return new Intent(IntentType.UNKNOWN, 0.0, "Unable to determine intent");
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public IntentType getType() {
        return type;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isConfident() {
        return confidence >= 0.7;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format("Intent{type=%s, confidence=%.2f, desc='%s'}", 
            type, confidence, description);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Intent intent = (Intent) o;
        return Double.compare(intent.confidence, confidence) == 0 &&
               type == intent.type;
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        long temp = Double.doubleToLongBits(confidence);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
