package io.webetl.model.component;

import io.webetl.runtime.ExecutionContext;

/**
 * ExecutableComponent is a component that can be executed.
 * It has a method to execute the component.
 * The execution context is provided to the component to 
 * pass configuration, state, and other dependencies to the component.
 */
public interface ExecutableComponent {
    /**
     * Execute the component.
     * @param context the execution context
     */
    public void execute(ExecutionContext context) throws Exception;
}
