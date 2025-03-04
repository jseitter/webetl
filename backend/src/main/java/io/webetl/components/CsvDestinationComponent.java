package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.DestinationComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.data.Row;
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
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Implementation for CSV writing
        info(context, "Executing CSV destination component");
        
        try {
            info(context, "Waiting for incoming rows...");
            Row row = super.takeInputRow();
            info(context, "Processing row: " + row);
            
            // Add actual CSV writing logic here
            String filepath = getParameter("filepath", String.class);
            String delimiter = getParameter("delimiter", String.class);
            
            info(context, "Writing to file: " + filepath + " with delimiter: " + delimiter);
            
            // Implement actual file writing logic
            
        } catch (InterruptedException e) {
            error(context, "CSV writing was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            error(context, "Error writing CSV data", e);
            throw e;
        }
    }
    
    private <T> T getParameter(String name, Class<T> type) {
        return (T) getParameters().stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(p -> p.getValue())
            .orElse(null);
    }
} 