package io.webetl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to define the metadata for an ETL component.
 * It is used to fill the Palette in the UI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ETLComponentDefinition {
    /**
     * Unique identifier for this component type.
     * This ID will be used in the JSON serialization and component instantiation.
     */
    String id();
    String label();
    String description();
    String icon();
    String backgroundColor() default "#f0f7ff";
    boolean supportsControlFlow() default false;
    String[] inputTypes() default {};
    String[] outputTypes() default {};
    String implementationClass() default "";  // Will be auto-filled by scanner
} 