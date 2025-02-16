package io.webetl.runtime;

import java.util.List;
import java.util.Map;

public interface DataStream {
    List<Map<String, Object>> read(int batchSize);
    boolean hasMore();
    void close();
} 