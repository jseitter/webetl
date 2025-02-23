package io.webetl.compiler;

public class CompilationException extends RuntimeException {
    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
} 