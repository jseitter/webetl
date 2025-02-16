package io.webetl.model.component;

import java.util.ArrayList;

public class DestinationComponent extends ETLComponent {
    private String destinationType;
    private String[] acceptedTypes;

    public DestinationComponent() {
        super(null, null, null, null, "#f0fff4", new ArrayList<>());
    }

    public DestinationComponent(String id, String label, String description, String icon,
                              String destinationType, String[] acceptedTypes) {
        super(id, label, description, icon, "#f0fff4", new ArrayList<>());
        this.destinationType = destinationType;
        this.acceptedTypes = acceptedTypes;
    }

    public String getDestinationType() { return destinationType; }
    public void setDestinationType(String destinationType) { this.destinationType = destinationType; }

    public String[] getAcceptedTypes() { return acceptedTypes; }
    public void setAcceptedTypes(String[] acceptedTypes) { this.acceptedTypes = acceptedTypes; }
} 