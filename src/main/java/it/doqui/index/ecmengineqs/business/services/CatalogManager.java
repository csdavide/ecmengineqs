package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.converters.ContentConverter;
import it.doqui.index.ecmengineqs.business.converters.NodeMapper;
import it.doqui.index.ecmengineqs.business.dto.*;
import it.doqui.index.ecmengineqs.business.entities.*;
import it.doqui.index.ecmengineqs.business.exceptions.BadDataException;
import it.doqui.index.ecmengineqs.business.exceptions.BadRequestException;
import it.doqui.index.ecmengineqs.business.exceptions.NotFoundException;
import it.doqui.index.ecmengineqs.business.storage.ContentStoreManager;
import it.doqui.index.ecmengineqs.business.validators.NodeValidator;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.integration.indexing.IndexingOperationSet;
import it.doqui.index.ecmengineqs.integration.indexing.IndexingOperationType;
import it.doqui.index.ecmengineqs.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_SYS_REFERENCEABLE;
import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.PROP_CM_NAME;
import static it.doqui.index.ecmengineqs.foundation.Constants.ARCHIVE;
import static it.doqui.index.ecmengineqs.foundation.Constants.WORKSPACE;

@ApplicationScoped
@Slf4j
public class CatalogManager {

    @Inject
    UserContextManager userContextManager;

    @Inject
    NodeService nodeService;

    @Inject
    AclService aclService;

    @Inject
    NodeMapper nodeMapper;

    @Inject
    RelationshipService relationshipService;

    @Inject
    TransactionService txService;

    @Inject
    ModelService modelService;

    @Inject
    ContentStoreManager contentStoreManager;

    @Inject
    ContentConverter contentConverter;

    @Inject
    NodeValidator nodeValidator;

