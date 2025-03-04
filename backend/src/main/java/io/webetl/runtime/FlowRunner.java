package io.webetl.runtime;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FlowRunner loads and executes CompiledFlow instances from jar files.
 * It handles the jar-in-jar loading mechanism to support self-contained flow jars.
 */
public class FlowRunner implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FlowRunner.class);
    private final JarClassLoader classLoader;
    
    /**
     * Creates a new FlowRunner with a custom class loader.
     */
    public FlowRunner() throws IOException {
        this.classLoader = new JarClassLoader(getClass().getClassLoader());
    }
    
    /**
     * Loads a flow jar and executes it.
     *
     * @param jarPath path to the flow jar
     * @throws Exception if loading or execution fails
     */
    public void runFlow(Path jarPath) throws Exception {
        log.info("Loading flow jar: {}", jarPath);
        
        // Load the jar and its embedded dependencies
        classLoader.loadJar(jarPath);
        
        // Set the context class loader for this thread
        Thread.currentThread().setContextClassLoader(classLoader);
        
        // Find the flow class (either from manifest or by scanning)
        String flowClassName = JarLauncher.getFlowClass(jarPath);
        Class<?> flowClass;
        
        if (flowClassName != null) {
            log.info("Using flow class from manifest: {}", flowClassName);
            flowClass = classLoader.loadClass(flowClassName);
        } else {
            // Find the flow class by scanning
            flowClass = findFlowClass(classLoader);
            if (flowClass == null) {
                throw new IllegalArgumentException("No CompiledFlow implementation found in jar: " + jarPath);
            }
            log.info("Found flow class by scanning: {}", flowClass.getName());
        }
        
        // Create an execution context
        ExecutionContext context = createExecutionContext();
        
        // Instantiate and execute the flow
        log.info("Instantiating flow class: {}", flowClass.getName());
        Object flowInstance = flowClass.getDeclaredConstructor().newInstance();
        Method executeMethod = flowClass.getMethod("execute", ExecutionContext.class);
        
        log.info("Executing flow...");
        executeMethod.invoke(flowInstance, context);
        log.info("Flow execution completed successfully");
    }
    
    /**
     * Creates a new execution context for the flow.
     */
    private ExecutionContext createExecutionContext() {
        return new ExecutionContext();
    }
    
    /**
     * Finds the class that extends CompiledFlow in the loaded jar.
     */
    private Class<?> findFlowClass(ClassLoader loader) throws IOException {
        try {
            // First load the CompiledFlow class through our class loader
            Class<?> compiledFlowClass = loader.loadClass("io.webetl.compiler.CompiledFlow");
            
            // Try to load the expected flow class (assuming naming convention from FlowCompilerNG)
            try {
                Class<?> flowClass = loader.loadClass("io.webetl.runtime.GeneratedFlow");
                if (compiledFlowClass.isAssignableFrom(flowClass)) {
                    return flowClass;
                }
            } catch (ClassNotFoundException e) {
                // Expected class not found, try scanning for other classes
                log.warn("Could not find standard GeneratedFlow class, will try to scan for a class extending CompiledFlow");
            }
            
            // Try other common naming patterns
            String[] possibleClassNames = {
                "io.webetl.runtime.Flow",
                "io.webetl.generated.GeneratedFlow",
                "io.webetl.generated.Flow"
            };
            
            for (String className : possibleClassNames) {
                try {
                    Class<?> flowClass = loader.loadClass(className);
                    if (compiledFlowClass.isAssignableFrom(flowClass)) {
                        return flowClass;
                    }
                } catch (ClassNotFoundException e) {
                    // Class not found, continue to next pattern
                }
            }
            
            // If standard class not found, try to scan for any class that extends CompiledFlow
            // This would require a more complex implementation to scan all loaded classes
            // For now, we'll rely on the standard naming conventions and manifest attribute
            return null;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load CompiledFlow class", e);
        }
    }
    
    @Override
    public void close() throws Exception {
        classLoader.close();
    }
    
    /**
     * Main method for running a flow from command line.
     * 
     * @param args command line arguments. First argument should be the path to the flow jar.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar runner.jar <flow-jar-path>");
            System.exit(1);
        }
        
        String jarPath = args[0];
        
        try (FlowRunner runner = new FlowRunner()) {
            System.out.println("Running flow from: " + jarPath);
            runner.runFlow(Paths.get(jarPath));
            System.out.println("Flow execution completed successfully");
        } catch (Exception e) {
            System.err.println("Error running flow: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 