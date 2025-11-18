
package io.com.llmguardian.security.entity;
import java.time.Instant;
import java.util.UUID;

import io.com.llmguardian.security.PIIPattern;
import jakarta.persistence.*;

/**
 * JPA Entity for PII audit logs.
 * 
 * Table: pii_audit
 * 
 * Stores metadata about PII detections for compliance and analytics.
 * DOES NOT store actual PII values (only that they were detected).
 * 
 * Use cases:
 * - Compliance reporting: "We detected 1,234 emails last month"
 * - Security audit: "PII detection is working correctly"
 * - Pattern analysis: "Most common PII type is EMAIL (60%)"
 * 
 * Retention: Configurable (default 90 days), then auto-deleted
 * 
 * @author LLMGuardian Team
 */
@Entity
@Table(
    name = "pii_audit",
    indexes = {
        @Index(name = "idx_pii_audit_request", columnList = "request_id"),
        @Index(name = "idx_pii_audit_type", columnList = "pii_type"),
        @Index(name = "idx_pii_audit_created", columnList = "created_at")
    }
)
public class PIIAuditEntity {
    
    // ========================================
    // FIELDS
    // ========================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * Request ID from PIIContext.
     * Links all PII detections for a single request.
     */
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    
    /**
     * Type of PII detected (EMAIL, PHONE, etc.).
     * Stored as String for flexibility.
     */
    @Column(name = "pii_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PIIPattern piiType;
    
    /**
     * The token that was generated (e.g., [EMAIL_TOKEN_a7f3e2]).
     * Useful for debugging if issues arise.
     */
    @Column(name = "token", nullable = false, length = 100)
    private String token;
    
    /**
     * Length of original PII value (NOT the value itself!).
     * Useful for analytics.
     */
    @Column(name = "original_length", nullable = false)
    private int originalLength;
    
    /**
     * Action taken (always REDACTED in our case).
     * Future: Could be BLOCKED, MASKED, etc.
     */
    @Column(name = "action", nullable = false, length = 20)
    private String action;
    
    /**
     * Optional: Position in original text (for debugging).
     */
    @Column(name = "position_start")
    private Integer positionStart;
    
    @Column(name = "position_end")
    private Integer positionEnd;
    
    /**
     * When the PII was detected.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    /**
     * Default constructor (required by JPA).
     */
    public PIIAuditEntity() {
        this.createdAt = Instant.now();
        this.action = "REDACTED";
    }
    
    /**
     * Constructor with essential fields.
     */
    public PIIAuditEntity(
        UUID requestId,
        PIIPattern piiType,
        String token,
        int originalLength
    ) {
        this();
        this.requestId = requestId;
        this.piiType = piiType;
        this.token = token;
        this.originalLength = originalLength;
    }
    
    /**
     * Constructor with position info.
     */
    public PIIAuditEntity(
        UUID requestId,
        PIIPattern piiType,
        String token,
        int originalLength,
        int positionStart,
        int positionEnd
    ) {
        this(requestId, piiType, token, originalLength);
        this.positionStart = positionStart;
        this.positionEnd = positionEnd;
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getRequestId() {
        return requestId;
    }
    
    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
    
    public PIIPattern getPiiType() {
        return piiType;
    }
    
    public void setPiiType(PIIPattern piiType) {
        this.piiType = piiType;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public int getOriginalLength() {
        return originalLength;
    }
    
    public void setOriginalLength(int originalLength) {
        this.originalLength = originalLength;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Integer getPositionStart() {
        return positionStart;
    }
    
    public void setPositionStart(Integer positionStart) {
        this.positionStart = positionStart;
    }
    
    public Integer getPositionEnd() {
        return positionEnd;
    }
    
    public void setPositionEnd(Integer positionEnd) {
        this.positionEnd = positionEnd;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "PIIAuditEntity{id=%s, requestId=%s, type=%s, action=%s, createdAt=%s}",
            id,
            requestId,
            piiType != null ? piiType.getName() : "null",
            action,
            createdAt
        );
    }
}