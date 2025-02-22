package io.webetl.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import io.webetl.compiler.FlowCompilerCLI;

@Service
public class CompilerService {
    private final Path dataDirectory;
    private final SimpMessagingTemplate messagingTemplate;

    public CompilerService(
        Path dataDirectory, 
        SimpMessagingTemplate messagingTemplate
    ) {
        this.dataDirectory = dataDirectory;
        this.messagingTemplate = messagingTemplate;
    }

    public CompletableFuture<Void> compileSheet(String projectId, String sheetId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path projectDir = dataDirectory.resolve("projects").resolve(projectId);
                Path compiledDir = projectDir.resolve("compiled");
                compiledDir.toFile().mkdirs();

                // Capture compiler output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                try {
                    // Call compiler directly
                    String[] args = new String[] {
                        "compile",
                        projectDir.resolve("sheets").resolve(sheetId + ".json").toString(),
                        compiledDir.resolve(sheetId + ".jar").toString()
                    };
                    
                    FlowCompilerCLI.main(args);
                    
                    // Send captured output
                    String output = baos.toString();
                    for (String line : output.split("\n")) {
                        messagingTemplate.convertAndSend("/topic/compiler/" + sheetId, line);
                    }
                    
                    messagingTemplate.convertAndSend(
                        "/topic/compiler/" + sheetId,
                        "Compilation completed successfully"
                    );
                } finally {
                    System.setOut(oldOut);
                }
            } catch (Exception e) {
                messagingTemplate.convertAndSend(
                    "/topic/compiler/" + sheetId,
                    "Compilation error: " + e.getMessage()
                );
            }
        });
    }
} 