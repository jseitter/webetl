package io.webetl.controller;

import io.webetl.model.component.ComponentCategory;
import io.webetl.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * ComponentController is a REST controller that provides endpoints for managing components.
 */
@RestController
@RequestMapping("/api/components")
public class ComponentController {
    
    @Autowired
    private ComponentService componentService;
    
    @GetMapping
    public List<ComponentCategory> getComponents() {
        return componentService.getAvailableComponents();
    }

} 