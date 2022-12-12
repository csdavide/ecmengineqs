package it.doqui.index.ecmengineqs.business.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class TypeDescriptor extends PropertyContainerDescriptor {

    @Setter
    private String modelName;

    @Setter
    private boolean archive;

    private final Set<String> mandatoryAspects;

    public TypeDescriptor() {
        super();
        this.mandatoryAspects = new HashSet<>();
    }

}
