package io.webetl.service;

import io.webetl.model.Sheet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.util.Map;

@Service
public class SheetService {
    private static final Logger logger = LoggerFactory.getLogger(SheetService.class);
    private final Path sheetsDirectory = Paths.get("sheets");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int CURRENT_SHEET_VERSION = 1; // Increment when schema changes

    public SheetService() {
        try {
            Files.createDirectories(sheetsDirectory);
            logger.info("Sheets directory initialized at: {}", sheetsDirectory.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create sheets directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create sheets directory", e);
        }
    }

    public void saveSheet(String id, Sheet sheet) {
        try {
            Path filePath = sheetsDirectory.resolve(id + ".json");
            logger.info("Saving sheet with id: {} to path: {}", id, filePath);
            objectMapper.writeValue(filePath.toFile(), sheet);
            logger.debug("Sheet saved successfully. Sheet name: {}, Nodes: {}, Edges: {}", 
                sheet.getName(), 
                sheet.getNodes().size(),
                sheet.getEdges().size());
        } catch (IOException e) {
            logger.error("Failed to save sheet with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to save sheet: " + id, e);
        }
    }

    public Sheet getSheet(String id) {
        try {
            Path filePath = sheetsDirectory.resolve(id + ".json");
            logger.info("Loading sheet with id: {} from path: {}", id, filePath);
            Sheet sheet = objectMapper.readValue(filePath.toFile(), Sheet.class);
            logger.debug("Sheet loaded successfully. Sheet name: {}, Nodes: {}, Edges: {}", 
                sheet.getName(), 
                sheet.getNodes().size(),
                sheet.getEdges().size());
            return migrateSheet(sheet);
        } catch (IOException e) {
            logger.error("Failed to read sheet with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to read sheet: " + id, e);
        }
    }

    public List<Sheet> getAllSheets() {
        List<Sheet> sheets = new ArrayList<>();
        try {
            logger.info("Loading all sheets from directory: {}", sheetsDirectory);
            Files.list(sheetsDirectory)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        Sheet sheet = objectMapper.readValue(path.toFile(), Sheet.class);
                        sheets.add(sheet);
                        logger.debug("Loaded sheet: {}, Nodes: {}, Edges: {}", 
                            sheet.getName(), 
                            sheet.getNodes().size(),
                            sheet.getEdges().size());
                    } catch (IOException e) {
                        logger.error("Failed to read sheet from path {}: {}", path, e.getMessage(), e);
                        throw new RuntimeException("Failed to read sheet: " + path, e);
                    }
                });
            logger.info("Successfully loaded {} sheets", sheets.size());
        } catch (IOException e) {
            logger.error("Failed to list sheets: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list sheets", e);
        }
        return sheets;
    }

    private Sheet migrateSheet(Sheet sheet) {
        int version = sheet.getVersion() != null ? sheet.getVersion() : 0;
        
        if (version < 1) {
            // Migrate to version 1: Add parameters to nodes
            sheet.getNodes().forEach(node -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) node.get("data");
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> componentData = (Map<String, Object>) data.get("componentData");
                    if (componentData != null && !componentData.containsKey("parameters")) {
                        componentData.put("parameters", new ArrayList<>());
                    }
                }
            });
        }
        
        // Add more version migrations here as needed
        
        sheet.setVersion(CURRENT_SHEET_VERSION);
        return sheet;
    }

    public Sheet updateSheetName(String id, String name) {
        Sheet sheet = getSheet(id);
        sheet.setName(name);
        saveSheet(id, sheet);
        return sheet;
    }
} 