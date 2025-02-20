package io.webetl.model.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.webetl.model.data.Row;

/**
 * TransformComponent is a component that transforms data.
 * It can also just passthrough the data and e.g log it.
 * It has Input and Output ports to connect to other components.
 */
public abstract class TransformComponent extends ETLComponent implements InputQueueProvider, OutputQueueProvider, ExecutableComponent {
    private String transformationType;
    private String[] inputTypes;
    private String[] outputTypes;
    private final BlockingQueue<Row> inputQueue;
    private final List<InputQueueProvider> outputQueues;

    public TransformComponent() {
        super(null, null, null, null, "#fff7f0", new ArrayList<>());
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueues = new CopyOnWriteArrayList<>();
    }

    public TransformComponent(String id, String label, String description, String icon,
                            String transformationType, String[] inputTypes, String[] outputTypes) {
        super(id, label, description, icon, "#fff7f0", new ArrayList<>());
        this.transformationType = transformationType;
        this.inputTypes = inputTypes;
        this.outputTypes = outputTypes;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueues = new CopyOnWriteArrayList<>();
    }

    public String getTransformationType() { return transformationType; }
    public void setTransformationType(String transformationType) { this.transformationType = transformationType; }

    public String[] getInputTypes() { return inputTypes; }
    public void setInputTypes(String[] inputTypes) { this.inputTypes = inputTypes; }

    public String[] getOutputTypes() { return outputTypes; }
    public void setOutputTypes(String[] outputTypes) { this.outputTypes = outputTypes; }

    @Override
    public void putRow(Row row) {
        try {
            inputQueue.put(row);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while putting row into queue", e);
        }
    }

    protected Row takeInputRow() throws InterruptedException {
        return inputQueue.take();
    }

    @Override
    public void registerInputQueue(InputQueueProvider provider) {
        outputQueues.add(provider);
    }

    @Override
    public void sendRow(Row row) {
        for (InputQueueProvider queue : outputQueues) {
            queue.putRow(row);
        }
    }

} 