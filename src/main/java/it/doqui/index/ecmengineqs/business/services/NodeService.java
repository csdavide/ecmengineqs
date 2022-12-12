package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.dto.NodeInfo;
import it.doqui.index.ecmengineqs.business.entities.ApplicationTransaction;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.entities.NodePath;
import it.doqui.index.ecmengineqs.business.repositories.NodePathRepository;
import it.doqui.index.ecmengineqs.business.repositories.NodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class NodeService {

    @Inject
    NodeRepository nodeRepository;

    @Inject
    NodePathRepository pathRepository;

    public Optional<NodeData> findNodeByUUID(String tenant, String uuid, String kind) {
        return nodeRepository.find("tenant = ?1 and uuid = ?2 and kind = ?3", tenant, uuid, kind).firstResultOptional();
    }

    public Optional<NodeData> findNodeByUUID(String tenant, String uuid) {
        return nodeRepository.find("tenant = ?1 and uuid = ?2", tenant, uuid).firstResultOptional();
    }

    public List<NodeData> findNodesInIds(Collection<Long> ids) {
        return nodeRepository.find("id in ?1", ids).list();
    }

    public List<NodeData> listNodesInUUIDs(String tenant, Collection<String> uuids, String kind) {
        return nodeRepository.list("tenant = ?1 and uuid in ?2 and kind = ?3", tenant, uuids, kind);
    }

    public Map<String,NodeData> mapNodesInUUIDs(String tenant, Collection<String> uuids, String kind) {
        Map<String,NodeData> map = new LinkedHashMap<>();
        log.debug("Retrieving node having uuid '{}'", uuids);
        if (!uuids.isEmpty()) {
            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();

            conditions.add("tenant = :tenant");
            params.put("tenant", tenant);

            conditions.add("uuid in :uuids");
            params.put("uuids", uuids);

            if (StringUtils.isNotBlank(kind)) {
                conditions.add("kind = :kind");
                params.put("kind", kind);
            }

            nodeRepository
                .stream(String.join(" and ", conditions), params)
                .forEach(n -> map.put(n.getUuid(), n));
        }

        return map;
    }

    public Map<Long,NodeData> mapAncestors(Collection<NodeData> nodes) {
        Map<Long,NodeData> nodeMap = new HashMap<>();
        nodes.forEach(node -> {
            nodeMap.put(node.getId(), node);
        });

        log.debug("Retrieving paths");
        Set<Long> parentIds = nodes.stream()
            .flatMap(node -> node.getPaths().stream())
            .map(np -> np.getPath())
            .map(path -> path.split("\\:"))
            .flatMap(x -> Arrays.stream(x))
            .map(x -> Long.parseLong(x))
            .collect(Collectors.toSet());
        log.debug("{} parent ids calculated", parentIds.size());

        findNodesInIds(parentIds).forEach(p -> nodeMap.put(p.getId(), p));
        log.debug("Found {} parents", nodeMap.size() - nodes.size());

        return nodeMap;
    }

    public Optional<NodeInfo> findNodeInfoByUUID(String tenant, String uuid, String kind) {
        return nodeRepository.find("tenant = ?1 and uuid = ?2 and kind = ?3", tenant, uuid, kind).project(NodeInfo.class).firstResultOptional();
    }

    public List<NodeData> findChildrenWherePrimary(Collection<NodeData> nodes) {
        return nodeRepository.find("from NodeData n where n in (select child from ParentChildRelationship where parent in ?1 and primary = ?2)", nodes, true).list();
    }

    public NodeData save(NodeData node) {
        nodeRepository.persist(node);
        return node;
    }

    public List<NodeData> saveNodes(NodeData... nodes) {
        List<NodeData> list = List.of(nodes);
        nodeRepository.persist(List.of(nodes));
        return list;
    }

    public List<NodeData> saveNodes(List<NodeData> nodes) {
        nodeRepository.persist(nodes);
        return nodes;
    }

    public List<NodeData> saveNodes(Stream<NodeData> nodes) {
        List<NodeData> list = nodes.collect(Collectors.toList());
        nodeRepository.persist(list);
        return list;
    }

    public void savePaths(Stream<NodePath> paths) {
        pathRepository.persist(paths);
    }

    public List<NodePath> savePaths(List<NodePath> paths) {
        pathRepository.persist(paths);
        return paths;
    }

    public void updatePaths(String pathPrefix, String prefixSource, String targetPrefix) {
        pathRepository.update("filePath = FUNCTION('replace', filePath, ?2, ?3) where path like ?1", pathPrefix + "%", prefixSource, targetPrefix);
    }

    public void updatePathsEnabled(String pathPrefix, boolean enabled) {
        pathRepository.update("enabled = ?1 where path like ?2", enabled, pathPrefix + "%");
    }

    public void setPathsEnabled(String pathInfix, String enabled) {
        pathRepository.update("enabled = ?1 where path like ?2", enabled, "%" + pathInfix + "%");
    }

    public void setTransactionWherePathPrefix(ApplicationTransaction tx, String pathPrefix) {
        nodeRepository.update("from NodeData n set n.tx = ?1 where n in (select node from NodePath where path like ?2)", tx, pathPrefix + "%");
    }

    public List<NodeData> findNodesHavingTx(String tenant, String transactionId) {
        return nodeRepository.find("tx.tenant = ?1 and tx.uuid = ?2", tenant, transactionId).list();
    }

}
