package io.com.llmguardian.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for generating cache keys.
 * 
 * Cache keys are deterministic hashes that uniquely identify:
 * - Prompt content
 * - Model used
 * - Optional parameters (temperature, max_tokens, etc.)
 * 
 * Key Format: {prefix}:{hash}
 * Example: "llm:a7f3e2b8c4d1"
 * 
 * Uses SHA-256 for collision-resistant hashing.
 * 
 * @author LLMGuardian Team
 */
@Component
public class CacheKeyGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(CacheKeyGenerator.class);
    
    // ========================================
    // CONFIGURATION
    // ========================================
    
    private static final String KEY_PREFIX = "llm";
    private static final String SEPARATOR = ":";
    private static final int HASH_LENGTH = 12; // First 12 chars of hash
    
    // ========================================
    // PUBLIC API
    // ========================================
    
    /**
     * Generate cache key for a prompt and model.
     * 
     * @param prompt The prompt text
     * @param modelId The model identifier (e.g., "gpt-4o-mini")
     * @return Cache key (e.g., "llm:a7f3e2b8c4d1")
     */
    public String generateKey(String prompt, String modelId) {
        return generateKey(prompt, modelId, null);
    }
    
    /**
     * Generate cache key with optional parameters.
     * 
     * @param prompt The prompt text
     * @param modelId The model identifier
     * @param parameters Optional parameters (temperature, max_tokens, etc.)
     * @return Cache key
     */
    public String generateKey(String prompt, String modelId, String parameters) {
        // Build composite string to hash
        StringBuilder composite = new StringBuilder();
        composite.append(prompt);
        composite.append("|");
        composite.append(modelId);
        
        if (parameters != null && !parameters.isEmpty()) {
            composite.append("|");
            composite.append(parameters);
        }
        
        // Generate hash
        String hash = generateHash(composite.toString());
        
        // Build key
        String key = KEY_PREFIX + SEPARATOR + hash;
        
        log.trace("Generated cache key: {} (prompt length: {}, model: {})", 
            key, 
            prompt.length(), 
            modelId);
        
        return key;
    }
    
    /**
     * Generate cache key from multiple components.
     * 
     * Useful for custom caching scenarios.
     * 
     * @param components Variable components to include in key
     * @return Cache key
     */
    public String generateKeyFromComponents(String... components) {
        String composite = String.join("|", components);
        String hash = generateHash(composite);
        return KEY_PREFIX + SEPARATOR + hash;
    }
    
    // ========================================
    // HASHING
    // ========================================
    
    /**
     * Generate SHA-256 hash of input string.
     * 
     * Returns first 12 characters of base64-encoded hash
     * (72 bits = ~4.7 trillion combinations, collision-resistant for our use case)
     * 
     * @param input String to hash
     * @return First 12 chars of hash
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to base64 (URL-safe, no padding)
            String base64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);
            
            // Take first 12 characters
            return base64.substring(0, HASH_LENGTH);
            
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            log.error("SHA-256 algorithm not available", e);
            // Fallback: use simple hashCode (not recommended for production)
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
    
    // ========================================
    // VALIDATION
    // ========================================
    
    /**
     * Check if a string is a valid cache key.
     * 
     * @param key Key to validate
     * @return true if valid format
     */
    public boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        // Check format: "llm:hash"
        if (!key.startsWith(KEY_PREFIX + SEPARATOR)) {
            return false;
        }
        
        // Check hash length
        String hash = key.substring(KEY_PREFIX.length() + SEPARATOR.length());
        return hash.length() == HASH_LENGTH;
    }
    
    /**
     * Extract hash from cache key.
     * 
     * @param key Cache key
     * @return Hash portion (without prefix)
     */
    public String extractHash(String key) {
        if (!isValidKey(key)) {
            throw new IllegalArgumentException("Invalid cache key: " + key);
        }
        
        return key.substring(KEY_PREFIX.length() + SEPARATOR.length());
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    /**
     * Get key prefix.
     */
    public String getKeyPrefix() {
        return KEY_PREFIX;
    }
    
    /**
     * Check if key belongs to our cache.
     * 
     * Useful for Redis where multiple apps share same instance.
     */
    public boolean belongsToThisCache(String key) {
        return key != null && key.startsWith(KEY_PREFIX + SEPARATOR);
    }
}
