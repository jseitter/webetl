package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@SuperBuilder
@JsonTypeName("secret")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SecretParameter extends Parameter<String> {
    public SecretParameter() {}

    public SecretParameter(String name, String label, String description, boolean required) {
        super(name, label, description, required);
    }
} 