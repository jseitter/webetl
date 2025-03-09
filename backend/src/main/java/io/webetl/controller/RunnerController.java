package io.webetl.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.webetl.service.RunnerService;

@RestController
@RequestMapping("/api/runner")
public class RunnerController {
    private final RunnerService runnerService;

    public RunnerController(RunnerService runnerService) {
        this.runnerService = runnerService;
    }

    @PostMapping("/sheets/{sheetId}/run")
    public ResponseEntity<?> runSheet(
        @PathVariable String sheetId,
        @RequestParam String projectId
    ) {
        try {
            runnerService.runSheet(projectId, sheetId);
            return ResponseEntity.accepted().build();
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.ok().build(); // Still return OK as errors are sent via WebSocket
        }
    }
} 