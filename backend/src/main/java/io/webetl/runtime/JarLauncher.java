package io.webetl.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * JarLauncher is a simple entry point for executing ETL flow jars.
 * It handles the bootstrapping process for loading the jar and its dependencies.
 */
public class JarLauncher {
    private static boolean verbose = false;
    
    // Replace SLF4J logger with simple built-in logging
    private static void log(String level, String message) {
        System.out.println("[" + level + "] [JarLauncher] " + message);
    }
    
    private static void info(String message) {
        log("INFO", message);
    }
    
    private static void debug(String message) {
        if (verbose) {
            log("DEBUG", message);
        }
    }
    
    private static void warn(String message) {
        log("WARN", message);
    }
    
    private static void warn(String message, Exception e) {
        log("WARN", message);
        e.printStackTrace();
    }

    /**
     * Main entry point for the jar launcher.
     * 
     * @param args command line arguments - first argument must be path to the flow jar
     */
    public static void main(String[] args) {
        log("INFO", "WebETL flow standalone jar starting");
        // Check for verbose flag
        for (String arg : args) {
            if (arg.equals("--verbose") || arg.equals("-v")) {
                verbose = true;
                debug("Verbose mode enabled");
            }
        }
        
        try {
            // Determine the JAR file path
            String jarPath = findCurrentJarPath();
            if (jarPath == null) {
                error("Unable to determine JAR path");
                System.exit(1);
            }
            
            info("Launching JAR: " + jarPath);
            runJar(jarPath, args);
        } catch (Exception e) {
            error("Error executing flow: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String getJarPath() {
        try {
            return new File(JarLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getPath();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Finds the path of the current jar file.
     * 
     * @return path to the current jar or null if not running from a jar
     */
    private static String findCurrentJarPath() {
        try {
            URL url = JarLauncher.class.getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                String path = url.getPath();
                if (path.endsWith(".jar")) {
                    // URL decode to handle spaces and special characters
                    return java.net.URLDecoder.decode(path, "UTF-8");
                }
            }
        } catch (Exception e) {
            warn("Could not determine current jar path", e);
        }
        return null;
    }
    
    /**
     * Runs a jar using the FlowRunner.
     * 
     * @param jarPath path to the jar file
     * @param args command line arguments
     */
    private static void runJar(String jarPath, String[] args) {
        Path jarPathObj = Paths.get(jarPath);
        
        try {
            // Make sure jar exists
            if (!Files.exists(jarPathObj)) {
                error("Jar file does not exist: " + jarPath);
                System.exit(1);
            }
            
            debug("Checking JAR manifest");
            
            // Extract flow class from jar manifest
            String flowClass = getFlowClass(jarPathObj);
            if (flowClass != null) {
                info("Found Flow-Class in manifest: " + flowClass);
                runWithFlowClass(jarPathObj, flowClass);
            } else {
                // Try to find a class that extends CompiledFlow
                info("No Flow-Class found in manifest, using default execution mode");
                runWithDirectExecution(jarPathObj);
            }
        } catch (Exception e) {
            error("Error launching jar: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Runs a jar using a specific flow class.
     * 
     * @param jarPath path to the jar file
     * @param flowClassName the flow class to run
     * @throws Exception if anything goes wrong
     */
    private static void runWithFlowClass(Path jarPath, String flowClassName) throws Exception {
        log("INFO", "Running with flow class: " + flowClassName);
        // Create a new class loader for the jar and its dependencies
        log("INFO", "Creating new class loader");
        JarClassLoader loader = new JarClassLoader(ClassLoader.getSystemClassLoader());  // Use system classloader as parent for JRE access
        
        try {
            // Enable verbose mode if requested
            loader.setVerbose(verbose);
            
            // Set the context class loader before loading any classes
            Thread.currentThread().setContextClassLoader(loader);
            debug("Set thread context classloader to JarClassLoader");
            
            // Load the jar
            info("Loading JAR and dependencies");
            loader.loadJar(jarPath);
            
            // Print the classpath for debugging
            if (verbose) {
                loader.printClasspath();
            }
            
            // Pre-load SLF4J classes from parent classloader to avoid conflicts
            try {
                Class.forName("org.slf4j.LoggerFactory", true, ClassLoader.getSystemClassLoader());
                debug("Pre-loaded SLF4J LoggerFactory from system classloader");
            } catch (ClassNotFoundException e) {
                warn("Could not pre-load SLF4J LoggerFactory: " + e.getMessage());
            }
            
            // Load and invoke the flow class
            debug("Loading flow class: " + flowClassName);
            Class<?> flowClass = loader.loadClass(flowClassName);
            debug("Successfully loaded flow class");
            
            // Print class information for debugging
            debug("Class name: " + flowClass.getName());
            debug("Class loader: " + flowClass.getClassLoader().getClass().getName());
            debug("Superclass: " + flowClass.getSuperclass().getName());
            debug("Interfaces: " + Arrays.toString(flowClass.getInterfaces()));
            
            // Print all methods for debugging
            debug("Available methods:");
            for (Method m : flowClass.getMethods()) {
                debug("  " + m.getName() + "(" + Arrays.toString(m.getParameterTypes()) + ")");
            }
            
            info("Instantiating flow class");
            Object flowInstance = flowClass.getDeclaredConstructor().newInstance();
            debug("Successfully instantiated flow class");
            
            // Find and invoke the execute method using reflection with class name matching
            debug("Looking for execute method with parameter type: " + ExecutionContext.class.getName());
            
            // Find the execute method by name and parameter type name
            Method executeMethod = null;
            for (Method method : flowClass.getMethods()) {
                if ("execute".equals(method.getName()) && 
                    method.getParameterCount() == 1 && 
                    "io.webetl.runtime.ExecutionContext".equals(method.getParameterTypes()[0].getName())) {
                    executeMethod = method;
                    debug("Found execute method: " + executeMethod);
                    break;
                }
            }
            
            if (executeMethod == null) {
                throw new NoSuchMethodException(flowClassName + ".execute(io.webetl.runtime.ExecutionContext)");
            }
            
            // Create an ExecutionContext using the same classloader that loaded the flow class
            debug("Creating ExecutionContext using flow class's classloader");
            Class<?> contextClass = loader.loadClass("io.webetl.runtime.ExecutionContext");
            Object context = contextClass.getDeclaredConstructor().newInstance();
            debug("Successfully created ExecutionContext instance");
            
            info("Executing flow class: " + flowClassName);
            executeMethod.invoke(flowInstance, context);
            info("Flow execution completed successfully");
        } finally {
            loader.close();
        }
    }
    
    /**
     * Gets the Flow-Class attribute from a jar's manifest.
     * 
     * @param jarPath path to the jar file
     * @return the flow class or null if not specified
     * @throws IOException if the jar cannot be read
     */
    public static String getFlowClass(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            if (jarFile.getManifest() == null) {
                debug("No manifest found in JAR");
                return null;
            }
            String flowClass = jarFile.getManifest().getMainAttributes().getValue("Flow-Class");
            debug("Flow-Class from manifest: " + flowClass);
            return flowClass;
        }
    }
    
    /**
     * Gets the Main-Class attribute from a jar's manifest.
     * 
     * @param jarPath path to the jar file
     * @return the main class or null if not specified
     * @throws IOException if the jar cannot be read
     */
    private static String getMainClass(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            if (jarFile.getManifest() == null) {
                return null;
            }
            return jarFile.getManifest().getMainAttributes().getValue("Main-Class");
        }
    }
    
    /**
     * Simple direct execution of a flow JAR.
     * 
     * @param jarPath path to the jar file
     * @throws Exception if anything goes wrong
     */
    private static void runWithDirectExecution(Path jarPath) throws Exception {
        // Use JarClassLoader to ensure all dependencies are loaded
        try (JarClassLoader loader = new JarClassLoader(JarLauncher.class.getClassLoader())) {
            // Enable verbose mode if requested
            loader.setVerbose(verbose);
            
            // Load the jar
            info("Loading JAR and dependencies");
            loader.loadJar(jarPath);
            
            // Set the context class loader
            Thread.currentThread().setContextClassLoader(loader);
            
            // Try to find a class that extends CompiledFlow
            String flowClassName = null;
            
            // Get the flow class name from the manifest
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                flowClassName = jar.getManifest().getMainAttributes().getValue("Flow-Class");
                if (flowClassName == null) {
                    throw new IllegalArgumentException("No Flow-Class defined in JAR manifest");
                }
            }
            
            // Load and instantiate the flow class
            debug("Loading flow class: " + flowClassName);
            Class<?> flowClass = loader.loadClass(flowClassName);
            info("Instantiating flow class");
            Object flowInstance = flowClass.getDeclaredConstructor().newInstance();
            
            // Create an execution context
            ExecutionContext context = new ExecutionContext();
            
            // Find and invoke the execute method
            Method[] methods = flowClass.getMethods();
            Method executeMethod = null;
            for (Method method : methods) {
                if ("execute".equals(method.getName()) && 
                    method.getParameterCount() == 1 && 
                    "io.webetl.runtime.ExecutionContext".equals(method.getParameterTypes()[0].getName())) {
                    executeMethod = method;
                    break;
                }
            }
            
            if (executeMethod != null) {
                info("Executing flow class: " + flowClassName);
                executeMethod.invoke(flowInstance, context);
                info("Flow execution completed successfully");
            } else {
                throw new NoSuchMethodException("execute(io.webetl.runtime.ExecutionContext)");
            }
        }
    }
    
    private static void error(String message) {
        System.err.println("[ERROR] [JarLauncher] " + message);
    }
} 