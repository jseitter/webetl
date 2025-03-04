package io.webetl.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;

import io.webetl.compiler.CompilationException;
import io.webetl.compiler.FlowCompilerCLI;

@Service
public class CompilerService {
    private final Path dataDirectory;
    private final SimpMessagingTemplate messagingTemplate;
    private final boolean useNewCompiler;

    public CompilerService(
        Path dataDirectory, 
        SimpMessagingTemplate messagingTemplate,
        @Value("${compiler.use-new-implementation:false}") boolean useNewCompiler
    ) {
        this.dataDirectory = dataDirectory;
        this.messagingTemplate = messagingTemplate;
        this.useNewCompiler = useNewCompiler;
    }

    public CompletableFuture<Void> compileSheet(String projectId, String sheetId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path projectDir = dataDirectory.resolve("projects").resolve(projectId);
                Path compiledDir = projectDir.resolve("compiled");
                compiledDir.toFile().mkdirs();

                // Create a sequence counter for this compilation session
                AtomicInteger sequenceCounter = new AtomicInteger(0);

                // Capture compiler output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                try {
                    // Call compiler directly
                    FlowCompilerCLI.compileSheet(
                        projectDir.resolve("sheets").resolve(sheetId + ".json").toString(),
                        compiledDir.resolve(sheetId + ".jar").toString(),
                        true,
                        useNewCompiler
                    );
                    
                    // Send captured output with sequence numbers
                    String output = baos.toString();
                    for (String line : output.split("\\R")) {
                        sendSequencedMessage(line, sequenceCounter.getAndIncrement(), sheetId);
                    }
                    
                    // Final success message
                    sendSequencedMessage("Compilation completed successfully", 
                        sequenceCounter.getAndIncrement(), sheetId);
                    
                } catch (CompilationException | IllegalStateException e) {
                    sendSequencedMessage("Compilation error: " + e.getMessage(), 
                        sequenceCounter.getAndIncrement(), sheetId);
                } catch (Throwable e) {
                    sendSequencedMessage("Unexpected error during compilation: " + e.getMessage(), 
                        sequenceCounter.getAndIncrement(), sheetId);
                    e.printStackTrace();
                } finally {
                    System.setOut(oldOut);
                }
            } catch (Throwable e) {
                // For uncaught exceptions, we don't have a sequence counter, so just send directly
                messagingTemplate.convertAndSend(
                    "/topic/compiler/" + sheetId,
                    Map.of(
                        "sequence", -1,
                        "content", "Compilation error: " + e.getMessage(),
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
    private void sendSequencedMessage(String content, int sequence, String sheetId) {
        Map<String, Object> message = new HashMap<>();
        message.put("sequence", sequence);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/compiler/" + sheetId, message);
        
        // Small delay to help with network packet ordering
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 