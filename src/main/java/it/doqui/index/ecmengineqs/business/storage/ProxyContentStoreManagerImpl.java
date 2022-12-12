package it.doqui.index.ecmengineqs.business.storage;

import io.quarkus.arc.properties.IfBuildProperty;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@ApplicationScoped
@IfBuildProperty(name = "proxy.store.enabled", stringValue = "true")
@Slf4j
public class ProxyContentStoreManagerImpl implements ContentStoreManager {

    @ConfigProperty(name = "content-store")
    Map<String,String> contentStoreMap;

    @ConfigProperty(name = "proxy.store.url")
    String remoteStoreUrl;

    @Inject
    UserContextManager userContextManager;

    private final CloseableHttpClient httpClient;

    public ProxyContentStoreManagerImpl() {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(10);
        poolingConnManager.setDefaultMaxPerRoute(10);

        httpClient = HttpClients.custom().setConnectionManager(poolingConnManager).build();
    }

    @Override
    public String getStorePath(String name) {
        return contentStoreMap.get(name);
    }

    @Override
    public File getFileWithContentURI(String contentUrl) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public byte[] readFileWithContentURI(String contentUrl) throws IOException {
        log.info("Retrieving url {}", contentUrl);
        try {
            URI uri = new URIBuilder(remoteStoreUrl)
                .addParameter("contentUrl", contentUrl)
                .build();
            HttpGet req = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(req)) {
                try {
                    int statusCode = response.getStatusLine().getStatusCode();
                    log.debug("Remote Content Store returned: {}", statusCode);
                    if (statusCode != 200) {
                        throw new RuntimeException(String.format("Remote Content Store returned %d http error", statusCode));
                    }

                    HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        throw new RuntimeException("Remote Content Store returned an empty response");
                    }

                    return EntityUtils.toByteArray(entity);
                } finally {
                    EntityUtils.consume(response.getEntity());
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeFileWithContentURI(String contentUrl, byte[] buffer) throws IOException {
        throw new RuntimeException("Not implemented");
    }
}
