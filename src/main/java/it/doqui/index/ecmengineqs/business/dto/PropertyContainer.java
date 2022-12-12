package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.business.schema.PropertyDescriptor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyContainer {

    private PropertyDescriptor descriptor;
    private Object value;

    @Override
    public String toString() {
        return value == null ? null : value.toString();
    }
}
