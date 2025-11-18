package io.com.llmguardian.security;

import java.time.Instant;

/**
 * Represents a single PII detection for audit purposes.
 * 
 * CRITICAL: This class does NOT store the actual PII value!
 * We only store:
 * - What type was detected (EMAIL, PHONE, etc.)
 * - The token that was generated
 * - When it was detected
 * - Metadata (length, position)
 * 
 * This ensures compliance - we can prove we detected and protected PII
 * without storing the actual sensitive values.
 * 
 * @author LLMGuardian Team
 */
public class PIIDetection {
    
    // ========================================
    // FIELDS (All final - immutable)
    // ========================================
    
    private final PIIPattern type;          // What type of PII
    private final String token;             // The generated token (e.g., [EMAIL_TOKEN_a7f3e2])
    private final int originalLength;       // Length of original value (not the value itself!)
    private final Instant detectedAt;       // When detected
    
    // Optional: Position in original text (useful for debugging)
    private final Integer positionStart;    // Can be null
    private final Integer positionEnd;      // Can be null
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    /**
     * Create detection record with minimal info.
     * 
     * @param type PII type
     * @param token Generated token
     * @param originalLength Length of original PII value
     * @param detectedAt When detected
     */
    public PIIDetection(
        PIIPattern type, 
        String token, 
        int originalLength, 
        Instant detectedAt
    ) {
        this.type = type;
        this.token = token;
        this.originalLength = originalLength;
        this.detectedAt = detectedAt;
        this.positionStart = null;
        this.positionEnd = null;
    }
    
    /**
     * Create detection record with position info.
     * 
     * @param type PII type
     * @param token Generated token
     * @param originalLength Length of original PII value
     * @param detectedAt When detected
     * @param positionStart Start position in text
     * @param positionEnd End position in text
     */
    public PIIDetection(
        PIIPattern type, 
        String token, 
        int originalLength, 
        Instant detectedAt,
        int positionStart,
        int positionEnd
    ) {
        this.type = type;
        this.token = token;
        this.originalLength = originalLength;
        this.detectedAt = detectedAt;
        this.positionStart = positionStart;
        this.positionEnd = positionEnd;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public PIIPattern getType() {
        return type;
    }
    
    public String getToken() {
        return token;
    }
    
    public int getOriginalLength() {
        return originalLength;
    }
    
    public Instant getDetectedAt() {
        return detectedAt;
    }
    
    public Integer getPositionStart() {
        return positionStart;
    }
    
    public Integer getPositionEnd() {
        return positionEnd;
    }
    
    public boolean hasPositionInfo() {
        return positionStart != null && positionEnd != null;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        if (hasPositionInfo()) {
            return String.format(
                "PIIDetection{type=%s, token=%s, length=%d, position=%d-%d, at=%s}",
                type.getName(),
                token,
                originalLength,
                positionStart,
                positionEnd,
                detectedAt
            );
        } else {
            return String.format(
                "PIIDetection{type=%s, token=%s, length=%d, at=%s}",
                type.getName(),
                token,
                originalLength,
                detectedAt
            );
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PIIDetection that = (PIIDetection) o;
        
        return originalLength == that.originalLength &&
               type == that.type &&
               token.equals(that.token) &&
               detectedAt.equals(that.detectedAt);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + token.hashCode();
        result = 31 * result + originalLength;
        result = 31 * result + detectedAt.hashCode();
        return result;
    }
}