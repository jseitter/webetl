package io.webetl.model.component;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.webetl.model.data.Row;

/**
 * DestinationComponent is a component that writes data to a destination.
 * It has only input ports.
 */
public abstract class DestinationComponent extends ETLComponent implements InputQueueProvider {
    private final BlockingQueue<Row> inputQueue;

    public DestinationComponent() {
        super(null, null, null, null, "#f0fff4", new ArrayList<>());
        this.inputQueue = new LinkedBlockingQueue<>();
    }

    public DestinationComponent(String id, String label, String description, String icon,
                              String destinationType, String[] acceptedTypes) {
        super(id, label, description, icon, "#f0fff4", new ArrayList<>());
        this.inputQueue = new LinkedBlockingQueue<>();
    }



    @Override
    public void putRow(Row row) {
        try {
            inputQueue.put(row);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while putting row into queue", e);
        }
    }

    protected Row takeRow() throws InterruptedException {
        return inputQueue.take();
    }

    protected boolean hasRows() {
        return !inputQueue.isEmpty();
    }

} 