package it.doqui.index.ecmengineqs.business.dto;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AclData {
    private Boolean inherits;
    private final Multimap<String,AclPermission> grants;

    public AclData() {
        grants = HashMultimap.create();
    }

    public void addPermission(String authority, AclPermission permission) {
        grants.put(authority, permission);
    }
}
