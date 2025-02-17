package io.webetl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Project is a collection of sheets.
 * 
 */
public class Project {
    // Project id
    private String id;
    // Name of the project
    private String name;
    // Sheets of the project
    private List<Sheet> sheets;

    // Constructor
    public Project() {
        super();
        this.id = UUID.randomUUID().toString();
        this.sheets = new ArrayList<>();
    }

    // Getter for id
    public String getId() {
        return id;
    }

    // Setter for id
    public void setId(String id) {
        this.id = id;
    }

    // Getter for name
    public String getName() {
        return name;
    }

    // Setter for name
    public void setName(String name) {
        this.name = name;
    }

    // Getter for sheets
    public List<Sheet> getSheets() {
        return sheets;
    }

    // Setter for sheets
    public void setSheets(List<Sheet> sheets) {
        this.sheets = sheets;
    }
} 