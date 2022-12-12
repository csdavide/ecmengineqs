package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.dto.AclData;
import it.doqui.index.ecmengineqs.business.dto.AclPermission;
import it.doqui.index.ecmengineqs.business.dto.AclRight;
import it.doqui.index.ecmengineqs.business.dto.ContentAcl;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.exceptions.ForbiddenException;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class AclService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    NodeService nodeService;

    public Map<Long,ContentAcl> calculateACLs(Collection<NodeData> nodes) {
        Map<Long,ContentAcl> aclMap = new HashMap<>();
        Map<Long,NodeData> nodeMap = new HashMap<>();
        nodes.forEach(node -> {
            aclMap.put(node.getId(), new ContentAcl());
            nodeMap.put(node.getId(), node);
        });

        log.debug("Retrieving paths");
        List<Long> parentIds = nodes.stream()
            .flatMap(node -> node.getPaths().stream())
            .filter(np -> np.isEnabled())
            .map(np -> np.getPath())
            .map(path -> path.split("\\:"))
            .flatMap(x -> Arrays.stream(x))
            .map(x -> Long.parseLong(x))
            .filter(x -> !aclMap.containsKey(x))
            .collect(Collectors.toList());
        log.debug("{} parent ids calculated", parentIds.size());

        nodeService.findNodesInIds(parentIds).forEach(p -> nodeMap.put(p.getId(), p));
        log.debug("Found {} parents", nodeMap.size() - nodes.size());

        //TODO: convertire metodo usando nodeService.mapAncestors
        for (NodeData node : nodes) {
            fillACLs(aclMap.get(node.getId()), node, nodeMap);
        }

        return aclMap;
    }

    public ContentAcl calculateACLs(NodeData node, boolean includeInherited) {
        ContentAcl contentAcl = new ContentAcl();
        if (includeInherited) {
            List<Long> parentIds = node.getPaths().stream()
                .filter(p -> p.isEnabled())
                .map(p -> {log.debug("Node {} PATH: {}", node.getId(), p.getPath()); return p.getPath();})
                .map(path -> path.split("\\:"))
                .flatMap(x -> Arrays.stream(x))
                .map(x -> Long.parseLong(x))
                .filter(x -> x != node.getId()) //TODO: verificare distinct
                .collect(Collectors.toList());
            Map<Long,NodeData> nodeMap = new HashMap<>();
            nodeService.findNodesInIds(parentIds).forEach(p -> nodeMap.put(p.getId(), p));
            log.debug("Found {} parents", nodeMap.size());
            nodeMap.put(node.getId(), node);
            fillACLs(contentAcl, node, nodeMap);
        } else {
            fillACL(contentAcl, node);
        }

        log.debug("Permissions: {}", contentAcl.getRights());

        return contentAcl;
    }

    private void fillACLs(ContentAcl contentAcl, NodeData node, Map<Long,NodeData> nodeMap) {
        node.getPaths().stream()
            .filter(x -> x.isEnabled())
            .map(x -> x.getPath())
            .forEach(path -> {
                log.debug("Analyzing path {}", path);
                String[] array = path.split("\\:");
                for (int i=array.length-1;i>=0;i--) {
                    NodeData n = nodeMap.get(Long.parseLong(array[i]));
                    if (n != null) {
                        if (!fillACL(contentAcl, n)) {
                            break;
                        }
                    } else {
                        log.warn("Node {} not present in parent set", n.getId());
                    } // end if
                }
            });
    }

    private boolean fillACL(ContentAcl contentAcl, NodeData n) {
        log.debug("Processing acl of node {}", n.getId());
        Map<String, Object> acl = (Map<String, Object>) n.getData().get("acl");
        if (acl != null) {
            log.debug("Found ACL {} in node {}", acl, n.getId());
            Map<String, Object> grants = (Map<String, Object>) acl.get("grants");
            if (grants != null) {
                for (Map.Entry<String, Object> entry : grants.entrySet()) {
                    String authority = entry.getKey();
                    if (entry.getValue() instanceof Collection) {
                        Collection<String> permissions = (Collection<String>) entry.getValue();
                        for (String p : permissions) {
                            contentAcl.addPermission(authority, AclPermission.valueOf(p));
                        }
                    } else {
                        contentAcl.addPermission(authority, AclPermission.valueOf(entry.getValue().toString()));
                    }
                }
            }

            if (!ObjectUtils.getAsBoolean(acl.get("inherits"), true)) {
                return false;
            }
        }
        return true;
    }

    public void verifyPermission(ContentAcl acl, int requiredRight) {
        log.debug("Verifying permissions {} on ACL {}", requiredRight, acl);
        AclRight p = acl.getRight(userContextManager.getContext().getUsername());
        if (p == null) {
            p = acl.getRight("GROUP_EVERYONE");
        }

        if (!AclRight.hasRight(p, requiredRight)) {
            throw new ForbiddenException();
        }
    }

    public void checkPermission(ContentAcl acl, int requiredRight) {
        if (!userContextManager.getContext().isAdmin()) {
            verifyPermission(acl, requiredRight);
        }
    }

    public void checkPermission(NodeData node, int requiredRight) {
        if (!userContextManager.getContext().isAdmin()) {
            ContentAcl acl = calculateACLs(node, true);
            verifyPermission(acl, requiredRight);
        }
    }

    public void checkPermissions(Collection<NodeData> nodes, int requiredRight) {
        if (!userContextManager.getContext().isAdmin()) {
            Map<Long,ContentAcl> aclMap = calculateACLs(nodes);
            for (NodeData node : nodes) {
                verifyPermission(aclMap.get(node.getId()), requiredRight);
            }
        }
    }

    public AclData aclData(NodeData n) {
        AclData result = new AclData();
        Map<String, Object> acl = (Map<String, Object>) n.getData().get("acl");
        if (acl != null) {
            log.debug("Found ACL {} in node {}", acl, n.getId());
            Map<String, Object> grants = (Map<String, Object>) acl.get("grants");
            if (grants != null) {
                for (Map.Entry<String, Object> entry : grants.entrySet()) {
                    String authority = entry.getKey();
                    if (entry.getValue() instanceof Collection) {
                        Collection<String> permissions = (Collection<String>) entry.getValue();
                        for (String p : permissions) {
                            result.getGrants().put(authority, AclPermission.valueOf(p));
                        }
                    } else {
                        result.getGrants().put(authority, AclPermission.valueOf(entry.getValue().toString()));
                    }
                }
            }

            Object inherits = acl.get("inherits");
            if (inherits != null && inherits instanceof Boolean) {
                result.setInherits((Boolean) inherits);
            }
        }
        return result;
    }

    public Map<String,Object> aclMap(AclData aclData) {
        Map<String, Object> acl = new HashMap<>();
        acl.put("inherits", aclData.getInherits());

        Map<String, Object> grants = new HashMap<>();
        for (String authority : aclData.getGrants().keySet()) {
            List<String> permissions = new ArrayList<>();
            for (AclPermission p : aclData.getGrants().get(authority)) {
                permissions.add(p.toString());
            }
            grants.put(authority, permissions);
        }
        acl.put("grants", grants);

        return acl;
    }

    public AclData mergeWith(AclData a, AclData b) {
        if (b != null) {
            if (b.getInherits() != null) {
                a.setInherits(b.getInherits());
            }

            a.getGrants().putAll(b.getGrants());
        }

        if (a.getInherits() == null) {
            a.setInherits(true);
        }

        return a;
    }
}
