package io.webetl.compiler;

import io.webetl.model.Sheet;
import org.springframework.stereotype.Service;
import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

@Service
public class FlowCompiler {
    private final Path tempDir;
    
    public FlowCompiler() throws IOException {
        this.tempDir = Files.createTempDirectory("flow-compiler");
    }
    
    public File compileToJar(Sheet sheet) throws IOException {
        // Generate Java source code
        String className = "Flow_" + sheet.getId().replace("-", "_");
        String sourceCode = generateSourceCode(className, sheet);
        
        // Generate CompiledFlow class
        String compiledFlowSource = 
            "public abstract class CompiledFlow {\n" +
            "    public abstract void execute(java.util.Map<String, Object> context);\n" +
            "    protected void log(String message) {\n" +
            "        System.out.println(\"[\" + getClass().getSimpleName() + \"] \" + message);\n" +
            "    }\n" +
            "}\n";
        
        // Write both source files
        Path sourcePath = tempDir.resolve(className + ".java");
        Path compiledFlowPath = tempDir.resolve("CompiledFlow.java");
        Files.writeString(compiledFlowPath, compiledFlowSource);
        Files.writeString(sourcePath, sourceCode);
        
        // Compile both sources
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        Iterable<? extends JavaFileObject> compilationUnits = fileManager
            .getJavaFileObjectsFromFiles(Arrays.asList(sourcePath.toFile(), compiledFlowPath.toFile()));
        
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, null, null, compilationUnits);
        
        boolean success = task.call();
        if (!success) {
            throw new RuntimeException("Compilation failed: " + diagnostics.getDiagnostics());
        }
        
        // Create JAR file
        Path classFile = tempDir.resolve(className + ".class");
        Path jarPath = tempDir.resolve("flow.jar");
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add both compiled classes
            jos.putNextEntry(new JarEntry(className + ".class"));
            jos.write(Files.readAllBytes(classFile));
            jos.closeEntry();
            
            jos.putNextEntry(new JarEntry("CompiledFlow.class"));
            jos.write(Files.readAllBytes(tempDir.resolve("CompiledFlow.class")));
            jos.closeEntry();
            
            // Add manifest
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, className);
            
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            manifest.write(jos);
            jos.closeEntry();
        }
        
        return jarPath.toFile();
    }
    
    private String generateSourceCode(String className, Sheet sheet) {
        StringBuilder code = new StringBuilder();
        code.append("import java.util.Map;\n\n");
        
        code.append("public class ").append(className)
            .append(" extends CompiledFlow {\n");
        code.append("    @Override\n");
        code.append("    public void execute(Map<String, Object> context) {\n");
        
        // Generate execution code based on sheet nodes and edges
        generateExecutionCode(code, sheet);
        
        code.append("    }\n");
        code.append("    \n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        new ").append(className).append("().execute(new java.util.HashMap<>());\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private void generateExecutionCode(StringBuilder code, Sheet sheet) {
        // Sort nodes in execution order
        List<Map<String, Object>> sortedNodes = sortNodesInExecutionOrder(sheet);
        
        for (Map<String, Object> node : sortedNodes) {
            String nodeId = (String) node.get("id");
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String type = (String) componentData.get("type");
            
            code.append("        log(\"Executing node: ").append(nodeId).append("\");\n");
            
            switch (type) {
                case "source":
                    generateSourceCode(code, nodeId, componentData);
                    break;
                case "transform":
                    generateTransformCode(code, nodeId, componentData);
                    break;
                case "destination":
                    generateDestinationCode(code, nodeId, componentData);
                    break;
            }
        }
    }
    
    private List<Map<String, Object>> sortNodesInExecutionOrder(Sheet sheet) {
        // Implement topological sort based on edges
        // For now, just return nodes in original order
        // find start node
        String startNodeId = null;
        for (Map<String, Object> node : sheet.getNodes()) {
            if (node.get("type").equals("start")) {
                startNodeId = (String) node.get("id");
            }
        }
        return sheet.getNodes();
    }
    
    private void generateSourceCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        // Source node: ").append(nodeId).append("\n");
        // Add source-specific code generation
    }
    
    private void generateTransformCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        // Transform node: ").append(nodeId).append("\n");
        // Add transform-specific code generation
    }
    
    private void generateDestinationCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        // Destination node: ").append(nodeId).append("\n");
        // Add destination-specific code generation
    }
} 