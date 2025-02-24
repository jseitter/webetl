package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;

/**
 * FileSourceComponent is a component that reads data from files.
 */
@ETLComponentDefinition(
    id = "file-source",
    label = "File Source",
    description = "Reads data from files",
    icon = "FileIcon",
    backgroundColor = "#f0f7ff"
)
public class FileSourceComponent extends SourceComponent {
    @Override
    public void execute(ExecutionContext context) {
        // Implementation for reading from file
        System.out.println("Executing file source component");
    }
} 