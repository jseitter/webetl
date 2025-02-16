package io.webetl.runtime;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import io.webetl.model.Sheet;
import io.webetl.runtime.executors.*;

@Service
public class FlowExecutor {
    private final Map<String, ComponentExecutor> executors = new HashMap<>();
    private final Map<String, RuntimeMetrics> sheetMetrics = new HashMap<>();

    @PostConstruct
    void init() {
        // Register component executors with dummy implementations
        executors.put("db-source", new DatabaseSourceExecutor());
        executors.put("file-source", new DummySourceExecutor());
        executors.put("filter", new DummyFilterExecutor());
        executors.put("db-dest", new DummyDestinationExecutor());
    }

    public ExecutionResult execute(Sheet sheet, ExecutionContext context) {
        RuntimeMetrics metrics = new RuntimeMetrics();
        sheetMetrics.put(sheet.getId(), metrics);

        try {
            // Simple sequential execution for prototype
            for (Map<String, Object> node : sheet.getNodes()) {
                executeNode(node, context, metrics);
            }

            return new ExecutionResult(true, "Execution completed", metrics);
        } catch (Exception e) {
            return new ExecutionResult(false, "Execution failed: " + e.getMessage(), metrics);
        }
    }

    public RuntimeMetrics getMetrics(String sheetId) {
        return sheetMetrics.get(sheetId);
    }

    private void executeNode(Map<String, Object> node, ExecutionContext context, RuntimeMetrics metrics) {
        String nodeId = (String) node.get("id");
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
        String type = (String) componentData.get("type");

        ComponentExecutor executor = executors.get(type);
        if (executor == null) {
            throw new RuntimeException("No executor found for type: " + type);
        }

        // Create node metrics
        RuntimeMetrics.NodeMetrics nodeMetrics = new RuntimeMetrics.NodeMetrics();
        nodeMetrics.setNodeId(nodeId);
        nodeMetrics.setStatus("running");
        metrics.updateMetrics(nodeId, nodeMetrics);

        // Execute with dummy data
        DataStream result = executor.execute(context, new ComponentConfig(componentData));
        
        // Process dummy data
        int totalRecords = 0;
        while (result.hasMore()) {
            List<Map<String, Object>> batch = result.read(1000);
            totalRecords += batch.size();
            
            // Update metrics
            nodeMetrics.setRecordsProcessed(totalRecords);
            metrics.updateMetrics(nodeId, nodeMetrics);
        }

        nodeMetrics.setStatus("completed");
        metrics.updateMetrics(nodeId, nodeMetrics);
    }
} 