package io.webetl.compiler;

import io.webetl.model.Sheet;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FlowCompilerCLI {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: FlowCompilerCLI <input-sheet.json> <output.jar>");
            System.exit(1);
        }

        try {
            String inputFile = args[0];
            String outputFile = args[1];

            // Read the sheet JSON
            ObjectMapper mapper = new ObjectMapper();
            Sheet sheet = mapper.readValue(Paths.get(inputFile).toFile(), Sheet.class);

            // Compile the sheet
            FlowCompiler compiler = new FlowCompiler();
            Path jarFile = compiler.compileToJar(sheet).toPath();

            // Move to desired output location
            Files.move(jarFile, Paths.get(outputFile));

            System.out.println("Successfully compiled flow to: " + outputFile);
            System.out.println("You can run it with: java -jar " + outputFile);

        } catch (Exception e) {
            System.err.println("Error compiling flow: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 