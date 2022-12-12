package it.doqui.index.ecmengineqs.rest;

import it.doqui.index.ecmengineqs.integration.indexing.IndexerDelegate;
import it.doqui.index.ecmengineqs.integration.indexing.IndexingOperationSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/indexer/operations")
@Slf4j
public class IndexingResource {

    @Inject
    IndexerDelegate indexerDelegate;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addIndexingOperation(IndexingOperationSet operationSet) {
        log.debug("Got a request to index operationSet: {}", operationSet);
        if (StringUtils.isBlank(operationSet.getTxId())) {
            indexerDelegate.submitIndexAll(operationSet.getTenant());
        } else {
            indexerDelegate.submit(operationSet, true);
        }

        return Response.accepted().build();
    }

}
