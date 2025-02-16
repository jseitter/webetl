package io.webetl.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.Duration;

public class RuntimeMetrics {
    private final Map<String, NodeMetrics> nodeMetrics = new ConcurrentHashMap<>();
    
    public static class NodeMetrics {
        private String nodeId;
        private long recordsProcessed;
        private long bytesProcessed;
        private Duration processingTime;
        private String status; // "running", "completed", "error"

        // Getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public long getRecordsProcessed() { return recordsProcessed; }
        public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }

        public long getBytesProcessed() { return bytesProcessed; }
        public void setBytesProcessed(long bytesProcessed) { this.bytesProcessed = bytesProcessed; }

        public Duration getProcessingTime() { return processingTime; }
        public void setProcessingTime(Duration processingTime) { this.processingTime = processingTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public void updateMetrics(String nodeId, NodeMetrics metrics) {
        nodeMetrics.put(nodeId, metrics);
    }

    // Add getter for Jackson serialization
    public Map<String, NodeMetrics> getNodeMetrics() {
        return nodeMetrics;
    }
} 