    private NodeData createNode(ApplicationTransaction tx, ContentNode contentNode, AclData acl, byte[] buffer) {
        String parentUUID = contentNode.getPrimaryParent().getTargetUUID();
        NodeData parent = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), parentUUID, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(parentUUID));

        aclService.checkPermission(parent, AclRight.CREATE);
        contentNode.setUuid(UUID.randomUUID().toString());
        contentNode.setTenant(userContextManager.getContext().getTenant());
        contentNode.setKind(WORKSPACE);

        if (contentNode.getContentProperty() != null) {
            if (buffer == null || buffer.length == 0) {
                throw new BadDataException("No content data provided");
            }

            // add contentUrl to content property
            // format example contentUrl=store://2012/3/23/16/46/60b73f1b-74ff-11e1-aeda-b7ce474e1849.bin
            Calendar cal = Calendar.getInstance();
            String contentUrl = String.format("store://%d/%d/%d/%d/%d/%s.bin",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                contentNode.getUuid()
            );

            contentNode.getContentProperty().setAttribute("contentUrl", contentUrl);

            // save content
            try {
                contentStoreManager.writeFileWithContentURI(contentUrl, buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        NodeData n = nodeMapper.mapFields(contentNode);
        if (StringUtils.isBlank(ObjectUtils.getAsString(n.getProperty(PROP_CM_NAME)))) {
            PrefixedQName q = PrefixedQName.valueOf(contentNode.getPrimaryParent().getName());
            n.setProperty(PROP_CM_NAME, q.getLocalPart());
        }

        if (acl != null) {
            n.getData().put("acl", aclService.aclMap(acl));
        }

        n.setTx(tx);
        nodeService.save(n);

        ParentChildRelationship parentRelationship = new ParentChildRelationship();
        parentRelationship.setParent(parent);
        parentRelationship.setChild(n);
        parentRelationship.setPrimary(true);
        parentRelationship.setTypeName(contentNode.getPrimaryParent().getTypeName());
        parentRelationship.setName(contentNode.getPrimaryParent().getName());
        n.getParents().add(parentRelationship);
        relationshipService.save(parentRelationship);

        n.getPaths().addAll(
            parent.getPaths()
                .stream()
                .filter(p -> p.isEnabled())
                .map(p -> {
                    NodePath q = new NodePath();
                    q.setNode(n);
                    q.setEnabled(true);
                    q.setLev(p.getLev() + 1);
                    q.setPrimary(p.isPrimary());
                    q.setPath(String.format("%s%d:", p.getPath(), n.getId()));
                    q.setFilePath(String.format("%s%s/", p.getFilePath(), parentRelationship.getName()));
                    return q;
                })
                .collect(Collectors.toList()));

        nodeService.savePaths(n.getPaths().stream());
        return n;
    }

    @Transactional
    public TxResponse<NodeData> createNode(ContentNode contentNode, AclData acl, byte[] buffer) throws IOException {
        ApplicationTransaction tx = txService.createTransaction();
        NodeData n = createNode(tx, contentNode, acl, buffer);
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.ADD, n);

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<List<NodeData>> createNodes(Collection<Pair<ContentNode,byte[]>> list) {
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);

        List<NodeData> nodes = list.stream()
            .map(pair -> createNode(tx, pair.getLeft(), null, pair.getRight()))
            .collect(Collectors.toList());
        operationSet.addAll(IndexingOperationType.ADD, nodes);

        return TxResponse.<List<NodeData>>builder()
            .result(nodes)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<List<NodeData>> deleteNodes(Collection<String> uuids, DeleteMode mode) {
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);

        Map<String,NodeData> nodeMap = nodeService.mapNodesInUUIDs(userContextManager.getContext().getTenant(), uuids, WORKSPACE);
        aclService.checkPermissions(nodeMap.values(), AclRight.DELETE);
        List<NodeData> deletedNodes = deleteNodes(tx, nodeMap.values());
        //TODO: gestire il caso di cancellazione dei contenuti e purge dei nodi
        // la cancellazione dei contenuti fisici deve corrispondere ad un rename del file
        // la cancellazione del file (ma anche solo il rename) può avvenire solo dopo il commit
        // quindi sarà da gestire nel catalog service

        return TxResponse.<List<NodeData>>builder()
            .result(deletedNodes)
            .operationSet(operationSet)
            .build();
    }

    private List<NodeData> deleteNodes(ApplicationTransaction tx, Collection<NodeData> nodes) {
        List<NodeData> deletedNodes = new ArrayList<>();
        if (!nodes.isEmpty()) {
            for (NodeData n : nodes) {
                n.setKind(ARCHIVE);
                n.setTx(tx);
                deletedNodes.add(n);

                n.getPaths()
                    .stream()
                    .filter(p -> p.isEnabled())
                    .forEach(p -> {
                        nodeService.updatePathsEnabled(p.getPath(), false);
                        nodeService.setTransactionWherePathPrefix(tx, p.getPath());
                    });
            }

            nodeService.saveNodes(deletedNodes);
            deletedNodes.addAll(deleteNodes(tx, nodeService.findChildrenWherePrimary(nodes)));
        }

        return deletedNodes;
    }

    @Transactional
    public TxResponse<NodeData> updateAcl(String uuid, AclData acl, boolean replaceMode) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));
        aclService.checkPermission(n, AclRight.UPDATE);

        AclData oldAcl = aclService.aclData(n);
        if (replaceMode) {
            oldAcl.getGrants().clear();
        }

        n.getData().put("acl", aclService.aclMap(aclService.mergeWith(oldAcl, acl)));
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.UPDATE_ACL, nodeService.save(n.setTx(tx)));

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<NodeData> removeAcl(String uuid, AclData acl) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));
        aclService.checkPermission(n, AclRight.UPDATE);

        AclData oldAcl = aclService.aclData(n);
        acl.getGrants().entries().forEach(entry -> {
            oldAcl.getGrants().remove(entry.getKey(), entry.getValue());
        });

        n.getData().put("acl", aclService.aclMap(oldAcl));
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.UPDATE_ACL, nodeService.save(n.setTx(tx)));

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<NodeData> resetAcl(String uuid, String authority, AclPermission permission) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));
        aclService.checkPermission(n, AclRight.UPDATE);

        AclData acl = aclService.aclData(n);
        if (authority == null) {
            acl.getGrants().clear();
        } else if (permission == null) {
            acl.getGrants().removeAll(authority);
        } else {
            acl.getGrants().remove(authority, permission);
        }

        n.getData().put("acl", aclService.aclMap(acl));
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.UPDATE_ACL, nodeService.save(n.setTx(tx)));

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<NodeData> setAclInheritance(String uuid, boolean inherits) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));
        aclService.checkPermission(n, AclRight.UPDATE);

        Map<String, Object> acl = (Map<String, Object>) n.getData().get("acl");
        if (acl == null) {
            acl = new HashMap<>();
            n.getData().put("acl", acl);
        }

        acl.put("inherits", inherits);

        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.UPDATE_ACL, nodeService.save(n.setTx(tx)));

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    private NodeData updateNode(ApplicationTransaction tx, ContentMetadata cm) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), cm.getUuid(), WORKSPACE)
            .orElseThrow(() -> new NotFoundException(cm.getUuid()));

        cm.setTypeName(n.getTypeName());
        ContentNode contentNode = contentConverter.getAsContentNode(cm);
        log.debug("Updating {}", contentNode);
        nodeValidator.validateForUpdateMetadata(contentNode);
        aclService.checkPermission(n, AclRight.UPDATE);

        contentNode.getProperties()
            .values()
            .stream()
            .forEach(pc -> {
                String name = pc.getDescriptor().getName();
                Object value = pc.getValue();
                n.setProperty(name, value);
            });

        if (!contentNode.getAspects().isEmpty()) {
            List<String> aspects = new ArrayList<>();
            aspects.addAll(contentNode.getAspects().keySet());
            aspects.add(ASPECT_SYS_REFERENCEABLE);
            n.getData().put("aspects", aspects);
        }

        n.setTx(tx);
        nodeService.save(n);
        return n;
    }

    @Transactional
    public TxResponse<List<NodeData>> updateNodes(Collection<ContentMetadata> list) {
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);

        List<NodeData> nodes = list.stream()
            .map(cm -> updateNode(tx, cm))
            .collect(Collectors.toList());
        operationSet.addAll(IndexingOperationType.UPDATE_METADATA, nodes);

        return TxResponse.<List<NodeData>>builder()
            .result(nodes)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<NodeData> updateNode(ContentMetadata cm) {
        ApplicationTransaction tx = txService.createTransaction();
        IndexingOperationSet operationSet = new IndexingOperationSet(tx);

        NodeData n = updateNode(tx, cm);
        operationSet.add(IndexingOperationType.UPDATE_METADATA, n);

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    @Transactional
    public TxResponse<NodeData> renameNode(String uuid, String key, String value, boolean primaryOnly) {
        NodeData n = nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));

        if (n.getProperty(key) == null) {
            throw new BadRequestException("Property "+ key + " not found");
        }

        ApplicationTransaction tx = txService.createTransaction();
        n.setProperty(key, value);
        n.setTx(tx);
        nodeService.save(n);

        Set<Long> updatedSet = relationshipService.save(
                n.getParents()
                    .stream()
                    .filter(r -> r.isPrimary() || !primaryOnly)
                    .map(r -> r.setName(value))
                    .collect(Collectors.toList())
            )
            .stream()
            .map(r -> r.getParent().getId())
            .collect(Collectors.toSet());

        n.getPaths()
            .stream()
            .filter(p -> {
                String[] parts = p.getPath().split("\\:");
                if (parts.length > 1) {
                    String x = parts[parts.length - 2];
                    return updatedSet.contains(Long.parseLong(x));
                }

                return false;
            })
            .forEach(p -> {
                String[] parts = p.getFilePath().split("\\/");
                parts[parts.length - 1] = value;
                String target = String.join("/", parts) + "/";

                nodeService.updatePaths(p.getPath(), p.getFilePath(), target);
                nodeService.setTransactionWherePathPrefix(tx, p.getPath());
            });

