package it.doqui.index.ecmengineqs.rest;

import io.quarkus.arc.properties.IfBuildProperty;
import it.doqui.index.ecmengineqs.business.storage.ContentStoreManager;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/files")
@IfBuildProperty(name = "downloader.enabled", stringValue = "true")
public class StoreResource {

    @Inject
    ContentStoreManager contentStoreManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileData(@QueryParam("contentUrl") String contentUrl) throws IOException {
        byte[] buffer = contentStoreManager.readFileWithContentURI(contentUrl);
        return Response
            .ok(buffer)
            .header("Content-Type", "application/octet-stream")
            .build();
    }
}
