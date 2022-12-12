package it.doqui.index.ecmengineqs.business.storage;

import io.quarkus.arc.DefaultBean;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@ApplicationScoped
@DefaultBean
public class DefaultContentStoreManagerImpl implements ContentStoreManager {

    @ConfigProperty(name = "content-store")
    Map<String,String> contentStoreMap;

    @Override
    public String getStorePath(String name) {
        return contentStoreMap.get(name);
    }

    @Override
    public File getFileWithContentURI(String contentUrl) {
        try {
            URI uri = new URI(contentUrl);
            String path = contentStoreMap.get(uri.getScheme());
            if (path == null) {
                throw new RuntimeException("Invalid content store in url " + uri);
            }

            File f = new File(String.format("%s/%s%s", path, uri.getAuthority(),uri.getPath()));
            if (!f.exists()) {
                throw new RuntimeException("Unable to locale file " + f.getPath());
            }

            return f;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readFileWithContentURI(String contentUrl) throws IOException {
        File f = getFileWithContentURI(contentUrl);
        try (InputStream is = new FileInputStream(f)) {
            return is.readAllBytes();
        }
    }

    @Override
    public void writeFileWithContentURI(String contentUrl, byte[] buffer) throws IOException {
        File f = getFileWithContentURI(contentUrl);
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(buffer);
        }
    }

}
