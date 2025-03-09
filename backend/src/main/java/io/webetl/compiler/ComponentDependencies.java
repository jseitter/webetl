package io.webetl.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring dependencies required by a component.
 * This annotation should be placed on component classes to specify
 * which dependencies should be included when the component is used in a flow.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentDependencies {
    /**
     * The list of dependencies required by this component.
     */
    Dependency[] value();
} 