package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.integration.indexing.IndexingOperationSet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TxResponse<T> {

    private IndexingOperationSet operationSet;
    private boolean asyncRequired;
    private T result;

}
