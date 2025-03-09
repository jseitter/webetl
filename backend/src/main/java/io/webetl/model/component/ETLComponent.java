package io.webetl.model.component;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.webetl.model.component.parameter.Parameter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import lombok.Data;
import io.webetl.runtime.ExecutionContext;

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
public abstract class ETLComponent implements ExecutableComponent {
    private String id;
    private String label;
    private String description;
    private String icon;
    private String backgroundColor;
    private boolean supportsControlFlow;
    private String implementationClass;  // Store just the implementation class
    @JsonProperty("parameters")
    private List<Parameter<?>> parameters = new ArrayList<>();

    // Default constructor for Jackson
    public ETLComponent() {
    }

    @JsonIgnore
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public ETLComponent(String id, String label, String description, String icon, String backgroundColor, List<Parameter<?>> parameters) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.backgroundColor = backgroundColor;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public void setParameter(String name, Object value) {
        @SuppressWarnings("unchecked")
        Parameter<Object> param = (Parameter<Object>) parameters.stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Parameter not found: " + name));
        param.setValue(value);
    }

    public Object getParameter(String name) {
        return parameters.stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(Parameter::getValue)
            .orElse(null);
    }

    public Map<String, Object> getParameterValues() {
        Map<String, Object> values = new HashMap<>();
        parameters.forEach(p -> values.put(p.getName(), p.getValue()));
        return values;
    }

    @Override
    public void execute(ExecutionContext context) throws Exception {
        // Register this component's ID with the context for logging
        String previousComponent = context.getCurrentComponentId();
        
        try {
            if (this.id != null) {
                context.setCurrentComponentId(this.id);
            }
            
            // Log component execution start
            context.info("Starting execution");
            
            // Perform component-specific execution
            executeComponent(context);
            
            // Log successful completion
            context.info("Execution completed successfully");
        } catch (Exception e) {
            // Log error
            context.error("Execution failed", e);
            throw e;
        } finally {
            // Restore previous component context
            if (previousComponent != null) {
                context.setCurrentComponentId(previousComponent);
            } else {
                context.clearCurrentComponentId();
            }
        }
    }
    
    /**
     * Component-specific execution logic. Subclasses should override this method
     * instead of the execute method to benefit from the standard logging.
     *
     * @param context the execution context
     * @throws Exception if an error occurs during execution
     */
    protected abstract void executeComponent(ExecutionContext context) throws Exception;
    
    /**
     * Helper method to log at DEBUG level.
     *
     * @param context the execution context
     * @param message the message to log
     */
    protected void debug(ExecutionContext context, String message) {
        context.debug(message);
    }
    
    /**
     * Helper method to log at INFO level.
     *
     * @param context the execution context
     * @param message the message to log
     */
    protected void info(ExecutionContext context, String message) {
        context.info(message);
    }
    
    /**
     * Helper method to log at WARN level.
     *
     * @param context the execution context
     * @param message the message to log
     */
    protected void warn(ExecutionContext context, String message) {
        context.warn(message);
    }
    
    /**
     * Helper method to log at ERROR level.
     *
     * @param context the execution context
     * @param message the message to log
     */
    protected void error(ExecutionContext context, String message) {
        context.error(message);
    }
    
    /**
     * Helper method to log an exception at ERROR level.
     *
     * @param context the execution context
     * @param message the message to log
     * @param e the exception
     */
    protected void error(ExecutionContext context, String message, Throwable e) {
        context.error(message, e);
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

    public String getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(String implementationClass) {
        this.implementationClass = implementationClass;
    }

    public boolean isSupportsControlFlow() {
        return supportsControlFlow;
    }

    public void setSupportsControlFlow(boolean supportsControlFlow) {
        this.supportsControlFlow = supportsControlFlow;
    }

} 