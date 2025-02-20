package io.webetl.service;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.ETLComponent;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to scan the classpath for components that are annotated with @ETLComponentDefinition.
 * It is used to get the available components for the ETL pipeline.
 */
@Service
public class ComponentScanner {
    private final List<Class<? extends ETLComponent>> componentClasses = new ArrayList<>();

    @PostConstruct
    public void scanComponents() {
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ETLComponentDefinition.class));

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

    public List<ETLComponent> createComponentInstances() {
        List<ETLComponent> components = new ArrayList<>();
        for (Class<? extends ETLComponent> componentClass : componentClasses) {
            ETLComponentDefinition def = componentClass.getAnnotation(ETLComponentDefinition.class);
            try {
                ETLComponent component = componentClass.getDeclaredConstructor().newInstance();
                component.setId(def.id());
                component.setLabel(def.label());
                component.setDescription(def.description());
                component.setIcon(def.icon());
                component.setBackgroundColor(def.backgroundColor());
                components.add(component);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate component", e);
            }
        }
        return components;
    }
} 