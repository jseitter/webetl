package io.webetl.compiler;

import io.webetl.model.Sheet;
import org.springframework.stereotype.Service;
import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
/**
 * FlowCompiler is a service that compiles a sheet into a JAR file.
 * It generates a Java source code for the sheet and a CompiledFlow class that extends the generated class.
 * The generated class implements the execute method that takes a context map as parameter.
 */
@Service
public class FlowCompiler {
    private final Path tempDir;
    
    public FlowCompiler() throws IOException {
        this.tempDir = Files.createTempDirectory("flow-compiler");
    }
    
    public File compileToJar(Sheet sheet, boolean verbose) throws IOException {
        // Generate Java source code
        String className = "Flow_" + sheet.getId().replace("-", "_");
        String sourceCode = generateSourceCode(className, sheet, verbose);
        
        // Generate CompiledFlow class
        /**
         * CompiledFlow is an abstract class that defines the execute method.
         * It also defines a log method that prints a message to the console.
         * The log method is protected to be accessible to the generated class.
         */
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
    
    private String generateSourceCode(String className, Sheet sheet, boolean verbose) {
        StringBuilder code = new StringBuilder();
        code.append("import java.util.Map;\n\n");
        code.append("import io.webetl.model.data.*;\n");
        
        code.append("public class ").append(className)
            .append(" extends CompiledFlow {\n");
        code.append("    @Override\n");
        code.append("    public void execute(Map<String, Object> context) {\n");
        
        // Generate execution code based on sheet nodes and edges
        generateExecutionCode(code, sheet, verbose);
        
        code.append("    }\n");
        code.append("    \n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        new ").append(className).append("().execute(new java.util.HashMap<>());\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private void generateExecutionCode(StringBuilder code, Sheet sheet, boolean verbose) {
        // Sort nodes in execution order
        List<Map<String, Object>> sortedNodes = sortNodesInExecutionOrder(sheet, verbose);
        
        // Generate context initialization
        code.append("        java.util.Map<String, java.util.concurrent.BlockingQueue<Row>> queues = new java.util.HashMap<>();\n");
        code.append("        java.util.List<Thread> threads = new java.util.ArrayList<>();\n\n");
        
        for (Map<String, Object> node : sortedNodes) {
            String nodeId = (String) node.get("id");
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String type = (String) componentData.get("type");
            
            code.append("        log(\"Executing node: ").append(nodeId).append("\");\n");
            
            // Skip start and stop nodes for data flow
            if ("start".equals(componentData.get("id")) || "stop".equals(componentData.get("id"))) {
                continue;
            }
            
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
        
        // Start all threads
        code.append("        threads.forEach(Thread::start);\n");
        code.append("        threads.forEach(t -> { try { t.join(); } catch (InterruptedException e) { throw new RuntimeException(e); } });\n");
    }
    
    private List<Map<String, Object>> sortNodesInExecutionOrder(Sheet sheet, boolean verbose) {
        List<Map<String, Object>> sortedNodes = new ArrayList<>();
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        
        // Build node map for quick lookup
        for (Map<String, Object> node : sheet.getNodes()) {
            nodeMap.put((String) node.get("id"), node);
        }
        
        // Find start node
        Map<String, Object> startNode = sheet.getNodes().stream()
            .filter(node -> {
                if(verbose) System.out.println("Node: " + node);
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                return "start".equals(componentData.get("id"));
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No start node found in sheet"));
        
        // Add start node
        sortedNodes.add(startNode);
        
        // Follow control flow edges
        String currentNodeId = (String) startNode.get("id");
        while (currentNodeId != null) {
            String nextNodeId = findNextControlFlowNode(currentNodeId, sheet.getEdges(), nodeMap);
            if (nextNodeId != null) {
                Map<String, Object> nextNode = nodeMap.get(nextNodeId);
                sortedNodes.add(nextNode);
                currentNodeId = nextNodeId;
            } else {
                currentNodeId = null;
            }
        }
       //log output of sorted nodes
       System.out.println("Sorted nodes: " + sortedNodes);
        return sortedNodes;
    }
    
    private String findNextControlFlowNode(String sourceNodeId, List<Map<String, Object>> edges,
                                         Map<String, Map<String, Object>> nodeMap) {
        return edges.stream()
            .filter(edge -> {
                String source = (String) edge.get("source");
                String sourceHandle = (String) edge.get("sourceHandle");
                return source.equals(sourceNodeId) && "control-flow-out".equals(sourceHandle);
            })
            .map(edge -> (String) edge.get("target"))
            .findFirst()
            .orElse(null);
    }
    
    private void generateSourceCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        // Create output queue for source node\n");
        code.append("        queues.put(\"").append(nodeId).append("_out\", new java.util.concurrent.LinkedBlockingQueue<>());\n");
        code.append("        threads.add(new Thread(() -> {\n");
        code.append("            try {\n");
        
        // Generate source-specific code based on component type
        String sourceType = (String) componentData.get("id");
        switch (sourceType) {
            case "db-source":
                generateDatabaseSourceCode(code, nodeId, componentData);
                break;
            case "file-source":
                generateFileSourceCode(code, nodeId, componentData);
                break;
        }
        
        code.append("            } catch (Exception e) {\n");
        code.append("                throw new RuntimeException(e);\n");
        code.append("            }\n");
        code.append("        }));\n\n");
    }
    
    private void generateTransformCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        // Create output queue for transform node\n");
        code.append("        queues.put(\"").append(nodeId).append("_out\", new java.util.concurrent.LinkedBlockingQueue<>());\n");
        code.append("        threads.add(new Thread(() -> {\n");
        code.append("            try {\n");
        code.append("                var inputQueue = queues.get(\"" + getInputQueueId(nodeId) + "\");\n");
        
        // Generate transform-specific code based on component type
        String transformType = (String) componentData.get("id");
        switch (transformType) {
            case "filter":
                generateFilterCode(code, nodeId, componentData);
                break;
            case "map":
                generateMapCode(code, nodeId, componentData);
                break;
        }
        
        code.append("            } catch (Exception e) {\n");
        code.append("                throw new RuntimeException(e);\n");
        code.append("            }\n");
        code.append("        }));\n\n");
    }
    
    private void generateDestinationCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("        threads.add(new Thread(() -> {\n");
        code.append("            try {\n");
        code.append("                var inputQueue = queues.get(\"" + getInputQueueId(nodeId) + "\");\n");
        
        // Generate destination-specific code based on component type
        String destType = (String) componentData.get("id");
        switch (destType) {
            case "db-dest":
                generateDatabaseDestinationCode(code, nodeId, componentData);
                break;
            case "file-dest":
                generateFileDestinationCode(code, nodeId, componentData);
                break;
        }
        
        code.append("            } catch (Exception e) {\n");
        code.append("                throw new RuntimeException(e);\n");
        code.append("            }\n");
        code.append("        }));\n\n");
    }
    
    private String getInputQueueId(String nodeId) {
        // TODO: Implement by looking up the source node from edges
        System.out.println("Getting input queue id for node: " + nodeId);
        return nodeId + "_in";
    }
    
    private void generateDatabaseSourceCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement database source\n");
        code.append("                var outputQueue = queues.get(\"" + nodeId + "_out\");\n");
    }
    
    private void generateFileSourceCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement file source\n");
        code.append("                var outputQueue = queues.get(\"" + nodeId + "_out\");\n");
    }
    
    private void generateFilterCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement filter\n");
        code.append("                var outputQueue = queues.get(\"" + nodeId + "_out\");\n");
    }
    
    private void generateMapCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement map\n");
        code.append("                var outputQueue = queues.get(\"" + nodeId + "_out\");\n");
    }
    
    private void generateDatabaseDestinationCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement database destination\n");
    }
    
    private void generateFileDestinationCode(StringBuilder code, String nodeId, Map<String, Object> componentData) {
        code.append("                // TODO: Implement file destination\n");
    }
} 