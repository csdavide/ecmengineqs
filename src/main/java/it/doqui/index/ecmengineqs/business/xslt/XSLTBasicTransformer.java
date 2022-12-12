package it.doqui.index.ecmengineqs.business.xslt;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class XSLTBasicTransformer implements XSLTTransformer {

    @Override
    public String getDefaultMimeType() {
        return "text/html";
    }
}
