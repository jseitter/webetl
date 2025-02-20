package io.webetl.model.data;

import lombok.Data;
import java.time.Instant;

/**
 * RowMetadata is a class that contains metadata about a row.
 * It is used to store the timestamp, sourceId, and sourceType of a row.
 */ 
@Data
public class RowMetadata {
    private Instant timestamp=Instant.now();
    private String sourceId;
    private String sourceType;
} 