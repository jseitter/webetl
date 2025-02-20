package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.DestinationComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.runtime.ExecutionContext;

@ETLComponentDefinition(
    id = "csv-destination",
    label = "CSV Writer",
    description = "Writes data to CSV files",
    icon = "FileIcon",
    backgroundColor = "#e8f5e9"
)
public class CsvDestinationComponent extends DestinationComponent {
    public CsvDestinationComponent() {
        getParameters().add(StringParameter.builder()
            .name("filepath")
            .label("File Path")
            .required(true)
            .build());
            
        getParameters().add(StringParameter.builder()
            .name("delimiter")
            .label("Delimiter")
            .required(false)
            .defaultValue(",")
            .build());
    }

    @Override
    public void execute(ExecutionContext context) {
        // Implementation for CSV writing
    }
} 