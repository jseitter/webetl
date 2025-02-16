package io.webetl.model.component.parameter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonSubTypes.Type(value = SQLParameter.class, name = "sql")
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
    
    @JsonProperty("value")
    private T value;

    public Parameter() {}

    public Parameter(String name, String label, String description, boolean required) {
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
} 