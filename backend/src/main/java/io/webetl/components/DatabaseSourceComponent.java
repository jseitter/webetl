package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;

@ETLComponentDefinition(
    id = "db-source",
    label = "Database Source",
    description = "Reads data from databases",
    icon = "DatabaseIcon",
    backgroundColor = "#f0f7ff"
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
            
        getParameters().add(SQLParameter.builder()
            .name("query")
            .label("SQL Query")
            .required(true)
            .build());
    }

    @Override
    public void execute(ExecutionContext context) {
        // Implementation for database reading
        System.out.println("Executing database source component");
        for(int i=0; i<5; i++) {
            Row row = new Row();
            System.out.println("producing Row: " + i);
            super.sendRow(row);
        }
        System.out.println("finished producing rows");
        // Send terminator after all rows are processed
        sendRow(Row.createTerminator());
        context.setTerminator(true);
        System.out.println("sent terminator");
    }
} 