package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.parameter.StringParameter;
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
    public void execute(ExecutionContext context) {
        // Implementation for filtering
    }
} 