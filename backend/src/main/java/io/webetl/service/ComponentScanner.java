package io.webetl.service;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.ETLComponent;
import io.webetl.model.component.SourceComponent;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to scan the classpath for components that are annotated with @ETLComponentDefinition.
 * It is used to get the available components for the ETL pipeline.
 */
@Service
public class ComponentScanner {
    private final List<Class<? extends ETLComponent>> componentClasses = new ArrayList<>();

    private Map<String, Object> createComponentDefinition(Class<?> componentClass) {
        ETLComponentDefinition annotation = componentClass.getAnnotation(ETLComponentDefinition.class);
        Map<String, Object> definition = new HashMap<>();
        definition.put("id", annotation.id());
        definition.put("label", annotation.label());
        definition.put("description", annotation.description());
        definition.put("icon", annotation.icon());
        definition.put("backgroundColor", annotation.backgroundColor());
        definition.put("supportsControlFlow", annotation.supportsControlFlow());
        definition.put("implementationClass", componentClass.getName());
        return definition;
    }

    @PostConstruct
    public void scanComponents() {
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ETLComponentDefinition.class));
        /**
         * scan the classpath for components that are annotated with @ETLComponentDefinition
         * and add them to the componentClasses list
         */
        for (BeanDefinition bd : scanner.findCandidateComponents("io.webetl.components")) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (ETLComponent.class.isAssignableFrom(clazz)) {
                    componentClasses.add((Class<? extends ETLComponent>) clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load component class", e);
            }
        }
    }
    /**
     * create a list of ETLComponent instances
     * @return list of ETLComponent instances
     */
    public List<ETLComponent> createComponentInstances() {
        List<ETLComponent> components = new ArrayList<>();
        for (Class<? extends ETLComponent> componentClass : componentClasses) {
            ETLComponentDefinition def = componentClass.getAnnotation(ETLComponentDefinition.class);
            try {
                ETLComponent component = componentClass.getDeclaredConstructor().newInstance();
                Map<String, Object> definition = createComponentDefinition(componentClass);
                component.setId((String) definition.get("id"));
                component.setLabel((String) definition.get("label"));
                component.setDescription((String) definition.get("description"));
                component.setIcon((String) definition.get("icon"));
                component.setBackgroundColor((String) definition.get("backgroundColor"));
                component.setImplementationClass(componentClass.getName());
                
                // Control flow only for start/stop nodes and source components
                if (component instanceof SourceComponent || 
                    "start".equals(definition.get("id")) || 
                    "stop".equals(definition.get("id"))) {
                    component.setSupportsControlFlow(true);
                }
                
                components.add(component);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate component", e);
            }
        }
        return components;
    }
} 