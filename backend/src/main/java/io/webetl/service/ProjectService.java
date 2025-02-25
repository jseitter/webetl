package io.webetl.service;

import io.webetl.model.Project;
import io.webetl.model.Sheet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final Path projectsPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectService(Path dataDirectory) {
        this.projectsPath = dataDirectory.resolve("projects");
        try {
            Files.createDirectories(projectsPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize projects directory", e);
        }
    }

    private Path getProjectSheetsPath(String projectId) {
        return projectsPath.resolve(projectId).resolve("sheets");
    }

    public Project createProject(Project project) {
        project.setId(UUID.randomUUID().toString());
        try {
            // Create project directory and sheets subdirectory
            Path projectDir = projectsPath.resolve(project.getId());
            Files.createDirectories(projectDir.resolve("sheets"));
            
            // Save project metadata inside the project directory
            objectMapper.writeValue(projectDir.resolve("project.json").toFile(), project);
            return project;
        } catch (IOException e) {
            throw new RuntimeException("Error creating project", e);
        }
    }

    public Sheet createSheet(String projectId, Sheet sheet) {
        try {
            sheet.setId(UUID.randomUUID().toString());
            Path sheetsPath = getProjectSheetsPath(projectId);
            objectMapper.writeValue(sheetsPath.resolve(sheet.getId() + ".json").toFile(), sheet);
            return sheet;
        } catch (IOException e) {
            throw new RuntimeException("Error creating sheet", e);
        }
    }

    public List<Sheet> getProjectSheets(String projectId) {
        try {
            Path sheetsPath = getProjectSheetsPath(projectId);
            if (!Files.exists(sheetsPath)) {
                return new ArrayList<>();
            }
            
            List<Sheet> sheets = new ArrayList<>();
            Files.list(sheetsPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        sheets.add(objectMapper.readValue(path.toFile(), Sheet.class));
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading sheet", e);
                    }
                });
            return sheets;
        } catch (IOException e) {
            throw new RuntimeException("Error reading project sheets", e);
        }
    }

    public void deleteProject(String id) {
        try {
            Files.deleteIfExists(projectsPath.resolve(id + ".json"));
            // Also delete associated sheets
            Files.deleteIfExists(projectsPath.resolve(id));
        } catch (IOException e) {
            throw new RuntimeException("Error deleting project", e);
        }
    }

    private Project getProject(String id) throws IOException {
        File projectFile = projectsPath.resolve(id + ".json").toFile();
        return objectMapper.readValue(projectFile, Project.class);
    }

    private void saveProject(Project project) throws IOException {
        objectMapper.writeValue(projectsPath.resolve(project.getId() + ".json").toFile(), project);
    }

    public List<Project> getAllProjects() {
        try {
            List<Project> projects = new ArrayList<>();
            Files.list(projectsPath)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        Project project = objectMapper.readValue(
                            dir.resolve("project.json").toFile(), 
                            Project.class
                        );
                        projects.add(project);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading project", e);
                    }
                });
            return projects;
        } catch (IOException e) {
            throw new RuntimeException("Error listing projects", e);
        }
    }

    public Sheet updateSheet(String projectId, String sheetId, Sheet sheet) {
        try {
            Path sheetsPath = getProjectSheetsPath(projectId);
            // Create directories if they don't exist
            Files.createDirectories(sheetsPath);
            
            File sheetFile = sheetsPath.resolve(sheetId + ".json").toFile();
            objectMapper.writeValue(sheetFile, sheet);
            return sheet;
        } catch (IOException e) {
            throw new RuntimeException("Error updating sheet", e);
        }
    }

    public Sheet getProjectSheet(String projectId, String sheetId) {
        try {
            Path sheetsPath = getProjectSheetsPath(projectId);
            File sheetFile = sheetsPath.resolve(sheetId + ".json").toFile();
            return objectMapper.readValue(sheetFile, Sheet.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading sheet", e);
        }
    }

    public Sheet updateSheetName(String projectId, String sheetId, String newName) {
        Sheet sheet = getProjectSheet(projectId, sheetId);
        sheet.setName(newName);
        return updateSheet(projectId, sheetId, sheet);
    }

    public void moveSheetToTrash(String projectId, String sheetId) throws IOException {
        Path sheetsPath = getProjectSheetsPath(projectId);
        Path trashPath = sheetsPath.resolve("trash");
        Files.createDirectories(trashPath);
        
        Path sourcePath = sheetsPath.resolve(sheetId + ".json");
        Path targetPath = trashPath.resolve(sheetId + ".json");
        
        Files.move(sourcePath, targetPath);
    }
} 