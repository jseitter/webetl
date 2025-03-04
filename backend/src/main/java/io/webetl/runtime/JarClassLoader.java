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
     * Override to log class loading attempts if verbose is enabled
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (verbose) {
            log("Finding class: " + name);
        }
        try {
            Class<?> c = super.findClass(name);
            if (verbose) {
                log("Found class: " + name + " from " + c.getProtectionDomain().getCodeSource().getLocation());
            }
            return c;
        } catch (ClassNotFoundException e) {
            if (verbose) {
                log("Class not found: " + name);
            }
            throw e;
        }
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
} 