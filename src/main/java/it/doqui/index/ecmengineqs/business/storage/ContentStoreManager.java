package it.doqui.index.ecmengineqs.business.storage;

import java.io.File;
import java.io.IOException;

public interface ContentStoreManager {

    String getStorePath(String name);
    File getFileWithContentURI(String contentUrl);
    byte[] readFileWithContentURI(String contentUrl) throws IOException;
    void writeFileWithContentURI(String contentUrl, byte[] buffer) throws IOException;

}
