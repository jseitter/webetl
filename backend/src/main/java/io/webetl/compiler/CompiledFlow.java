package io.webetl.compiler;

import java.util.Map;

public abstract class CompiledFlow {
    public abstract void execute(Map<String, Object> context);
    
    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }
} 