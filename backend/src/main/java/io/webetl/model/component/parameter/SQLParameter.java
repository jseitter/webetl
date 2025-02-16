package io.webetl.model.component.parameter;

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