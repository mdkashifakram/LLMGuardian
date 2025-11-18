package io.com.llmguardian.security;

/**
 * Represents a single PII match found in text.
 * 
 * Immutable data holder containing:
 * - What type of PII was found
 * - The actual value (e.g., "john@example.com")
 * - Where it was found (start/end positions)
 * 
 * @author LLMGuardian Team
 */
public class PIIMatch {
    
    // ========================================
    // FIELDS (All final - immutable)
    // ========================================
    
    private final PIIPattern type;      // What kind of PII
    private final String value;         // The actual PII value
    private final int startIndex;       // Character position where it starts
    private final int endIndex;         // Character position where it ends
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public PIIMatch(PIIPattern type, String value, int startIndex, int endIndex) {
        this.type = type;
        this.value = value;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    
    // ========================================
    // GETTERS (No setters - immutable!)
    // ========================================
    
    public PIIPattern getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public int getLength() {
        return endIndex - startIndex;
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "PIIMatch{type=%s, value='***', position=%d-%d}",
            type.getName(),
            startIndex,
            endIndex
        );
        // Note: We hide the actual value in toString for security
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PIIMatch piiMatch = (PIIMatch) o;
        
        return startIndex == piiMatch.startIndex &&
               endIndex == piiMatch.endIndex &&
               type == piiMatch.type &&
               value.equals(piiMatch.value);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + startIndex;
        result = 31 * result + endIndex;
        return result;
    }
}