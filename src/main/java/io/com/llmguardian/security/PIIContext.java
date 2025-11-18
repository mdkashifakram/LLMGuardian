package io.com.llmguardian.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request-scoped context for storing PII mappings during a request.
 * 
 * Lifecycle:
 * 1. Created when request starts
 * 2. Used by PIIRedactor to store/retrieve mappings
 * 3. Used by AuditLogger to log detections
 * 4. Destroyed when request ends (mappings gone!)
 * 
 * This bean is created once per HTTP request and shared across
 * all components processing that request.
 * 
 * @author LLMGuardian Team
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PIIContext {
    
    private static final Logger log = LoggerFactory.getLogger(PIIContext.class);
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final UUID requestId;                          // Unique ID for this request
    private final Map<String, String> tokenMap;            // token → original PII value
    private final List<PIIDetection> detections;           // What was detected (for audit)
    private final Instant createdAt;                       // When context was created
    private final AtomicInteger sequenceCounter;           // For sequential token generation
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public PIIContext() {
        this.requestId = UUID.randomUUID();
        this.tokenMap = new ConcurrentHashMap<>();         // Thread-safe
        this.detections = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = Instant.now();
        this.sequenceCounter = new AtomicInteger(1);
        
        log.debug("Created PIIContext for request: {}", requestId);
    }
    
    // ========================================
    // PUBLIC API - TOKEN MAPPING
    // ========================================
    
    /**
     * Add a PII mapping (token → original value).
     * 
     * Called by PIIRedactor during redaction.
     * 
     * @param token The generated token (e.g., [EMAIL_TOKEN_a7f3e2])
     * @param originalValue The actual PII value
     * @param type The PII type
     */
    public void addMapping(String token, String originalValue, PIIPattern type) {
        tokenMap.put(token, originalValue);
        
        // Also record the detection (for audit)
        PIIDetection detection = new PIIDetection(
            type,
            token,
            originalValue.length(),  // Store length, not value (security)
            Instant.now()
        );
        detections.add(detection);
        
        log.trace("Added mapping: {} → *** (length: {})", 
            token, 
            originalValue.length());
    }
    
    /**
     * Get original PII value for a token.
     * 
     * Called by PIIRedactor during restoration.
     * 
     * @param token The token to look up
     * @return Optional containing original value, or empty if not found
     */
    public Optional<String> getOriginalValue(String token) {
        return Optional.ofNullable(tokenMap.get(token));
    }
    
    /**
     * Check if a token exists in this context.
     * 
     * @param token The token to check
     * @return true if token was created in this request
     */
    public boolean hasToken(String token) {
        return tokenMap.containsKey(token);
    }
    
    /**
     * Get all tokens created in this request.
     * 
     * Useful for validation or debugging.
     * 
     * @return Set of all tokens (keys only, not values)
     */
    public Set<String> getAllTokens() {
        return new HashSet<>(tokenMap.keySet());
    }
    
    // ========================================
    // PUBLIC API - DETECTION INFO
    // ========================================
    
    /**
     * Get all PII detections made in this request.
     * 
     * Used by AuditLogger for compliance logging.
     * 
     * @return Unmodifiable list of detections
     */
    public List<PIIDetection> getDetections() {
        return Collections.unmodifiableList(new ArrayList<>(detections));
    }
    
    /**
     * Get count of detections by type.
     * 
     * Example: {EMAIL=2, PHONE=1}
     * 
     * @return Map of type → count
     */
    public Map<PIIPattern, Long> getDetectionCounts() {
        return detections.stream()
            .collect(Collectors.groupingBy(
                PIIDetection::getType,
                Collectors.counting()
            ));
    }
    
    /**
     * Check if any PII was detected in this request.
     * 
     * @return true if PII was found
     */
    public boolean hasDetections() {
        return !detections.isEmpty();
    }
    
    // ========================================
    // PUBLIC API - METADATA
    // ========================================
    
    public UUID getRequestId() {
        return requestId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get next sequential number (for sequential token generation).
     * 
     * @return Next number in sequence (1, 2, 3, ...)
     */
    public int getNextSequenceNumber() {
        return sequenceCounter.getAndIncrement();
    }
    
    /**
     * Get total number of PII values redacted.
     * 
     * @return Count of mappings
     */
    public int getRedactionCount() {
        return tokenMap.size();
    }
    
    /**
     * Get request lifetime (useful for performance tracking).
     * 
     * @return Milliseconds since context was created
     */
    public long getLifetimeMs() {
        return Instant.now().toEpochMilli() - createdAt.toEpochMilli();
    }
    
    // ========================================
    // CLEANUP (Optional)
    // ========================================
    
    /**
     * Clear all mappings (for manual cleanup if needed).
     * 
     * Note: Spring automatically destroys this bean at request end,
     * so manual cleanup is usually not needed.
     */
    public void clear() {
        tokenMap.clear();
        detections.clear();
        log.debug("Cleared PIIContext for request: {}", requestId);
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        return String.format(
            "PIIContext{requestId=%s, redactions=%d, detections=%d, lifetime=%dms}",
            requestId,
            tokenMap.size(),
            detections.size(),
            getLifetimeMs()
        );
    }
}