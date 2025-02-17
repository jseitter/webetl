package io.webetl.model.component;

import io.webetl.model.data.Row;

/**
 * OutputQueueProvider is a component that can send rows to registered input queues.
 */
public interface OutputQueueProvider {
    /**
     * Register an input queue provider to receive rows from this component.
     * @param provider the input queue provider to register
     */
    void registerInputQueue(InputQueueProvider provider);
    
    /**
     * Send a row to all registered input queues.
     * @param row the row to send
     */
    void sendRow(Row row);
} 