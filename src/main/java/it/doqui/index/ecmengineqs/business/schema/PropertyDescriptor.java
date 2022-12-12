package it.doqui.index.ecmengineqs.business.schema;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PropertyDescriptor {
    String name;
    String typeName;
    boolean mandatory;
    boolean multiple;
    boolean indexed;
}
