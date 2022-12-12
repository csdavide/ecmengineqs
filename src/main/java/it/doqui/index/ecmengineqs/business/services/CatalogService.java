package it.doqui.index.ecmengineqs.business.services;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import it.doqui.index.ecmengineqs.business.converters.NodeMapper;
import it.doqui.index.ecmengineqs.business.dto.*;
import it.doqui.index.ecmengineqs.business.entities.NodeAssociation;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.entities.NodePath;
import it.doqui.index.ecmengineqs.business.entities.ParentChildRelationship;
import it.doqui.index.ecmengineqs.business.exceptions.ForbiddenException;
import it.doqui.index.ecmengineqs.business.exceptions.NotFoundException;
import it.doqui.index.ecmengineqs.business.schema.SchemaConstants;
import it.doqui.index.ecmengineqs.business.validators.NodeValidator;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.integration.indexing.IndexerDelegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.doqui.index.ecmengineqs.foundation.Constants.WORKSPACE;

@ApplicationScoped
@Slf4j
public class CatalogService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    NodeService nodeService;

    @Inject
    NodeMapper nodeMapper;

    @Inject
    AclService aclService;

    @Inject
    RelationshipService relationshipService;

    @Inject
    TenantService tenantService;

    @Inject
    NodeValidator nodeValidator;

    @Inject
    CatalogManager catalogManager;

    @Inject
    IndexerDelegate indexerDelegate;

    public CatalogResponse<NodeData> createNode(ContentNode contentNode, byte[] buffer) throws IOException {
        return createNode(contentNode, null, buffer);
    }

    public CatalogResponse<NodeData> createNode(ContentNode contentNode, AclData acl, byte[] buffer) throws IOException {
        log.debug("Input Node: {}", contentNode);
        nodeValidator.validateForCreation(contentNode);
        TxResponse<NodeData> response = catalogManager.createNode(contentNode, acl, buffer);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<List<NodeData>> createNodes(Collection<Pair<ContentNode,byte[]>> list) {
        TxResponse<List<NodeData>> response = catalogManager.createNodes(list);
        boolean indexed = index(response);
        return CatalogResponse.<List<NodeData>>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<List<NodeData>> deleteNodes(Collection<String> uuids, DeleteMode mode) {
        TxResponse<List<NodeData>> response = catalogManager.deleteNodes(uuids, mode);
        boolean indexed = index(response);
        return CatalogResponse.<List<NodeData>>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeData> updateAcl(String uuid, AclData aclData, boolean replaceMode) {
        TxResponse<NodeData> response = catalogManager.updateAcl(uuid, aclData, replaceMode);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeData> removeAcl(String uuid, AclData acl) {
        TxResponse<NodeData> response = catalogManager.removeAcl(uuid, acl);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeData> resetAcl(String uuid, String authority, AclPermission permission) {
        TxResponse<NodeData> response = catalogManager.resetAcl(uuid, authority, permission);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeData> setAclInheritance(String uuid, boolean inherits) {
        TxResponse<NodeData> response = catalogManager.setAclInheritance(uuid, inherits);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public Optional<AclData> getAclData(String uuid) {
        return nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .map(node -> {
                log.debug("Retrieved node {}", node.getId());
                aclService.checkPermission(node, AclRight.READ);
                return aclService.aclData(node);
            });
    }

    public Optional<ContentAcl> getContentAcl(String uuid, boolean includeInherited) {
        return nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .map(node -> {
                log.debug("Retrieved node {}", node.getId());
                aclService.checkPermission(node, AclRight.READ);
                return aclService.calculateACLs(node, includeInherited);
            });
    }

    public CatalogResponse<NodeData> updateNode(ContentMetadata cm) {
        TxResponse<NodeData> response = catalogManager.updateNode(cm);
        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeData> renameNode(String uuid, String key, String value, boolean primaryOnly) {
        TxResponse<NodeData> response = catalogManager.renameNode(
            uuid,
            Objects.requireNonNullElse(key, SchemaConstants.PROP_CM_NAME),
            value,
            primaryOnly);

        boolean indexed = index(response);
        return CatalogResponse.<NodeData>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<List<NodeData>> updateNodes(Collection<ContentMetadata> list) {
        TxResponse<List<NodeData>> response = catalogManager.updateNodes(list);
        boolean indexed = index(response);
        return CatalogResponse.<List<NodeData>>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<ParentChildRelationship> linkParent(String parentUUID, String childUUID, String type, String name) {
        TxResponse<ParentChildRelationship> response = catalogManager.linkParent(parentUUID, childUUID, type, name);
        boolean indexed = index(response);
        return CatalogResponse.<ParentChildRelationship>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    public CatalogResponse<NodeAssociation> linkNode(String sourceUUID, String targetUUID, String type) {
        log.debug("Source {}, Target {}. Type {}", sourceUUID, targetUUID, type);
        TxResponse<NodeAssociation> response = catalogManager.linkNode(sourceUUID, targetUUID, type);
        boolean indexed = index(response);
        return CatalogResponse.<NodeAssociation>builder()
            .indexed(indexed)
            .result(response.getResult())
            .build();
    }

    private boolean index(TxResponse<?> response) {
        Future<Boolean> r = indexerDelegate.submit(response.getOperationSet(), response.isAsyncRequired());
        boolean indexed = false;
        try {
            indexed = r.get();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

        return indexed;
    }

    public List<String> getAllTenants() {
        if (!userContextManager.getContext().isAdmin()) {
            throw new ForbiddenException();
        }

        return tenantService.getAllTenants();
    }

    public boolean tenantExists(String tenantName) {
        if (!userContextManager.getContext().isAdmin()) {
            throw new ForbiddenException();
        }

        return tenantService.exists(tenantName);
    }

    public Optional<NodeInfo> getNodeInfo(String uuid) {
        return nodeService
            .findNodeInfoByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE);
    }

    public Optional<ContentNode> getContentMetadata(String uuid) {
        return nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .map(node -> {
                log.debug("Retrieved node {}", node.getId());
                aclService.checkPermission(node, AclRight.READ);
                return nodeMapper.map(node);
            });
    }

    public List<ContentNode> listContentMetadata(Collection<String> uuids) {
        return listContentMetadata(uuids, null);
    }

    public List<ContentNode> listContentMetadata(Collection<String> uuids, Collection<String> propertyNames) {
        List<NodeData> nodes = nodeService.listNodesInUUIDs(userContextManager.getContext().getTenant(), uuids, WORKSPACE);
        if (nodes.size() != uuids.size()) {
            throw new NotFoundException();
        }

        aclService.checkPermissions(nodes, AclRight.READ);
        return nodes.stream()
            .map(node -> nodeMapper.map(node, propertyNames))
            .collect(Collectors.toList());
    }

    public Map<String,ContentNode> mapAllowedContentNodes(Collection<String> uuids, boolean includeMetadata) {
        List<NodeData> nodes = nodeService.listNodesInUUIDs(userContextManager.getContext().getTenant(), uuids, WORKSPACE);
        boolean isAdmin = userContextManager.getContext().isAdmin();
        final Map<Long,ContentAcl> aclMap = isAdmin ? null : aclService.calculateACLs(nodes);
        Map<String,ContentNode> nodeMap = new HashMap<>();
        nodes.stream()
            .filter(node -> {
                if (isAdmin) {
                    return true;
                }

                try {
                    ContentAcl acl = aclMap.get(node.getId());
                    if (acl != null) {
                        aclService.verifyPermission(acl, AclRight.READ);
                        return true;
                    }
                } catch (ForbiddenException e) {
                }
                return false;
            })
            .map(node -> {
                if (includeMetadata) {
                    return nodeMapper.map(node);
                }

                ContentNode n = new ContentNode();
                n.setUuid(node.getUuid());
                n.setTenant(node.getTenant());
                return n;
            })
            .forEach(node -> {
                nodeMap.put(node.getUuid(), node);
            });

        return nodeMap;
    }

    public List<NodePath> getPaths(String uuid) {
        NodeData node = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException());
        aclService.checkPermission(node, AclRight.READ);
        return node.getPaths();
    }

    public Pair<Long, List<Relationship>> getAssociations(String uuid, AssociationType type, Collection<String> filterTypes, Pageable pageable, Long limit) {
        NodeData node = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException());
        aclService.checkPermission(node, AclRight.READ);

        Long count;
        List<Relationship> relationships;
        switch (type) {
            case CHILD:
            case PARENT: {
                PanacheQuery<ParentChildRelationship> associations = null;
                switch (type) {
                    case PARENT:
                        associations = relationshipService.getParents(node, filterTypes);
                        break;
                    case CHILD:
                        associations = relationshipService.getChildren(node, filterTypes);
                        break;
                }
                count = associations.count();
                Stream<ParentChildRelationship> stream = null;
                if (pageable != null) {
                    stream = associations.page(pageable.getPage(), pageable.getSize()).stream();
                } else if (limit != null) {
                    stream = associations.stream().limit(limit);
                } else {
                    stream = associations.stream();
                }

                relationships = stream.map(a -> {
                    Relationship r = new Relationship();
                    r.setId(a.getId());
                    r.setTypeName(a.getTypeName());
                    r.setName(a.getName());

                    switch (type) {
                        case PARENT:
                            r.setTargetUUID(a.getParent().getUuid());
                            break;
                        case CHILD:
                            r.setTargetUUID(a.getChild().getUuid());
                            break;
                    }

                    return r;
                })
                .collect(Collectors.toList());
                break;
            }
            case SOURCE:
            case TARGET: {
                PanacheQuery<NodeAssociation> associations = null;
                switch (type) {
                    case SOURCE:
                        associations = relationshipService.getOriginatedAssociations(node, filterTypes);
                        break;
                    case TARGET:
                        associations = relationshipService.getTerminatedAssociations(node, filterTypes);
                        break;
                }
                count = associations.count();
                Stream<NodeAssociation> stream = null;
                if (pageable != null) {
                    stream = associations.page(pageable.getPage(), pageable.getSize()).stream();
                } else if (limit != null) {
                    stream = associations.stream().limit(limit);
                } else {
                    stream = associations.stream();
                }

                relationships = stream.map(a -> {
                        Relationship r = new Relationship();
                        r.setId(a.getId());
                        r.setTypeName(a.getTypeName());

                        switch (type) {
                            case SOURCE:
                                r.setTargetUUID(a.getSource().getUuid());
                                break;
                            case TARGET:
                                r.setTargetUUID(a.getTarget().getUuid());
                                break;
                        }

                        return r;
                    })
                    .collect(Collectors.toList());
                break;
            }
            default:
                throw new RuntimeException("Unsupported type " + type);
        }

        return new ImmutablePair<>(count, relationships);
    }

}
