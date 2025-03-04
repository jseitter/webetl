package io.webetl.model.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Row represents a row of data in the ETL pipeline.
 */
public class Row {
    private String id;
    private Map<String, Object> data;
    private boolean terminator = false;
    private RowMetadata metadata;        // optional: timestamp, source info, etc.
    private Schema schema;               // defines the structure of this row

    public Row() {
        this.data = new HashMap<>();
    }

    public Row(String line) {
        this.data = new HashMap<>();
        this.data.put("line", line);
        this.metadata = new RowMetadata();
    }

    public void setValue(String column, Object value) {
        if (schema != null && !schema.validateValue(column, value)) {
            throw new IllegalArgumentException(
                String.format("Value %s is not valid for column %s", value, column));
        }
        data.put(column, value);
    }

    public Object getValue(String column) {
        return data.get(column);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(data);
    }

    public static Row createTerminator() {
        Row row = new Row();
        row.terminator = true;
        return row;
    }
    
    public boolean isTerminator() {
        return terminator;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        if (isTerminator()) {
            return "Row[TERMINATOR]";
        }
        return "Row[id=" + id + ", data=" + data + "]";
    }
} 