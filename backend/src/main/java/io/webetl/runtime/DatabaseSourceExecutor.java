package io.webetl.runtime;

import java.util.*;

public class DatabaseSourceExecutor implements ComponentExecutor {
    private MetricsCallback metricsCallback;

    @Override
    public DataStream execute(ExecutionContext context, ComponentConfig config) {
        return new DummyDataStream(100); // Generate 100 dummy records
    }

    @Override
    public void registerMetricsCallback(MetricsCallback callback) {
        this.metricsCallback = callback;
    }

    private static class DummyDataStream implements DataStream {
        private int remainingRecords;
        private final Random random = new Random();

        public DummyDataStream(int totalRecords) {
            this.remainingRecords = totalRecords;
        }

        @Override
        public List<Map<String, Object>> read(int batchSize) {
            if (!hasMore()) return Collections.emptyList();
            
            int count = Math.min(batchSize, remainingRecords);
            List<Map<String, Object>> batch = new ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", remainingRecords - i);
                record.put("value", "Data-" + random.nextInt(1000));
                batch.add(record);
            }
            
            remainingRecords -= count;
            return batch;
        }

        @Override
        public boolean hasMore() {
            return remainingRecords > 0;
        }

        @Override
        public void close() {
            // Cleanup resources if needed
        }
    }
} 