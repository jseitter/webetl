package io.webetl.model.component;

import java.util.ArrayList;

public class SourceComponent extends ETLComponent {
    private String sourceType;
    private boolean supportsControlFlow;

    public SourceComponent() {
        super(null, null, null, null, "#f0f7ff", new ArrayList<>());
    }

    public SourceComponent(String id, String label, String description, String icon, 
                         String sourceType, boolean supportsControlFlow) {
        super(id, label, description, icon, "#f0f7ff", new ArrayList<>());
        this.sourceType = sourceType;
        this.supportsControlFlow = supportsControlFlow;
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public boolean isSupportsControlFlow() { return supportsControlFlow; }
    public void setSupportsControlFlow(boolean supportsControlFlow) { this.supportsControlFlow = supportsControlFlow; }
} 