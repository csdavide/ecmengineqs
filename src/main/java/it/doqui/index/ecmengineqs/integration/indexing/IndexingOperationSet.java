package it.doqui.index.ecmengineqs.integration.indexing;

import it.doqui.index.ecmengineqs.business.entities.ApplicationTransaction;
import it.doqui.index.ecmengineqs.foundation.NodeReferenceable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class IndexingOperationSet {

    private String tenant;
    private String txId;
    private final List<IndexingOperation> operations;

    public IndexingOperationSet() {
        this.operations = new ArrayList<>();
    }

    public IndexingOperationSet(ApplicationTransaction tx) {
        this();
        this.tenant = tx.getTenant();
        this.txId = tx.getUuid();
    }

    public IndexingOperationSet add(IndexingOperationType type, NodeReferenceable ref) {
        IndexingOperation op = new IndexingOperation();
        op.setType(type);
        op.setRef(ref);
        this.operations.add(op);
        return this;
    }

    public IndexingOperationSet addAll(IndexingOperationType type, Collection<? extends NodeReferenceable> refs) {
        for (NodeReferenceable ref : refs) {
            add(type, ref);
        }
        return this;
    }

}
