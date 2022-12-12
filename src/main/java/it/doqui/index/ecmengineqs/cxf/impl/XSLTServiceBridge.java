package it.doqui.index.ecmengineqs.cxf.impl;

import it.doqui.index.ecmengine.mtom.dto.MtomOperationContext;
import it.doqui.index.ecmengine.mtom.dto.Node;
import it.doqui.index.ecmengine.mtom.dto.RenditionDocument;
import it.doqui.index.ecmengine.mtom.dto.RenditionTransformer;
import it.doqui.index.ecmengine.mtom.exception.*;
import it.doqui.index.ecmengineqs.business.dto.RenditionDTO;
import it.doqui.index.ecmengineqs.business.exceptions.ForbiddenException;
import it.doqui.index.ecmengineqs.business.exceptions.NotFoundException;
import it.doqui.index.ecmengineqs.business.xslt.XSLTService;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.rmi.RemoteException;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class XSLTServiceBridge extends AbstractServiceBridge {

    @Inject
    XSLTService xsltService;

    public RenditionTransformer[] getRenditionTransformers(Node xml, String propertyContent, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {

        try {
            Objects.requireNonNull(xml, "Node XML must not be null");
            Objects.requireNonNull(xml.getUid(), "Node XML UID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            return xsltService
                .findRenditionTransformers(xml.getUid())
                .stream()
                .map(r -> {
                    RenditionTransformer t = new RenditionTransformer();
                    t.setNodeId(r.getUuid());
                    t.setDescription(r.getDescription());
                    t.setGenMymeType(r.getGenMimeType());
                    t.setMimeType(r.getMimeType());
                    t.setRenditionUid(r.getRenditionId());
                    t.setContent(r.getBinaryData());

                    return t;
                })
                .collect(Collectors.toList())
                .toArray(new RenditionTransformer[0]);

        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public RenditionTransformer getRenditionTransformer(Node nodoTransformer, String propertyContent,
                                                        MtomOperationContext context) throws InvalidParameterException, InsertException, NoSuchNodeException,
        RemoteException, InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {
        try {
            Objects.requireNonNull(nodoTransformer, "Node transformer must not be null");
            Objects.requireNonNull(nodoTransformer.getUid(), "Node transformer UID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            return xsltService
                .getRenditionTransformer(nodoTransformer.getUid())
                .map(r -> {
                    RenditionTransformer t = new RenditionTransformer();
                    t.setNodeId(r.getUuid());
                    t.setDescription(r.getDescription());
                    t.setGenMymeType(r.getGenMimeType());
                    t.setMimeType(r.getMimeType());
                    t.setRenditionUid(r.getRenditionId());
                    t.setContent(r.getBinaryData());

                    return t;
                })
                .orElseThrow(() -> new NoSuchNodeException(nodoTransformer.getUid()));

        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public RenditionDocument getRendition(Node nodoTransformer, String propertyContent, MtomOperationContext context)
        throws InvalidParameterException, InsertException, NoSuchNodeException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {
        try {
            Objects.requireNonNull(nodoTransformer, "Node transformer must not be null");
            Objects.requireNonNull(nodoTransformer.getUid(), "Node transformer UID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            RenditionDTO r = xsltService.getRenditionDocument(nodoTransformer.getUid(), propertyContent);
            RenditionDocument t = new RenditionDocument();
            t.setNodeId(r.getRenditionId());
            t.setDescription(r.getDescription());
            t.setContent(r.getBinaryData());

            return t;

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
