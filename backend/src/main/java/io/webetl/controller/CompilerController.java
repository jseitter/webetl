package io.webetl.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.webetl.service.CompilerService;

@RestController
@RequestMapping("/api/compiler")
public class CompilerController {
    private final CompilerService compilerService;

    public CompilerController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @PostMapping("/sheets/{sheetId}/compile")
    public ResponseEntity<?> compileSheet(
        @PathVariable String sheetId,
        @RequestParam String projectId
    ) {
        try {
            compilerService.compileSheet(projectId, sheetId);
            return ResponseEntity.accepted().build();
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.ok().build(); // Still return OK as errors are sent via WebSocket
        }
    }
} 