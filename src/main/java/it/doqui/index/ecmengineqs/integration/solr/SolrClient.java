package it.doqui.index.ecmengineqs.integration.solr;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static it.doqui.index.ecmengineqs.foundation.Constants.WORKSPACE;

@ApplicationScoped
@Slf4j
public class SolrClient {

    private CloseableHttpClient httpClient = null;

    @ConfigProperty(name = "solr.endpoint")
    String endpoint;

    @ConfigProperty(name = "solr.username")
    Optional<String> username;

    @ConfigProperty(name = "solr.password")
    Optional<String> password;

    @ConfigProperty(name = "solr.maxConnection", defaultValue = "100")
    int maxConnection;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserContextManager userContextManager;

    @PostConstruct
    void init() {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(maxConnection);
        poolingConnManager.setDefaultMaxPerRoute(maxConnection);

        httpClient = HttpClients.custom().setConnectionManager(poolingConnManager).build();
    }

    public String collectionName() {
        String tenant = userContextManager.getContext().getTenant().replace("@", "-");
        if (tenant.startsWith("-")) {
            tenant = tenant.substring(1);
        }

        String repository = userContextManager.getContext().getRepository();
        return String.format("%s_%s_%s-%s", repository, WORKSPACE, tenant, "SpacesStore").toLowerCase();
    }

    public SolrResponse update(String collectionName, List<Map<String,Object>> documents) throws IOException {
        HttpPost req = createPostRequest(String.format("%s/%s/update/json/docs?commit=true", endpoint, collectionName), null);
        req.setEntity(new StringEntity(objectMapper.writeValueAsString(documents), ContentType.APPLICATION_JSON));
        return objectMapper.readValue(execute(req), SolrResponse.class);
    }

    public SolrResponse delete(String collectionName, Map<String,Object> doc) throws IOException {
        HttpPost req = createPostRequest(String.format("%s/%s/update/json?commit=true", endpoint, collectionName), null);
        req.setEntity(new StringEntity(objectMapper.writeValueAsString(doc), ContentType.APPLICATION_JSON));
        return objectMapper.readValue(execute(req), SolrResponse.class);
    }

    public SolrResponse find(String collectionName, String query, String fl, String sort, Long offset, Long limit) throws IOException {
        HttpPost req = createPostRequest(String.format("%s/%s/select", endpoint, collectionName), "application/x-www-form-urlencoded");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("q", query));
        params.add(new BasicNameValuePair("wt", "json"));
        params.add(new BasicNameValuePair("indent", "true"));

        if (fl != null) {
            params.add(new BasicNameValuePair("fl", "ID"));
        }

        if (offset != null) {
            params.add(new BasicNameValuePair("start", String.valueOf(offset)));
        }

        if (limit != null) {
            params.add(new BasicNameValuePair("rows", String.valueOf(limit)));
        }

        if (sort != null) {
            params.add(new BasicNameValuePair("sort", sort));
        }

        req.setEntity(new UrlEncodedFormEntity(params));
        return objectMapper.readValue(execute(req), SolrResponse.class);
    }

    private HttpPost createPostRequest(String uri, String contentType) {
        log.debug("Preparing request to {} content-type: '{}'", uri, contentType);
        HttpPost req = new HttpPost(uri);
        if (contentType != null) {
            req.setHeader("Content-Type", contentType);
        }

        if (username.isPresent() && password.isPresent()) {
            req.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", username.get(), password.get()).getBytes()));
        }

        return req;
    }

    private String execute(HttpPost req) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(req)) {
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                log.debug("SOLR returned: {}", statusCode);

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new RuntimeException("Solr returned an empty response");
                }

                String payload = EntityUtils.toString(entity);
                log.debug("SOLR payload: {}", payload);

                if (statusCode != 200) {
                    throw new RuntimeException(String.format("Solr returned %d http error", statusCode));
                }

                return payload;
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        }
    }
}
