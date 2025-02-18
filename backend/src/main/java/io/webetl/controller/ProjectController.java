package io.webetl.controller;

import io.webetl.model.Project;
import io.webetl.model.Sheet;
import io.webetl.service.ProjectService;
import io.webetl.runtime.FlowExecutor;
import io.webetl.compiler.FlowCompiler;
import io.webetl.runtime.ExecutionResult;
import io.webetl.runtime.ExecutionContext;
import io.webetl.runtime.RuntimeMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.io.File;
import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private FlowCompiler flowCompiler;

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.createProject(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/sheets")
    public List<Sheet> getProjectSheets(@PathVariable String id) {
        return projectService.getProjectSheets(id);
    }

    @PostMapping("/{id}/sheets")
    public Sheet createSheet(@PathVariable String id, @RequestBody Sheet sheet) {
        return projectService.createSheet(id, sheet);
    }

    @PutMapping("/{projectId}/sheets/{sheetId}")
    public ResponseEntity<Sheet> updateSheet(
            @PathVariable String projectId,
            @PathVariable String sheetId,
            @RequestBody Sheet sheet) {
        Sheet updatedSheet = projectService.updateSheet(projectId, sheetId, sheet);
        return ResponseEntity.ok(updatedSheet);
    }

    @PostMapping("/{projectId}/sheets/{sheetId}/execute")
    public ExecutionResult executeSheet(
            @PathVariable String projectId,
            @PathVariable String sheetId) {
        Sheet sheet = projectService.getProjectSheet(projectId, sheetId);
        return flowExecutor.execute(sheet, new ExecutionContext());
    }

    @GetMapping("/{projectId}/sheets/{sheetId}/metrics")
    public RuntimeMetrics getMetrics(
            @PathVariable String projectId,
            @PathVariable String sheetId) {
        return flowExecutor.getMetrics(sheetId);
    }

    @GetMapping("/{projectId}/sheets/{sheetId}/export")
    public ResponseEntity<Resource> exportJar(
            @PathVariable String projectId,
            @PathVariable String sheetId) throws IOException {
        Sheet sheet = projectService.getProjectSheet(projectId, sheetId);
        File jarFile = flowCompiler.compileToJar(sheet, false);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"flow.jar\"")
            .body(new FileSystemResource(jarFile));
    }
} 