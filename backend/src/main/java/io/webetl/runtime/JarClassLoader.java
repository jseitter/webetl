package io.webetl.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Custom ClassLoader that can load classes from jars embedded within another jar.
 * This supports the jar-in-jar model where dependencies are packaged inside the flow jar.
 */
public class JarClassLoader extends URLClassLoader {
    private static final String LIB_DIR = "META-INF/lib/";
    private final Path tempDir;
    private boolean verbose = false;

    /**
     * Creates a new JarClassLoader with the parent ClassLoader.
     *
     * @param parent the parent class loader
     */
    public JarClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        this.tempDir = Files.createTempDirectory("jar-loader");
        tempDir.toFile().deleteOnExit();
    }
    
    /**
     * Enable verbose logging of class loading
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Loads a jar file and all its embedded jars.
     *
     * @param jarPath path to the main jar file
     * @throws IOException if an I/O error occurs
     */
    public void loadJar(Path jarPath) throws IOException {
        log("Loading JAR: " + jarPath);
        
        // Add the main jar to the classpath
        addURL(jarPath.toUri().toURL());
        
        // Extract and load embedded jars
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            List<URL> embeddedJarUrls = extractEmbeddedJars(jarFile);
            log("Found " + embeddedJarUrls.size() + " embedded JARs");
            
            for (URL url : embeddedJarUrls) {
                log("Adding URL to classpath: " + url);
                addURL(url);
            }
        }
    }

    /**
     * Extracts embedded jars from a jar file and returns their URLs.
     *
     * @param jarFile the jar file to extract from
     * @return list of URLs for the extracted jars
     * @throws IOException if an I/O error occurs
     */
    private List<URL> extractEmbeddedJars(JarFile jarFile) throws IOException {
        List<URL> jarUrls = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(LIB_DIR) && entry.getName().endsWith(".jar")) {
                // Extract the embedded jar to a temp file
                Path extractedJar = extractJarEntry(jarFile, entry);
                jarUrls.add(extractedJar.toUri().toURL());
                
                // For nested JARs, we might want to extract their dependencies too
                if (entry.getName().contains("runtime.jar")) {
                    try (JarFile nestedJar = new JarFile(extractedJar.toFile())) {
                        jarUrls.addAll(extractEmbeddedJars(nestedJar));
                    } catch (Exception e) {
                        log("Warning: Failed to process nested JAR: " + e.getMessage());
                    }
                }
            }
        }

        return jarUrls;
    }

    /**
     * Extracts a jar entry to a temporary file.
     *
     * @param jarFile the jar file containing the entry
     * @param entry the jar entry to extract
     * @return path to the extracted file
     * @throws IOException if an I/O error occurs
     */
    private Path extractJarEntry(JarFile jarFile, JarEntry entry) throws IOException {
        String fileName = new File(entry.getName()).getName();
        Path extractedPath = tempDir.resolve(fileName);
        
        log("Extracting: " + entry.getName() + " to " + extractedPath);
        
        try (InputStream in = jarFile.getInputStream(entry)) {
            Files.copy(in, extractedPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        extractedPath.toFile().deleteOnExit();
        return extractedPath;
    }
    
    /**
     * Override to change the class loading order - try our classloader first, then parent
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // First check if the class has already been loaded
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        // First try to find the class in our classloader
        try {
            if (verbose) {
                log("Attempting to load from our classloader: " + name);
            }
            // Use findLoadedClass to check if we've already loaded this class
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                if (verbose) {
                    log("Found already loaded class in our classloader: " + name);
                }
                return c;
            }

            // Try to find the class in our JARs
            String classPath = name.replace('.', '/') + ".class";
            for (URL url : getURLs()) {
                if (url.getProtocol().equals("file")) {
                    try {
                        JarFile jarFile = new JarFile(new File(url.toURI()));
                        JarEntry entry = jarFile.getJarEntry(classPath);
                        if (entry != null) {
                            if (verbose) {
                                log("Found class file in JAR: " + url);
                            }
                            // Load the class from the JAR
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                byte[] classData = is.readAllBytes();
                                c = defineClass(name, classData, 0, classData.length);
                                if (verbose) {
                                    log("Successfully defined class: " + name + " from " + url);
                                }
                                return c;
                            }
                        }
                        jarFile.close();
                    } catch (Exception e) {
                        if (verbose) {
                            log("Error checking JAR " + url + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (verbose) {
                log("Error loading from our classloader: " + e.getMessage());
            }
        }

        // If we get here, try the parent classloader
        ClassLoader parent = getParent();
        if (parent != null) {
            try {
                if (verbose) {
                    log("Attempting to load from parent classloader: " + name);
                    log("Parent classloader: " + parent.getClass().getName());
                }
                Class<?> c = parent.loadClass(name);
                if (verbose) {
                    // Safely get the class location
                    String location = "unknown";
                    try {
                        if (c.getProtectionDomain() != null && 
                            c.getProtectionDomain().getCodeSource() != null && 
                            c.getProtectionDomain().getCodeSource().getLocation() != null) {
                            location = c.getProtectionDomain().getCodeSource().getLocation().toString();
                        }
                    } catch (Exception e) {
                        // Ignore any errors getting the location
                    }
                    log("Found class in parent classloader: " + name + " from " + location);
                }
                return c;
            } catch (ClassNotFoundException e) {
                if (verbose) {
                    log("Class not found in parent classloader: " + name);
                }
            }
        }

        // If we get here, the class wasn't found anywhere
        if (verbose) {
            log("Class not found in any classloader: " + name);
            // Print the classpath for debugging
            log("Current classpath:");
            for (URL url : getURLs()) {
                log("  - " + url);
            }
            // Try to find the class file in the JARs
            log("Searching for class file in JARs:");
            for (URL url : getURLs()) {
                if (url.getProtocol().equals("file")) {
                    try {
                        JarFile jarFile = new JarFile(new File(url.toURI()));
                        String classPath = name.replace('.', '/') + ".class";
                        if (jarFile.getEntry(classPath) != null) {
                            log("  Found in JAR: " + url);
                        }
                        jarFile.close();
                    } catch (Exception ex) {
                        log("  Error checking JAR " + url + ": " + ex.getMessage());
                    }
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Override to log resource loading attempts
     */
    @Override
    public URL findResource(String name) {
        if (verbose) {
            log("Finding resource: " + name);
        }
        URL url = super.findResource(name);
        if (verbose) {
            if (url != null) {
                log("Found resource: " + name + " at " + url);
            } else {
                log("Resource not found: " + name);
            }
        }
        return url;
    }

    /**
     * Override to log resource loading attempts
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (verbose) {
            log("Finding resources: " + name);
        }
        Enumeration<URL> urls = super.findResources(name);
        if (verbose) {
            if (urls.hasMoreElements()) {
                log("Found resources: " + name);
                while (urls.hasMoreElements()) {
                    log("  - " + urls.nextElement());
                }
            } else {
                log("No resources found: " + name);
            }
        }
        return urls;
    }

    /**
     * Simple logging method for the class loader
     */
    private void log(String message) {
        if (verbose) {
            System.out.println("[JarClassLoader] " + message);
        }
    }

    /**
     * Clean up temporary resources.
     */
    @Override
    public void close() throws IOException {
        super.close();
        // Additional cleanup can be added here if needed
    }

    /**
     * Prints the classpath of this JarClassLoader.
     * This is useful for debugging class loading issues.
     */
    public void printClasspath() {
        log("JarClassLoader classpath:");
        for (URL url : getURLs()) {
            log("  - " + url.toString());
        }
    }
} 