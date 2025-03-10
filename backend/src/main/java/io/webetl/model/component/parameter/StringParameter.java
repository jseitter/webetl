package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@SuperBuilder
@JsonTypeName("string")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StringParameter extends Parameter<String> {
    private int maxLength;
    private String pattern;

    public StringParameter() {}

    public StringParameter(String name, String label, String description, boolean required, 
                         int maxLength, String pattern) {
        super(name, label, description, required);
        this.maxLength = maxLength;
        this.pattern = pattern;
    }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
} 