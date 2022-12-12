package it.doqui.index.ecmengineqs.business.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
public abstract class PropertyContainerDescriptor {
    @Setter
    private String name;

    @Setter
    private String parent;

    private final LinkedHashMap<String,PropertyDescriptor> properties;

    public PropertyContainerDescriptor() {
        this.properties = new LinkedHashMap<>();
    }
}
