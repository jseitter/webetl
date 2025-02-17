package io.webetl.model.data;

import lombok.Data;

@Data
public class ColumnDefinition {
    private String name;
    private DataType type;
    private boolean nullable;
    private int length;    // For string/binary types
    private int precision; // For numeric types
    private int scale;     // For numeric types
    
    public boolean isValidValue(Object value) {
        if (value == null) return nullable;
        return type.isValidValue(value);
    }
} 