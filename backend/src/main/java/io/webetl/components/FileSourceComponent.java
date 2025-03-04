package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

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
    
    public FileSourceComponent() {
        getParameters().add(StringParameter.builder()
            .name("filepath")
            .label("File Path")
            .description("Path to the file to read")
            .required(true)
            .build());
            
        getParameters().add(StringParameter.builder()
            .name("delimiter")
            .label("Delimiter")
            .description("Character used to separate fields (for CSV/TSV files)")
            .required(false)
            .defaultValue(",")
            .build());
            
        getParameters().add(StringParameter.builder()
            .name("encoding")
            .label("Encoding")
            .description("Character encoding of the file")
            .required(false)
            .defaultValue("UTF-8")
            .build());
    }
    
    @Override
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Implementation for reading from file
        info(context, "Executing file source component");
        
        // Get file configuration parameters
        String filepath = getParameter("filepath", String.class);
        String delimiter = getParameter("delimiter", String.class);
        String encoding = getParameter("encoding", String.class);
        
        info(context, "Reading file: " + filepath);
        info(context, "Using delimiter: '" + delimiter + "', encoding: " + encoding);
        
        Path path = Paths.get(filepath);
        
        if (!Files.exists(path)) {
            error(context, "File does not exist: " + filepath);
            throw new RuntimeException("File not found: " + filepath);
        }
        
        int lineCount = 0;
        int rowCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            String[] headers = null;
            
            // Read the first line as headers if it's a CSV/TSV file
            if ((line = reader.readLine()) != null && delimiter != null) {
                headers = line.split(delimiter);
                lineCount++;
                debug(context, "Found headers: " + String.join(", ", headers));
            }
            
            // Process the remaining lines
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                if (line.trim().isEmpty()) {
                    debug(context, "Skipping empty line: " + lineCount);
                    continue;
                }
                
                Row row = createRow(line, headers, delimiter, lineCount);
                rowCount++;
                
                if (rowCount % 1000 == 0) {
                    info(context, "Processed " + rowCount + " rows");
                }
                
                debug(context, "Sending row: " + row);
                sendRow(row);
            }
            
            info(context, "Completed reading file. Total lines: " + lineCount + ", rows processed: " + rowCount);
            
            // Send terminator row to signal end of data
            info(context, "Sending terminator row");
            sendRow(Row.createTerminator());
            
        } catch (Exception e) {
            error(context, "Error reading file: " + filepath, e);
            throw e;
        }
    }
    
    private Row createRow(String line, String[] headers, String delimiter, int lineNumber) {
        Row row = new Row();
        row.setId(UUID.randomUUID().toString());
        
        HashMap<String, Object> data = new HashMap<>();
        
        // Add the line number
        data.put("_line", lineNumber);
        
        // If we have headers and delimiter, parse as structured data
        if (headers != null && delimiter != null) {
            String[] values = line.split(delimiter, -1); // -1 to include empty trailing fields
            
            // Map each value to its corresponding header
            for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                data.put(headers[i], values[i]);
            }
        } else {
            // Otherwise just store the raw line
            data.put("line", line);
        }
        
        row.setData(data);
        return row;
    }
    
    private <T> T getParameter(String name, Class<T> type) {
        return (T) getParameters().stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(p -> p.getValue())
            .orElse(null);
    }
} 