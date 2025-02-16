package io.webetl.runtime;

public interface MetricsCallback {
    void onMetricsUpdate(String nodeId, RuntimeMetrics.NodeMetrics metrics);
} 