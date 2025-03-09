package io.webetl.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;

import io.webetl.runtime.FlowRunner;

@Service
public class RunnerService {
    private final Path dataDirectory;
    private final SimpMessagingTemplate messagingTemplate;

    public RunnerService(
        Path dataDirectory, 
        SimpMessagingTemplate messagingTemplate
    ) {
        this.dataDirectory = dataDirectory;
        this.messagingTemplate = messagingTemplate;
    }

    public CompletableFuture<Void> runSheet(String projectId, String sheetId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path projectDir = dataDirectory.resolve("projects").resolve(projectId);
                Path compiledDir = projectDir.resolve("compiled");
                Path jarPath = compiledDir.resolve(sheetId + ".jar");
                
                if (!jarPath.toFile().exists()) {
                    sendMessage("Error: Compiled JAR not found. Please compile the flow first.", sheetId);
                    return;
                }

                // Create a sequence counter for this execution session
                AtomicInteger sequenceCounter = new AtomicInteger(0);
                
                sendMessage("Starting flow execution...", sequenceCounter.getAndIncrement(), sheetId);
                sendMessage("Using JAR: " + jarPath, sequenceCounter.getAndIncrement(), sheetId);
                
                // Run the flow in a separate process
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", 
                    "-jar", 
                    jarPath.toString()
                );
                
                // Redirect error stream to output stream
                processBuilder.redirectErrorStream(true);
                
                // Start the process
                Process process = processBuilder.start();
                
                // Read the output stream
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sendMessage(line, sequenceCounter.getAndIncrement(), sheetId);
                    }
                }
                
                // Wait for the process to complete
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    sendMessage("Flow execution completed successfully", sequenceCounter.getAndIncrement(), sheetId);
                } else {
                    sendMessage("Flow execution failed with exit code: " + exitCode, sequenceCounter.getAndIncrement(), sheetId);
                }
                
            } catch (Throwable e) {
                // For uncaught exceptions, we don't have a sequence counter, so just send directly
                messagingTemplate.convertAndSend(
                    "/topic/runner/" + sheetId,
                    Map.of(
                        "sequence", -1,
                        "content", "Execution error: " + e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )
                );
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Sends a message to the websocket with sequence information to ensure
     * messages can be ordered correctly on the client side.
     */
    private void sendMessage(String content, int sequence, String sheetId) {
        Map<String, Object> message = new HashMap<>();
        message.put("sequence", sequence);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/runner/" + sheetId, message);
        
        // Small delay to help with network packet ordering
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Sends a message without sequence information.
     */
    private void sendMessage(String content, String sheetId) {
        messagingTemplate.convertAndSend(
            "/topic/runner/" + sheetId,
            Map.of(
                "sequence", -1,
                "content", content,
                "timestamp", System.currentTimeMillis()
            )
        );
    }
} 