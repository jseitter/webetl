package io.webetl.service;

import io.webetl.model.component.ETLComponent;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.ComponentCategory;
import io.webetl.model.component.DestinationComponent;
import io.webetl.model.component.parameter.Parameter;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.AllArgsConstructor;
/**
 * ComponentService is a service that provides the available components for the ETL pipeline.
 * It is used to fill the Palette in the UI.
 * 
 */
@Service
public class ComponentService {
    private final ComponentScanner componentScanner;

    public ComponentService(ComponentScanner componentScanner) {
        this.componentScanner = componentScanner;
    }
    
    public List<ComponentCategory> getAvailableComponents() {
        List<ETLComponent> components = componentScanner.createComponentInstances();
        
        Map<String, List<ETLComponent>> categorizedComponents = new HashMap<>();
        categorizedComponents.put("Source", new ArrayList<>());
        categorizedComponents.put("Transform", new ArrayList<>());
        categorizedComponents.put("Destination", new ArrayList<>());
        
        for (ETLComponent component : components) {
            if (component instanceof SourceComponent) {
                categorizedComponents.get("Source").add(component);
            } else if (component instanceof TransformComponent) {
                categorizedComponents.get("Transform").add(component);
            } else if (component instanceof DestinationComponent) {
                categorizedComponents.get("Destination").add(component);
            }
        }
        
        return categorizedComponents.entrySet().stream()
            .map(entry -> new ComponentCategory(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
}
