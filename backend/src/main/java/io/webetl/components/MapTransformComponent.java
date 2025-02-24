package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.parameter.SQLParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;

@ETLComponentDefinition(
    id = "map-transform",
    label = "Map Transform",
    description = "Maps input data to a new schema using SQL expressions",
    icon = "MapIcon",
    backgroundColor = "#fff3e0"
)
public class MapTransformComponent extends TransformComponent {
    
    public MapTransformComponent() {
        getParameters().add(SQLParameter.builder()
            .name("mappingExpression")
            .label("Mapping Expression")
            .description("SQL expression to map input fields to output fields (e.g., SELECT field1 as newField1, field2 as newField2)")
            .required(true)
            .build());
    }

    @Override
    public void execute(ExecutionContext context) {
        // Implementation for mapping transformation
        System.out.println("Executing map transform component");
        Row row;
        try {
            System.out.println("waiting for Row");
            row = super.takeInputRow();
            System.out.println("passing Row: " + row + " to next component");
            sendRow(row);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
} 