package it.doqui.index.ecmengineqs.business.validators;

import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.exceptions.BadDataException;
import it.doqui.index.ecmengineqs.business.schema.AspectDescriptor;
import it.doqui.index.ecmengineqs.business.schema.PropertyContainerDescriptor;
import it.doqui.index.ecmengineqs.business.schema.TypeDescriptor;
import it.doqui.index.ecmengineqs.business.services.ModelService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class NodeValidator {

    private enum PropertyValidationMode {
        CREATE,
        UPDATE
    }

    @Inject
    ModelService modelService;

    public void validateForCreation(ContentNode n) {
        try {
            Objects.requireNonNull(n.getPrimaryParent().getTargetUUID(), "Missing parent UUID");
            Objects.requireNonNull(n.getType(), "Missing type");
            Objects.requireNonNull(n.getPrimaryParent().getTypeName(), "Missing parent association type");
            Objects.requireNonNull(n.getPrimaryParent().getName(), "Missing name");
        } catch (NullPointerException e) {
            throw new BadDataException(e.getMessage());
        }

        checkMandatoryData(n, n.getType(), PropertyValidationMode.CREATE);
    }

    public void validateForUpdateMetadata(ContentNode n) {
        checkMandatoryData(n, n.getType(), PropertyValidationMode.UPDATE);
    }

    private void checkMandatoryData(ContentNode n, TypeDescriptor type, PropertyValidationMode mode) {
        switch (mode) {
            case CREATE:
                checkMandatoryProperties(n, type);
                break;
            case UPDATE:
                n.getProperties()
                    .values()
                    .stream()
                    .filter(pc -> pc.getDescriptor().isMandatory() && pc.getValue() == null)
                    .forEach(pc -> {
                        throw new BadDataException(String.format("Cannot remove mandatory property %s", pc.getDescriptor().getName()));
                    });
                break;
        }

        if (mode != PropertyValidationMode.UPDATE || !n.getAspects().isEmpty()) {
            type.getMandatoryAspects().forEach(a -> {
                if (!a.startsWith("sys:")) {
                    AspectDescriptor ad = n.getAspects().get(a);
                    if (ad == null) {
                        throw new BadDataException(String.format("Missing mandatory aspect %s for type %s", a, type.getName()));
                    }

                    checkMandatoryProperties(n, ad);
                }
            });
        }

        if (type.getParent() != null) {
            checkMandatoryData(n, modelService.getType(type.getParent()), mode);
        }
    }

    private void checkMandatoryProperties(ContentNode n, PropertyContainerDescriptor d) {
        d.getProperties()
            .values()
            .stream()
            .filter(pd -> pd.isMandatory())
            .forEach(pd -> {
                if (n.getProperties().get(pd.getName()) == null) {
                    throw new BadDataException(String.format("Missing mandatory property %s for type %s", pd.getName(), d.getName()));
                }
            });
    }
}
