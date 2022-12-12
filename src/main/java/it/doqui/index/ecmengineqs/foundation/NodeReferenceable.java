package it.doqui.index.ecmengineqs.foundation;

import java.net.URI;

public interface NodeReferenceable {
    String getTenant();
    String getUuid();
    URI getURI();
}
