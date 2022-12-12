package it.doqui.index.ecmengineqs.business.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RegisterForReflection
@Getter
@AllArgsConstructor
public class NodeInfo {
    private Long id;
    private String kind;
    private String tenant;
    private String uuid;
    private String typeName;
}
