package it.doqui.index.ecmengineqs.business.schema;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
@Startup
@Slf4j
public class SchemaInitializer {

    @Inject
    SchemaManager schemaManager;

    void onStart(@Observes StartupEvent ev) {
        log.debug("Loaded namespaces {}", schemaManager.getDefault().getNamespaceMap());
    }
}
