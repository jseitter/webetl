package io.webetl.runtime;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {
    private final Map<String, Object> contextData = new HashMap<>();

    public void setValue(String key, Object value) {
        contextData.put(key, value);
    }

    public Object getValue(String key) {
        return contextData.get(key);
    }
} 