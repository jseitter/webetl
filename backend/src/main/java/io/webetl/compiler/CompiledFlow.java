package io.webetl.compiler;

import io.webetl.runtime.ExecutionContext;
import java.util.Map;

/**
 * Base class of all compiled flows.
 */
public abstract class CompiledFlow {
    
    public abstract void execute(ExecutionContext context) throws Exception;
   
} 