package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;

import java.util.HashMap;
import java.util.UUID;

@ETLComponentDefinition(
    id = "database-source",
    label = "Database Source",
    description = "Reads data from a database",
    icon = "DatabaseIcon",
    backgroundColor = "#e3f2fd"
)
public class DatabaseSourceComponent extends SourceComponent {
    
    public DatabaseSourceComponent() {
        // Add parameters
        getParameters().add(StringParameter.builder()
            .name("url")
            .label("Database URL")
            .required(true)
            .build());
            
        getParameters().add(StringParameter.builder()
            .name("username")
            .label("Username")
            .required(true)
            .build());
            
        getParameters().add(SecretParameter.builder()
            .name("password")
            .label("Password")
            .required(true)
            .build());
            
        getParameters().add(StringParameter.builder()
            .name("query")
            .label("SQL Query")
            .required(true)
            .build());
            
    }

    @Override
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Log the start of execution
        info(context, "Starting database source component");
        
        // Get parameters
        String query = getParameter("query", String.class);
        String connectionString = getParameter("connectionString", String.class);
        
        info(context, "Executing query: " + query);
        info(context, "Using connection: " + connectionString);
        
        try {
            // Simulate database query execution
            info(context, "Simulating database query execution");
            
            // Create sample data for testing
            for (int i = 0; i < 5; i++) {
                Row row = new Row();
                row.setId(UUID.randomUUID().toString());
                
                HashMap<String, Object> data = new HashMap<>();
                data.put("id", i);
                data.put("name", "Test " + i);
                data.put("value", Math.random() * 100);
                
                row.setData(data);
                
                debug(context, "Sending row to output: " + row);
                super.sendRow(row);
                
                // Simulate delay
                Thread.sleep(500);
            }
            
            // Send terminator row
            info(context, "Query execution complete, sending terminator");
            super.sendRow(Row.createTerminator());
            
        } catch (InterruptedException e) {
            error(context, "Database source was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            error(context, "Error executing database query", e);
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