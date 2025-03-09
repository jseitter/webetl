package io.webetl.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying a Maven dependency.
 * Used within {@link ComponentDependencies} to declare 
 * dependencies required by a component.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})  // Only allowed within ComponentDependencies
public @interface Dependency {
    /**
     * The Maven group ID of the dependency.
     * Example: "org.postgresql"
     */
    String groupId();
    
    /**
     * The Maven artifact ID of the dependency.
     * Example: "postgresql"
     */
    String artifactId();
    
    /**
     * The version of the dependency.
     * Example: "42.6.0"
     */
    String version();
    
    /**
     * Whether this dependency is optional.
     * Optional dependencies are only included if specifically requested.
     */
    boolean optional() default false;
} 