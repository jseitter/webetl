package io.webetl.runtime.executors;

import io.webetl.runtime.*;
import java.util.*;

public class DummyDestinationExecutor implements ComponentExecutor {
    private MetricsCallback metricsCallback;

    @Override
    public DataStream execute(ExecutionContext context, ComponentConfig config) {
        return new DummyDataStream(25); // Simulate writing records
    }

    @Override
    public void registerMetricsCallback(MetricsCallback callback) {
        this.metricsCallback = callback;
    }

    private static class DummyDataStream implements DataStream {
        private int remainingRecords;

        public DummyDataStream(int totalRecords) {
            this.remainingRecords = totalRecords;
        }

        @Override
        public List<Map<String, Object>> read(int batchSize) {
            if (!hasMore()) return Collections.emptyList();
            int count = Math.min(batchSize, remainingRecords);
            remainingRecords -= count;
            return Collections.nCopies(count, new HashMap<>());
        }

        @Override
        public boolean hasMore() {
            return remainingRecords > 0;
        }

        @Override
        public void close() {}
    }
} 