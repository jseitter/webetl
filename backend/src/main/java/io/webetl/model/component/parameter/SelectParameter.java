package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;
import java.util.ArrayList;

/**
 * Parameter type for dropdown selections with predefined options.
 */
@SuperBuilder
@JsonTypeName("select")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SelectParameter extends Parameter<String> {
    private List<String> options;
    private String defaultValue;

    public SelectParameter() {
        this.options = new ArrayList<>();
    }

    public SelectParameter(String name, String label, String description, boolean required, 
                          List<String> options, String defaultValue) {
        super(name, label, description, required);
        this.options = options;
        this.defaultValue = defaultValue;
        
        // Initialize with default value if provided
        if (defaultValue != null && !defaultValue.isEmpty()) {
            this.setValue(defaultValue);
        } else if (options != null && !options.isEmpty()) {
            // Set first option as default if no default provided
            this.setValue(options.get(0));
        }
    }

    public List<String> getOptions() { 
        return options; 
    }
    
    public void setOptions(List<String> options) { 
        this.options = options; 
    }
    
    public String getDefaultValue() { 
        return defaultValue; 
    }
    
    public void setDefaultValue(String defaultValue) { 
        this.defaultValue = defaultValue;
        if (defaultValue != null && !defaultValue.isEmpty()) {
            this.setValue(defaultValue);
        }
    }
} 