package it.doqui.index.ecmengineqs.business.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class ContentMetadata {
    private String uuid;
    private String typeName;
    private final Map<String, List<String>> properties;
    private final Set<String> aspects;

    public ContentMetadata() {
        this.properties = new HashMap<>();
        this.aspects = new HashSet<>();
    }
}
