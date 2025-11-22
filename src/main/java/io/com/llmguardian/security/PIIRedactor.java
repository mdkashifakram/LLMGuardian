package io.com.llmguardian.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import io.com.llmguardian.security.PIIConfiguration.TokenGenerationStrategy;

/**
 * Service responsible for redacting and restoring PII.
 * 
 * Redaction Process:
 * 1. Take original text + PIIResult
 * 2. Generate unique tokens for each PII match
 * 3. Replace PII with tokens
 * 4. Store mappings in PIIContext
 * 5. Return redacted text
 * 
 * Restoration Process:
 * 1. Take redacted text
 * 2. Find all tokens
 * 3. Look up original values from PIIContext
 * 4. Replace tokens with original values
 * 5. Return restored text
 * 
 * @author LLMGuardian Team
 */
@Service
public class PIIRedactor {
    
    private static final Logger log = LoggerFactory.getLogger(PIIRedactor.class);
    
    // Token pattern: [EMAIL_TOKEN_a7f3e2]
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "\\[([A-Z_]+)_TOKEN_([a-f0-9]+)\\]"
    );
    
    // ========================================
    // DEPENDENCIES
    // ========================================
    
    @Autowired
    private PIIContext context;  // Request-scoped storage
    
    @Autowired
    private PIIConfiguration config;
    
    // ========================================
    // PUBLIC API - REDACTION
    // ========================================
    
    /**
     * Redact all PII found in text, replacing with tokens.
     * 
     * Important: Replacements are done in reverse order (from end to start)
     * to maintain correct positions.
     * 
     * @param text Original text
     * @param piiResult Detection result
     * @return Redacted text with tokens
     * 
     * Example:
     * <pre>
     * Original: "Email john@example.com or call 555-1234"
     * Redacted: "Email [EMAIL_TOKEN_a7f3e2] or call [PHONE_TOKEN_b8c4d1]"
     * </pre>
     */
    public String redact(String text, PIIResult piiResult) {
        // No PII found, return as-is
        if (!piiResult.isDetected()) {
            log.debug("No PII to redact");
            return text;
        }
        
        log.info("Redacting {} PII matches", piiResult.getTotalMatches());
        
        // Use StringBuilder for efficient string manipulation
        StringBuilder redacted = new StringBuilder(text);
        
        // Sort matches by position (descending) to replace from end to start
        // Why? To keep positions valid after each replacement
        var matches = piiResult.getMatches()
            .stream()
            .sorted((m1, m2) -> Integer.compare(m2.getStartIndex(), m1.getStartIndex()))
            .toList();
        
        // Replace each match with a token
        for (PIIMatch match : matches) {
            String token = generateToken(match.getType());
            
            // Store mapping in context
            context.addMapping(token, match.getValue(), match.getType());
            
            // Replace in text
            redacted.replace(
                match.getStartIndex(),
                match.getEndIndex(),
                token
            );
            
            log.debug("Redacted {} at position {}-{} with token {}", 
                match.getType().getName(),
                match.getStartIndex(),
                match.getEndIndex(),
                token);
        }
        
        String result = redacted.toString();
        
        log.info("Redaction complete: {} → {} characters", 
            text.length(), 
            result.length());
        
        return result;
    }
    
    // ========================================
    // PUBLIC API - RESTORATION
    // ========================================
    
    /**
     * Restore all PII tokens back to original values.
     * 
     * This is called after LLM processing to restore any tokens
     * that appear in the response.
     * 
     * @param text Text containing tokens (e.g., LLM response)
     * @return Text with tokens replaced by original values
     * 
     * Example:
     * <pre>
     * Tokenized: "We'll send details to [EMAIL_TOKEN_a7f3e2]"
     * Restored:  "We'll send details to john@example.com"
     * </pre>
     */
    public String restore(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Find all tokens in text
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder restored = new StringBuilder(text);
    AtomicInteger restoredCount = new AtomicInteger(0);

        
        // Process matches in reverse order (same reason as redaction)
        List<TokenMatch> tokenMatches = new ArrayList<>();
        while (matcher.find()) {
            String fullToken = matcher.group(0);  // [EMAIL_TOKEN_a7f3e2]
            int start = matcher.start();
            int end = matcher.end();
            
            tokenMatches.add(new TokenMatch(fullToken, start, end));
        }
        
        // Sort descending by position
        tokenMatches.sort((t1, t2) -> Integer.compare(t2.start, t1.start));
        
        // Replace each token
        for (TokenMatch tokenMatch : tokenMatches) {
            String token = tokenMatch.token;
            
            // Look up original value
            context.getOriginalValue(token).ifPresent(originalValue -> {
                restored.replace(
                    tokenMatch.start,
                    tokenMatch.end,
                    originalValue
                );
                
                log.debug("Restored token {} to original value (length {})", 
                    token, 
                    originalValue.length());
                
                    restoredCount.incrementAndGet();

            });
            
            // If token not found in context, leave it as-is
            // (might be a token generated by LLM itself)
        }
        
        if (restoredCount.get() > 0) {
            log.info("Restoration complete: {} tokens restored", restoredCount);
        } else {
            log.debug("No tokens found to restore");
        }
        
        return restored.toString();
    }
    
    // ========================================
    // PRIVATE METHODS
    // ========================================
    
    /**
     * Generate a unique token for a PII type.
     * 
     * Format: [TYPE_TOKEN_RANDOM]
     * Example: [EMAIL_TOKEN_a7f3e2]
     * 
     * Token generation strategies:
     * - RANDOM: Uses UUID (secure, no collisions)
     * - SEQUENTIAL: Uses counter (deterministic, easier debugging)
     * 
     * @param type The PII type
     * @return Generated token
     */
    private String generateToken(PIIPattern type) {
        String tokenId;
        
        if (config.getTokenGeneration() == TokenGenerationStrategy.RANDOM) {
            // Random hex (recommended)
            tokenId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, config.getTokenLength());
        } else {
            // Sequential (for debugging)
            tokenId = String.valueOf(context.getNextSequenceNumber());
        }
        
        return String.format("[%s_TOKEN_%s]", type.getName(), tokenId);
    }
    
    /**
     * Helper class for token matching during restoration
     */
    private static class TokenMatch {
        final String token;
        final int start;
        final int end;
        
        TokenMatch(String token, int start, int end) {
            this.token = token;
            this.start = start;
            this.end = end;
        }
    }
    
    // ========================================
    // OPTIONAL: PARTIAL REDACTION
    // ========================================
    
    /**
     * Redact only specific PII types.
     * 
     * Useful when you want to keep some PII visible
     * (e.g., show emails but hide SSNs).
     * 
     * @param text Original text
     * @param piiResult Detection result
     * @param typesToRedact Which types to redact
     * @return Partially redacted text
     */
    public String redactSelective(
        String text, 
        PIIResult piiResult, 
        Set<PIIPattern> typesToRedact
    ) {
        // Filter matches to only requested types
        List<PIIMatch> filteredMatches = piiResult.getMatches()
            .stream()
            .filter(match -> typesToRedact.contains(match.getType()))
            .toList();
        
        // Create filtered result
        PIIResult filtered = PIIResult.builder()
            .matches(filteredMatches)
            .build();
        
        // Use normal redaction
        return redact(text, filtered);
    }
    
    // ========================================
    // OPTIONAL: MASKING (Alternative to tokens)
    // ========================================
    
    /**
     * Mask PII instead of tokenizing.
     * 
     * Instead of: [EMAIL_TOKEN_a7f3e2]
     * Shows: j***@e***.com
     * 
     * Use case: When you want human-readable masked values
     * 
     * @param value The PII value
     * @param type The PII type
     * @return Masked value
     */
    private String mask(String value, PIIPattern type) {
        switch (type) {
            case EMAIL:
                return maskEmail(value);
            case PHONE_INTERNATIONAL:
                return maskPhone(value);
            case CREDIT_CARD:
                return maskCreditCard(value);
            default:
                return "*".repeat(value.length());
        }
    }
    
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***@***.***";
        }
        
        String username = parts[0];
        String domain = parts[1];
        
        return username.charAt(0) + "***@" + 
               domain.charAt(0) + "***" + 
               domain.substring(domain.lastIndexOf('.'));
    }
    
    private String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***-****";
        }
        return "***-" + digits.substring(digits.length() - 4);
    }
    
    private String maskCreditCard(String card) {
        String digits = card.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****-****-****-****";
        }
        return "****-****-****-" + digits.substring(digits.length() - 4);
    }
}