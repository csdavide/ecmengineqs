package it.doqui.index.ecmengineqs.business.converters;

import it.doqui.index.ecmengine.mtom.dto.AclRecord;
import it.doqui.index.ecmengine.mtom.dto.Aspect;
import it.doqui.index.ecmengine.mtom.dto.Content;
import it.doqui.index.ecmengine.mtom.dto.Property;
import it.doqui.index.ecmengineqs.business.dto.*;
import it.doqui.index.ecmengineqs.business.exceptions.BadDataException;
import it.doqui.index.ecmengineqs.business.schema.AspectDescriptor;
import it.doqui.index.ecmengineqs.business.schema.PropertyDescriptor;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import it.doqui.index.ecmengineqs.foundation.Localizable;
import it.doqui.index.ecmengineqs.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class ContentConverter {

    @Inject
    ModelService modelService;

    @Inject
    PropertyConverter propertyConverter;

    public AclData convertACLs(AclRecord[] acls, Boolean inherited) {
        AclData result = new AclData();
        result.setInherits(inherited);
        for (AclRecord acl : acls) {
            AclPermission permission = AclPermission.valueOf(acl.getPermission());
            result.getGrants().put(acl.getAuthority(), permission);
        }
        return result;
    }

    public ContentMetadata getAsContentMetadata(Content c) {
        ContentMetadata cm = new ContentMetadata();
        cm.setUuid(c.getUid());
        cm.setTypeName(c.getTypePrefixedName());
        Arrays.stream(c.getProperties()).forEach(p -> {
            List<String> values = (p.getValues() == null || p.getValues().length == 0)
                ? null
                : Arrays.asList(p.getValues());
            cm.getProperties().put(p.getPrefixedName(), values);
        });

        Arrays.stream(c.getAspects()).forEach(a -> {
            cm.getAspects().add(a.getPrefixedName());
            Arrays.stream(a.getProperties()).forEach(p -> {
                List<String> values = (p.getValues() == null || p.getValues().length == 0)
                    ? null
                    : Arrays.asList(p.getValues());
                cm.getProperties().put(p.getPrefixedName(), values);
            });
        });
        return cm;
    }

    public ContentNode getAsContentNode(ContentMetadata cm) {
        TypeDescriptor type = modelService.getType(cm.getTypeName());
        if (type == null) {
            throw new BadDataException(String.format("Missing type %s in the model", cm.getTypeName()));
        }

        ContentNode n = new ContentNode();
        n.setUuid(cm.getUuid());
        n.setType(type);

        processType(type, n, cm.getProperties());
        cm.getAspects().forEach(a -> {
            AspectDescriptor ad = modelService.getAspect(a);
            if (ad == null) {
                throw new BadDataException(String.format("Missing aspect %s in the model", a));
            }

            n.getAspects().put(ad.getName(), ad);
            ad.getProperties().values().forEach(pd -> fillProperty(n, pd, cm.getProperties()));
        });

        if (!cm.getProperties().isEmpty()) {
            throw new BadDataException("Unknown properties: " + cm.getProperties().keySet());
        }

        return n;
    }

    public ContentNode getAsContentNode(Content c) {
        TypeDescriptor type = modelService.getType(c.getTypePrefixedName());
        if (type == null) {
            throw new BadDataException(String.format("Missing type %s in the model", c.getTypePrefixedName()));
        }

        ContentNode n = new ContentNode();
        n.setUuid(c.getUid());
        n.setModelName(c.getModelPrefixedName());
        n.setType(type);

        Relationship parent = new Relationship();
        parent.setName(c.getPrefixedName());
        parent.setTypeName(c.getParentAssocTypePrefixedName());
        n.setPrimaryParent(parent);

        if (!StringUtils.isBlank(c.getContentPropertyPrefixedName())) {
            ContentProperty cm = new ContentProperty();
            cm.setName(c.getContentPropertyPrefixedName());
            cm.setAttribute("mimetype", c.getMimeType());
            cm.setAttribute("encoding", c.getEncoding());
            cm.setAttribute("locale", Locale.getDefault().toString());
            if (c.getContent() != null) {
                cm.setAttribute("size", String.valueOf(c.getContent().length));
            }

            n.setContentProperty(cm);
        }

        Map<String, List<String>> propertyMap = new HashMap<>();
        if (c.getProperties() != null) {
            for (Property p : c.getProperties()) {
                propertyMap.put(p.getPrefixedName(), ObjectUtils.asList(p.getValues()));
            }
        }

        if (c.getAspects() != null) {
            for (Aspect a : c.getAspects()) {
                AspectDescriptor ad = modelService.getAspect(a.getPrefixedName());
                if (ad == null) {
                    throw new BadDataException(String.format("Missing aspect %s in the model", a.getPrefixedName()));
                }

                n.getAspects().put(ad.getName(), ad);
                if (a.getProperties() != null) {
                    for (Property p : a.getProperties()) {
                        propertyMap.put(p.getPrefixedName(), ObjectUtils.asList(p.getValues()));
                    }
                }
            }
        }

        processType(type, n, propertyMap);
        for (AspectDescriptor ad : n.getAspects().values()) {
            processAspect(ad, n, propertyMap);
        }

        if (!propertyMap.isEmpty()) {
            throw new BadDataException("Unknown properties: " + propertyMap.keySet());
        }

        return n;
    }

    private void fillProperty(ContentNode n, PropertyDescriptor pd, Map<String, List<String>> propertyMap) {
        if (propertyMap.containsKey(pd.getName())) {
            List<String> values = propertyMap.get(pd.getName());
            n.getProperties().put(pd.getName(), convertProperty(pd, values));
            propertyMap.remove(pd.getName());
        }
    }

    private void processType(TypeDescriptor type, ContentNode n, Map<String, List<String>> propertyMap) {
        log.debug("Processing type {}", type.getName());
        type.getProperties().values().forEach(pd -> fillProperty(n, pd, propertyMap));

        String parent = type.getParent();
        if (parent != null) {
            TypeDescriptor parentType = modelService.getType(parent);
            if (parentType == null) {
                throw new BadDataException(String.format("Missing type %s in the model", parent));
            }

            processType(parentType, n, propertyMap);
        }
    }

    private void processAspect(AspectDescriptor ad, ContentNode n, Map<String, List<String>> propertyMap) {
        ad.getProperties().values().forEach(pd -> fillProperty(n, pd, propertyMap));

        String parent = ad.getParent();
        if (parent != null) {
            AspectDescriptor parentAspect = modelService.getAspect(parent);
            if (parentAspect == null) {
                throw new BadDataException(String.format("Missing aspect %s in the model", parent));
            }

            processAspect(parentAspect, n, propertyMap);
        }
    }

    private PropertyContainer convertProperty(PropertyDescriptor pd, List<String> values) {
        Object value;
        if (values == null) {
            value = null;
        } else if (pd.isMultiple()) {
            value = values;
        } else if (!values.isEmpty()) {
            value = values.get(0);
        } else {
            value = null;
        }

        return propertyConverter.convertProperty(pd, value);
    }

    public Content getAsContent(ContentNode n) {
        Content c = new Content();
        c.setUid(n.getUuid());
        c.setModelPrefixedName(n.getModelName());

        Locale locale;
        ContentProperty cm = n.getContentProperty();
        if (cm != null) {
            locale = cm.getLocale();
            c.setSize(NumberUtils.toLong(cm.getAttribute("size"), 0));
            c.setMimeType(cm.getAttribute("mimetype"));
            c.setEncoding(cm.getAttribute("encoding"));
            c.setContentPropertyPrefixedName(cm.getName());
        } else {
            locale = Locale.getDefault();
        }

        // properties
        List<Property> properties = n.getProperties().entrySet().stream()
            .map(entry -> {
                PropertyContainer pc = entry.getValue();

                Property p = new Property();
                p.setPrefixedName(entry.getKey());
                p.setMultivalue(pc.getDescriptor().isMultiple());

                if (pc.getValue() == null) {
                    p.setValues(new String[1]);
                } else {
                    List<String> values = new ArrayList<>();
                    if (pc.getValue() instanceof Collection) {
                        Collection<Object> collection = (Collection<Object>) pc.getValue();
                        for (Object obj : collection) {
                            fillPropertyValue(locale, values, obj);
                        }
                    } else {
                        fillPropertyValue(locale, values, pc.getValue());
                    }
                    p.setValues(values.toArray(new String[0]));
                }

                return p;
            })
            .collect(Collectors.toList());
        c.setProperties(properties.toArray(new Property[0]));

        // aspects
        List<Aspect> aspects = n.getAspects().values().stream()
            .map(a -> new Aspect(a.getName(), null))
            .collect(Collectors.toList());
        c.setAspects(aspects.toArray(new Aspect[0]));

        // primary parent
        if (n.getPrimaryParent() != null) {
            c.setParentAssocTypePrefixedName(n.getPrimaryParent().getTypeName());
            c.setPrefixedName(n.getPrimaryParent().getName());
        }

        return c;
    }

    private void fillPropertyValue(Locale locale, List<String> values, Object obj) {
        if (obj != null) {
            if (obj instanceof Localizable) {
                Object localizedObj = ((Localizable) obj).getLocalizedValue(locale);
                if (localizedObj != null) {
                    values.add(localizedObj.toString());
                }
            } else {
                values.add(obj.toString());
            }
        }
    }
}
