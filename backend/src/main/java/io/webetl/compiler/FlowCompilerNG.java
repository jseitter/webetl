package io.webetl.compiler;

import com.palantir.javapoet.*;
import io.webetl.model.Sheet;
import io.webetl.model.component.ETLComponent;
import io.webetl.model.component.ExecutableComponent;
import io.webetl.model.component.InputQueueProvider;
import io.webetl.model.component.OutputQueueProvider;
import io.webetl.runtime.ExecutionContext;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;

@Service
public class FlowCompilerNG {
    private final Path tempDir;
    
    public FlowCompilerNG() throws IOException {
        this.tempDir = Files.createTempDirectory("flow-compiler");
    }

    public File compileToJar(Sheet sheet, boolean verbose) throws CompilationException {
        try {
            validateFlow(sheet);
            String className = "Flow_" + sheet.getId().replace("-", "_");
            
            // Generate Java code using JavaPoet
            TypeSpec.Builder flowClass = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ExecutableComponent.class)
                .addField(FieldSpec.builder(
                    ParameterizedTypeName.get(Map.class, String.class, ETLComponent.class),
                    "components",
                    Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T<>()", HashMap.class)
                    .build());

            // Add constructor
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

            // Initialize components
            for (Map<String, Object> node : sheet.getNodes()) {
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                String nodeId = (String) node.get("id");
                String componentClass = (String) componentData.get("implementationClass");

                constructor.addStatement("components.put($S, new $L())", 
                    nodeId, componentClass);
            }

            // Add execute method
            MethodSpec.Builder executeMethod = MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ExecutionContext.class, "context")
                .addException(Exception.class)
                .returns(void.class);

            // Add control flow logic
            List<Map<String, Object>> controlFlow = buildControlFlow(sheet.getNodes(), sheet.getEdges());
            for (Map<String, Object> node : controlFlow) {
                String nodeId = (String) node.get("id");
                executeMethod.addStatement("components.get($S).execute(context)", nodeId);
            }

            // Add data flow connections
            executeMethod.addComment("Connect data flow components");
            List<List<Map<String, Object>>> dataFlowChains = buildDataFlowChains(sheet.getNodes(), sheet.getEdges());
            for (List<Map<String, Object>> chain : dataFlowChains) {
                connectDataFlowComponents(executeMethod, chain);
            }

            // Build the class
            TypeSpec flowTypeSpec = flowClass
                .addMethod(constructor.build())
                .addMethod(executeMethod.build())
                .build();

            // Generate Java file
            JavaFile javaFile = JavaFile.builder("io.webetl.generated", flowTypeSpec)
                .build();

            // Write to temp directory
            Path sourcePath = tempDir.resolve("src");
            Files.createDirectories(sourcePath);
            javaFile.writeTo(sourcePath);

            // Compile
            if (verbose) {
                System.out.println("Compiling generated code...");
            }
            
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            List<String> options = Arrays.asList("-d", tempDir.resolve("classes").toString());
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                Files.walk(sourcePath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList())
            );

            compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            // Create JAR
            Path jarPath = tempDir.resolve(className + ".jar");
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
                Path classesDir = tempDir.resolve("classes");
                Files.walk(classesDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String entryName = classesDir.relativize(path).toString().replace('\\', '/');
                            jos.putNextEntry(new JarEntry(entryName));
                            Files.copy(path, jos);
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            }

            return jarPath.toFile();

        } catch (Exception e) {
            throw new CompilationException("Failed to compile flow: " + e.getMessage(), e);
        }
    }

    private void validateFlow(Sheet sheet) throws CompilationException {
        if (sheet.getNodes() == null || sheet.getNodes().isEmpty()) {
            throw new CompilationException("Flow must contain at least one node");
        }
        if (sheet.getEdges() == null) {
            throw new CompilationException("Flow edges cannot be null");
        }
    }

    private List<Map<String, Object>> buildControlFlow(List<Map<String, Object>> nodes, 
            List<Map<String, Object>> edges) {
        // Find nodes without incoming edges (start nodes)
        List<Map<String, Object>> startNodes = nodes.stream()
            .filter(node -> edges.stream()
                .noneMatch(edge -> edge.get("target").equals(node.get("id"))))
            .collect(Collectors.toList());

        // Build execution order through DFS
        List<Map<String, Object>> executionOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Map<String, Object> startNode : startNodes) {
            dfsTraversal(startNode, nodes, edges, executionOrder, visited);
        }

        return executionOrder;
    }

    private void dfsTraversal(Map<String, Object> node, List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges, List<Map<String, Object>> executionOrder,
            Set<String> visited) {
        String nodeId = (String) node.get("id");
        if (visited.contains(nodeId)) {
            return;
        }

        visited.add(nodeId);
        executionOrder.add(node);

        // Find all outgoing edges and traverse them
        edges.stream()
            .filter(edge -> edge.get("source").equals(nodeId))
            .forEach(edge -> {
                String targetId = (String) edge.get("target");
                nodes.stream()
                    .filter(n -> n.get("id").equals(targetId))
                    .findFirst()
                    .ifPresent(targetNode -> 
                        dfsTraversal(targetNode, nodes, edges, executionOrder, visited));
            });
    }

    private List<List<Map<String, Object>>> buildDataFlowChains(List<Map<String, Object>> nodes, 
            List<Map<String, Object>> edges) {
        // Find source nodes (nodes that implement SourceComponent)
        List<Map<String, Object>> sourceNodes = nodes.stream()
            .filter(node -> {
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                String implementationClass = (String) componentData.get("implementationClass");
                return implementationClass.contains("SourceComponent");
            })
            .collect(Collectors.toList());

        // Build chains starting from each source node
        List<List<Map<String, Object>>> chains = new ArrayList<>();
        for (Map<String, Object> sourceNode : sourceNodes) {
            List<Map<String, Object>> chain = new ArrayList<>();
            buildChain(sourceNode, nodes, edges, chain, new HashSet<>());
            if (!chain.isEmpty()) {
                chains.add(chain);
            }
        }

        return chains;
    }

    private void buildChain(Map<String, Object> node, List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges, List<Map<String, Object>> currentChain,
            Set<String> visited) {
        String nodeId = (String) node.get("id");
        if (visited.contains(nodeId)) {
            return;
        }

        visited.add(nodeId);
        currentChain.add(node);

        // Find next node in the chain
        edges.stream()
            .filter(edge -> edge.get("source").equals(nodeId))
            .forEach(edge -> {
                String targetId = (String) edge.get("target");
                nodes.stream()
                    .filter(n -> n.get("id").equals(targetId))
                    .findFirst()
                    .ifPresent(targetNode -> 
                        buildChain(targetNode, nodes, edges, currentChain, visited));
            });
    }

    private void connectDataFlowComponents(MethodSpec.Builder method, List<Map<String, Object>> chain) {
        for (int i = 0; i < chain.size() - 1; i++) {
            Map<String, Object> sourceNode = chain.get(i);
            Map<String, Object> targetNode = chain.get(i + 1);
            String sourceId = (String) sourceNode.get("id");
            String targetId = (String) targetNode.get("id");
            
            method.addComment("Connect $L to $L", sourceId, targetId);
            method.beginControlFlow("if (components.get($S) instanceof $T && components.get($S) instanceof $T)",
                sourceId, OutputQueueProvider.class, targetId, InputQueueProvider.class)
                .addStatement("(($T)components.get($S)).registerInputQueue(($T)components.get($S))",
                    OutputQueueProvider.class, sourceId, InputQueueProvider.class, targetId)
                .endControlFlow();
        }
    }
} 