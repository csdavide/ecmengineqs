package it.doqui.index.ecmengineqs.business.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Relationship {
    private Long id;
    private String typeName;
    private String name;
    private String targetUUID;
    private String targetName;
}
