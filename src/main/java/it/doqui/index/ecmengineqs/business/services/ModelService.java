package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.schema.AspectDescriptor;
import it.doqui.index.ecmengineqs.business.schema.SchemaManager;
import it.doqui.index.ecmengineqs.business.schema.TenantSchema;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.foundation.UserContext;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_CM_CLASSIFIABLE;

@ApplicationScoped
@Slf4j
public class ModelService {

    @Inject
    SchemaManager schemaManager;

    @Inject
    UserContextManager userContextManager;

    public Optional<String> getNamespaceURI(String prefix) {
        UserContext context = userContextManager.getContext();
        TenantSchema schema = schemaManager.getTenant(context.getRepository(), context.getTenant());
        String s = schema.getNamespaceMap().inverse().get(prefix);
        if (s == null) {
            schema = schemaManager.getDefault();
            s = schema.getNamespaceMap().inverse().get(prefix);
        }

        return Optional.ofNullable(s);
    }

    public String getNamespacePrefix(String uri) {
        UserContext context = userContextManager.getContext();
        TenantSchema schema = schemaManager.getTenant(context.getRepository(), context.getTenant());
        String s = schema.getNamespaceMap().get(uri);
        if (s == null) {
            schema = schemaManager.getDefault();
            s = schema.getNamespaceMap().get(uri);
        }

        return s == null ? uri : s;
    }

    public TypeDescriptor getType(String prefixedTypeName) {
        UserContext context = userContextManager.getContext();
        TenantSchema schema = schemaManager.getTenant(context.getRepository(), context.getTenant());
        TypeDescriptor type = schema.getTypeMap().get(prefixedTypeName);
        if (type == null) {
            schema = schemaManager.getDefault();
            type = schema.getTypeMap().get(prefixedTypeName);
        }

        return type;
    }

    public AspectDescriptor getAspect(String prefixedTypeName) {
        UserContext context = userContextManager.getContext();
        TenantSchema schema = schemaManager.getTenant(context.getRepository(), context.getTenant());
        AspectDescriptor a = schema.getAspectMap().get(prefixedTypeName);
        if (a == null) {
            schema = schemaManager.getDefault();
            a = schema.getAspectMap().get(prefixedTypeName);
        }

        return a;
    }

    public List<TypeDescriptor> getHierarchyTypes(TypeDescriptor type) {
        List<TypeDescriptor> types = new ArrayList<>();
        types.add(type);
        if (type.getParent() != null) {
            TypeDescriptor parentType = this.getType(type.getParent());
            if (parentType == null) {
                throw new RuntimeException(String.format("Missing type %s in the model", type.getParent()));
            }

            types.addAll(getHierarchyTypes(parentType));
        }
        return types;
    }

    public boolean isCategorised(AspectDescriptor a) {
        if (StringUtils.equals(a.getName(), ASPECT_CM_CLASSIFIABLE)) {
            return true;
        }

        String parent = a.getParent();
        if (parent != null) {
            AspectDescriptor parentDescriptor = getAspect(parent);
            if (parentDescriptor != null) {
                throw new RuntimeException(String.format("Missing aspect %s in the model", parent));
            }

            return isCategorised(parentDescriptor);
        }

        return false;
    }

    public QName convertPrefixedQName(PrefixedQName pq) {
        return StringUtils.isBlank(pq.getNamespaceURI())
            ? pq
            : new QName(
                getNamespaceURI(pq.getNamespaceURI())
                    .orElseThrow(() -> new RuntimeException(String.format("Unable to find prefix %s in the model", pq.getNamespaceURI()))),
                pq.getLocalPart());
    }

    public String convertPrefixedString(String s) {
        return s == null ? null : convertPrefixedQName(PrefixedQName.valueOf(s)).toString();
    }
}
