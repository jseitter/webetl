package io.webetl.service;

import io.webetl.model.component.ETLComponent;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.TransformComponent;
import io.webetl.model.component.DestinationComponent;
import io.webetl.model.component.parameter.Parameter;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class ComponentService {
    
    public List<ETLComponent> getAvailableComponents() {
        List<ETLComponent> components = new ArrayList<>();
        
        // Add source components
        SourceComponent fileSource = new SourceComponent(
            "file-source",
            "File Source",
            "Reads data from files",
            "FileIcon",
            "file",
            true
        );
        fileSource.setType("source");
        components.add(fileSource);
        
        // Database source with parameters
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

        // Add debug logging
        System.out.println("DB Source: " + dbSource);
        System.out.println("DB Parameters: " + dbSource.getParameters());
        
        components.add(dbSource);

        // Add transform components
        TransformComponent filter = new TransformComponent(
            "filter",
            "Filter",
            "Filters data based on conditions",
            "FilterIcon",
            "filter",
            new String[]{"any"},
            new String[]{"any"}
        );
        filter.setType("transform");
        components.add(filter);
        
        TransformComponent map = new TransformComponent(
            "map",
            "Map",
            "Transforms data structure",
            "MapIcon",
            "map",
            new String[]{"any"},
            new String[]{"any"}
        );
        map.setType("transform");
        components.add(map);

        // Add destination components
        DestinationComponent fileDest = new DestinationComponent(
            "file-dest",
            "File Destination",
            "Writes data to files",
            "FileIcon",
            "file",
            new String[]{"any"}
        );
        fileDest.setType("destination");
        components.add(fileDest);
        
        DestinationComponent dbDest = new DestinationComponent(
            "db-dest",
            "Database Destination",
            "Writes data to databases",
            "DatabaseIcon",
            "database",
            new String[]{"any"}
        );
        dbDest.setType("destination");
        components.add(dbDest);

        return components;
    }
} 