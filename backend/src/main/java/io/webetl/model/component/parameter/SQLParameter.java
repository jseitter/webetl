package io.webetl.model.component.parameter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@SuperBuilder
@JsonTypeName("sql")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SQLParameter extends Parameter<String> {
    private int maxRows;

    public SQLParameter() {}

    public SQLParameter(String name, String label, String description, boolean required, int maxRows) {
        super(name, label, description, required);
        this.maxRows = maxRows;
    }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
} 