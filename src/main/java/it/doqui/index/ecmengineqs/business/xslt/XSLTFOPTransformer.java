package it.doqui.index.ecmengineqs.business.xslt;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class XSLTFOPTransformer implements XSLTTransformer {

    @Override
    public String getDefaultMimeType() {
        return "application/pdf";
    }
}
