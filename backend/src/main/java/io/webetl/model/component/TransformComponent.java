package io.webetl.model.component;

import java.util.ArrayList;

public class TransformComponent extends ETLComponent {
    private String transformationType;
    private String[] inputTypes;
    private String[] outputTypes;

    public TransformComponent() {
        super(null, null, null, null, "#fff7f0", new ArrayList<>());
    }

    public TransformComponent(String id, String label, String description, String icon,
                            String transformationType, String[] inputTypes, String[] outputTypes) {
        super(id, label, description, icon, "#fff7f0", new ArrayList<>());
        this.transformationType = transformationType;
        this.inputTypes = inputTypes;
        this.outputTypes = outputTypes;
    }

    public String getTransformationType() { return transformationType; }
    public void setTransformationType(String transformationType) { this.transformationType = transformationType; }

    public String[] getInputTypes() { return inputTypes; }
    public void setInputTypes(String[] inputTypes) { this.inputTypes = inputTypes; }

    public String[] getOutputTypes() { return outputTypes; }
    public void setOutputTypes(String[] outputTypes) { this.outputTypes = outputTypes; }
} 