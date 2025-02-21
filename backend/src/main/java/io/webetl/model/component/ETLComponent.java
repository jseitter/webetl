package io.webetl.model.component;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.webetl.model.component.parameter.Parameter;
import java.util.List;
import java.util.ArrayList;
import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SourceComponent.class, name = "source"),
    @JsonSubTypes.Type(value = TransformComponent.class, name = "transform"),
    @JsonSubTypes.Type(value = DestinationComponent.class, name = "destination")
})
/**
 * ETLComponent is the base class for all components in the ETL pipeline.
 * It defines the common properties and methods for all components to be used in the UI
 * and to be serialized to JSON.
 */
@Data
public abstract class ETLComponent {
    private String id;
    private String label;
    private String description;
    private String icon;
    private String backgroundColor;
    private boolean supportsControlFlow;
    @JsonProperty("parameters")
    private List<Parameter<?>> parameters = new ArrayList<>();

    // Default constructor for Jackson
    public ETLComponent() {
    }

    public ETLComponent(String id, String label, String description, String icon, String backgroundColor, List<Parameter<?>> parameters) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.backgroundColor = backgroundColor;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public List<Parameter<?>> getParameters() { return parameters; }
    public void setParameters(List<Parameter<?>> parameters) { this.parameters = parameters; }


} 