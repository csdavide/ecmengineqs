package it.doqui.index.ecmengineqs.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/version")
public class VersionResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackageVersion() throws Exception {
        ClassLoader cl = this.getClass().getClassLoader();
        ObjectMapper objectMapper = new ObjectMapper();
        return Response.ok(objectMapper.readValue(cl.getResourceAsStream("git.json"), Map.class)).build();
    }
}
