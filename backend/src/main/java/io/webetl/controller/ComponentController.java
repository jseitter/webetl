package io.webetl.controller;

import io.webetl.model.component.ETLComponent;
import io.webetl.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.ArrayList;
import io.webetl.model.component.parameter.Parameter;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;
import io.webetl.model.component.SourceComponent;

@RestController
@RequestMapping("/api/components")
public class ComponentController {
    
    @Autowired
    private ComponentService componentService;
    
    @GetMapping
    public List<ETLComponent> getComponents() {
        return componentService.getAvailableComponents();
    }

    @GetMapping("/debug")
    public ETLComponent getDebugComponent() {
        List<Parameter<?>> dbParams = new ArrayList<>();
        dbParams.add(new StringParameter("connectionString", "Connection String", 
            "Database connection string", true, 500, null));
        dbParams.add(new StringParameter("username", "Username", 
            "Database username", true, 50, null));
        dbParams.add(new SecretParameter("password", "Password", 
            "Database password", true));
        dbParams.add(new SQLParameter("query", "SQL Query", 
            "SQL query to execute", true, 1000));

        SourceComponent dbSource = new SourceComponent(
            "db-source",
            "Database Source",
            "Reads data from databases",
            "DatabaseIcon",
            "database",
            true
        );
        dbSource.setParameters(dbParams);
        dbSource.setType("source");
        
        return dbSource;
    }
} 