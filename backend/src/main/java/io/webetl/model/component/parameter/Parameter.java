package io.webetl.model.component.parameter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "parameterType",
    include = JsonTypeInfo.As.PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StringParameter.class, name = "string"),
    @JsonSubTypes.Type(value = NumberParameter.class, name = "number"),
    @JsonSubTypes.Type(value = BooleanParameter.class, name = "boolean"),
    @JsonSubTypes.Type(value = SecretParameter.class, name = "secret"),
    @JsonSubTypes.Type(value = SQLParameter.class, name = "sql"),
    @JsonSubTypes.Type(value = SelectParameter.class, name = "select")
})
public abstract class Parameter<T> {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("label")
    private String label;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("required")
    private boolean required;
    
    @JsonProperty("defaultValue")
    private T defaultValue;
    
    @JsonProperty("value")
    private T value;
    
    @JsonProperty("displayName")
    private String displayName;
    
    protected Parameter() {}

    protected Parameter(String name, String label, String description, boolean required) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.required = required;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    
    public T getDefaultValue() { return defaultValue; }
    public void setDefaultValue(T defaultValue) { this.defaultValue = defaultValue; }
    
    public String getDisplayName() { return displayName != null ? displayName : label; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
} 