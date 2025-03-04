package io.webetl.runtime;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExecutionContext provides a context for executing components.
 * It contains variables and data that can be shared between components.
 */
public class ExecutionContext {
    // Store execution-wide variables
    private final Map<String, Object> variables;
    
    // Thread-local tracking of the current component ID for multi-threaded logging
    private static final ThreadLocal<String> CURRENT_COMPONENT = new ThreadLocal<>();
    
    // Global message sequence counter to ensure messages appear in order
    private static final AtomicInteger messageSequence = new AtomicInteger(0);
    
    // Date time formatter for log timestamps
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Log level enumeration
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    // Default constructor
    public ExecutionContext() {
        this.variables = new HashMap<>();
        CURRENT_COMPONENT.set("main");
    }

    /**
     * Get a variable from the context.
     *
     * @param name the name of the variable
     * @return the value of the variable
     */
    public Object getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Set a variable in the context.
     *
     * @param name the name of the variable
     * @param value the value of the variable
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }
    
    /**
     * Set the current component ID for logging purposes.
     * Components should call this method at the beginning of their execute method.
     *
     * @param componentId the ID of the component
     */
    public void setCurrentComponentId(String componentId) {
        CURRENT_COMPONENT.set(componentId);
    }
    
    /**
     * Get the current component ID.
     *
     * @return the current component ID
     */
    public String getCurrentComponentId() {
        return CURRENT_COMPONENT.get();
    }
    
    /**
     * Clear the current component ID when execution is complete.
     */
    public void clearCurrentComponentId() {
        CURRENT_COMPONENT.remove();
    }
    
    /**
     * Log a message at INFO level.
     *
     * @param message the message to log
     */
    public void log(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Log a message at the specified level.
     *
     * @param level the log level
     * @param message the message to log
     */
    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String componentId = CURRENT_COMPONENT.get();
        String componentIdStr = (componentId != null) ? componentId : "unknown";
        int sequence = messageSequence.getAndIncrement();
        
        // Format: [SEQ] TIMESTAMP [LEVEL] [COMPONENT] MESSAGE
        System.out.println(String.format("[%06d] %s [%s] [%s] %s", 
            sequence, timestamp, level, componentIdStr, message));
    }
    
    /**
     * Log a debug message.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * Log an info message.
     *
     * @param message the message to log
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Log a warning message.
     *
     * @param message the message to log
     */
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    /**
     * Log an error message.
     *
     * @param message the message to log
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    /**
     * Log an error message with an exception.
     *
     * @param message the message to log
     * @param e the exception
     */
    public void error(String message, Throwable e) {
        log(LogLevel.ERROR, message + ": " + e.getMessage());
        e.printStackTrace();
    }
} 