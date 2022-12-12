package it.doqui.index.ecmengineqs.business.converters;

import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.dto.ContentProperty;
import it.doqui.index.ecmengineqs.business.dto.PropertyContainer;
import it.doqui.index.ecmengineqs.business.dto.Relationship;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.schema.AspectDescriptor;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_SYS_REFERENCEABLE;

@ApplicationScoped
@Slf4j
public class NodeMapper {

    @Inject
    ModelService modelService;

    @Inject
    PropertyConverter propertyConverter;

    public NodeData mapFields(ContentNode c) {
        NodeData n = new NodeData();
        n.setId(c.getId());
        n.setTenant(c.getTenant());
        n.setKind(c.getKind());
        n.setUuid(c.getUuid());
        n.setTypeName(c.getType().getName());

        List<String> aspects = new ArrayList<>();
        aspects.addAll(c.getAspects().keySet());
        aspects.add(ASPECT_SYS_REFERENCEABLE);
        n.getData().put("aspects", aspects);

        Map<String,Object> properties = new HashMap<>();
        c.getProperties().values().forEach(pc -> properties.put(pc.getDescriptor().getName(), pc.getValue()));
        if (c.getContentProperty() != null) {
            properties.put(c.getContentProperty().getName(), c.getContentProperty().toString());
        }
        n.getData().put("properties", properties);

        return n;
    }

    public ContentNode map(NodeData node) {
        return map(node, null);
    }

    public ContentNode map(NodeData node, Collection<String> filterInPropertyNames) {
        TypeDescriptor type = modelService.getType(node.getTypeName());
        if (type == null) {
            throw new RuntimeException(String.format("Missing type %s in the model", node.getTypeName()));
        }

        ContentNode contentNode = new ContentNode();
        contentNode.setId(node.getId());
        contentNode.setKind(node.getKind());
        contentNode.setTenant(node.getTenant());
        contentNode.setUuid(node.getUuid());
        contentNode.setType(type);
        contentNode.setModelName(type.getModelName());

        processType(type, contentNode, node, filterInPropertyNames);
        processAspects(contentNode, node, filterInPropertyNames);

        for (Map.Entry<String, PropertyContainer> entry : contentNode.getProperties().entrySet()) {
            PropertyContainer pc = entry.getValue();
            if (pc.getValue() instanceof ContentProperty) {
                contentNode.setContentProperty((ContentProperty) pc.getValue());
                break;
            }
        }

        node.getParents().stream().findFirst().ifPresent(parent -> {
            if (parent.isPrimary()) {
                Relationship primaryParent = new Relationship();
                primaryParent.setId(parent.getId());
                primaryParent.setTypeName(parent.getTypeName());
                primaryParent.setName(parent.getName());
                contentNode.setPrimaryParent(primaryParent);
            }
        });
        return contentNode;
    }

    private void processAspects(ContentNode contentNode, NodeData node, Collection<String> filterInPropertyNames) {
        List<String> aspects = (List<String>) node.getData().get("aspects");
        if (aspects != null) {
            for (String a : aspects) {
                processAspect(a, contentNode, node, filterInPropertyNames);
            }
        }
    }

    private void processAspect(String a, ContentNode contentNode, NodeData node, Collection<String> filterInPropertyNames) {
        log.warn("Processing aspect {}", a);
        AspectDescriptor ad = modelService.getAspect(a);
        if (ad == null) {
            throw new RuntimeException(String.format("Missing aspect %s in the model", a));
        }

        ad.getProperties()
            .values()
            .stream()
            .filter(p -> filterInPropertyNames == null || filterInPropertyNames.contains(p.getName()))
            .forEach(pd -> contentNode.getProperties().put(pd.getName(), propertyConverter.convertProperty(pd, node.getProperty(pd.getName()))));
        contentNode.getAspects().put(ad.getName(), ad);

        String parent = ad.getParent();
        if (parent != null) {
            processAspect(parent, contentNode, node, filterInPropertyNames);
        }
    }

    private void processType(TypeDescriptor type, ContentNode contentNode, NodeData node, Collection<String> filterInPropertyNames) {
        log.debug("Processing type {}", type.getName());
        type.getProperties()
            .values()
            .stream()
            .filter(p -> filterInPropertyNames == null || filterInPropertyNames.contains(p.getName()))
            .forEach(pd -> contentNode.getProperties().put(pd.getName(), propertyConverter.convertProperty(pd, node.getProperty(pd.getName()))));

        String parent = type.getParent();
        if (parent != null) {
            TypeDescriptor parentType = modelService.getType(parent);
            if (parentType == null) {
                throw new RuntimeException(String.format("Missing type %s in the model", parent));
            }

            processType(parentType, contentNode, node, filterInPropertyNames);
        }
    }

}
