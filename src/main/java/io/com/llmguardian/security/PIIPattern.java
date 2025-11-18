package io.com.llmguardian.security;

import java.util.regex.Pattern;
import java.util.function.Predicate;

/**
 * Defines all PII (Personally Identifiable Information) patterns that can be detected.
 * 
 * Each pattern includes:
 * - Type name (EMAIL, PHONE, etc.)
 * - Regex pattern for detection
 * - Geographic region (Universal, US, India, etc.)
 * - Default enabled status
 * - Validation function to reduce false positives
 * 
 * @author LLMGuardian Team
 */
public enum PIIPattern {
    
    // ========================================
    // TIER 1: UNIVERSAL (Always important)
    // ========================================
    
    EMAIL(
        "EMAIL",
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        "Universal",
        true,
        PIIPattern::validateEmail
    ),
    
    PHONE_INTERNATIONAL(
        "PHONE",
        "\\+?[1-9]\\d{1,14}",  // E.164 format: 7-15 digits
        "Universal",
        true,
        PIIPattern::validatePhone
    ),
    
    CREDIT_CARD(
        "CREDIT_CARD",
        "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b",
        "Universal",
        true,
        PIIPattern::validateCreditCard  // Luhn algorithm
    ),
    
    API_KEY(
        "API_KEY",
        "\\b(sk|pk|api)[-_]?[a-zA-Z0-9]{20,}\\b",
        "Universal",
        true,
        value -> true  // No additional validation needed
    ),
    
    // ========================================
    // TIER 2: GEOGRAPHIC (Configurable)
    // ========================================
    
    SSN_US(
        "SSN",
        "\\b\\d{3}-\\d{2}-\\d{4}\\b",
        "United States",
        false,  // Disabled by default, enable per region
        PIIPattern::validateSSN
    ),
    
    AADHAAR_INDIA(
        "AADHAAR",
        "\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b",  // 12 digits, optional spaces
        "India",
        false,
        PIIPattern::validateAadhaar
    ),
    
    PAN_INDIA(
        "PAN",
        "\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b",  // Format: ABCDE1234F
        "India",
        false,
        value -> true  // Pattern is specific enough
    ),
    
    NATIONAL_INSURANCE_UK(
        "NI",
        "\\b[A-Z]{2}\\d{6}[A-Z]\\b",  // Format: AB123456C
        "United Kingdom",
        false,
        value -> true
    ),
    
    // ========================================
    // TIER 3: CONTEXT-SPECIFIC (Optional)
    // ========================================
    
    IP_ADDRESS(
        "IP_ADDRESS",
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b",  // IPv4 only for now
        "Universal",
        false,  // Often needed in logs
        PIIPattern::validateIPAddress
    );
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final String name;
    private final Pattern compiledPattern;  // Pre-compiled for performance
    private final String region;
    private final boolean defaultEnabled;
    private final Predicate<String> validator;  // Custom validation function
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    PIIPattern(
        String name, 
        String regex, 
        String region, 
        boolean defaultEnabled,
        Predicate<String> validator
    ) {
        this.name = name;
        this.compiledPattern = Pattern.compile(regex);  // Compile once
        this.region = region;
        this.defaultEnabled = defaultEnabled;
        this.validator = validator;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public String getName() {
        return name;
    }
    
    public Pattern getPattern() {
        return compiledPattern;
    }
    
    public String getRegion() {
        return region;
    }
    
    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
    
    /**
     * Validates if the matched string is actually PII (reduces false positives)
     * 
     * @param value The matched string
     * @return true if likely real PII, false if likely false positive
     */
    public boolean validate(String value) {
        return validator.test(value);
    }
    
    // ========================================
    // VALIDATION FUNCTIONS
    // ========================================
    
    /**
     * Validates email format
     * Basic check - regex already did most validation
     */
    private static boolean validateEmail(String email) {
        // Reject common test/fake emails
        if (email.contains("test@") || email.contains("fake@")) {
            return false;
        }
        
        // Reject if TLD is too short (minimum 2 chars)
        String[] parts = email.split("\\.");
        if (parts.length == 0 || parts[parts.length - 1].length() < 2) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates phone number
     * Reduces false positives like order IDs, sequential numbers
     */
    private static boolean validatePhone(String phone) {
        // Remove all non-digits
        String digitsOnly = phone.replaceAll("\\D", "");
        
        // 1. Length check (7-15 digits per E.164)
        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            return false;
        }
        
        // 2. Reject if all same digit (e.g., 1111111111)
        if (digitsOnly.matches("(\\d)\\1+")) {
            return false;
        }
        
        // 3. Reject sequential (e.g., 1234567890)
        if (isSequential(digitsOnly)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates SSN using IRS rules
     * See: https://www.ssa.gov/history/ssn/geocard.html
     */
    private static boolean validateSSN(String ssn) {
        String digits = ssn.replaceAll("-", "");
        
        // Must be exactly 9 digits
        if (digits.length() != 9) {
            return false;
        }
        
        // Area number (first 3 digits) validation
        String area = digits.substring(0, 3);
        if (area.equals("000") || area.equals("666") || area.startsWith("9")) {
            return false;  // Not issued by SSA
        }
        
        // Group number (middle 2 digits) validation
        String group = digits.substring(3, 5);
        if (group.equals("00")) {
            return false;
        }
        
        // Serial number (last 4 digits) validation
        String serial = digits.substring(5);
        if (serial.equals("0000")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates credit card using Luhn algorithm
     * See: https://en.wikipedia.org/wiki/Luhn_algorithm
     */
    private static boolean validateCreditCard(String cardNumber) {
        String digits = cardNumber.replaceAll("[\\s-]", "");
        
        // Length check (13-19 digits for major card networks)
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        
        // Luhn algorithm
        int sum = 0;
        boolean alternate = false;
        
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
    
    /**
     * Validates Aadhaar number
     * Basic validation - 12 digits, not all same
     */
    private static boolean validateAadhaar(String aadhaar) {
        String digits = aadhaar.replaceAll("\\s", "");
        
        // Must be exactly 12 digits
        if (digits.length() != 12) {
            return false;
        }
        
        // Not all same digit
        if (digits.matches("(\\d)\\1+")) {
            return false;
        }
        
        // Aadhaar uses Verhoeff algorithm (complex)
        // For now, basic validation is sufficient
        
        return true;
    }
    
    /**
     * Validates IP address
     * Each octet must be 0-255
     */
    private static boolean validateIPAddress(String ip) {
        String[] octets = ip.split("\\.");
        
        if (octets.length != 4) {
            return false;
        }
        
        for (String octet : octets) {
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Helper: Check if digits are sequential
     */
    private static boolean isSequential(String digits) {
        for (int i = 1; i < digits.length(); i++) {
            int current = Character.getNumericValue(digits.charAt(i));
            int previous = Character.getNumericValue(digits.charAt(i - 1));
            
            // Allow descending too
            if (Math.abs(current - previous) != 1) {
                return false;
            }
        }
        return true;
    }
}