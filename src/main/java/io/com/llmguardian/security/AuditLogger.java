package io.com.llmguardian.security;

import io.com.llmguardian.security.entity.PIIAuditEntity;
import io.com.llmguardian.security.repository.PIIAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for logging PII detections to database for audit/compliance.
 * 
 * Features:
 * - Async logging (doesn't slow down request)
 * - Batch logging (efficient)
 * - Automatic cleanup (retention policy)
 * - Analytics queries
 * 
 * @author LLMGuardian Team
 */
@Service
public class AuditLogger {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private PIIAuditRepository auditRepository;
    
    @Autowired
    private PIIConfiguration config;
    
    // ========================================
    // PUBLIC API - LOGGING
    // ========================================
    
    /**
     * Log all PII detections from a request context.
     * 
     * This is called at the end of request processing to persist
     * audit records to the database.
     * 
     * Async: Runs in separate thread to not block request.
     * 
     * @param context The PII context containing detections
     */
    @Async
    @Transactional
    public void logDetections(PIIContext context) {
        // Check if audit is enabled
        if (!config.isAuditEnabled()) {
            log.trace("Audit logging disabled, skipping");
            return;
        }
        
        // Check if any detections to log
        if (!context.hasDetections()) {
            log.trace("No PII detections to log for request: {}", context.getRequestId());
            return;
        }
        
        try {
            List<PIIDetection> detections = context.getDetections();
            
            log.debug("Logging {} PII detections for request: {}", 
                detections.size(), 
                context.getRequestId());
            
            // Convert to entities based on audit level
            List<PIIAuditEntity> entities = detections.stream()
                .map(detection -> createAuditEntity(context, detection))
                .collect(Collectors.toList());
            
            // Batch save (efficient)
            auditRepository.saveAll(entities);
            
            log.info("Successfully logged {} audit records for request: {}", 
                entities.size(), 
                context.getRequestId());
            
        } catch (Exception e) {
            // Log error but don't fail the request
            log.error("Failed to log PII audit for request: {}", 
                context.getRequestId(), 
                e);
        }
    }
    
    /**
     * Log a single PII detection immediately.
     * 
     * Use case: Real-time audit requirements
     * 
     * @param requestId Request ID
     * @param detection PII detection
     */
    @Async
    @Transactional
    public void logDetection(UUID requestId, PIIDetection detection) {
        if (!config.isAuditEnabled()) {
            return;
        }
        
        try {
            PIIAuditEntity entity = new PIIAuditEntity(
                requestId,
                detection.getType(),
                detection.getToken(),
                detection.getOriginalLength()
            );
            
            // Add position info if available and audit level is DETAILED
            if (detection.hasPositionInfo() && 
                config.getAuditLevel() == PIIConfiguration.AuditLevel.DETAILED) {
                entity.setPositionStart(detection.getPositionStart());
                entity.setPositionEnd(detection.getPositionEnd());
            }
            
            auditRepository.save(entity);
            
            log.debug("Logged {} detection for request: {}", 
                detection.getType().getName(), 
                requestId);
            
        } catch (Exception e) {
            log.error("Failed to log PII detection", e);
        }
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Create audit entity from detection based on audit level.
     */
    private PIIAuditEntity createAuditEntity(PIIContext context, PIIDetection detection) {
        PIIAuditEntity entity = new PIIAuditEntity(
            context.getRequestId(),
            detection.getType(),
            detection.getToken(),
            detection.getOriginalLength()
        );
        
        // Include position info only for DETAILED level
        if (detection.hasPositionInfo() && 
            config.getAuditLevel() == PIIConfiguration.AuditLevel.DETAILED) {
            entity.setPositionStart(detection.getPositionStart());
            entity.setPositionEnd(detection.getPositionEnd());
        }
        
        return entity;
    }
    
    // ========================================
    // ANALYTICS METHODS
    // ========================================
    
    /**
     * Get count of each PII type detected.
     * 
     * @return Map of PIIPattern → count
     */
    public Map<PIIPattern, Long> getDetectionCountsByType() {
        List<Object[]> results = auditRepository.countByPiiType();
        
        return results.stream()
            .collect(Collectors.toMap(
                row -> (PIIPattern) row[0],
                row -> (Long) row[1]
            ));
    }
    
    /**
     * Get total detections in last N days.
     * 
     * @param days Number of days to look back
     * @return Total count
     */
    public long getDetectionCountLastNDays(int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant end = Instant.now();
        
        return auditRepository.countByCreatedAtBetween(start, end);
    }
    
    /**
     * Get detections for a specific request.
     * 
     * @param requestId Request ID
     * @return List of audit entities
     */
    public List<PIIAuditEntity> getDetectionsByRequest(UUID requestId) {
        return auditRepository.findByRequestId(requestId);
    }
    
    // ========================================
    // CLEANUP (Retention Policy)
    // ========================================
    
    /**
     * Clean up old audit logs based on retention policy.
     * 
     * Scheduled to run daily at 2 AM.
     * Deletes records older than configured retention period.
     */
    @Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
    @Transactional
    public void cleanupOldAuditLogs() {
        if (!config.isAuditEnabled()) {
            return;
        }
        
        int retentionDays = config.getAuditRetentionDays();
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        log.info("Starting audit log cleanup: deleting records older than {} days", 
            retentionDays);
        
        try {
            // Count before deleting (for logging)
            long countToDelete = auditRepository.countByCreatedAtBefore(cutoffDate);
            
            if (countToDelete == 0) {
                log.info("No audit logs to clean up");
                return;
            }
            
            // Delete old records
            int deleted = auditRepository.deleteByCreatedAtBefore(cutoffDate);
            
            log.info("Cleaned up {} audit log records (older than {} days)", 
                deleted, 
                retentionDays);
            
        } catch (Exception e) {
            log.error("Failed to cleanup audit logs", e);
        }
    }
    
    /**
     * Manually trigger cleanup (for testing or admin operations).
     * 
     * @param retentionDays Delete records older than this many days
     * @return Number of records deleted
     */
    @Transactional
    public int cleanupManual(int retentionDays) {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        log.info("Manual cleanup triggered: deleting records older than {} days", 
            retentionDays);
        
        int deleted = auditRepository.deleteByCreatedAtBefore(cutoffDate);
        
        log.info("Manually deleted {} audit log records", deleted);
        
        return deleted;
    }
}