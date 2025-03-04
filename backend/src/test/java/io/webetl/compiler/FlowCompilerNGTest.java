package io.webetl.compiler;

import io.webetl.model.Sheet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.io.File;
import java.util.jar.JarFile;
import io.webetl.components.DatabaseSourceComponent;
import io.webetl.components.CsvDestinationComponent;
import io.webetl.model.component.parameter.Parameter;

class FlowCompilerNGTest {

    @Test
    void testSimpleFlow() throws Exception {
        // Enable test logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        
        System.out.println("\n=== Starting Flow Compilation Test ===\n");
        
        // Create actual component instances
        DatabaseSourceComponent dbSource = new DatabaseSourceComponent();
        dbSource.setId("db-source");
        dbSource.getParameters().stream()
            .filter(p -> p.getName().equals("query"))
            .findFirst()
            .ifPresent(p -> ((Parameter<String>)p).setValue("SELECT * FROM test_table"));
        
        CsvDestinationComponent csvDest = new CsvDestinationComponent();
        csvDest.setId("csv-dest");
        csvDest.getParameters().stream()
            .filter(p -> p.getName().equals("filepath"))
            .findFirst()
            .ifPresent(p -> ((Parameter<String>)p).setValue("/tmp/output.csv"));
        
        Sheet sheet = new Sheet();
        sheet.setId("test-flow");
        
        // Create nodes list using actual component data
        List<Map<String, Object>> nodes = new ArrayList<>();
        
        // Add start node
        Map<String, Object> startNode = new HashMap<>();
        startNode.put("id", "start1");
        startNode.put("type", "start");  // Special type for control flow
        Map<String, Object> startData = new HashMap<>();
        Map<String, Object> startComponentData = new HashMap<>();
        startComponentData.put("id", "start");  // Just use the ID to mark as start
        startData.put("componentData", startComponentData);
        startNode.put("data", startData);
        
        // Add DB source node
        Map<String, Object> sourceNode = new HashMap<>();
        sourceNode.put("id", "source1");
        sourceNode.put("type", "source");
        Map<String, Object> sourceData = new HashMap<>();
        Map<String, Object> sourceComponentData = new HashMap<>();
        sourceComponentData.put("implementationClass", dbSource.getClass().getName());
        sourceComponentData.put("id", dbSource.getId());
        sourceComponentData.put("parameters", dbSource.getParameters());
        sourceComponentData.put("supportsControlFlow", true);
        sourceData.put("componentData", sourceComponentData);
        sourceNode.put("data", sourceData);
        
        // Add CSV destination node
        Map<String, Object> destNode = new HashMap<>();
        destNode.put("id", "dest1");
        destNode.put("type", "destination");
        Map<String, Object> destData = new HashMap<>();
        Map<String, Object> destComponentData = new HashMap<>();
        destComponentData.put("implementationClass", csvDest.getClass().getName());
        destComponentData.put("id", csvDest.getId());
        destComponentData.put("parameters", csvDest.getParameters());
        destComponentData.put("supportsControlFlow", true);
        destData.put("componentData", destComponentData);
        destNode.put("data", destData);
        
        // Add stop node
        Map<String, Object> stopNode = new HashMap<>();
        stopNode.put("id", "stop1");
        stopNode.put("type", "stop");  // Special type for control flow
        Map<String, Object> stopData = new HashMap<>();
        Map<String, Object> stopComponentData = new HashMap<>();
        stopComponentData.put("id", "stop");  // Just use the ID to mark as stop
        stopData.put("componentData", stopComponentData);
        stopNode.put("data", stopData);
        
        nodes.add(startNode);
        nodes.add(sourceNode);
        nodes.add(destNode);
        nodes.add(stopNode);
        
        // Create edge connecting source to destination
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, Object> edge = new HashMap<>();
        edge.put("id", "edge1");
        edge.put("source", "source1");
        edge.put("target", "dest1");
        edge.put("sourceHandle", "data-source");
        edge.put("targetHandle", "data-target");
        edges.add(edge);
        
        // Add control flow edges
        Map<String, Object> controlEdge1 = new HashMap<>();
        controlEdge1.put("id", "control-edge1");
        controlEdge1.put("source", "start1");
        controlEdge1.put("target", "source1");
        controlEdge1.put("sourceHandle", "control-flow-out");
        controlEdge1.put("targetHandle", "control-flow-in");
        edges.add(controlEdge1);
        
        Map<String, Object> controlEdge2 = new HashMap<>();
        controlEdge2.put("id", "control-edge2");
        controlEdge2.put("source", "source1");
        controlEdge2.put("target", "stop1");
        controlEdge2.put("sourceHandle", "control-flow-out");
        controlEdge2.put("targetHandle", "control-flow-in");
        edges.add(controlEdge2);
        
        sheet.setNodes(nodes);
        sheet.setEdges(edges);

        // Print test configuration
        System.out.println("Test Sheet Configuration:");
        System.out.println("  Sheet ID: " + sheet.getId());
        System.out.println("  Nodes: " + sheet.getNodes().size());
        for (Map<String, Object> node : sheet.getNodes()) {
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
            System.out.println("    - Node: " + node.get("id") + 
                             " (Type: " + node.get("type") + 
                             ", Implementation: " + componentData.get("implementationClass") + ")");
        }
        System.out.println("  Edges: " + sheet.getEdges().size() + "\n");

        try {
            // Test compilation
            FlowCompilerNG compiler = new FlowCompilerNG();
            File jarFile = compiler.compileToJar(sheet, true /* verbose */);
            
            // Verify jar was created
            assertTrue(jarFile.exists(), "JAR file should exist");
            assertTrue(jarFile.length() > 0, "JAR file should not be empty");
            
            // Print jar contents for debugging
            System.out.println("\nCompilation successful!");
            System.out.println("JAR file: " + jarFile.getAbsolutePath());
            try (JarFile jar = new JarFile(jarFile)) {
                System.out.println("JAR contents:");
                jar.stream().forEach(entry -> 
                    System.out.println("  " + entry.getName())
                );
            }
        } catch (CompilationException e) {
            System.err.println("\nCompilation Failed!");
            System.err.println("Error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("\nCause: ");
                e.getCause().printStackTrace();
            }
            throw e;
        }
        System.out.println("\n=== Test Complete ===\n");
    }
} 