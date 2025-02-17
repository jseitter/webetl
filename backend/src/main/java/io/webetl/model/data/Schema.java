package io.webetl.model.data;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema is a collection of column definitions.
 */
@Data
public class Schema {
    private String name;
    private Map<String, ColumnDefinition> columns;
    
    public Schema() {
        this.columns = new LinkedHashMap<>(); // Preserve column order
    }
    
    public void addColumn(String name, ColumnDefinition definition) {
        columns.put(name, definition);
    }
    
    public boolean validateValue(String columnName, Object value) {
        ColumnDefinition def = columns.get(columnName);
        return def != null && def.isValidValue(value);
    }
} 