//        nodeService.savePaths(
//            n.getPaths()
//                .stream()
//                .filter(p -> {
//                    String[] parts = p.getPath().split("\\:");
//                    if (parts.length > 1) {
//                        String x = parts[parts.length - 2];
//                        return updatedSet.contains(Long.parseLong(x));
//                    }
//
//                    return false;
//                })
//                .map(p -> {
//                    String[] parts = p.getFilePath().split("\\/");
//                    parts[parts.length - 1] = value;
//                    p.setFilePath(String.join("/", parts) + "/");
//                    return p;
//                }));

        //TODO: propagare la modifica a tutti i nodi che contengono quel path nei figli e discendenti
        // usare la sequenza di nodi che non varia per aggiornare i path


        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.RENAME, n);
        operationSet.add(IndexingOperationType.PARENTS, null);

        return TxResponse.<NodeData>builder()
            .result(n)
            .operationSet(operationSet)
            .build();
    }

    private Pair<NodeData,NodeData> findEdges(String sourceUUID, String targetUUID, String type) {
        Map<String,NodeData> nodeMap = nodeService.mapNodesInUUIDs(userContextManager.getContext().getTenant(), List.of(sourceUUID, targetUUID), WORKSPACE);
        NodeData source = Optional.of(nodeMap.get(sourceUUID)).orElseThrow(() -> new NotFoundException(sourceUUID));
        NodeData target = Optional.of(nodeMap.get(targetUUID)).orElseThrow(() -> new NotFoundException(targetUUID));

        // resolve type
        PrefixedQName p = PrefixedQName.valueOf(type);
        modelService
            .getNamespaceURI(p.getNamespaceURI())
            .orElseThrow(() -> new BadRequestException("QName resolution exception (namespacePrefixResolver error): " + p));

        return new ImmutablePair<>(source, target);
    }

    @Transactional
    public TxResponse<ParentChildRelationship> linkParent(String parentUUID, String childUUID, String type, String name) {
        final Pair<NodeData,NodeData> edges = findEdges(parentUUID, childUUID, type);
        final NodeData parent = edges.getLeft();
        final NodeData child = edges.getRight();

        if (name != null) {
            PrefixedQName p = PrefixedQName.valueOf(name);
            modelService
                .getNamespaceURI(p.getNamespaceURI())
                .orElseThrow(() -> new BadRequestException("QName resolution exception (namespacePrefixResolver error): " + p));
        } else {
            name = child.getParents()
                .stream()
                .filter(r -> r.isPrimary()).map(r -> r.getName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Child association prefixed name null or not found"));
        }

        Set<Long> ancestors = parent.getPaths()
            .stream()
            .flatMap(p -> Arrays.stream(p.getPath().split("\\:")))
            .map(x -> Long.valueOf(x))
            .collect(Collectors.toSet());

        if (ancestors.contains(child.getId())) {
            throw new BadRequestException(String.format("Cannot create a circular relationship: %s is an ancestor of %s", child.getUuid(), parent.getUuid()));
        }

        ParentChildRelationship relationship = new ParentChildRelationship();
        relationship.setParent(parent);
        relationship.setChild(child);
        relationship.setTypeName(type);
        relationship.setName(name);
        relationship.setPrimary(false);
        relationshipService.save(relationship);

        final List<NodePath> paths = new ArrayList<>();
        fillPaths(child, parent.getPaths(), relationship.isPrimary(), relationship.getName(), paths);
        nodeService.savePaths(paths.stream());

        final ApplicationTransaction tx = txService.createTransaction();
        final IndexingOperationSet operationSet = new IndexingOperationSet(tx);

        nodeService
            .saveNodes(paths.stream().map(p -> p.getNode()).distinct().map(n -> n.setTx(tx)))
            .stream()
            .forEach(n -> operationSet.add(IndexingOperationType.PARENTS, n));

        return TxResponse.<ParentChildRelationship>builder()
            .operationSet(operationSet)
            .result(relationship)
            .build();
    }

    private void fillPaths(NodeData n, List<NodePath> list, boolean isPrimary, String parentName, List<NodePath> paths) {
        List<NodePath> result = list.stream()
            .map(p -> {
                NodePath q = new NodePath();
                q.setNode(n);
                q.setEnabled(true);
                q.setLev(p.getLev() + 1);
                q.setPrimary(p.isPrimary() && isPrimary);
                q.setPath(String.format("%s%d:", p.getPath(), n.getId()));
                q.setFilePath(String.format("%s%s/", p.getFilePath(), parentName));
                paths.add(q);
                return q;
            }).collect(Collectors.toList());

        n.getChildren()
            .stream()
            .forEach(r -> fillPaths(r.getChild(), result, isPrimary, r.getName(), paths));
    }

    @Transactional
    public TxResponse<NodeAssociation> linkNode(String sourceUUID, String targetUUID, String type) {
        final Pair<NodeData,NodeData> edges = findEdges(sourceUUID, targetUUID, type);
        final NodeData source = edges.getLeft();
        final NodeData target = edges.getRight();

        NodeAssociation association = new NodeAssociation();
        association.setSource(source);
        association.setTarget(target);
        association.setTypeName(type);
        relationshipService.save(association);

        ApplicationTransaction tx = txService.createTransaction();
        source.setTx(tx);
        target.setTx(tx);
        nodeService.saveNodes(source, target);

        IndexingOperationSet operationSet = new IndexingOperationSet(tx);
        operationSet.add(IndexingOperationType.ASSOCIATIONS, source);
        operationSet.add(IndexingOperationType.ASSOCIATIONS, target);

        return TxResponse.<NodeAssociation>builder()
            .operationSet(operationSet)
            .result(association)
            .build();
    }
}
