package it.doqui.index.ecmengineqs.business.dto;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class ContentAcl {
    @Getter
    private final Map<String, AclRight> rights;

    @Getter
    private final Multimap<String, AclPermission> grants;

    public ContentAcl() {
        rights = new HashMap<>();
        grants = HashMultimap.create();
    }

    public void addPermission(String authority, AclPermission p) {
        grants.put(authority, p);
        rights.put(authority, AclRight.addPermission(rights.get(authority), p));
    }

    public AclRight getRight(String authority) {
        return rights.get(authority);
    }
}
