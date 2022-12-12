package it.doqui.index.ecmengineqs.cxf.impl;

import it.doqui.index.ecmengine.mtom.dto.AclListParams;
import it.doqui.index.ecmengine.mtom.dto.AclRecord;
import it.doqui.index.ecmengine.mtom.dto.MtomOperationContext;
import it.doqui.index.ecmengine.mtom.dto.Node;
import it.doqui.index.ecmengine.mtom.exception.*;
import it.doqui.index.ecmengineqs.business.converters.ContentConverter;
import it.doqui.index.ecmengineqs.business.dto.AclData;
import it.doqui.index.ecmengineqs.business.dto.AclPermission;
import it.doqui.index.ecmengineqs.business.exceptions.BadDataException;
import it.doqui.index.ecmengineqs.business.exceptions.BadRequestException;
import it.doqui.index.ecmengineqs.business.exceptions.ForbiddenException;
import it.doqui.index.ecmengineqs.business.exceptions.NotFoundException;
import it.doqui.index.ecmengineqs.business.services.CatalogService;
import it.doqui.index.ecmengineqs.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.rmi.RemoteException;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class AclServiceBridge extends AbstractServiceBridge {

    @Inject
    ContentConverter contentConverter;

    @Inject
    CatalogService catalogService;

    private void updateAcl(Node node, AclRecord[] acls, Boolean inherits, boolean replaceMode, MtomOperationContext context)
        throws AclEditException, NoSuchNodeException, RemoteException, InvalidParameterException,
        EcmEngineTransactionException, InvalidCredentialsException, PermissionDeniedException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
            Objects.requireNonNull(acls, "ACLs must not be null");
            for (AclRecord acl : acls) {
                Objects.requireNonNull(acl, "ACL record must not be null");
                Objects.requireNonNull(StringUtils.stripToNull(acl.getAuthority()), "ACL authority is either null or empty");
                Objects.requireNonNull(StringUtils.stripToNull(acl.getPermission()), "ACL permission is either null or empty");
            }
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            AclData acl = contentConverter.convertACLs(acls, inherits);
            catalogService.updateAcl(node.getUid(), acl, replaceMode);
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }

    }

    public void addAcl(Node node, AclRecord[] acls, MtomOperationContext context)
        throws AclEditException, NoSuchNodeException, RemoteException, InvalidParameterException,
        EcmEngineTransactionException, InvalidCredentialsException, PermissionDeniedException {
        updateAcl(node, acls, null, false, context);
    }

    public void changeAcl(Node node, AclRecord[] acls, MtomOperationContext context)
        throws InvalidParameterException, InvalidCredentialsException, AclEditException, NoSuchNodeException,
        EcmEngineTransactionException, PermissionDeniedException, RemoteException {
        updateAcl(node, acls, null, true, context);
    }

    public void updateAcl(Node node, AclRecord[] acls, MtomOperationContext context)
        throws AclEditException, NoSuchNodeException, RemoteException, InvalidParameterException,
        InvalidCredentialsException, EcmEngineTransactionException, PermissionDeniedException {
        updateAcl(node, acls, true, true, context);
    }

    public void removeAcl(Node node, AclRecord[] acls, MtomOperationContext context)
        throws AclEditException, NoSuchNodeException, RemoteException, InvalidParameterException,
        InvalidCredentialsException, EcmEngineTransactionException, PermissionDeniedException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
            Objects.requireNonNull(acls, "ACLs must not be null");
            for (AclRecord acl : acls) {
                Objects.requireNonNull(acl, "ACL record must not be null");
                Objects.requireNonNull(StringUtils.stripToNull(acl.getAuthority()), "ACL authority is either null or empty");
                Objects.requireNonNull(StringUtils.stripToNull(acl.getPermission()), "ACL permission is either null or empty");
            }
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            AclData acl = contentConverter.convertACLs(acls, null);
            catalogService.removeAcl(node.getUid(), acl);
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public void resetAcl(Node node, AclRecord filter, MtomOperationContext context)
        throws InvalidParameterException, AclEditException, NoSuchNodeException, RemoteException,
        InvalidCredentialsException, EcmEngineTransactionException, PermissionDeniedException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            String authority = null;
            AclPermission permission = null;
            if (filter != null) {
                authority = filter.getAuthority();
                if (filter.getPermission() != null) {
                    permission = AclPermission.valueOf(filter.getPermission());
                }
            }

            catalogService.resetAcl(node.getUid(), authority, permission);
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public void setInheritsAcl(Node node, boolean inherits, MtomOperationContext context)
        throws NoSuchNodeException, RemoteException, AclEditException, InvalidParameterException,
        InvalidCredentialsException, EcmEngineTransactionException, PermissionDeniedException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            catalogService.setAclInheritance(node.getUid(), inherits);
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }

    }

    public boolean isInheritsAcl(Node node, MtomOperationContext context) throws NoSuchNodeException, RemoteException,
        AclEditException, InvalidParameterException, EcmEngineTransactionException, InvalidCredentialsException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            return catalogService
                .getAclData(node.getUid())
                .map(aclData -> ObjectUtils.getAsBoolean(aclData.getInherits(), true))
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public AclRecord[] listAcl(Node node, AclListParams params, MtomOperationContext context)
        throws NoSuchNodeException, RemoteException, AclEditException, InvalidParameterException,
        InvalidCredentialsException, EcmEngineTransactionException, PermissionDeniedException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
            Objects.requireNonNull(params, "AclListParams must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            return catalogService
                .getContentAcl(node.getUid(), params.isShowInherited())
                .map(contentAcl -> contentAcl
                    .getGrants()
                    .entries()
                    .stream()
                    .map(entry -> {
                        AclRecord r = new AclRecord();
                        r.setAuthority(entry.getKey());
                        r.setPermission(entry.getValue().toString());
                        r.setAccessAllowed(true);
                        return r;
                    })
                    .collect(Collectors.toList())
                    .toArray(new AclRecord[0]))
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (BadDataException | BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException(e.getMessage());
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }
}
