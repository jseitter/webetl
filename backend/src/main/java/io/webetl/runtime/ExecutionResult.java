package io.webetl.runtime;

public class ExecutionResult {
    private final boolean success;
    private final String message;
    private final RuntimeMetrics metrics;

    public ExecutionResult(boolean success, String message, RuntimeMetrics metrics) {
        this.success = success;
        this.message = message;
        this.metrics = metrics;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public RuntimeMetrics getMetrics() { return metrics; }
} 