package io.webetl.model.component.parameter;

public class SecretParameter extends Parameter<String> {
    public SecretParameter() {}

    public SecretParameter(String name, String label, String description, boolean required) {
        super(name, label, description, required);
    }
} 