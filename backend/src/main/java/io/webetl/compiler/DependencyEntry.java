package io.webetl.compiler;

import java.util.Objects;

/**
 * Represents a Maven dependency with group ID, artifact ID, and version.
 */
public class DependencyEntry {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final boolean optional;
    
    /**
     * Creates a dependency entry from a Dependency annotation.
     */
    public static DependencyEntry fromAnnotation(Dependency dependency) {
        return new DependencyEntry(
            dependency.groupId(),
            dependency.artifactId(),
            dependency.version(),
            dependency.optional()
        );
    }
    
    public DependencyEntry(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, false);
    }
    
    public DependencyEntry(String groupId, String artifactId, String version, boolean optional) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.optional = optional;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * Gets the pattern to match this dependency in a JAR filename.
     * For example, "postgresql-42.6.0"
     */
    public String getPattern() {
        return artifactId + "-" + version;
    }
    
    /**
     * Gets the fully qualified name of the dependency.
     * For example, "org.postgresql:postgresql:42.6.0"
     */
    public String getFullName() {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    /**
     * Gets the path to the dependency in a Maven repository.
     * For example, "org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar"
     */
    public String getMavenPath() {
        return groupId.replace('.', '/') + "/" + 
               artifactId + "/" + 
               version + "/" + 
               artifactId + "-" + version + ".jar";
    }
    
    /**
     * Gets the JAR filename for this dependency.
     * For example, "postgresql-42.6.0.jar"
     */
    public String getJarFilename() {
        return artifactId + "-" + version + ".jar";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyEntry that = (DependencyEntry) o;
        return Objects.equals(groupId, that.groupId) &&
               Objects.equals(artifactId, that.artifactId) &&
               Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }
    
    @Override
    public String toString() {
        return getFullName() + (optional ? " (optional)" : "");
    }
} 