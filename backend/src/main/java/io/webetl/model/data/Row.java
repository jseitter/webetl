package io.webetl.model.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Row is a collection of values.
 */
public class Row {
    private Map<String, Object> values;  // column name -> value
    private RowMetadata metadata;        // optional: timestamp, source info, etc.
    private Schema schema;               // defines the structure of this row

    public Row() {
        this.values = new HashMap<>();
    }

    public Row(String line) {
        this.values = new HashMap<>();
        this.values.put("line", line);
        this.metadata = new RowMetadata();
    }

    public void setValue(String column, Object value) {
        if (schema != null && !schema.validateValue(column, value)) {
            throw new IllegalArgumentException(
                String.format("Value %s is not valid for column %s", value, column));
        }
        values.put(column, value);
    }

    public Object getValue(String column) {
        return values.get(column);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }
} 