package io.webetl.compiler;

import io.webetl.model.Sheet;
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
                
                // Add runtime classes
                Path runtimeJar = findRuntimeJar();
                try (JarInputStream jis = new JarInputStream(new FileInputStream(runtimeJar.toFile()))) {
                    JarEntry entry;
                    while ((entry = jis.getNextJarEntry()) != null) {
                        if (!entry.isDirectory()) {
                            jos.putNextEntry(new JarEntry(entry.getName()));
                            jos.write(jis.readAllBytes());
                            jos.closeEntry();
                        }
                    }
                }
                
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
        
        // Add common imports at class level
        code.append("import java.util.*;\n");
        code.append("import java.util.concurrent.BlockingQueue;\n");
        code.append("import java.util.concurrent.LinkedBlockingQueue;\n");
        code.append("import io.webetl.model.data.Row;\n");
        code.append("import io.webetl.model.component.*;\n");
        code.append("import io.webetl.runtime.ExecutionContext;\n");
        
        // Add component-specific imports
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
        
        // Generate class
        code.append("public class ").append(className).append(" extends CompiledFlow {\n");
        
        // Generate execute method
        code.append("    @Override\n");
        code.append("    public void execute(ExecutionContext context) throws Exception {\n");
        generateExecutionCode(code, sheet, verbose);
        code.append("    }\n");
        
        // Generate main method
        code.append("\n    public static void main(String[] args) {\n");
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
            code.append("        log(\"Starting flow execution\");\n\n");
            
            // Sort nodes in control flow order
            List<Map<String, Object>> controlFlowNodes = sortNodesInExecutionOrder(sheet, verbose);
            
            // Initialize components
            code.append("        log(\"Initializing components\");\n");
            code.append("        Map<String, ExecutableComponent> components = new HashMap<>();\n");
            code.append("        List<Thread> threads = new ArrayList<>();\n\n");
            
            // Initialize all components first
            for (Map<String, Object> node : sheet.getNodes()) {
                initializeComponent(code, node);
            }
            
            // Add debug thread generation
            code.append("\n        // Create debug monitoring thread\n");
            code.append("        Thread debugThread = new Thread(() -> {\n");
            code.append("            while (!Thread.currentThread().isInterrupted()) {\n");
            code.append("                try {\n");
            code.append("                    log(\"\\n=== Thread Status Report ===\");\n");
            code.append("                    for (Thread t : threads) {\n");
            code.append("                        log(String.format(\"Thread %s: %s\", t.getName(), t.getState()));\n");
            code.append("                    }\n");
            code.append("                    log(\"===========================\\n\");\n");
            code.append("                    Thread.sleep(5000); // Report every 5 seconds\n");
            code.append("                } catch (InterruptedException e) {\n");
            code.append("                    Thread.currentThread().interrupt();\n");
            code.append("                    break;\n");
            code.append("                }\n");
            code.append("            }\n");
            code.append("        }, \"debug-monitor\");\n");
            code.append("        debugThread.setDaemon(true);\n");
            code.append("        debugThread.start();\n\n");
            
            // For each control flow node, process its data flow chain
            for (Map<String, Object> controlNode : controlFlowNodes) {
                // Create threads for current data flow chain
                code.append("\n        // Process data flow chain starting from control node: " + controlNode.get("id") + "\n");
                List<Map<String, Object>> dataFlowChain = getDataFlowChain(controlNode, sheet.getEdges(), sheet.getNodes());
                
                // Connect components in this chain
                connectDataFlowComponents(code, dataFlowChain, sheet.getEdges());
                
                // Create threads for all components in this chain
                for (Map<String, Object> node : dataFlowChain) {
                    generateThreadForComponent(code, node);
                }
                
                // Start all threads for this chain
                code.append("\n        log(\"Starting threads for data flow chain\");\n");
                code.append("        for (Thread thread : threads) {\n");
                code.append("            thread.start();\n");
                code.append("        }\n\n");
                
                // Wait for all threads in this chain
                code.append("        log(\"Waiting for data flow chain to complete\");\n");
                code.append("        for (Thread thread : threads) {\n");
                code.append("            thread.join();\n");
                code.append("        }\n");
                code.append("        threads.clear();\n");  // Clear for next chain
            }
            
            code.append("        log(\"Flow execution completed\");\n");
            
        } catch (Throwable e) {
            throw new CompilationException("Failed to generate execution code: " + e.getMessage(), e);
        }
    }
    
    private void generateThreadForComponent(StringBuilder code, Map<String, Object> node) {
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
        String nodeId = (String) node.get("id");
        
        if (!"start".equals(componentData.get("id")) && !"stop".equals(componentData.get("id"))) {
            // Create multiple threads for components that support it
            int threadCount = getThreadCount(componentData);
            code.append("        log(\"Creating ").append(threadCount).append(" thread(s) for component: ")
                .append(nodeId).append("\");\n");
                
            for (int i = 0; i < threadCount; i++) {
                code.append("        threads.add(new Thread(() -> {\n");
                code.append("            try {\n");
                code.append("                if (components.get(\"").append(nodeId)
                    .append("\") instanceof io.webetl.model.component.InputQueueProvider) {\n");
                // Input queue consumer logic
                code.append("                    while (true) {\n");
                code.append("                        Row row = ((io.webetl.model.component.InputQueueProvider)components.get(\"")
                    .append(nodeId).append("\")).getInputQueue().peek();\n");  // BlockingQueue.take()
                code.append("                        if (row.isTerminator()) {\n");
                code.append("                            log(\"" + nodeId + " received terminator\");\n");
                code.append("                            if (components.get(\"").append(nodeId)
                    .append("\") instanceof io.webetl.model.component.OutputQueueProvider) {\n");
                code.append("                                ((io.webetl.model.component.OutputQueueProvider)components.get(\"")
                    .append(nodeId).append("\")).sendRow(row);\n");
                code.append("                            }\n");
                code.append("                            break;\n");
                code.append("                        }\n");
                code.append("                        components.get(\"").append(nodeId).append("\").execute(context);\n");
                code.append("                        if (components.get(\"").append(nodeId)
                    .append("\") instanceof io.webetl.model.component.OutputQueueProvider) {\n");
                code.append("                            ((io.webetl.model.component.OutputQueueProvider)components.get(\"")
                    .append(nodeId).append("\")).sendRow(row);\n");
                code.append("                        }\n");
                code.append("                    }\n");
                // Source component logic
                code.append("                } else {\n");
                code.append("                    components.get(\"").append(nodeId).append("\").execute(context);\n");
                code.append("                    if (components.get(\"").append(nodeId)
                    .append("\") instanceof io.webetl.model.component.OutputQueueProvider) {\n");
                code.append("                        ((io.webetl.model.component.OutputQueueProvider)components.get(\"")
                    .append(nodeId).append("\")).sendRow(Row.createTerminator());\n");
                code.append("                    }\n");
                code.append("                }\n");
                code.append("            } catch (Exception e) {\n");
                code.append("                e.printStackTrace();\n");
                code.append("            }\n");
                code.append("        }, \"").append(nodeId).append("-thread-").append(i + 1).append("\"));\n");
            }
        }
    }
   
    /**
     * Get the number of threads for a component.
     * @param componentData the component data
     * @return the amount of threads to create
     */
    private int getThreadCount(Map<String, Object> componentData) {
        // Default to 1 thread
        Integer threads = (Integer) componentData.get("threads");
        if (threads == null || threads < 1) {
            return 1;
        }
        return threads;
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
    
    private void initializeComponent(StringBuilder code, Map<String, Object> node) {
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
        String nodeId = (String) node.get("id");
        String implementationClass = (String) componentData.get("implementationClass");
        
        if (!"start".equals(componentData.get("id")) && !"stop".equals(componentData.get("id"))) {
            code.append("        log(\"Initializing component: ").append(nodeId).append("\");\n");
            code.append("        components.put(\"").append(nodeId)
                .append("\", new ").append(implementationClass).append("());\n");
            
            // Set parameters if any
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) componentData.get("parameters");
            if (parameters != null) {
                for (Map<String, Object> param : parameters) {
                    code.append("        ((").append(implementationClass).append(")components.get(\"")
                        .append(nodeId).append("\")).setParameter(\"")
                        .append(param.get("name")).append("\", ")
                        .append(formatParameterValue(param)).append(");\n");
                }
            }
        }
    }
    
    private String getInputQueueId(String nodeId) {
        // TODO: Implement by looking up the source node from edges
        System.out.println("Getting input queue id for node: " + nodeId);
        return nodeId + "_in";
    }

    private Path findRuntimeJar() throws IOException {
        // First try release structure (runtime.jar next to application jar)
        Path applicationPath = new File(FlowCompiler.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath()).toPath();
        Path releasePath = applicationPath.getParent().resolve("runtime.jar");
        
        System.out.println("Searching for runtime.jar in release path: " + releasePath);
        if (Files.exists(releasePath)) {
            System.out.println("Found runtime.jar in release path");
            return releasePath;
        }

        // Try development structure (build/libs/runtime.jar)
        Path projectRoot = Paths.get("").toAbsolutePath();
        System.out.println("Starting search from: " + projectRoot);
        
        while (projectRoot != null && !Files.exists(projectRoot.resolve("gradlew"))) {
            projectRoot = projectRoot.getParent();
            System.out.println("Looking for gradlew in: " + projectRoot);
        }
        
        if (projectRoot != null) {
            Path devPath = projectRoot.resolve("backend/build/libs/webetl-runtime.jar");
            System.out.println("Searching for runtime.jar in dev path: " + devPath);
            if (Files.exists(devPath)) {
                System.out.println("Found runtime.jar in dev path");
                return devPath;
            }
        }

        throw new IOException("Could not find runtime.jar in either:\n" +
            "Release path: " + releasePath + "\n" +
            "Dev path: " + (projectRoot != null ? projectRoot.resolve("backend/build/libs/webetl-runtime.jar") : "project root not found"));
    }

    private List<Map<String, Object>> getDataFlowChain(Map<String, Object> startNode, 
            List<Map<String, Object>> edges, List<Map<String, Object>> allNodes) {
        List<Map<String, Object>> chain = new ArrayList<>();
        chain.add(startNode);
        
        String currentId = (String)startNode.get("id");
        while (currentId != null) {
            String nextId = findNextDataFlowNode(currentId, edges);
            if (nextId != null) {
                Map<String, Object> nextNode = allNodes.stream()
                    .filter(n -> nextId.equals(n.get("id")))
                    .findFirst()
                    .orElse(null);
                if (nextNode != null) {
                    chain.add(nextNode);
                    currentId = nextId;
                } else {
                    currentId = null;
                }
            } else {
                currentId = null;
            }
        }
        return chain;
    }

    private String findNextDataFlowNode(String sourceId, List<Map<String, Object>> edges) {
        return edges.stream()
            .filter(edge -> sourceId.equals(edge.get("source")) && 
                   !((String)edge.get("sourceHandle")).startsWith("control-flow"))
            .map(edge -> (String)edge.get("target"))
            .findFirst()
            .orElse(null);
    }

    private List<Map<String, Object>> getDataFlowEdges(List<Map<String, Object>> nodes, 
            List<Map<String, Object>> allEdges) {
        Set<String> nodeIds = nodes.stream()
            .map(n -> (String)n.get("id"))
            .collect(Collectors.toSet());
            
        return allEdges.stream()
            .filter(edge -> nodeIds.contains(edge.get("source")) && 
                   nodeIds.contains(edge.get("target")) &&
                   !((String)edge.get("sourceHandle")).startsWith("control-flow"))
            .collect(Collectors.toList());
    }

    private void connectDataFlowComponents(StringBuilder code, List<Map<String, Object>> dataFlowChain, List<Map<String, Object>> edges) {
        for (int i = 0; i < dataFlowChain.size() - 1; i++) {
            Map<String, Object> sourceNode = dataFlowChain.get(i);
            Map<String, Object> targetNode = dataFlowChain.get(i + 1);
            String sourceId = (String) sourceNode.get("id");
            String targetId = (String) targetNode.get("id");
            
            code.append("        // Connect ").append(sourceId).append(" to ").append(targetId).append("\n");
            code.append("        if (components.get(\"").append(sourceId)
                .append("\") instanceof io.webetl.model.component.OutputQueueProvider && ")
                .append("components.get(\"").append(targetId)
                .append("\") instanceof io.webetl.model.component.InputQueueProvider) {\n")
                .append("            ((io.webetl.model.component.OutputQueueProvider)components.get(\"")
                .append(sourceId).append("\")).registerInputQueue(")
                .append("((io.webetl.model.component.InputQueueProvider)components.get(\"")
                .append(targetId).append("\"))")
                .append(");\n        }\n");
        }
    }
} 