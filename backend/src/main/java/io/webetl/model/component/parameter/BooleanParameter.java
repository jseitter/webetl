package io.webetl.model.component.parameter;

public class BooleanParameter extends Parameter<Boolean> {
    public BooleanParameter() {}

    public BooleanParameter(String name, String label, String description, boolean required) {
        super(name, label, description, required);
    }
} 