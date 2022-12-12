package it.doqui.index.ecmengineqs.integration.integrity;

import it.doqui.index.ecmengineqs.business.services.TenantService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class IntegrityService {

    public static int STATUS_OK       = 0x0000;
    public static int STATUS_KB_DB    = 0x0001;
    public static int STATUS_KB_SORL  = 0x0010;
    public static int STATUS_KO_ALL   = 0x1111;

    @Inject
    TenantService tenantService;

    public int systemStatus() {
        int status = STATUS_OK;
        try {
            tenantService.getAllTenants();
        } catch (Throwable e) {
            status |= STATUS_KB_DB;
        }

        //TODO: verificare SOLR

        return status;
    }
}
