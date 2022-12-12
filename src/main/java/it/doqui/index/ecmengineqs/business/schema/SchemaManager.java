package it.doqui.index.ecmengineqs.business.schema;

import it.doqui.index.ecmengineqs.business.repositories.CustomModelRepository;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class SchemaManager {

    public static final String DEFAULT = "default";

    @Inject
    SchemaProcessor schemaProcessor;

    @Inject
    CustomModelRepository customModelRepository;

    private Map<String,TenantSchema> tenants = new ConcurrentHashMap<>();

    public TenantSchema getDefault() {
        return this.tenants.get(DEFAULT);
    }

    public TenantSchema getTenant(String repository, String name) {
        String key = String.format("%s:%s", repository, name);
        TenantSchema schema = this.tenants.get(key);
        if (schema == null) {
            schema = loadTenant(repository, name);
            this.tenants.put(key, schema);
        }
        return schema;
    }

    @PostConstruct
    void init() {
        this.tenants.put(DEFAULT, loadTenant(null, DEFAULT));
    }

    private synchronized TenantSchema loadTenant(String repository, String name) {
        TenantSchema schema = new TenantSchema();
        schema.setTenant(name);
        customModelRepository.find("tenant = ?1 and is_active is true", name).stream().forEach(c -> {
            if (c.getData() != null) {
                try {
                    schemaProcessor.parseInputStream(schema, new ByteArrayInputStream(c.getData().getBytes(StandardCharsets.UTF_8)));
                    log.info("Custom model {} on tenant {} successfully loaded", c.getFilename(), c.getTenant());
                } catch (Exception e) {
                    log.error(String.format("Unable to parse custom model %s (id %d) on tenant %s: %s", c.getFilename(), c.getId(), c.getTenant(), e.getMessage()), e);
                }
            }
        });
        return schema;
    }

}
