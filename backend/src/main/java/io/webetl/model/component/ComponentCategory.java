package io.webetl.model.component;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
public class ComponentCategory {
    private String name;
    private List<ETLComponent> components;
} 