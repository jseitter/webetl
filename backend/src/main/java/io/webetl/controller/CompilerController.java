package io.webetl.controller;

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
    public void compileSheet(
        @PathVariable String sheetId,
        @RequestParam String projectId
    ) {
        compilerService.compileSheet(projectId, sheetId);
    }
} 