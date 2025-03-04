package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.parameter.SQLParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;
import java.util.HashMap;
import java.util.Map;

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
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Implementation for mapping transformation
        info(context, "Executing map transform component");
        
        String mappingExpression = getParameter("mappingExpression", String.class);
        info(context, "Using mapping expression: " + mappingExpression);
        
        try {
            while (true) {
                info(context, "Waiting for row to transform");
                Row row = super.takeInputRow();
                
                if (row.isTerminator()) {
                    info(context, "Received terminator row, ending map transform process");
                    sendRow(row); // Pass the terminator to the next component
                    break;
                }
                
                debug(context, "Transforming row: " + row);
                
                // Transform the row according to the mapping expression
                Row transformedRow = transformRow(row, mappingExpression);
                
                info(context, "Row transformed, forwarding to next component");
                sendRow(transformedRow);
            }
        } catch (InterruptedException e) {
            warn(context, "Map transform component was interrupted");
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            error(context, "Error in map transform component", e);
            throw e;
        }
    }
    
    private Row transformRow(Row row, String mappingExpression) {
        // TODO: Implement actual transformation based on mapping expression
        // This is a placeholder implementation
        Row transformedRow = new Row();
        transformedRow.setId(row.getId());
        
        // Just copy the data for now
        Map<String, Object> newData = new HashMap<>(row.getData());
        
        // Add a marker to show it was transformed
        newData.put("_transformed", true);
        
        transformedRow.setData(newData);
        return transformedRow;
    }
    
    private <T> T getParameter(String name, Class<T> type) {
        return (T) getParameters().stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(p -> p.getValue())
            .orElse(null);
    }
} 