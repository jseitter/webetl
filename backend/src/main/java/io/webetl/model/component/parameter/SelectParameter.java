package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    
    @JsonProperty("optionDisplayNames")
    private Map<String, String> optionDisplayNames;

    public SelectParameter() {
        this.options = new ArrayList<>();
        this.optionDisplayNames = new HashMap<>();
    }

    public SelectParameter(String name, String label, String description, boolean required, 
                          List<String> options, String defaultValue) {
        super(name, label, description, required);
        this.options = options;
        this.defaultValue = defaultValue;
        this.optionDisplayNames = new HashMap<>();
        
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
    
    /**
     * Sets a display name for a specific option value.
     * 
     * @param optionValue The option value
     * @param displayName The display name to show in the UI
     */
    public void setOptionDisplayName(String optionValue, String displayName) {
        if (optionDisplayNames == null) {
            optionDisplayNames = new HashMap<>();
        }
        optionDisplayNames.put(optionValue, displayName);
    }
    
    /**
     * Gets the display name for a specific option value.
     * 
     * @param optionValue The option value
     * @return The display name, or the option value itself if no display name is set
     */
    public String getOptionDisplayName(String optionValue) {
        if (optionDisplayNames != null && optionDisplayNames.containsKey(optionValue)) {
            return optionDisplayNames.get(optionValue);
        }
        return optionValue;
    }
    
    /**
     * Gets all option display names.
     * 
     * @return Map of option values to display names
     */
    public Map<String, String> getOptionDisplayNames() {
        return optionDisplayNames;
    }
    
    /**
     * Sets all option display names.
     * 
     * @param optionDisplayNames Map of option values to display names
     */
    public void setOptionDisplayNames(Map<String, String> optionDisplayNames) {
        this.optionDisplayNames = optionDisplayNames;
    }
} 