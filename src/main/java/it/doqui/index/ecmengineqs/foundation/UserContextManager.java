package it.doqui.index.ecmengineqs.foundation;

import it.doqui.index.ecmengineqs.business.schema.SchemaManager;
import it.doqui.index.ecmengineqs.business.schema.TenantSchema;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UserContextManager {

    @Inject
    SchemaManager schemaManager;

    private ThreadLocal<UserContext> contexts = new ThreadLocal<>();

    public UserContext getContext() {
        return contexts.get();
    }

    public void setContext(UserContext context) {
        contexts.set(context);
    }

    public TenantSchema getSchema() {
        UserContext context = getContext();
        return schemaManager.getTenant(context.getRepository(), context.getTenant());
    }
}
