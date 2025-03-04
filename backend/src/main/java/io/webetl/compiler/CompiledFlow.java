package io.webetl.compiler;

import io.webetl.runtime.ExecutionContext;
import java.util.Map;

public abstract class CompiledFlow {
    public abstract void execute(ExecutionContext context) throws Exception;
    
    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }
} 