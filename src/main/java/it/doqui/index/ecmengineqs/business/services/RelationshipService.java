package it.doqui.index.ecmengineqs.business.services;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import it.doqui.index.ecmengineqs.business.entities.NodeAssociation;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.entities.ParentChildRelationship;
import it.doqui.index.ecmengineqs.business.repositories.NodeAssociationRepository;
import it.doqui.index.ecmengineqs.business.repositories.ParentChildRelationshipRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RelationshipService {

    @Inject
    ParentChildRelationshipRepository parentChildRelationshipRepository;

    @Inject
    NodeAssociationRepository associationRepository;

    public ParentChildRelationship save(ParentChildRelationship relationship) {
        parentChildRelationshipRepository.persist(relationship);
        return relationship;
    }

    public List<ParentChildRelationship> save(List<ParentChildRelationship> relationships) {
        parentChildRelationshipRepository.persist(relationships);
        return relationships;
    }

    public NodeAssociation save(NodeAssociation association) {
        associationRepository.persist(association);
        return association;
    }

    public PanacheQuery<ParentChildRelationship> getParents(NodeData node, Collection<String> filterType) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("child = :node");
        params.put("node", node);

        conditions.add("parent.kind = :kind");
        params.put("kind", node.getKind());

        if (filterType != null && !filterType.isEmpty()) {
            conditions.add("parent.typeName in :filterType");
            params.put("filterType", filterType);
        }

        return parentChildRelationshipRepository.find(conditions.stream().collect(Collectors.joining(" and ")), params);
    }

    public PanacheQuery<ParentChildRelationship> getChildren(NodeData node, Collection<String> filterType) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("parent = :node");
        params.put("node", node);

        conditions.add("child.kind = :kind");
        params.put("kind", node.getKind());

        if (filterType != null && !filterType.isEmpty()) {
            conditions.add("child.typeName in :filterType");
            params.put("filterType", filterType);
        }

        return parentChildRelationshipRepository.find(conditions.stream().collect(Collectors.joining(" and ")), params);
    }

    public PanacheQuery<NodeAssociation> getOriginatedAssociations(NodeData node, Collection<String> filterType) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("source = :node");
        params.put("node", node);

        conditions.add("target.kind = :kind");
        params.put("kind", node.getKind());

        if (filterType != null && !filterType.isEmpty()) {
            conditions.add("target.typeName in :filterType");
            params.put("filterType", filterType);
        }

        return associationRepository.find(conditions.stream().collect(Collectors.joining(" and ")), params);
    }

    public PanacheQuery<NodeAssociation> getTerminatedAssociations(NodeData node, Collection<String> filterType) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("target = :node");
        params.put("node", node);

        conditions.add("source.kind = :kind");
        params.put("kind", node.getKind());

        if (filterType != null && !filterType.isEmpty()) {
            conditions.add("source.typeName in :filterType");
            params.put("filterType", filterType);
        }

        return associationRepository.find(conditions.stream().collect(Collectors.joining(" and ")), params);
    }
}
