package io.webetl.compiler;

import com.palantir.javapoet.*;
import io.webetl.model.Sheet;
import io.webetl.model.component.ETLComponent;
import io.webetl.model.component.ExecutableComponent;
import io.webetl.runtime.ExecutionContext;
import io.webetl.runtime.FlowRunner;
import io.webetl.runtime.JarClassLoader;
import io.webetl.runtime.JarLauncher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipException;

@Service
@Slf4j
public class FlowCompilerNG {
    private final Path tempDir;
    private List<Map<String, Object>> controlFlowNodes;
    private List<List<Map<String, Object>>> dataFlowPaths;
    private AtomicInteger messageSequence = new AtomicInteger(0);
    
    public FlowCompilerNG() throws IOException {
        this.tempDir = Files.createTempDirectory("flow-compiler");
    }

    /**
     * First pass: Build control flow execution order
     */
    private void buildControlFlow(Sheet sheet, boolean verbose) {
        controlFlowNodes = new ArrayList<>();
        // Find start nodes and traverse control flow
        for (Map<String, Object> node : sheet.getNodes()) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String componentId = (String) componentData.get("id");
            String nodeType = (String) node.get("type");
            
            // Identify start nodes by both type and component ID
            if ("start".equals(componentId) || "start".equals(nodeType)) {
                traverseControlFlow(node, sheet.getNodes(), sheet.getEdges(), new HashSet<>());
            }
        }
        
