package io.webetl.model;

import java.util.List;
import java.util.Map;
/**
 * Sheet is the main object in the WebETL platform.
 * It represents a flow of data from a source to a destination.
 * It contains the nodes and edges of the flow.
 * 
 * 
 */
public class Sheet {
    // Sheet id
    private String id;
    // Version of the sheet
    private Integer version;
    // Name of the sheet
    private String name;
    // Nodes of the sheet
    private List<Map<String, Object>> nodes;
    // Edges of the sheet
    private List<Map<String, Object>> edges;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Map<String, Object>> getNodes() { return nodes; }
    public void setNodes(List<Map<String, Object>> nodes) { this.nodes = nodes; }
    
    public List<Map<String, Object>> getEdges() { return edges; }
    public void setEdges(List<Map<String, Object>> edges) { this.edges = edges; }
} 