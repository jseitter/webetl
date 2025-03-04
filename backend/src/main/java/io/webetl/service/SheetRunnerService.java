package io.webetl.service;

import io.webetl.compiler.FlowCompilerNG;
import io.webetl.model.Sheet;
import io.webetl.runtime.FlowRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

/**
 * Service for running compiled ETL sheets.
 * This service handles the compilation and execution of flow sheets.
 */
@Service
@Slf4j
public class SheetRunnerService {
    private final FlowCompilerNG compiler;
    
    @Autowired
    public SheetRunnerService(FlowCompilerNG compiler) {
        this.compiler = compiler;
    }
    
    /**
     * Compiles and runs a sheet.
     *
     * @param sheet the sheet to run
     * @param verbose whether to log verbose compilation messages
     * @throws Exception if compilation or execution fails
     */
    public void runSheet(Sheet sheet, boolean verbose) throws Exception {
        log.info("Compiling sheet: {}", sheet.getName());
        
        // Compile the sheet to a jar
        File jarFile = compiler.compileToJar(sheet, verbose);
        
        log.info("Executing sheet from jar: {}", jarFile.getAbsolutePath());
        
        // Use the FlowRunner to execute the jar
        try (FlowRunner runner = new FlowRunner()) {
            runner.runFlow(jarFile.toPath());
        }
        
        log.info("Sheet execution completed: {}", sheet.getName());
    }
} 