        if (verbose) {
            System.out.println("\nPass 1 - Control Flow Order:");
            controlFlowNodes.forEach(node -> 
                System.out.println("  " + node.get("id")));
        }
    }

    /**
     * Second pass: Build data flow paths
     */
    private void buildDataFlowPaths(Sheet sheet, boolean verbose) {
        dataFlowPaths = new ArrayList<>();
        // For each source node, build data flow path
        for (Map<String, Object> node : sheet.getNodes()) {
            // Only source nodes that are not control flow nodes should start data flow paths
            if (isSourceNode(node) && !isControlFlowNode(node)) {
                List<Map<String, Object>> path = new ArrayList<>();
                traverseDataFlow(node, sheet.getNodes(), sheet.getEdges(), path, new HashSet<>());
                // Only add non-empty paths with at least one source and one other node
                if (path.size() > 1) {
                    dataFlowPaths.add(path);
                }
            }
        }
        
        if (verbose) {
            System.out.println("\nPass 2 - Data Flow Paths:");
            dataFlowPaths.forEach(path -> {
                System.out.println("  Path:");
                path.forEach(node -> 
                    System.out.println("    " + node.get("id")));
            });
        }
    }

    /**
     * Third pass: Generate code
     */
    private JavaFile generateCode(Sheet sheet, String className, boolean verbose) {
        ClassName etlComponent = ClassName.get("io.webetl.model.component", "ETLComponent");
        ClassName compiledFlow = ClassName.get("io.webetl.compiler", "CompiledFlow");
        ClassName executionContext = ClassName.get("io.webetl.runtime", "ExecutionContext");
        
        // Build the class using control flow and data flow information
        // inherits compiled flow to get the execute method and the logging system
        TypeSpec.Builder flowClass = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .superclass(compiledFlow);
        
        // Add components map field
        flowClass.addField(FieldSpec.builder(
                ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(Object.class)
                ),
                "components",
                Modifier.PRIVATE)
                .build());
        
        // Add constructor with component initialization
        MethodSpec.Builder constructor = buildConstructor(sheet);
        
        // Add execute method using control and data flow
        MethodSpec.Builder executeMethod = buildExecuteMethod(sheet);
        
        // Build the complete flow class
        TypeSpec flowTypeSpec = flowClass
            .addMethod(constructor.build())
            .addMethod(executeMethod.build())
            .build();
        
        JavaFile javaFile = JavaFile.builder("io.webetl.generated", flowTypeSpec)
            .build();
            
        if (verbose) {
            System.out.println("\nPass 3 - Generated Code:");
            try {
                javaFile.writeTo(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return javaFile;
    }

    public File compileToJar(Sheet sheet, boolean verbose) throws CompilationException {
        try {
            log.info("Compiling flow: {}", sheet.getId());
            validateFlow(sheet);
            
            // Extract component classes early
            Set<Class<?>> componentClasses = extractComponentClasses(sheet, verbose);
            
            // Generate code for the flow
            String className = "GeneratedFlow_" + sheet.getId().replaceAll("-", "_");
            log.info("Generated class name: {}", className);
            
            // Build control flow and data flow paths
            buildControlFlow(sheet, verbose);
            buildDataFlowPaths(sheet, verbose);
            
            JavaFile javaFile = generateCode(sheet, className, verbose);

            // Create JAR with dependencies
            return compileAndCreateJar(javaFile, className, componentClasses, verbose);
        } catch (IOException e) {
            throw new CompilationException("Failed to compile flow: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CompilationException("Unexpected error while compiling flow: " + e.getMessage(), e);
        }
    }

    private void validateFlow(Sheet sheet) throws CompilationException {
        if (sheet.getNodes() == null || sheet.getNodes().isEmpty()) {
            throw new CompilationException("Flow must contain at least one node");
        }
        if (sheet.getEdges() == null) {
            throw new CompilationException("Flow edges cannot be null");
        }
        // check if all nodes have a valid implementation class but not start and stop nodes
        for (Map<String, Object> node : sheet.getNodes()) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String implementationClass = (String) componentData.get("implementationClass");
            String componentId = (String) componentData.get("id");
            
            // implementation class can be null if it is a start or stop node but must be set for
            // all other nodes  
            if (implementationClass == null) {
                // Check both the node type and component ID to identify start/stop nodes
                if (node.get("type").equals("start") || node.get("type").equals("stop") ||
                    "start".equals(componentId) || "stop".equals(componentId)) {
                    continue;
                }
                throw new CompilationException("Node " + node.get("id") + " has no implementation class");
            }
            
            // check if the implementation class exists
            try {
                Class<?> implClass = Class.forName(implementationClass);
                if (!ETLComponent.class.isAssignableFrom(implClass)) {
                    throw new CompilationException("Implementation class " + implementationClass + " does not implement ETLComponent");
                }
                if (!ExecutableComponent.class.isAssignableFrom(implClass)) {
                    throw new CompilationException("Implementation class " + implementationClass + " does not implement ExecutableComponent");
                }
            } catch (ClassNotFoundException e) {
                throw new CompilationException("Implementation class " + implementationClass + " not found", e);
            }
        }
    }

    private void traverseControlFlow(Map<String, Object> node, List<Map<String, Object>> nodes, 
            List<Map<String, Object>> edges, Set<String> visited) {
        String nodeId = (String) node.get("id");
        if (!visited.add(nodeId)) return;
        
        // Only include control flow nodes (start/stop) and source nodes in control flow
        // Transform and destination components should be part of data flow, not control flow
        if (isControlFlowNode(node) || isSourceNode(node)) {
            controlFlowNodes.add(node);
        }
        
        // Find outgoing control flow edges
        edges.stream()
            .filter(edge -> edge.get("source").equals(nodeId))
            // Filter edges to only include control flow connections
            .filter(edge -> {
                Object sourceHandle = edge.get("sourceHandle");
                return sourceHandle == null || 
                       !(sourceHandle instanceof String) || 
                       ((String)sourceHandle).contains("control") ||
                       !((String)sourceHandle).contains("data");
            })
            .forEach(edge -> {
                String targetId = (String) edge.get("target");
                nodes.stream()
                    .filter(n -> n.get("id").equals(targetId))
                    .findFirst()
                    .ifPresent(targetNode -> 
                        traverseControlFlow(targetNode, nodes, edges, visited));
            });
    }

    private void traverseDataFlow(Map<String, Object> node, List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges, List<Map<String, Object>> path, Set<String> visited) {
        String nodeId = (String) node.get("id");
        if (!visited.add(nodeId)) return;
        
        // Only add data processing nodes to data flow paths (exclude control flow nodes like start/stop)
        if (!isControlFlowNode(node)) {
            path.add(node);
        }
        
        // Find outgoing data flow edges
        edges.stream()
            .filter(edge -> edge.get("source").equals(nodeId))
            // Filter edges to only include data flow connections
            .filter(edge -> {
                Object sourceHandle = edge.get("sourceHandle");
                return sourceHandle == null || 
                       !(sourceHandle instanceof String) || 
                       ((String)sourceHandle).contains("data") ||
                       !((String)sourceHandle).contains("control");
            })
            .forEach(edge -> {
                String targetId = (String) edge.get("target");
                nodes.stream()
                    .filter(n -> n.get("id").equals(targetId))
                    // Skip control flow nodes for data paths
                    .filter(n -> !isControlFlowNode(n))
                    .findFirst()
                    .ifPresent(targetNode -> 
                        traverseDataFlow(targetNode, nodes, edges, path, visited));
            });
    }

    private MethodSpec.Builder buildConstructor(Sheet sheet) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addException(Exception.class)
            .addStatement("this.components = new $T<>()", HashMap.class);
            
        // Create all component instances based on implementation class
        for (Map<String, Object> node : sheet.getNodes()) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            
            // Skip invalid nodes
            if (data == null || data.get("componentData") == null) {
                continue;
            }
            
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String implementationClass = (String) componentData.get("implementationClass");
            String nodeId = (String) node.get("id");
            String nodeType = (String) node.get("type");
            
            // Skip start and stop nodes, they are just control flow markers
            if ("start".equals(node.get("type")) || "stop".equals(node.get("type")) ||
                "start".equals(componentData.get("id")) || "stop".equals(componentData.get("id"))) {
                continue;
            }
            
            // Create a valid Java variable name by replacing hyphens with underscores
            String safeNodeId = nodeId.replaceAll("-", "_");
            
            // Create component instance using the implementation class
            constructor.addComment("Create component instance for node: $L", nodeId);
            constructor.addStatement("Object $L = Class.forName($S).newInstance()", 
                safeNodeId, implementationClass);
            
            // Add component instance to map
            constructor.addStatement("components.put($S, $L)", nodeId, safeNodeId);
            
            // Set component parameters if available
            List<?> parameters = (List<?>) componentData.get("parameters");
            if (parameters != null && !parameters.isEmpty()) {
                constructor.addComment("Set parameters for component: $L", nodeId);
                for (Object paramObj : parameters) {
                    String paramName;
                    Object paramValue;
                    String formattedValue;
                    
                    if (paramObj instanceof Map) {
                        // Handle parameters as Map (from JSON deserialization)
                        Map<String, Object> param = (Map<String, Object>) paramObj;
                        paramName = (String) param.get("name");
                        paramValue = param.get("value");
                        
                        if (paramValue != null) {
                            formattedValue = formatParameterValue(param);
                        } else {
                            continue; // Skip parameters with null values
                        }
                    } else {
                        // Handle parameters as Parameter objects (from Java code)
                        try {
                            // Use reflection to get parameter name and value
                            Class<?> paramClass = paramObj.getClass();
                            java.lang.reflect.Method getNameMethod = paramClass.getMethod("getName");
                            java.lang.reflect.Method getValueMethod = paramClass.getMethod("getValue");
                            
                            paramName = (String) getNameMethod.invoke(paramObj);
                            paramValue = getValueMethod.invoke(paramObj);
                            
                            if (paramValue == null) {
                                continue; // Skip parameters with null values
                            }
                            
                            // Format the value as a string literal for Java code
                            if (paramValue instanceof String) {
                                formattedValue = "\"" + escapeJavaString((String) paramValue) + "\"";
                            } else if (paramValue instanceof Number || paramValue instanceof Boolean) {
                                formattedValue = paramValue.toString();
                            } else {
                                formattedValue = "\"" + escapeJavaString(paramValue.toString()) + "\"";
                            }
                        } catch (Exception e) {
                            // Log warning and skip this parameter if reflection fails
                            log.warn("Failed to process parameter object: " + paramObj, e);
                            continue;
                        }
                    }
                    
                    constructor.addStatement("(($T) $L).setParameter($S, $L)", 
                        ClassName.get("io.webetl.model.component", "ETLComponent"),
                        safeNodeId, 
                        paramName, 
                        formattedValue);
                }
            }
        }
        
        return constructor;
    }
    
    /**
     * Format a parameter value for Java code generation
     */
    private String formatParameterValue(Map<String, Object> param) {
        Object value = param.get("value");
        String parameterType = (String) param.get("parameterType");
        
        if (value == null) return "null";
        
        // Handle different parameter types
        if ("string".equals(parameterType) || "secret".equals(parameterType) || "sql".equals(parameterType) || "select".equals(parameterType)) {
            return "\"" + escapeJavaString(value.toString()) + "\"";
        } else if ("number".equals(parameterType)) {
            return value.toString();
        } else if ("boolean".equals(parameterType)) {
            return value.toString();
        }
        
        // Default to string representation in quotes
        return "\"" + escapeJavaString(value.toString()) + "\"";
    }
    
    /**
     * Escape special characters in Java strings
     */
    private String escapeJavaString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private MethodSpec.Builder buildExecuteMethod(Sheet sheet) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("execute")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.VOID)  // Explicitly specify return type as void
            .addParameter(ClassName.get("io.webetl.runtime", "ExecutionContext"), "context")
            .addException(Exception.class)
            .addStatement("context.log(\"Starting flow execution\")");

        // Add diagnostic logging for classloader issues
        method.addStatement("context.log(\"Current classloader: \" + getClass().getClassLoader().getClass().getName())")
              .addStatement("context.log(\"Parent classloader: \" + getClass().getClassLoader().getParent().getClass().getName())")
              .addStatement("context.log(\"Thread context classloader: \" + Thread.currentThread().getContextClassLoader().getClass().getName())");

        // Connect component queues
        method.addComment("Connect component queues");
        
        // Generate unique path identifiers
        for (int pathIndex = 0; pathIndex < dataFlowPaths.size(); pathIndex++) {
            List<Map<String, Object>> path = dataFlowPaths.get(pathIndex);
            String pathId = "path" + pathIndex;
            
            method.addComment("Data flow path " + pathIndex);
            
            for (int i = 0; i < path.size() - 1; i++) {
                Map<String, Object> sourceNode = path.get(i);
                Map<String, Object> targetNode = path.get(i + 1);
                String sourceId = (String) sourceNode.get("id");
                String targetId = (String) targetNode.get("id");
                
                // Use unique variable names with path identifier
                String sourceVar = "source_" + pathId + "_" + i;
                String targetVar = "target_" + pathId + "_" + i;
                
                method.addStatement("Object $L = components.get($S)", sourceVar, sourceId)
                      .addStatement("Object $L = components.get($S)", targetVar, targetId)
                      .beginControlFlow("if ($L instanceof $T && $L instanceof $T)",
                          sourceVar, ClassName.get("io.webetl.model.component", "OutputQueueProvider"),
                          targetVar, ClassName.get("io.webetl.model.component", "InputQueueProvider"))
                      .addStatement("(($T)$L).registerInputQueue(($T)$L)",
                          ClassName.get("io.webetl.model.component", "OutputQueueProvider"),
                          sourceVar,
                          ClassName.get("io.webetl.model.component", "InputQueueProvider"),
                          targetVar)
                      .endControlFlow();
            }
        }

        // Create worker threads for each component
        method.addComment("Create worker threads for each component");
        method.addStatement("$T<$T> workers = new $T<>()", List.class, Thread.class, ArrayList.class);

        for (Map<String, Object> node : controlFlowNodes) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String nodeId = (String) node.get("id");
            String safeNodeId = nodeId.replaceAll("-", "_");

            // Skip start and stop nodes
            if ("start".equals(componentData.get("id")) || "stop".equals(componentData.get("id"))) {
                continue;
            }

            method.beginControlFlow("$T worker$L = new $T(() -> ",
                ClassName.get(Thread.class), safeNodeId, ClassName.get(Thread.class))
                .addStatement("String componentName = $S", getDisplayNameForComponent(nodeId, componentData))
                .beginControlFlow("try")
                .addStatement("context.setCurrentComponentId(componentName)")
                .addStatement("context.log(\"Starting execution of \" + componentName)")
                .addStatement("Object component = components.get($S)", nodeId)
                .beginControlFlow("if (component instanceof $T)",
                    ClassName.get("io.webetl.model.component", "ExecutableComponent"))
                .addStatement("(($T)component).execute(context)",
                    ClassName.get("io.webetl.model.component", "ExecutableComponent"))
                .nextControlFlow("else")
                .addStatement("throw new $T(\"Component \" + componentName + \" does not implement ExecutableComponent\")",
                    ClassName.get(IllegalStateException.class))
                .endControlFlow()
                .addStatement("context.log(\"Execution of \" + componentName + \" completed\")")
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("context.log(\"Error during execution of \" + componentName + \": \" + e.getMessage())")
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .beginControlFlow("finally")
                .addStatement("context.clearCurrentComponentId()")
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("workers.add(worker$L)", safeNodeId)
                .addStatement("worker$L.start()", safeNodeId);
        }

        // Wait for all workers to complete
        method.addComment("Wait for all worker threads to complete");
        method.beginControlFlow("for ($T worker : workers)", Thread.class)
            .beginControlFlow("try")
            .addStatement("worker.join()")
            .nextControlFlow("catch ($T e)", InterruptedException.class)
            .addStatement("Thread.currentThread().interrupt()")
            .addStatement("throw new $T(\"Execution interrupted\", e)", RuntimeException.class)
            .endControlFlow()
            .endControlFlow();

        method.addStatement("context.log(\"Flow execution completed\")");
              
        return method;
    }

    /**
     * Gets a user-friendly display name for a component
     */
    private String getDisplayNameForComponent(String nodeId, Map<String, Object> componentData) {
        String componentId = (String) componentData.get("id");
        String label = (String) componentData.get("label");
        
        if (label != null && !label.trim().isEmpty()) {
            return label + " (" + nodeId + ")";
        } else if (componentId != null) {
            return componentId + " (" + nodeId + ")";
        } else {
            return nodeId;
        }
    }

    private void connectDataFlowComponents(MethodSpec.Builder method, List<Map<String, Object>> path) {
        // We need at least two components to make a connection
        if (path.size() < 2) {
            return;
        }
        
        for (int i = 0; i < path.size() - 1; i++) {
            Map<String, Object> sourceNode = path.get(i);
            Map<String, Object> targetNode = path.get(i + 1);
            
            String sourceId = (String) sourceNode.get("id");
            String targetId = (String) targetNode.get("id");
            
            method.addComment("Connect data flow: $L -> $L", sourceId, targetId);
            
            // Connect output queue of source to input queue of target
            method.addStatement("(($T)components.get($S)).registerInputQueue(($T)components.get($S))",
                ClassName.get("io.webetl.model.component", "OutputQueueProvider"),
                sourceId,
                ClassName.get("io.webetl.model.component", "InputQueueProvider"),
                targetId);
        }
    }

    private File compileAndCreateJar(JavaFile javaFile, String className, Set<Class<?>> componentClasses, boolean verbose) throws IOException {
        // Create temporary directories for source, class, and jar files
        Path sourcePath = tempDir.resolve("src");
        Path classPath = tempDir.resolve("classes");
        Path libDir = tempDir.resolve("META-INF/lib");
        
        // Create directories if they don't exist
        Files.createDirectories(sourcePath);
        Files.createDirectories(classPath);
        Files.createDirectories(libDir);
        
        // Write source file
        javaFile.writeTo(sourcePath);
        
        
        // Copy dependencies to lib directory
        Set<DependencyEntry> dependencies = collectComponentDependencies(componentClasses, verbose);
        copyDependencies(libDir, dependencies, verbose);
        
        // Build classpath with dependencies
        String classpath = buildClasspathWithDependencies(libDir);
        
        // Copy logback.xml configuration to classes directory
        Path logbackConfig = tempDir.resolve("classes/logback.xml");
        Files.createDirectories(logbackConfig.getParent());
        
        // Simple logback configuration for standalone execution
        String logbackConfigContent = 
            "<configuration>\n" +
            "  <appender name=\"STDOUT\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
            "    <encoder>\n" +
            "      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>\n" +
            "    </encoder>\n" +
            "  </appender>\n" +
            "  <root level=\"info\">\n" +
            "    <appender-ref ref=\"STDOUT\" />\n" +
            "  </root>\n" +
            "</configuration>";
        Files.write(logbackConfig, logbackConfigContent.getBytes());
        
        // Compile source file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Find all source files to compile
        List<File> sourceFiles = new ArrayList<>();
        Files.walk(sourcePath)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(path -> sourceFiles.add(path.toFile()));
        
        if (verbose) {
            log.info("Found {} source files to compile", sourceFiles.size());
            for (File file : sourceFiles) {
                log.info("  - {}", file.getName());
            }
        }
        
        // Compile classes in the correct order - ensure interfaces come first
        Iterable<? extends JavaFileObject> compilationUnits = 
            fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            
        // Add the current classpath to ensure all required classes are available during compilation
        List<String> options = Arrays.asList(
            "-d", classPath.toString(), 
            "-classpath", System.getProperty("java.class.path") + File.pathSeparator + classpath
        );
        
        if (verbose) {
            log.info("Compilation options:");
            for (String option : options) {
                log.info("  - {}", option);
            }
        }
        
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, options, null, compilationUnits);

        boolean success = task.call();
        
        // Check for compilation errors
        if (!success) {
            StringBuilder errorMsg = new StringBuilder("Failed to compile flow: [");
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append(diagnostic.getSource())
                       .append(":")
                       .append(diagnostic.getLineNumber())
                       .append(": error: ")
                       .append(diagnostic.getMessage(Locale.US))
                       .append(", ");
            }
            errorMsg.append("]");
            throw new CompilationException(errorMsg.toString());
        }
        
        if (verbose) {
            log.info("Compilation successful");
            log.info("Checking compiled class files:");
            try {
                Files.walk(classPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> log.info("  - {}", classPath.relativize(path)));
            } catch (IOException e) {
                log.warn("Error listing compiled files", e);
            }
        }

        // Create JAR with manifest
        Path jarPath = tempDir.resolve(className + ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "io.webetl.runtime.JarLauncher");
        manifest.getMainAttributes().put(new Attributes.Name("Flow-Class"), "io.webetl.generated." + className);
        manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "WebETL Flow Compiler");

        if (verbose) {
            log.info("Creating JAR file: {}", jarPath);
            log.info("Manifest entries:");
            log.info("  - Main-Class: {}", "io.webetl.runtime.JarLauncher");
            log.info("  - Flow-Class: {}", "io.webetl.generated." + className);
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
            // Add the compiled flow class
            Files.walk(classPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String entryName = classPath.relativize(path).toString().replace('\\', '/');
                        JarEntry entry = new JarEntry(entryName);
                        jos.putNextEntry(entry);
                        jos.write(Files.readAllBytes(path));
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            
            // Add core component interfaces and implementations
            addClassesToJar(jos, 
                "io.webetl.compiler.CompiledFlow",
                "io.webetl.runtime.ExecutionContext",
                "io.webetl.model.component.ETLComponent",
                "io.webetl.model.component.ExecutableComponent",
                "io.webetl.model.component.InputQueueProvider",
                "io.webetl.model.component.OutputQueueProvider"
            );
            
            // Add SLF4J packages
            addPackageClassesToJar(jos, "org.slf4j", getClass().getClassLoader(), verbose);
            addPackageClassesToJar(jos, "org.slf4j.helpers", getClass().getClassLoader(), verbose);
            addPackageClassesToJar(jos, "org.slf4j.spi", getClass().getClassLoader(), verbose);
            addPackageClassesToJar(jos, "org.slf4j.event", getClass().getClassLoader(), verbose);
            
            // Add component classes
            for (Class<?> componentClass : componentClasses) {
                addClass(jos, componentClass);
            }
            
            // Add the entire io.webetl.runtime package
            addPackageClassesToJar(jos, "io.webetl.runtime", getClass().getClassLoader(), verbose);
            
            // Add the entire io.webetl.model package and subpackages
            addPackageClassesToJar(jos, "io.webetl.model", getClass().getClassLoader(), verbose);
            
            // add the entire io.webetl.components package and subpackages
            addPackageClassesToJar(jos, "io.webetl.components", getClass().getClassLoader(), verbose);
            
            // Add logback configuration to jar
            try {
                JarEntry logbackEntry = new JarEntry("logback.xml");
                jos.putNextEntry(logbackEntry);
                jos.write(Files.readAllBytes(logbackConfig));
                jos.closeEntry();
                if (verbose) {
                    System.out.println("Added custom logback.xml configuration to JAR");
                }
            } catch (ZipException e) {
                if (e.getMessage().contains("duplicate entry")) {
                    // Skip adding if it's a duplicate - the dependency JAR already has it
                    if (verbose) {
                        System.out.println("Skipped adding logback.xml as it already exists in the JAR");
                    }
                } else {
                    throw e; // Re-throw if it's a different ZipException
                }
            }
            
            // Include lib directory with all dependencies
            Files.walk(libDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String entryName = tempDir.relativize(path).toString().replace('\\', '/');
                        JarEntry entry = new JarEntry(entryName);
                        jos.putNextEntry(entry);
                        jos.write(Files.readAllBytes(path));
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
        
        if (verbose) {
            System.out.println("Created JAR: " + jarPath);
        }

        return jarPath.toFile();
    }

    /**
     * Adds multiple classes to the JAR.
     */
    private void addClassesToJar(JarOutputStream jos, String... classNames) throws IOException {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                addClassToJar(jos, className, clazz);
            } catch (ClassNotFoundException e) {
                log.warn("Could not find class: " + className);
            }
        }
    }

    /**
     * Adds a specific class to the JAR.
     */
    private void addClassToJar(JarOutputStream jos, String className, Class<?> clazz) throws IOException {
        String resourceName = className.replace('.', '/') + ".class";
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                jos.write(is.readAllBytes());
            } else {
                throw new IOException("Could not find resource: " + resourceName);
            }
        }
        
        jos.closeEntry();
        
        // Also add inner classes if any exist
        String innerClassPrefix = className + "$";
        String packagePath = className.substring(0, className.lastIndexOf('.')).replace('.', '/');
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        
        URL packageUrl = clazz.getClassLoader().getResource(packagePath);
        if (packageUrl != null && "file".equals(packageUrl.getProtocol())) {
            try {
                Path packageDir = Paths.get(packageUrl.toURI());
                if (Files.exists(packageDir)) {
                    // Look for inner classes with pattern: OuterClass$InnerClass.class
                    Files.list(packageDir)
                        .filter(path -> {
                            String fileName = path.getFileName().toString();
                            return fileName.startsWith(simpleClassName + "$") && 
                                   fileName.endsWith(".class");
                        })
                        .forEach(path -> {
                            try {
                                String fileName = path.getFileName().toString();
                                String innerResourceName = packagePath + "/" + fileName;
                                JarEntry innerEntry = new JarEntry(innerResourceName);
                                jos.putNextEntry(innerEntry);
                                jos.write(Files.readAllBytes(path));
                                jos.closeEntry();
                            } catch (IOException e) {
                                // Silent failure for inner classes
                            }
                        });
                }
            } catch (Exception e) {
                // Silent failure for inner classes
            }
        }
    }


    /**
     * Adds a class to a JAR file
     */
    private void addClass(JarOutputStream jos, Class<?> clazz) throws IOException {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                jos.write(is.readAllBytes());
            } else {
                throw new IOException("Could not find class resource: " + resourceName);
            }
        }
        
        jos.closeEntry();
    }

    private boolean isSourceNode(Map<String, Object> node) {
        String nodeType = (String) node.get("type");
        if (nodeType != null && "source".equals(nodeType)) {
            return true;
        }
        
        // Check by component ID if available
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        if (data != null && data.get("componentData") != null) {
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String componentId = (String) componentData.get("id");
            // Check if componentId contains keywords like "source"
            return componentId != null && componentId.toLowerCase().contains("source");
        }
        return false;
    }

    private boolean isDestinationNode(Map<String, Object> node) {
        String nodeType = (String) node.get("type");
        if (nodeType != null && "destination".equals(nodeType)) {
            return true;
        }
        
        // Check by component ID if available
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        if (data != null && data.get("componentData") != null) {
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String componentId = (String) componentData.get("id");
            // Check if componentId contains keywords like "destination"
            return componentId != null && 
                  (componentId.toLowerCase().contains("destination") || 
                   componentId.toLowerCase().contains("dest"));
        }
        return false;
    }

    private boolean isControlFlowNode(Map<String, Object> node) {
        String nodeType = (String) node.get("type");
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
        String componentId = (String) componentData.get("id");
        
        return "start".equals(nodeType) || "stop".equals(nodeType) ||
               "start".equals(componentId) || "stop".equals(componentId);
    }

    /**
     * Determines if a node is a transform component.
     * Transform components process data but are not part of control flow.
     */
    private boolean isTransformNode(Map<String, Object> node) {
        String nodeType = (String) node.get("type");
        if (nodeType != null && "transform".equals(nodeType)) {
            return true;
        }
        
        // Check by component ID if available
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        if (data != null && data.get("componentData") != null) {
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            String componentId = (String) componentData.get("id");
            // Check if componentId contains keywords like "transform", "filter", or "map"
            return componentId != null && 
                   (componentId.toLowerCase().contains("transform") || 
                    componentId.toLowerCase().contains("filter") || 
                    componentId.contains("map"));
        }
        return false;
    }

    /**
     * Collects dependencies from component classes
     */
    private Set<DependencyEntry> collectComponentDependencies(Set<Class<?>> componentClasses, boolean verbose) {
        Set<DependencyEntry> dependencies = new HashSet<>();
        
        // Add core dependencies that are always required
        dependencies.add(new DependencyEntry("org.slf4j", "slf4j-api", "1.7.36"));
        dependencies.add(new DependencyEntry("ch.qos.logback", "logback-classic", "1.4.11"));
        dependencies.add(new DependencyEntry("ch.qos.logback", "logback-core", "1.4.11"));
        dependencies.add(new DependencyEntry("org.apache.commons", "commons-lang3", "3.12.0"));
        dependencies.add(new DependencyEntry("com.fasterxml.jackson.core", "jackson-core", "2.15.2"));
        dependencies.add(new DependencyEntry("com.fasterxml.jackson.core", "jackson-databind", "2.15.2"));
        dependencies.add(new DependencyEntry("com.fasterxml.jackson.core", "jackson-annotations", "2.15.2"));
        
        
        if (verbose) {
            System.out.println("Collecting dependencies from component classes...");
        }
        
        System.out.println("Component classes size: " + componentClasses.size());
        for (Class<?> componentClass : componentClasses) {
            System.out.println("Processing class: " + componentClass.getName());
            ComponentDependencies annotation = componentClass.getAnnotation(ComponentDependencies.class);
            System.out.println("Found annotation: " + annotation);
            
            if (annotation != null) {
                if (verbose) {
                    System.out.println("Found dependencies for component " + componentClass.getName());
                }
                
                for (Dependency dependency : annotation.value()) {
                    DependencyEntry entry = DependencyEntry.fromAnnotation(dependency);
                    dependencies.add(entry);
                    
                    if (verbose) {
                        System.out.println("  - " + entry);
                    }
                }
            } else if (verbose) {
                System.out.println("No dependencies found for component " + componentClass.getName());
            }
        }
        
        return dependencies;
    }

    /**
     * Copies required dependencies to the lib directory
     */
    private void copyDependencies(Path libDir, Set<DependencyEntry> dependencies, boolean verbose) throws IOException {
        
        // Create a set of JAR name patterns to look for
        Set<String> dependencyPatterns = new HashSet<>();
        
        for (DependencyEntry dep : dependencies) {
            dependencyPatterns.add(dep.getPattern());
        }
        
        if (verbose) {
            System.out.println("Looking for these dependency patterns: " + dependencyPatterns);
        }
        
        // Find dependencies that match the patterns
        Set<DependencyEntry> missingDependencies = new HashSet<>(dependencies);
        
        // Check local Maven repository first
        Path m2Dir = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        if (Files.exists(m2Dir)) {
            try (Stream<Path> walk = Files.walk(m2Dir)) {
                List<Path> jarFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .collect(Collectors.toList());

                for (Path jarPath : jarFiles) {
                    String jarName = jarPath.getFileName().toString();
                    
                    // Check if this JAR matches any of our dependency patterns
                    boolean isMatch = false;
                    DependencyEntry matchedDep = null;
                    
                    for (DependencyEntry dep : dependencies) {
                        if (jarName.matches(dep.getPattern())) {
                            isMatch = true;
                            matchedDep = dep;
                            missingDependencies.remove(dep);
                            break;
                        }
                    }
                    
                    // For all dependencies, just copy the JAR
                    if (isMatch) {
                        Path destPath = libDir.resolve(jarName);
                        Files.copy(jarPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        
                        if (verbose) {
                            System.out.println("Copied dependency: " + jarPath + " to " + destPath);
                        }
                    }
                }
            }
        }
        
        // Download any missing dependencies
        if (!missingDependencies.isEmpty()) {
            downloadMissingDependencies(libDir, missingDependencies, verbose);
        }
    }

    /**
     * Downloads missing dependencies from Maven Central
     */
    private void downloadMissingDependencies(Path libDir, Set<DependencyEntry> dependencies, boolean verbose) {
        for (DependencyEntry dependency : dependencies) {
            Path jarPath = libDir.resolve(dependency.getJarFilename());
            
            if (!Files.exists(jarPath)) {
                if (verbose) {
                    System.out.println("Dependency not found in classpath: " + dependency);
                    System.out.println("Attempting to download from Maven Central...");
                }
                
                try {
                    // Try to download from Maven Central
                    URL url = new URL("https://repo1.maven.org/maven2/" + dependency.getMavenPath());
                    try (InputStream is = url.openStream()) {
                        Files.copy(is, jarPath, StandardCopyOption.REPLACE_EXISTING);
                        if (verbose) {
                            System.out.println("Downloaded dependency: " + dependency);
                        }
                    }
                } catch (IOException e) {
                    if (dependency.isOptional()) {
                        log.warn("Optional dependency not found: " + dependency + " - " + e.getMessage());
                    } else {
                        log.error("Failed to download dependency: " + dependency + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Checks if a JAR contains essential runtime classes that we need.
     */
    private boolean isEssentialRuntimeJar(Path jarPath, boolean verbose) {
        // These are critical packages/classes that must be included
        String[] essentialPackages = {
            "io.webetl.model", // Include ALL classes from the model package
            "io.webetl.compiler.CompiledFlow",
            "io.webetl.runtime.ExecutionContext"
        };
        
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.endsWith(".class")) {
                    // Convert path format to class name format
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    
                    // Check for essential packages/classes
                    for (String pkg : essentialPackages) {
                        if (className.startsWith(pkg) || className.equals(pkg)) {
                            if (verbose) {
                                System.out.println("JAR contains essential class: " + className);
                            }
                            return true;
                        }
                    }
                    
                }
            }
        } catch (IOException e) {
            // Could not read JAR, skip it
            if (verbose) {
                System.out.println("Could not check JAR: " + jarPath + " - " + e.getMessage());
            }
        }
        
        
        return false;
    }

    /**
     * Determines if a JAR should be skipped during dependency inclusion
     */
    private boolean shouldSkipJar(String jarName) {
        // Skip the main application JAR, we don't want to include ourselves
        if (jarName.startsWith("webetl-") || jarName.startsWith("backend-")) {
            return true;
        }
        
        // Skip test JARs
        if (jarName.contains("-tests.jar") || jarName.contains("-test.jar")) {
            return true;
        }
        
        // Skip Spring Boot fat JARs
        if (jarName.contains("spring-boot") && jarName.contains(".original")) {
            return true;
        }
        
        return false;
    }

    /**
     * Adds all classes from a package to the JAR.
     */
    private void addPackageClassesToJar(JarOutputStream jos, String packageName, ClassLoader classLoader, boolean verbose) {
        String packagePath = packageName.replace('.', '/');
        
        try {
            // Try to find the package resources using the class loader
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                
                if ("file".equals(protocol)) {
                    // Directory-based package
                    try {
                        Path packageDir = Paths.get(resource.toURI());
                        
                        if (Files.exists(packageDir) && Files.isDirectory(packageDir)) {
                            Files.walk(packageDir)
                                .filter(path -> path.toString().endsWith(".class"))
                                .forEach(path -> {
                                    try {
                                        // Calculate the class name from the file path
                                        String relativePath = packageDir.relativize(path).toString();
                                        String className = packageName + "." + relativePath
                                                .replace(File.separatorChar, '.')
                                                .substring(0, relativePath.length() - 6); // remove .class
                                        
                                        // Load the class
                                        try {
                                            Class<?> clazz = classLoader.loadClass(className);
                                            // Add the class to the JAR
                                            String resourcePath = className.replace('.', '/') + ".class";
                                            JarEntry entry = new JarEntry(resourcePath);
                                            jos.putNextEntry(entry);
                                            
                                            try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                                                if (is != null) {
                                                    jos.write(is.readAllBytes());
                                                    if (verbose) {
                                                        System.out.println("  - Added class: " + className);
                                                    }
                                                } else {
                                                    if (verbose) {
                                                        System.out.println("  - Could not find resource: " + resourcePath);
                                                    }
                                                }
                                            }
                                            
                                            jos.closeEntry();
                                        } catch (ClassNotFoundException e) {
                                            if (verbose) {
                                                System.out.println("  - Failed to load class: " + className);
                                            }
                                        }
                                    } catch (Exception e) {
                                        if (verbose) {
                                            System.out.println("  - Error processing class file: " + path + " - " + e.getMessage());
                                        }
                                    }
                                });
                        }
                    } catch (Exception e) {
                        if (verbose) {
                            System.out.println("  - Error processing directory resource: " + e.getMessage());
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    // JAR-based package
                    String jarPath = resource.getPath();
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring(5, jarPath.indexOf('!'));
                        try {
                            JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8));
                            Enumeration<JarEntry> entries = jar.entries();
                            
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                
                                if (name.startsWith(packagePath + "/") && name.endsWith(".class")) {
                                    try {
                                        // Add the class directly to our JAR
                                        JarEntry newEntry = new JarEntry(name);
                                        jos.putNextEntry(newEntry);
                                        
                                        try (InputStream is = jar.getInputStream(entry)) {
                                            jos.write(is.readAllBytes());
                                        }
                                        
                                        jos.closeEntry();
                                        
                                        if (verbose) {
                                            String className = name.substring(0, name.length() - 6).replace('/', '.');
                                            System.out.println("  - Added class from JAR: " + className);
                                        }
                                    } catch (Exception e) {
                                        if (verbose) {
                                            System.out.println("  - Failed to add JAR entry: " + name + " - " + e.getMessage());
                                        }
                                    }
                                }
                            }
                            
                            jar.close();
                        } catch (Exception e) {
                            if (verbose) {
                                System.out.println("  - Error processing JAR resource: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (verbose) {
                System.out.println("  - General error processing package: " + e.getMessage());
            }
        }
    }

    /**
     * Builds classpath string including all the dependencies
     */
    private String buildClasspathWithDependencies(Path libDir) throws IOException {
        StringBuilder classpath = new StringBuilder();
        classpath.append(System.getProperty("java.class.path"));
        
        // Add all jars in lib directory to classpath
        if (Files.exists(libDir)) {
            Files.list(libDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(jar -> {
                    classpath.append(File.pathSeparator);
                    classpath.append(jar.toAbsolutePath());
                });
        }
        
        return classpath.toString();
    }

    /**
     * Extracts component classes from the sheet
     */
    private Set<Class<?>> extractComponentClasses(Sheet sheet, boolean verbose) {
        Set<Class<?>> componentClasses = new HashSet<>();
        
        if (verbose) {
            System.out.println("Extracting component classes from sheet...");
        }
        
        for (Map<String, Object> node : sheet.getNodes()) {
            String nodeId = node.containsKey("id") ? (String) node.get("id") : "unknown";
            System.out.println("Processing node: " + nodeId + " of type: " + node.get("type"));
            
            // Skip nodes that don't have data
            if (!node.containsKey("data")) {
                System.out.println("Node doesn't have data: " + node);
                continue;
            }
            
            Object dataObj = node.get("data");
            if (!(dataObj instanceof Map)) {
                System.out.println("Node data is not a map: " + dataObj);
                continue;
            }
            
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // Print entire data structure for debugging
            System.out.println("Node data structure: " + data);
            
            // Check various possible paths to find the implementation class
            String implementationClass = null;
            
            // Try path: data.component.implementation
            if (data.containsKey("component") && data.get("component") instanceof Map) {
                Map<String, Object> component = (Map<String, Object>) data.get("component");
                if (component.containsKey("implementation")) {
                    implementationClass = (String) component.get("implementation");
                }
            }
            
            // Try path: data.componentData.implementationClass
            if (implementationClass == null && data.containsKey("componentData") && data.get("componentData") instanceof Map) {
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                if (componentData.containsKey("implementationClass")) {
                    implementationClass = (String) componentData.get("implementationClass");
                }
            }
            
            if (implementationClass == null) {
                System.out.println("Could not find implementation class in node: " + nodeId);
                continue;
            }
            
            System.out.println("Found implementation class: " + implementationClass + " for node " + nodeId);
            
            try {
                Class<?> componentClass = Class.forName(implementationClass);
                componentClasses.add(componentClass);
                
                // Print annotations on the class for debugging
                System.out.println("Annotations on class " + componentClass.getName() + ":");
                for (Annotation annotation : componentClass.getAnnotations()) {
                    System.out.println("  - " + annotation);
                }
                
                if (verbose) {
                    System.out.println("Added component class: " + implementationClass + " to the set");
                }
            } catch (ClassNotFoundException e) {
                log.warn("Component class not found: " + implementationClass, e);
            }
        }
        
        System.out.println("Extracted " + componentClasses.size() + " component classes");
        return componentClasses;
    }
} 