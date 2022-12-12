package it.doqui.index.ecmengineqs.business.xslt;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;

@ApplicationScoped
public class XSLTFactory {

    @Inject
    XSLTBasicTransformer basicTransformer;

    @Inject
    XSLTFOPTransformer fopTransformer;

    public XSLTTransformer getXSLT(File f) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(f);
            final NodeList nlist = doc.getElementsByTagName("xsl:stylesheet");
            final Node stylesheet = nlist.item(0);
            final NamedNodeMap nnm = stylesheet.getAttributes();
            for (int x = 0; x < nnm.getLength(); x++) {
                if (nnm.item(x).getNodeName().equalsIgnoreCase("xmlns:fo")) {
                    return fopTransformer;
                }
            }

            return basicTransformer;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find XSLT", e);
        }
    }

    public XSLTTransformer getXSLT(byte[] buffer) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try (ByteArrayInputStream is = new ByteArrayInputStream(buffer)) {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(is);
            final NodeList nlist = doc.getElementsByTagName("xsl:stylesheet");
            final Node stylesheet = nlist.item(0);
            final NamedNodeMap nnm = stylesheet.getAttributes();
            for (int x = 0; x < nnm.getLength(); x++) {
                if (nnm.item(x).getNodeName().equalsIgnoreCase("xmlns:fo")) {
                    return fopTransformer;
                }
            }

            return basicTransformer;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find XSLT", e);
        }
    }
}
