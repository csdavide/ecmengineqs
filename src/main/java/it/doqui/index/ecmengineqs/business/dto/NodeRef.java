package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.foundation.NodeReferenceable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;

import static it.doqui.index.ecmengineqs.foundation.Constants.WORKSPACE;

@Getter
@Setter
@ToString
public class NodeRef implements NodeReferenceable {

    private String tenant;
    private String uuid;

    @Override
    public URI getURI() {
        try {
            return new URI(String.format("%s://@%s@SpacesStore/%s", WORKSPACE, tenant, uuid));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
