package it.doqui.index.ecmengineqs.integration.indexing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.doqui.index.ecmengineqs.business.dto.NodeRef;
import it.doqui.index.ecmengineqs.foundation.NodeReferenceable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IndexingOperation {

    private IndexingOperationType type;

    @JsonDeserialize(as = NodeRef.class)
    private NodeReferenceable ref;

}
