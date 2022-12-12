package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.business.schema.AspectDescriptor;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_ECMSYS_LOCALIZABLE;
import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.PROP_ECMSYS_LOCALE;

@Getter
@Setter
@ToString
public class ContentNode {
    private Long id;
    private String kind;
    private String tenant;
    private String uuid;
    private TypeDescriptor type;
    private String modelName;
    private ContentProperty contentProperty;
    private Relationship primaryParent;
    private final LinkedHashMap<String,PropertyContainer> properties;
    private final Map<String, AspectDescriptor> aspects;

    public ContentNode() {
        properties = new LinkedHashMap<>();
        aspects = new HashMap<>();
    }

    public String getName() {
        return this.getPrimaryParent() == null ? null : this.getPrimaryParent().getName();
    }

    public PropertyContainer getProperty(String name) {
        return properties.get(name);
    }

    public String getPropertyAsString(String name) {
        String result = null;
        PropertyContainer p = properties.get(name);
        if (p != null) {
            Object value = p.getValue();
            if (value != null) {
                result = value.toString();
            }
        }
        return result;
    }

    public Locale getLocale() {
        if (aspects.keySet().contains(ASPECT_ECMSYS_LOCALIZABLE)) {
            PropertyContainer p = properties.get(PROP_ECMSYS_LOCALE);
            if (p != null) {
                Object value = p.getValue();
                if (value != null) {
                    return (Locale) value;
                }
            }
        }

        return null;
    }
}
