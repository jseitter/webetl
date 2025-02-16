package io.webetl.runtime;

import java.util.Map;

public class ComponentConfig {
    private final Map<String, Object> config;

    public ComponentConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getParameter(String name) {
        return config.get(name) != null ? config.get(name).toString() : null;
    }
} 