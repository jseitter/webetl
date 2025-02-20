package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@SuperBuilder
@JsonTypeName("number")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NumberParameter extends Parameter<Number> {
    private double min;
    private double max;

    public NumberParameter() {}

    public NumberParameter(String name, String label, String description, boolean required, 
                         double min, double max) {
        super(name, label, description, required);
        this.min = min;
        this.max = max;
    }

    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }

    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
} 