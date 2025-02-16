package io.webetl.runtime.executors;

import io.webetl.runtime.*;
import java.util.*;

public class DummySourceExecutor implements ComponentExecutor {
    private MetricsCallback metricsCallback;

    @Override
    public DataStream execute(ExecutionContext context, ComponentConfig config) {
        return new DummyDataStream(50);
    }

    @Override
    public void registerMetricsCallback(MetricsCallback callback) {
        this.metricsCallback = callback;
    }
} 