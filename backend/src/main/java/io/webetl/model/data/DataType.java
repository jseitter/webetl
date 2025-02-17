package io.webetl.model.data;

public enum DataType {
    STRING(String.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    DOUBLE(Double.class),
    BOOLEAN(Boolean.class),
    DATE(java.util.Date.class),
    TIMESTAMP(java.time.Instant.class);
    
    private final Class<?> javaType;
    
    DataType(Class<?> javaType) {
        this.javaType = javaType;
    }
    
    public boolean isValidValue(Object value) {
        return value == null || javaType.isInstance(value);
    }
    
    public Class<?> getJavaType() {
        return javaType;
    }
} 