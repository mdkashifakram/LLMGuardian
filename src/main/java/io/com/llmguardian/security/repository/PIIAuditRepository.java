package io.com.llmguardian.security.repository;

import io.com.llmguardian.security.PIIPattern;
import io.com.llmguardian.security.entity.PIIAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PII audit logs.
 * 
 * Provides methods for:
 * - Saving audit logs
 * - Querying by request, type, time
 * - Analytics queries
 * - Cleanup (retention policy)
 * 
 * @author LLMGuardian Team
 */
@Repository
public interface PIIAuditRepository extends JpaRepository<PIIAuditEntity, UUID> {
    
    // ========================================
    // BASIC QUERIES
    // ========================================
    
    /**
     * Find all audit logs for a specific request.
     * 
     * Use case: "Show me all PII detected in request X"
     * 
     * @param requestId The request ID
     * @return List of audit logs
     */
    List<PIIAuditEntity> findByRequestId(UUID requestId);
    
    /**
     * Find all audit logs of a specific PII type.
     * 
     * Use case: "Show me all EMAIL detections"
     * 
     * @param piiType The PII type
     * @return List of audit logs
     */
    List<PIIAuditEntity> findByPiiType(PIIPattern piiType);
    
    /**
     * Find audit logs created after a specific time.
     * 
     * Use case: "Show me detections from last 24 hours"
     * 
     * @param after Start time
     * @return List of audit logs
     */
    List<PIIAuditEntity> findByCreatedAtAfter(Instant after);
    
    /**
     * Find audit logs within a time range.
     * 
     * Use case: "Show me detections for January 2025"
     * 
     * @param start Start time (inclusive)
     * @param end End time (inclusive)
     * @return List of audit logs
     */
    List<PIIAuditEntity> findByCreatedAtBetween(Instant start, Instant end);
    
    // ========================================
    // ANALYTICS QUERIES
    // ========================================
    
    /**
     * Count detections by PII type.
     * 
     * Use case: "How many emails vs phones did we detect?"
     * 
     * Returns: List of [PIIPattern, count] pairs
     */
    @Query("SELECT a.piiType, COUNT(a) FROM PIIAuditEntity a GROUP BY a.piiType")
    List<Object[]> countByPiiType();
    
    /**
     * Count detections for a specific type in a time range.
     * 
     * Use case: "How many SSNs detected this month?"
     */
    @Query("SELECT COUNT(a) FROM PIIAuditEntity a " +
           "WHERE a.piiType = :type " +
           "AND a.createdAt BETWEEN :start AND :end")
    long countByTypeAndDateRange(
        @Param("type") PIIPattern type,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Get total detections in a time range.
     * 
     * Use case: "Total PII detections last week"
     */
    long countByCreatedAtBetween(Instant start, Instant end);
    
    /**
     * Get average PII length by type.
     * 
     * Use case: "What's the average email length?"
     */
    @Query("SELECT a.piiType, AVG(a.originalLength) " +
           "FROM PIIAuditEntity a " +
           "GROUP BY a.piiType")
    List<Object[]> averageLengthByType();
    
    /**
     * Find requests with most PII detections.
     * 
     * Use case: "Which requests had unusual amounts of PII?"
     */
    @Query("SELECT a.requestId, COUNT(a) as cnt " +
           "FROM PIIAuditEntity a " +
           "GROUP BY a.requestId " +
           "ORDER BY cnt DESC")
    List<Object[]> findRequestsWithMostDetections();
    
    // ========================================
    // CLEANUP QUERIES
    // ========================================
    
    /**
     * Delete audit logs older than specified date.
     * 
     * Use case: Retention policy enforcement (e.g., delete logs > 90 days old)
     * 
     * @param before Delete records created before this time
     * @return Number of records deleted
     */
    @Modifying
    @Query("DELETE FROM PIIAuditEntity a WHERE a.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") Instant before);
    
    /**
     * Count records that would be deleted by retention policy.
     * 
     * Use case: "How many records will be cleaned up?"
     */
    long countByCreatedAtBefore(Instant before);
}
