package io.webetl.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;

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
                    
                    // Send captured output
                    String output = baos.toString();
                    for (String line : output.split("\\R")) {
                        messagingTemplate.convertAndSend("/topic/compiler/" + sheetId, line);
                    }
                    
                    messagingTemplate.convertAndSend(
                        "/topic/compiler/" + sheetId,
                        "Compilation completed successfully"
                    );
                } catch (CompilationException | IllegalStateException e) {
                    messagingTemplate.convertAndSend(
                        "/topic/compiler/" + sheetId,
                        "Compilation error: " + e.getMessage()
                    );
                } catch (Throwable e) {
                    messagingTemplate.convertAndSend(
                        "/topic/compiler/" + sheetId,
                        "Unexpected error during compilation: " + e.getMessage()
                    );
                    e.printStackTrace();
                } finally {
                    System.setOut(oldOut);
                }
            } catch (Throwable e) {
                messagingTemplate.convertAndSend(
                    "/topic/compiler/" + sheetId,
                    "Compilation error: " + e.getMessage()
                );
                e.printStackTrace();
            }
        });
    }
} 