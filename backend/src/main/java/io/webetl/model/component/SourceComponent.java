package io.webetl.model.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import io.webetl.model.data.Row;

/**
 * SourceComponent is a component that reads data from a source.
 * It has only output ports.
 */
public abstract class SourceComponent extends ETLComponent implements OutputQueueProvider {
    private String sourceType;
    private boolean supportsControlFlow;
    private final List<InputQueueProvider> outputQueues;

    public SourceComponent() {
        super(null, null, null, null, "#f0f7ff", new ArrayList<>());
        this.outputQueues = new CopyOnWriteArrayList<>();
    }

    public SourceComponent(String id, String label, String description, String icon, 
                         String sourceType, boolean supportsControlFlow) {
        super(id, label, description, icon, "#f0f7ff", new ArrayList<>());
        this.sourceType = sourceType;
        this.supportsControlFlow = supportsControlFlow;
        this.outputQueues = new CopyOnWriteArrayList<>();
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public boolean isSupportsControlFlow() { return supportsControlFlow; }
    public void setSupportsControlFlow(boolean supportsControlFlow) { this.supportsControlFlow = supportsControlFlow; }

    @Override
    public void registerInputQueue(InputQueueProvider provider) {
        outputQueues.add(provider);
    }
    
    @Override
    public void sendRow(Row row) {
        for (InputQueueProvider queue : outputQueues) {
            System.out.println("sending Row: " + row );
            queue.putRow(row);
        }
    }
} 