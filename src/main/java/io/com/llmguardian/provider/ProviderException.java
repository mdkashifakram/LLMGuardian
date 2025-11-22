package io.com.llmguardian.provider;

/**
 * Exception thrown when LLM provider operations fail.
 * 
 * Wraps various provider-specific errors:
 * - API connection failures
 * - Rate limiting (429)
 * - Authentication errors (401)
 * - Invalid requests (400)
 * - Server errors (500)
 * - Timeout errors
 * 
 * @author LLMGuardian Team
 */
public class ProviderException extends RuntimeException {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final ErrorType errorType;
    private final Integer httpStatusCode;
    private final boolean retryable;
    
    // ========================================
    // ERROR TYPES
    // ========================================
    
    public enum ErrorType {
        // Client errors (4xx)
        AUTHENTICATION_ERROR,    // 401: Invalid API key
        RATE_LIMIT_ERROR,       // 429: Too many requests
        INVALID_REQUEST,        // 400: Bad request format
        NOT_FOUND,              // 404: Model not found
        
        // Server errors (5xx)
        SERVER_ERROR,           // 500: Provider internal error
        SERVICE_UNAVAILABLE,    // 503: Service temporarily down
        
        // Network errors
        TIMEOUT,                // Request timeout
        CONNECTION_ERROR,       // Network connection failed
        
        // Other
        UNKNOWN                 // Unexpected error
    }
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    public ProviderException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.httpStatusCode = null;
        this.retryable = false;
    }
    
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
        this.httpStatusCode = null;
        this.retryable = false;
    }
    
    public ProviderException(
        String message,
        ErrorType errorType,
        Integer httpStatusCode,
        boolean retryable
    ) {
        super(message);
        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }
    
    public ProviderException(
        String message,
        Throwable cause,
        ErrorType errorType,
        Integer httpStatusCode,
        boolean retryable
    ) {
        super(message, cause);
        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    /**
     * Create authentication error (401).
     */
    public static ProviderException authenticationError(String message) {
        return new ProviderException(
            message,
            ErrorType.AUTHENTICATION_ERROR,
            401,
            false  // Don't retry auth errors
        );
    }
    
    /**
     * Create rate limit error (429).
     */
    public static ProviderException rateLimitError(String message) {
        return new ProviderException(
            message,
            ErrorType.RATE_LIMIT_ERROR,
            429,
            true  // Retryable after backoff
        );
    }
    
    /**
     * Create invalid request error (400).
     */
    public static ProviderException invalidRequest(String message) {
        return new ProviderException(
            message,
            ErrorType.INVALID_REQUEST,
            400,
            false  // Don't retry bad requests
        );
    }
    
    /**
     * Create server error (500).
     */
    public static ProviderException serverError(String message) {
        return new ProviderException(
            message,
            ErrorType.SERVER_ERROR,
            500,
            true  // Retryable - provider issue
        );
    }
    
    /**
     * Create service unavailable error (503).
     */
    public static ProviderException serviceUnavailable(String message) {
        return new ProviderException(
            message,
            ErrorType.SERVICE_UNAVAILABLE,
            503,
            true  // Retryable - temporary
        );
    }
    
    /**
     * Create timeout error.
     */
    public static ProviderException timeout(String message) {
        return new ProviderException(
            message,
            ErrorType.TIMEOUT,
            null,
            true  // Retryable
        );
    }
    
    /**
     * Create connection error.
     */
    public static ProviderException connectionError(String message, Throwable cause) {
        return new ProviderException(
            message,
            cause,
            ErrorType.CONNECTION_ERROR,
            null,
            true  // Retryable
        );
    }
    
    /**
     * Create from HTTP status code.
     */
    public static ProviderException fromHttpStatus(int statusCode, String message) {
        ErrorType type;
        boolean retryable;
        
        switch (statusCode) {
            case 401:
            case 403:
                type = ErrorType.AUTHENTICATION_ERROR;
                retryable = false;
                break;
            
            case 429:
                type = ErrorType.RATE_LIMIT_ERROR;
                retryable = true;
                break;
            
            case 400:
                type = ErrorType.INVALID_REQUEST;
                retryable = false;
                break;
            
            case 404:
                type = ErrorType.NOT_FOUND;
                retryable = false;
                break;
            
            case 500:
            case 502:
            case 504:
                type = ErrorType.SERVER_ERROR;
                retryable = true;
                break;
            
            case 503:
                type = ErrorType.SERVICE_UNAVAILABLE;
                retryable = true;
                break;
            
            default:
                type = ErrorType.UNKNOWN;
                retryable = statusCode >= 500; // Retry server errors
        }
        
        return new ProviderException(message, type, statusCode, retryable);
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ProviderException{");
        sb.append("type=").append(errorType);
        
        if (httpStatusCode != null) {
            sb.append(", httpStatus=").append(httpStatusCode);
        }
        
        sb.append(", retryable=").append(retryable);
        sb.append(", message='").append(getMessage()).append("'");
        sb.append('}');
        
        return sb.toString();
    }
}
