package io.webetl.compiler;

import io.webetl.model.Sheet;
import io.webetl.model.Node;
import io.webetl.model.Edge;
import org.springframework.stereotype.Service;
import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.jar.*;
import io.webetl.runtime.ExecutionContext;
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
    
    private void validateFlow(Sheet sheet) throws CompilationException {
        // output sheet metadata
        System.out.println("validating Sheet: " + sheet.getName());
        // Check for start node
        Map<String, Object> startNode = sheet.getNodes().stream()
            .filter(node -> {
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                return "start".equals(componentData.get("id"));
            })
            .findFirst()
            .orElseThrow(() -> new CompilationException("No start node found in flow"));

        // Check for stop node
        Map<String, Object> stopNode = sheet.getNodes().stream()
            .filter(node -> {
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                return "stop".equals(componentData.get("id"));
            })
            .findFirst()
            .orElseThrow(() -> new CompilationException("No stop node found in flow"));

        // Check path between start and stop
        if (!hasPathBetween(sheet, startNode, stopNode)) {
            throw new CompilationException("No valid path found between start and stop nodes");
        }
    }

    private boolean hasPathBetween(Sheet sheet, Map<String, Object> start, Map<String, Object> target) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add((String) start.get("id"));

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (currentId.equals(target.get("id"))) {
                return true;
            }

            if (visited.add(currentId)) {
                // Get all edges from current node
                sheet.getEdges().stream()
                    .filter(edge -> currentId.equals(edge.get("source")))
                    .map(edge -> edge.get("target"))
                    .forEach(edge -> queue.add((String) edge));
            }
        }

        return false;
    }
    
    public File compileToJar(Sheet sheet, boolean verbose) throws IOException {
        try {
            System.out.println("Validating flow structure...");
            validateFlow(sheet);
            System.out.println("Flow structure validation successful");

            System.out.println("Generating compilation units...");
            // Generate Java source code
            String className = "Flow_" + sheet.getId().replace("-", "_");
            String sourceCode = generateSourceCode(className, sheet, verbose);
           if(verbose) System.out.println("Source code: " + sourceCode);
            // Generate CompiledFlow class
            /**
             * CompiledFlow is an abstract class that defines the execute method.
             * It also defines a log method that prints a message to the console.
             * The log method is protected to be accessible to the generated class.
             */
            String compiledFlowSource = 
                "public abstract class CompiledFlow {\n" +
                "    public abstract void execute(io.webetl.runtime.ExecutionContext context) throws Exception;\n" +
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
            
            // Add classpath
            List<String> options = new ArrayList<>();
            String classpath = System.getProperty("java.class.path");
            options.add("-classpath");
            options.add(classpath);
            
            Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(Arrays.asList(sourcePath.toFile(), compiledFlowPath.toFile()));
            
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits);
            
            boolean success = task.call();
            if (!success) {
                StringBuilder error = new StringBuilder();
                error.append("Compilation failed!\n\n");
                error.append("Compiler Errors:\n");
                error.append("---------------\n");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    error.append(String.format("Line %d: %s\n", 
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(null)));
                }
                if (verbose) {
                    error.append("\nGenerated Source Code:\n");
                    error.append("--------------------\n");
                    String[] lines = sourceCode.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        error.append(String.format("%4d | %s\n", i + 1, lines[i]));
                    }
                }
                throw new CompilationException(
                    error.toString());
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
        } catch (Throwable e) {
            if (e instanceof CompilationException) {
                throw (CompilationException) e;
            }
            throw new CompilationException("Compilation failed: " + e.getMessage(), e);
        }
    }
    
    private String generateSourceCode(String className, Sheet sheet, boolean verbose) {
        StringBuilder code = new StringBuilder();
        // Add required imports at class level
        code.append("import java.util.Map;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.ArrayList;\n");
        code.append("import java.util.concurrent.BlockingQueue;\n");
        code.append("import java.util.concurrent.LinkedBlockingQueue;\n");
        code.append("import io.webetl.model.data.*;\n");
        code.append("import io.webetl.model.component.ExecutableComponent;\n");
        code.append("import io.webetl.runtime.ExecutionContext;\n");

        // Import specific component classes
        Set<String> componentClasses = new HashSet<>();
        for (Map<String, Object> node : sheet.getNodes()) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String implementationClass = (String) componentData.get("implementationClass");
            if (implementationClass != null) {
                componentClasses.add(implementationClass);
            }
        }
        for (String componentClass : componentClasses) {
            code.append("import ").append(componentClass).append(";\n");
        }
        code.append("\n");
        
        code.append("public class ").append(className)
            .append(" extends CompiledFlow {\n");
        code.append("    @Override\n");
        code.append("    public void execute(ExecutionContext context) throws Exception {\n");
        
        // Generate execution code based on sheet nodes and edges
        generateExecutionCode(code, sheet, verbose);
        
        code.append("    }\n");
        code.append("    \n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        try {\n");
        code.append("            new ").append(className).append("().execute(new ExecutionContext());\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            e.printStackTrace();\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private void generateExecutionCode(StringBuilder code, Sheet sheet, boolean verbose) {
        try {
            // Sort nodes in execution order
            List<Map<String, Object>> sortedNodes = sortNodesInExecutionOrder(sheet, verbose);
            
            // Validate component implementation classes
            for (Map<String, Object> node : sortedNodes) {
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                String nodeId = (String) node.get("id");
                String implementationClass = (String) componentData.get("implementationClass");
                
                if (implementationClass == null && !"start".equals(componentData.get("id")) && !"stop".equals(componentData.get("id"))) {
                    throw new CompilationException("No implementation class found for component: " + nodeId + 
                        " (type: " + componentData.get("id") + ")");
                }
            }
            
            // Generate component and queue initialization
            code.append("        java.util.Map<String, java.util.concurrent.BlockingQueue<Row>> queues = new java.util.HashMap<>();\n");
            code.append("        java.util.Map<String, ExecutableComponent> components = new java.util.HashMap<>();\n");
            code.append("        java.util.List<Thread> threads = new java.util.ArrayList<>();\n\n");
            
            for (Map<String, Object> node : sortedNodes) {
                String nodeId = (String) node.get("id");
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                String implementationClass = (String) componentData.get("implementationClass");
                String type = (String) componentData.get("type");
                
                code.append("        log(\"Executing node: ").append(nodeId).append("\");\n");
                
                // Skip start and stop nodes for data flow
                if ("start".equals(componentData.get("id")) || "stop".equals(componentData.get("id"))) {
                    continue;
                }
                
                // Instantiate component
                code.append("        components.put(\"").append(nodeId).append("\", new ")
                    .append(implementationClass).append("());\n");
                
                // Configure component parameters
                if (componentData.containsKey("parameters")) {
                    List<Map<String, Object>> parameters = (List<Map<String, Object>>) componentData.get("parameters");
                    for (Map<String, Object> param : parameters) {
                        code.append("        ((").append(implementationClass).append(")components.get(\"")
                            .append(nodeId).append("\")).setParameter(\"")
                            .append(param.get("name")).append("\", ")
                            .append(formatParameterValue(param)).append(");\n");
                    }
                }
            }
            
            // Create execution threads
            code.append("\n        // Create execution threads\n");
            for (Map<String, Object> node : sortedNodes) {
                String nodeId = (String) node.get("id");
                code.append("        threads.add(new Thread(() -> {\n");
                code.append("            try {\n");
                code.append("                components.get(\"")
                    .append(nodeId).append("\").execute(context);\n");
                code.append("            } catch (Exception e) {\n");
                code.append("                throw new RuntimeException(e);\n");
                code.append("            }\n");
                code.append("        }));\n");
            }
            
            // Start all threads
            code.append("        threads.forEach(Thread::start);\n");
            code.append("        threads.forEach(t -> { try { t.join(); } catch (InterruptedException e) { throw new RuntimeException(e); } });\n");
        } catch (Throwable e) {
            throw new CompilationException("Failed to generate execution code: " + e.getMessage(), e);
        }
    }
    
    private String formatParameterValue(Map<String, Object> param) {
        Object value = param.get("value");
        String type = (String) param.get("type");
        
        if (value == null) return "null";
        
        switch (type) {
            case "string":
                return "\"" + value.toString() + "\"";
            case "number":
                return value.toString();
            case "boolean":
                return value.toString();
            default:
                return "null";
        }
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
       System.out.println("Sorted nodes: ");
       // log only the node id
        sortedNodes.stream().map(node -> node.get("id")).forEach(System.out::println);
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