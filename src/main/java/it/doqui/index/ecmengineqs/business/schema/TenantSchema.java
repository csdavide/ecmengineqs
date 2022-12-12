package it.doqui.index.ecmengineqs.business.schema;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TenantSchema {

    @Setter
    private String tenant;

    private final BiMap<String, String> namespaceMap = HashBiMap.create();
    private final Map<String, TypeDescriptor> typeMap = new HashMap<>();
    private final Map<String, AspectDescriptor> aspectMap = new HashMap<>();

}
