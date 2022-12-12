package it.doqui.index.ecmengineqs.business.xslt;

import it.doqui.index.ecmengineqs.business.converters.NodeMapper;
import it.doqui.index.ecmengineqs.business.converters.PropertyConverter;
import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.dto.ContentProperty;
import it.doqui.index.ecmengineqs.business.dto.RenditionDTO;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.exceptions.BadDataException;
import it.doqui.index.ecmengineqs.business.exceptions.DataIOException;
import it.doqui.index.ecmengineqs.business.exceptions.NotFoundException;
import it.doqui.index.ecmengineqs.business.services.NodeService;
import it.doqui.index.ecmengineqs.business.storage.ContentStoreManager;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.*;
import static it.doqui.index.ecmengineqs.foundation.Constants.WORKSPACE;

@ApplicationScoped
@Slf4j
public class XSLTService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    NodeService nodeService;

    @Inject
    PropertyConverter propertyConverter;

    @Inject
    NodeMapper nodeMapper;

    @Inject
    ContentStoreManager contentStoreManager;

    @Inject
    XSLTFactory xsltFactory;

    public List<RenditionDTO> findRenditionTransformers(String uuid) {
        String tenant = userContextManager.getContext().getTenant();
        NodeData n = nodeService
            .findNodeByUUID(tenant, uuid)
            .orElseThrow(() -> new NotFoundException(uuid));

        List<String> xslIds = (List<String>) n.getProperty(PROP_ECMSYS_XSLID);
        if (xslIds != null && !xslIds.isEmpty()) {
            Map<String,NodeData> nodeMap = nodeService.mapNodesInUUIDs(tenant, xslIds, WORKSPACE);
            if (nodeMap.size() != xslIds.size()) {
                for (String xslId : xslIds) {
                    if (!nodeMap.containsKey(xslId)) {
                        throw new NotFoundException(xslId);
                    }
                }
            }

            return nodeMap
                .values()
                .stream()
                .map(r -> nodeMapper.map(r))
                .map(c -> mapAsRenditionTransformer(c))
                .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public Optional<RenditionDTO> getRenditionTransformer(String uuid) {
        return nodeService
            .findNodeByUUID(userContextManager.getContext().getTenant(), uuid)
            .map(n -> nodeMapper.map(n))
            .map(c -> mapAsRenditionTransformer(c));
    }

    public RenditionDTO getRenditionDocument(String uuid, String contentPropertyName) {
        String tenant = userContextManager.getContext().getTenant();
        NodeData n = nodeService
            .findNodeByUUID(tenant, uuid, WORKSPACE)
            .orElseThrow(() -> new NotFoundException(uuid));

        String renditionId = ObjectUtils.getAsString(n.getProperty(PROP_ECMSYS_RENDITIONID));
        if (renditionId == null) {
            throw new BadDataException(String.format("Node %s is not a rendition transformer"));
        }

        log.debug("Processing rendition {}", renditionId);
        return nodeService
            .findNodeByUUID(tenant, renditionId, WORKSPACE)
            .map(rn -> {
                RenditionDTO r = new RenditionDTO();
                r.setUuid(n.getUuid());
                r.setRenditionId(rn.getUuid());
                r.setDescription(ObjectUtils.getAsString(rn.getProperty(PROP_ECMSYS_RENDITION_DESCRIPTION)));

                String contentPropertyValue = ObjectUtils.getAsString(rn.getProperty(contentPropertyName));
                if (contentPropertyValue == null) {
                    throw new BadDataException(String.format("Node %s has not a content property with name '%s'", rn.getUuid(), contentPropertyName));
                }

                ContentProperty contentProperty = new ContentProperty();
                contentProperty.parseAttributes(contentPropertyValue);

                try {
                    String contentUrl = contentProperty.getAttribute(CONTENT_ATTR_URL);
                    if (contentUrl == null) {
                        throw new BadDataException(String.format("Property %s is not a content property for node %s", contentPropertyName, rn.getUuid()));
                    }

                    r.setBinaryData(contentStoreManager.readFileWithContentURI(contentUrl));
                } catch (IOException e) {
                    throw new DataIOException(e.getMessage());
                }

                return r;
            })
            .orElseThrow(() -> new NotFoundException(renditionId));
    }

    private RenditionDTO mapAsRenditionTransformer(ContentNode c) {
        ContentProperty contentProperty = c.getContentProperty();
        if (contentProperty == null) {
            throw new RuntimeException(String.format("Node %s has not any content property", c.getUuid()));
        }

        // RenditionTransformer
        RenditionDTO r = new RenditionDTO();
        r.setUuid(c.getUuid());
        r.setDescription(c.getPropertyAsString(PROP_ECMSYS_TRANSFORMER_DESCRIPTION));

        try {
            String contentUrl = contentProperty.getAttribute(CONTENT_ATTR_URL);
            r.setBinaryData(contentStoreManager.readFileWithContentURI(contentUrl));
        } catch (IOException e) {
            throw new DataIOException(e.getMessage());
        }

        String genMimeType = c.getPropertyAsString(PROP_ECMSYS_GENMIMETYPE);
        if (StringUtils.isBlank(genMimeType)) {
            XSLTTransformer t = xsltFactory.getXSLT(r.getBinaryData());
            genMimeType = t.getDefaultMimeType();
        }
        r.setGenMimeType(genMimeType);
        r.setRenditionId(c.getPropertyAsString(PROP_ECMSYS_RENDITIONID));
        r.setMimeType(c.getContentProperty().getAttribute("mimetype"));

        return r;
    }

}
