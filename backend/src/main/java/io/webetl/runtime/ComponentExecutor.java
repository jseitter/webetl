package io.webetl.runtime;

import io.webetl.model.Sheet;
import java.util.List;
import java.util.Map;

public interface ComponentExecutor {
    DataStream execute(ExecutionContext context, ComponentConfig config);
    void registerMetricsCallback(MetricsCallback callback);
} 