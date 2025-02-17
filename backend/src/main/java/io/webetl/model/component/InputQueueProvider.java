package io.webetl.model.component;

import io.webetl.model.data.Row;

/**
 * InputQueueProvider is a component that provides a input queue
 * It is used to connect components that provide data to components that consume data.
 */
public interface InputQueueProvider {

    /**
     * Queue a row for processing.
     * @param row the row to queue
     */
    public void putRow(Row row);
}
