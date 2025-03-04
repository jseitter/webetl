package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;

@ETLComponentDefinition(
    id = "filter",
    label = "Filter",
    description = "Filters data based on conditions",
    icon = "FilterIcon",
    backgroundColor = "#e3f2fd"
)
public class FilterComponent extends TransformComponent {
    public FilterComponent() {
        getParameters().add(StringParameter.builder()
            .name("condition")
            .label("Filter Condition")
            .required(true)
            .build());
    }

    @Override
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Implementation for filtering
        info(context, "Executing filter component");
        
        String condition = getParameter("condition", String.class);
        info(context, "Using filter condition: " + condition);
        
        try {
            while (true) {
                info(context, "Waiting for row to filter");
                Row row = super.takeInputRow();
                
                if (row.isTerminator()) {
                    info(context, "Received terminator row, ending filter process");
                    sendRow(row); // Pass the terminator to the next component
                    break;
                }
                
                debug(context, "Filtering row: " + row);
                
                // TODO: Implement actual filtering based on condition
                // For now, we're passing all rows
                boolean passes = evaluateCondition(row, condition);
                
                if (passes) {
                    info(context, "Row passed filter, forwarding to next component");
                    sendRow(row);
                } else {
                    debug(context, "Row filtered out, not forwarding");
                }
            }
        } catch (InterruptedException e) {
            warn(context, "Filter component was interrupted");
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            error(context, "Error in filter component", e);
            throw e;
        }
    }
    
    private boolean evaluateCondition(Row row, String condition) {
        // TODO: Implement actual condition evaluation
        // This is a placeholder that passes all rows
        return true;
    }
    
    private <T> T getParameter(String name, Class<T> type) {
        return (T) getParameters().stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(p -> p.getValue())
            .orElse(null);
    }
} 