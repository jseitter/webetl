package io.webetl.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.webetl.model.Sheet;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FlowCompilerCLI {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        
        try {
            switch (command) {
                case "list":
                    listSheets();
                    break;
                case "compile":
                    if (args.length < 3) {
                        System.err.println("Error: Missing arguments for compile command");
                        printUsage();
                        System.exit(1);
                    }
                    boolean useNewCompiler = false;
                    boolean verbose = false;
                    for (int i = 3; i < args.length; i++) {
                        if ("--verbose".equals(args[i])) verbose = true;
                        if ("--new-compiler".equals(args[i])) useNewCompiler = true;
                    }
                    compileSheetCLI(args[1], args[2], verbose, useNewCompiler);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  List sheets:  compile.sh list");
        System.out.println("  Compile:      compile.sh compile <input-sheet.json> <output.jar> [--verbose] [--new-compiler]");
    }
    
    private static void listSheets() throws Exception {
        Path appDir = Paths.get(System.getProperty("user.home"), ".webetl");
        Path projectsDir = appDir.resolve("projects");
        
        if (!Files.exists(projectsDir)) {
            System.out.println("No projects found in " + projectsDir);
            return;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        
        Files.list(projectsDir).forEach(projectDir -> {
            if (Files.isDirectory(projectDir)) {
                System.out.println("\nProject: " + projectDir.getFileName());
                try {
                    Path sheetsDir = projectDir.resolve("sheets");
                    if (Files.exists(sheetsDir)) {
                        Files.list(sheetsDir)
                            .filter(p -> p.toString().endsWith(".json"))
                            .forEach(sheetFile -> {
                                try {
                                    Sheet sheet = mapper.readValue(sheetFile.toFile(), Sheet.class);
                                    System.out.printf("  - Sheet: %s (ID: %s)%n", 
                                        sheet.getName(), sheet.getId());
                                } catch (Exception e) {
                                    System.out.printf("  - Error reading %s: %s%n", 
                                        sheetFile.getFileName(), e.getMessage());
                                }
                            });
                    }
                } catch (Exception e) {
                    System.out.println("  Error listing sheets: " + e.getMessage());
                }
            }
        });
    }
    
    private static void compileSheetCLI(String inputFile, String outputFile, boolean verbose, boolean useNewCompiler) {
        try {
            compileSheet(inputFile, outputFile, verbose, useNewCompiler);
        } catch (Exception e) {
            System.err.println("Compilation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void compileSheet(String inputFile, String outputFile, boolean verbose, boolean useNewCompiler) throws CompilationException {
        // Read and parse sheet
        try {
            ObjectMapper mapper = new ObjectMapper();
            Sheet sheet = mapper.readValue(new File(inputFile), Sheet.class);

            // Compile
            File jarFile;
            if (useNewCompiler) {
                FlowCompilerNG compiler = new FlowCompilerNG();
                jarFile = compiler.compileToJar(sheet, verbose);
            } else {
                FlowCompiler compiler = new FlowCompiler();
                jarFile = compiler.compileToJar(sheet, verbose);
            }

            // Copy to output location
            Files.copy(jarFile.toPath(), new File(outputFile).toPath(), 
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Successfully compiled to: " + outputFile);
        } catch (Exception e) {
            throw new CompilationException("Failed to compile sheet: " + e.getMessage(), e);
        }
    }
} 