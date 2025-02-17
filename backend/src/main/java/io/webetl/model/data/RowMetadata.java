package io.webetl.model.data;

import lombok.Data;
import java.time.Instant;

@Data
public class RowMetadata {
    private Instant timestamp;
    private String sourceId;
    private String sourceType;
} 