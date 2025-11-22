package io.com.llmguardian.optimization.dto;

/**
 * Represents an extracted entity from a prompt.
 * 
 * Entity = Important information that must be preserved
 * 
 * Examples:
 * - "Write email to John" → Entity{type=PERSON, value="John"}
 * - "Convert 100 USD to EUR" → Entity{type=AMOUNT, value="100 USD"}
 * - "Explain React.js" → Entity{type=TECHNOLOGY, value="React.js"}
 * 
 * These are critical pieces that cannot be removed during optimization.
 * 
 * @author LLMGuardian Team
 */
public class Entity {
    
    // ========================================
    // FIELDS
    // ========================================
    
    private final EntityType type;
    private final String value;
    private final int startPosition;
    private final int endPosition;
    
    // ========================================
    // ENTITY TYPES
    // ========================================
    
    public enum EntityType {
        PERSON,             // Names
        ORGANIZATION,       // Company names
        LOCATION,           // Places
        DATE,               // Dates, times
        NUMBER,             // Numeric values
        AMOUNT,             // Money, quantities
        TECHNOLOGY,         // Tech terms, frameworks
        CONCEPT,            // Important concepts
        REQUIREMENT,        // Specific requirements
        CONSTRAINT,         // Limitations, rules
        UNKNOWN             // Unclassified but important
    }
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    public Entity(EntityType type, String value, int startPosition, int endPosition) {
        this.type = type;
        this.value = value;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    public static Entity of(EntityType type, String value) {
        return new Entity(type, value, -1, -1);
    }
    
    // ========================================
    // GETTERS
    // ========================================
    
    public EntityType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getStartPosition() {
        return startPosition;
    }
    
    public int getEndPosition() {
        return endPosition;
    }
    
    public boolean hasPosition() {
        return startPosition >= 0 && endPosition >= 0;
    }
    
    public int getLength() {
        return value.length();
    }
    
    // ========================================
    // UTILITY
    // ========================================
    
    @Override
    public String toString() {
        if (hasPosition()) {
            return String.format("Entity{type=%s, value='%s', pos=%d-%d}", 
                type, value, startPosition, endPosition);
        } else {
            return String.format("Entity{type=%s, value='%s'}", 
                type, value);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Entity entity = (Entity) o;
        return type == entity.type && value.equals(entity.value);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
