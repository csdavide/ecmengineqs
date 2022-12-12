package it.doqui.index.ecmengineqs.business.converters;

import it.doqui.index.ecmengineqs.business.dto.ContentProperty;
import it.doqui.index.ecmengineqs.business.dto.MLTextProperty;
import it.doqui.index.ecmengineqs.business.dto.PropertyContainer;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.schema.PropertyDescriptor;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import it.doqui.index.ecmengineqs.utils.I18NUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class PropertyConverter {

    public static final String TYPE_CONTENT = "d:content";

    @Inject
    ModelService modelService;

    public PropertyContainer getContentProperty(NodeData n) {
        Map<String, Object> properties = (Map<String, Object>) n.getData().get("properties");
        return getContentProperty(properties, n.getTypeName());
    }

    private PropertyContainer getContentProperty(Map<String, Object> properties, String typeName) {
        if (typeName == null) {
            return null;
        }

        TypeDescriptor type = modelService.getType(typeName);
        if (type == null) {
            throw new RuntimeException(String.format("Missing type %s in the model", typeName));
        }

        return type.getProperties()
            .values()
            .stream()
            .filter(p -> StringUtils.equals(p.getTypeName(), TYPE_CONTENT))
            .map(p -> convertProperty(p, properties.get(p.getName())))
            .findFirst()
            .orElse(getContentProperty(properties, type.getParent()));
    }

    public PropertyContainer convertProperty(PropertyDescriptor pd, Object value) {
        PropertyContainer pc = new PropertyContainer();
        pc.setDescriptor(pd);

        if (value != null) {
            try {
                if (pd.isMultiple()) {
                    List<Object> list = new ArrayList<>();
                    for(Object item : (List<Object>) value) {
                        list.add(convertPropertyValue(pd, item));
                    }
                    pc.setValue(list);
                } else {
                    pc.setValue(convertPropertyValue(pd, value));
                }
            } catch (RuntimeException e) {
                log.error("Cannot parse property value {} with descriptor {}", value, pd);
                throw e;
            }
        }

        return pc;
    }

    private Object convertPropertyValue(PropertyDescriptor pd, Object value) {
        if (value == null) {
            return null;
        }

        log.debug("Converting property {} type {}", pd.getName(), pd.getTypeName());
        switch (pd.getTypeName()) {
            case "d:any":
                return value;
            case "d:text":
                return value.toString();
            case "d:int":
                return Integer.valueOf(value.toString());
            case "d:long":
                return Long.valueOf(value.toString());
            case "d:boolean":
                return Boolean.valueOf(value.toString());
            case "d:float":
                return Float.valueOf(value.toString());
            case "d:double":
                return Double.valueOf(value.toString());
            case "d:date":
            case "d:datetime":
                if (value instanceof Number) {
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Number) value).longValue()), ZoneId.systemDefault());
                }
                return ZonedDateTime.parse(value.toString());
            case "d:mltext": {
                MLTextProperty ml = new MLTextProperty();
                if (value instanceof Map) {
                    Map<String,Object> map = (Map<String, Object>) value;
                    map.forEach((k, v) -> ml.put(I18NUtils.parseLocale(k), v));
                } else {
                    ml.put(Locale.getDefault(), value.toString());
                }

                return ml;
            }
            case TYPE_CONTENT: {
                ContentProperty cp = new ContentProperty();
                cp.setName(pd.getName());
                cp.parseAttributes(value.toString());
                return cp;
            }
            case "d:qname":
                return QName.valueOf(value.toString());
            case "d:locale":
                return I18NUtils.parseLocale(value.toString());
            case "d:category":
            case "d:noderef":
                return URI.create(value.toString());
            default:
                log.warn("Unknown data type {}", pd.getTypeName());
                return null;
        }
    }
}
