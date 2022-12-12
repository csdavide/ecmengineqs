package it.doqui.index.ecmengineqs.business.schema;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

@ApplicationScoped
@Slf4j
public class SchemaProcessor {

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    @PostConstruct
    void init() {
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void parse(TenantSchema schema, String filename) throws ParserConfigurationException, IOException, SAXException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream("models/"+filename)) {
            log.info("Parsing model file {}", filename);
            parseInputStream(schema, is);
        }
    }

    public void parseInputStream(TenantSchema schema, InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        parseNamespaces(schema, doc.getDocumentElement(), "imports", "import");
        parseNamespaces(schema, doc.getDocumentElement(), "namespaces", "namespace");

        String modelName = doc.getDocumentElement().getAttribute("name");
        parseElements(doc.getDocumentElement(), "types", "type", elem -> {
            TypeDescriptor type = parseType(elem);
            type.setModelName(modelName);
            schema.getTypeMap().put(type.getName(), type);
        });

        parseElements(doc.getDocumentElement(), "aspects", "aspect", elem -> {
            AspectDescriptor aspect = parseAspect(elem);
            log.debug("Parsed aspect {}", aspect.getName());
            schema.getAspectMap().put(aspect.getName(), aspect);
        });
    }

    private void parseNamespaces(TenantSchema schema, Element rootElement, String containerNodeName, String childNodeName) {
        NodeList importsList = rootElement.getElementsByTagName(containerNodeName);
        for (int i = 0; i < importsList.getLength(); i++) {
            Element importsElem = (Element) importsList.item(i);
            NodeList importList = importsElem.getElementsByTagName(childNodeName);
            for (int j = 0; j < importList.getLength(); j++) {
                Element importElem = (Element) importList.item(j);
                String uri = importElem.getAttribute("uri");
                String prefix = importElem.getAttribute("prefix");
                if (!StringUtils.isBlank(uri) && !StringUtils.isBlank(prefix)) {
                    schema.getNamespaceMap().put(uri, prefix);
                } else {
                    log.warn("Found empty namespace");
                }
            }
        }
    }

    private void parseElements(Element rootElement, String containerNodeName, String childNodeName, Consumer<Element> consumer) {
        NodeList containersList = rootElement.getElementsByTagName(containerNodeName);
        for (int i = 0; i < containersList.getLength(); i++) {
            Element containersElem = (Element) containersList.item(i);
            NodeList childList = containersElem.getElementsByTagName(childNodeName);
            for (int j = 0; j < childList.getLength(); j++) {
                Element elem = (Element) childList.item(j);
                consumer.accept(elem);
            }
        }
    }

    private TypeDescriptor parseType(Element typeElem) {
        TypeDescriptor type = new TypeDescriptor();
        type.setName(typeElem.getAttribute("name"));

        type.setParent(
            getFirstElementByTag(typeElem, "parent")
                .map(x -> x.getTextContent())
                .orElse(null));

        type.setArchive(
            getFirstElementByTag(typeElem, "archive")
                .map(x -> x.getTextContent())
                .map(x -> Boolean.valueOf(x))
                .orElse(false));

        parseElements(typeElem, "properties", "property", elem -> {
            PropertyDescriptor p = parseProperty(elem);
            type.getProperties().put(p.getName(), p);
        });

        parseElements(typeElem, "mandatory-aspects", "aspect", elem -> {
            type.getMandatoryAspects().add(elem.getTextContent());
        });

        parseElements(typeElem, "associations", "child-association", elem -> {
            //TODO: completare
        });

        parseElements(typeElem, "associations", "association", elem -> {
            //TODO: completare
        });

        return type;
    }

    private AspectDescriptor parseAspect(Element aspectElem) {
        AspectDescriptor aspect = new AspectDescriptor();
        aspect.setName(aspectElem.getAttribute("name"));

        aspect.setParent(
            getFirstElementByTag(aspectElem, "parent")
                .map(x -> x.getTextContent())
                .orElse(null));

        parseElements(aspectElem, "properties", "property", elem -> {
            PropertyDescriptor p = parseProperty(elem);
            aspect.getProperties().put(p.getName(), p);
        });

        parseElements(aspectElem, "associations", "child-association", elem -> {
            //TODO: completare
        });

        parseElements(aspectElem, "associations", "association", elem -> {
            //TODO: completare
        });

        return aspect;
    }

    private PropertyDescriptor parseProperty(Element elem) {
        PropertyDescriptor p = new PropertyDescriptor();
        p.setName(elem.getAttribute("name"));
        p.setTypeName(
            getFirstElementByTag(elem, "type")
                .map(x -> x.getTextContent())
                .orElse(null));
        p.setMandatory(
            getFirstElementByTag(elem, "mandatory")
                .map(x -> x.getTextContent())
                .map(y -> Boolean.valueOf(y))
                .orElse(false));
        p.setMultiple(
            getFirstElementByTag(elem, "multiple")
                .map(x -> x.getTextContent())
                .map(y -> Boolean.valueOf(y))
                .orElse(false));
        p.setIndexed(
            getFirstElementByTag(elem, "index")
                .map(x -> x.getAttribute("enabled"))
                .map(y -> Boolean.valueOf(y))
                .orElse(false));

        return p;
    }

    private void parseAssociation(Element elem) {
        //TODO: parsing association
    }

    private Optional<Element> getFirstElementByTag(Element parentElem, String name) {
        NodeList list = parentElem.getElementsByTagName(name);
        return  (list != null && list.getLength() > 0) ? Optional.of((Element) list.item(0)) : Optional.empty();
    }

}
