package it.doqui.index.ecmengineqs.cxf.impl;

import it.doqui.index.ecmengine.mtom.dto.*;
import it.doqui.index.ecmengine.mtom.exception.*;
import it.doqui.index.ecmengineqs.business.converters.ContentConverter;
import it.doqui.index.ecmengineqs.business.dto.*;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.exceptions.*;
import it.doqui.index.ecmengineqs.business.search.SearchResult;
import it.doqui.index.ecmengineqs.business.search.SearchService;
import it.doqui.index.ecmengineqs.business.search.SortDefinition;
import it.doqui.index.ecmengineqs.business.services.CatalogService;
import it.doqui.index.ecmengineqs.business.services.DeleteMode;
import it.doqui.index.ecmengineqs.business.services.MimeTypeManager;
import it.doqui.index.ecmengineqs.business.storage.ContentStoreManager;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.integration.integrity.IntegrityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.modelmapper.ModelMapper;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.CONTENT_ATTR_URL;

@ApplicationScoped
@Slf4j
public class ServiceWrapper extends AbstractServiceBridge {

    private static final long MASSIVE_MAX_RETRIEVE_SIZE = 9437184; // 9 MB, in Byte.

    @Inject
    ContentConverter contentConverter;

    @Inject
    CatalogService catalogService;

    @Inject
    SearchService searchService;

    @Inject
    MimeTypeManager mimeTypeManager;

    @Inject
    ModelMapper modelMapper;

    @Inject
    ContentStoreManager contentStoreManager;

    @Inject
    IntegrityService integrityService;

    public Content getContentMetadata(Node node, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, PermissionDeniedException,
        EcmEngineTransactionException, ReadException, InvalidCredentialsException, RemoteException {

        try {
            handleContext(context);

            return catalogService
                .getContentMetadata(node.getUid())
                .map(n -> contentConverter.getAsContent(n))
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    //NOTE: questo metodo dopo la migrazione potrebbe restituire un ID deifferente da quello precedente
    public long getDbIdFromUID(Node node, MtomOperationContext context) throws InvalidParameterException,
        InvalidCredentialsException, PermissionDeniedException, NoSuchNodeException, ReadException, RemoteException {

        try {
            handleContext(context);

            return catalogService
                .getNodeInfo(node.getUid())
                .map(n -> n.getId())
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public Content[] massiveGetContentMetadata(Node[] nodes, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, ReadException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            handleContext(context);

            List<Content> contents = catalogService
                .listContentMetadata(Arrays.stream(nodes).map(n -> n.getUid()).collect(Collectors.toList()))
                .stream()
                .map(n -> contentConverter.getAsContent(n))
                .collect(Collectors.toList());

            return contents.toArray(new Content[0]);
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public Content[] massiveGetContentMetadataPartial(Node[] nodes, Property[] properties, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, ReadException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            Objects.requireNonNull(nodes, "Nodes must not be null");
            Objects.requireNonNull(properties, "Properties must not be null");

            for (Node node : nodes) {
                Objects.requireNonNull(node.getUid(), "Node uid must not be null");
            }

            for (Property p : properties) {
                Objects.requireNonNull(p.getPrefixedName(), "Property name must not be null");
            }
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            List<String> uuids = Arrays.stream(nodes).map(n -> n.getUid()).collect(Collectors.toList());
            Set<String> propertyNames = Arrays.stream(properties).map(p -> p.getPrefixedName()).collect(Collectors.toSet());
            List<Content> contents = catalogService
                .listContentMetadata(uuids, propertyNames)
                .stream()
                .map(n -> contentConverter.getAsContent(n))
                .collect(Collectors.toList());

            return contents.toArray(new Content[0]);
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public byte[] retrieveContentData(Node node, Content content, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, ReadException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {

        try {
            //TODO: validate node

            // validate content property name
            String cname = content.getContentPropertyPrefixedName();
            if (cname == null) {
                throw new InvalidParameterException("CPPN: null");
            } else if (cname.length() < 3 || cname.indexOf(':') < 1 || cname.indexOf(':') >= cname.length() - 1) {
                throw new InvalidParameterException("CPPN: " + cname);
            }

            handleContext(context);

            return catalogService
                .getContentMetadata(node.getUid())
                .map(n -> {
                    String contentUrl = n.getContentProperty().getAttribute(CONTENT_ATTR_URL);
                    try {
                        return contentStoreManager.readFileWithContentURI(contentUrl);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public Attachment downloadMethod(String uid, String usr, String pwd, String repo, String prefixedName) throws SystemException {
        try {
            Objects.requireNonNull(uid);
            Objects.requireNonNull(usr);
            Objects.requireNonNull(pwd);
            Objects.requireNonNull(repo);
            Objects.requireNonNull(prefixedName);
        } catch (NullPointerException e) {
            throw new SystemException("Validation error: parameter must not be null");
        }

        try {
            MtomOperationContext context = new MtomOperationContext();
            context.setUsername(usr);
            context.setPassword(pwd);
            context.setRepository(repo);
            context.setFruitore(usr);
            context.setNomeFisico(usr);

            handleContext(context);

            return catalogService
                .getContentMetadata(uid)
                .map(n -> {
                    ContentProperty c = n.getContentProperty();
                    if (c == null) {
                        throw new RuntimeException("No content property");
                    }

                    if (!StringUtils.equals(c.getName(), prefixedName)) {
                        throw new RuntimeException("Content property does not match");
                    }

                    try {
                        File f = contentStoreManager.getFileWithContentURI(c.getAttribute("contentUrl"));

                        Attachment attachment = new Attachment();
                        attachment.fileName = PrefixedQName.valueOf(n.getName()).getLocalPart();
                        attachment.fileSize = f.length();
                        attachment.fileType = c.getAttribute("mimetype");
                        attachment.attachmentDataHandler = new DataHandler(new FileDataSource(f));

                        return attachment;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new NoSuchNodeException(uid));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SystemException(e.getMessage());
        }
    }

    public ContentData[] massiveRetrieveContentData(Node[] nodes, Content[] contents, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, ReadException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        //TODO: validate node

        // validate content property name
        for (int i=0; i<contents.length; i++) {
            Content content = contents[i];
            String cname = content.getContentPropertyPrefixedName();
            if (cname == null) {
                throw new InvalidParameterException(String.format("CPPN[%d]: null", i));
            } else if (cname.length() < 3 || cname.indexOf(':') < 1 || cname.indexOf(':') >= cname.length() - 1) {
                throw new InvalidParameterException(String.format("CPPN[%d]: %s",i ,cname));
            }
        }

        TreeSet<String> uuids = new TreeSet<>();
        for (Node n : nodes) {
            if (uuids.contains(n.getUid())) {
                throw new InvalidParameterException("Duplicate nodes");
            }
        }

        try {
            AtomicLong totalSize = new AtomicLong(0);
            Map<String, ContentData> resultMap = new HashMap<>();
            catalogService
                .listContentMetadata(uuids)
                .forEach(n -> {
                    String contentUrl = n.getContentProperty().getAttribute("contentUrl");
                    try {
                        ContentData c = new ContentData();
                        c.setContent(contentStoreManager.readFileWithContentURI(contentUrl));
                        if (totalSize.addAndGet(c.getContent().length) > MASSIVE_MAX_RETRIEVE_SIZE) {
                            throw new RuntimeException("Max retrieve size exceeded: " + MASSIVE_MAX_RETRIEVE_SIZE);
                        }

                        resultMap.put(n.getUuid(), c);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new DataIOException(e.getMessage());
                    }
                });

            if (resultMap.isEmpty()) {
                throw new NoSuchNodeException("ANY");
            }

            ContentData[] result = new ContentData[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                result[i] = resultMap.get(nodes[i].getUid());
            }

            return result;
        } catch (DataIOException e) {
            throw new ReadException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public String getTypePrefixedName(Node node, MtomOperationContext context) throws InvalidParameterException,
        NoSuchNodeException, PermissionDeniedException, InvalidCredentialsException, RemoteException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            return catalogService
                .getNodeInfo(node.getUid())
                .map(n -> n.getTypeName())
                .orElseThrow(() -> new NoSuchNodeException(node.getUid()));
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public Path[] getPaths(Node node, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, PermissionDeniedException, SearchException,
        RemoteException, InvalidCredentialsException {

        try {
            handleContext(context);

            return catalogService
                .getPaths(node.getUid())
                .stream()
                .map(p -> {
                    Path path = new Path();
                    path.setPath(
                        (!p.getFilePath().equals("/") && p.getFilePath().endsWith("/"))
                            ? p.getFilePath().substring(0, p.getFilePath().length() - 1)
                            : p.getFilePath());
                    path.setPrimary(p.isPrimary());
                    return path;
                })
                .collect(Collectors.toList())
                .toArray(new Path[0]);

        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public AssociationResponse getAssociations(Node node, AssociationsSearchParams associationsSearchParams,
                                               MtomOperationContext context) throws InvalidParameterException, NoSuchNodeException, SearchException,
        RemoteException, InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {

        try {
            handleContext(context);
            AssociationType type = AssociationType.valueOf(associationsSearchParams.getAssociationType());
            Collection<String> filterTypes = associationsSearchParams.getFilterType() == null
                ? null : Arrays.stream(associationsSearchParams.getFilterType()).collect(Collectors.toList());
            Pageable pageable = null;
            Long limit = null;
            if (associationsSearchParams.getPageSize() > 0) {
                pageable = new Pageable();
                pageable.setSize(associationsSearchParams.getPageSize());
                pageable.setPage(associationsSearchParams.getPageIndex());
            } else if (associationsSearchParams.getLimit() > 0) {
                limit = Long.valueOf(associationsSearchParams.getLimit());
            }

            Pair<Long, List<Relationship>> r = catalogService.getAssociations(node.getUid(), type, filterTypes, pageable, limit);
            AssociationResponse response = new AssociationResponse();
            response.setTotalResults(r.getLeft());

            List<Association> associations = r.getRight().stream().map(relationship -> {
                Association a = new Association();
                a.setTypePrefixedName(relationship.getTypeName());
                a.setPrefixedName(relationship.getName());
                a.setTargetUid(relationship.getTargetUUID());
                a.setTargetPrefixedName(relationship.getTargetName());

                switch (type) {
                    case PARENT:
                    case CHILD:
                        a.setChildAssociation(true);
                    default:
                        a.setChildAssociation(false);
                }

                return a;
            }).collect(Collectors.toList());
            response.setAssociationArray(associations.toArray(new Association[0]));
            return response;

        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public NodeResponse luceneSearchNoMetadata(SearchParams lucene, MtomOperationContext context)
        throws InvalidParameterException, TooManyResultsException, SearchException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {

        try {
            handleContext(context);

            List<SortDefinition> sortFields = lucene.getSortFields() == null ? null : Arrays.stream(lucene.getSortFields())
                .map(s -> SortDefinition.builder().fieldName(s.getFieldName()).ascending(s.isAscending()).build())
                .collect(Collectors.toList());
            Pageable pageable = null;
            Long limit = null;
            if (lucene.getPageSize() > 0) {
                pageable = new Pageable();
                pageable.setSize(lucene.getPageSize());
                pageable.setPage(lucene.getPageIndex());
            } else if (lucene.getLimit() > 0) {
                limit = Long.valueOf(lucene.getLimit());
            }

            SearchResult r = searchService.search(lucene.getLuceneQuery(), sortFields, pageable, limit, false);

            NodeResponse result = new NodeResponse();
            result.setTotalResults(Long.valueOf(r.getCount()).intValue());
            if (r.getPageable() != null) {
                result.setPageIndex(r.getPageable().getPage());
                result.setPageSize(r.getPageable().getSize());
            }

            result.setNodeArray(
                r.getNodes().stream()
                .map(n -> {
                    Node node = new Node();
                    node.setUid(n.getUuid());
                    return node;
                })
                .collect(Collectors.toList())
                .toArray(new Node[0])
            );

            return result;
        } catch (BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    @Deprecated
    public int getTotalResultsLucene(SearchParams lucene, MtomOperationContext context) throws InvalidParameterException, SearchException, InvalidCredentialsException, RemoteException, EcmEngineTransactionException {
        lucene.setLimit(1);
        lucene.setPageSize(0);
        lucene.setPageIndex(0);
        NodeResponse response = luceneSearchNoMetadata(lucene, context);
        return response.getTotalResults();
    }

    public SearchResponse luceneSearch(SearchParams lucene, MtomOperationContext context) throws SystemException {
        try {
            try {
                handleContext(context);

                List<SortDefinition> sortFields = lucene.getSortFields() == null ? null : Arrays.stream(lucene.getSortFields())
                    .map(s -> SortDefinition.builder().fieldName(s.getFieldName()).ascending(s.isAscending()).build())
                    .collect(Collectors.toList());
                Pageable pageable = null;
                Long limit = null;
                if (lucene.getPageSize() > 0) {
                    pageable = new Pageable();
                    pageable.setSize(lucene.getPageSize());
                    pageable.setPage(lucene.getPageIndex());
                } else if (lucene.getLimit() > 0) {
                    limit = Long.valueOf(lucene.getLimit());
                }

                SearchResult r = searchService.search(lucene.getLuceneQuery(), sortFields, pageable, limit, true);

                SearchResponse result = new SearchResponse();
                result.setTotalResults(Long.valueOf(r.getCount()).intValue());
                if (r.getPageable() != null) {
                    result.setPageIndex(r.getPageable().getPage());
                    result.setPageSize(r.getPageable().getSize());
                }

                result.setContentArray(
                    r.getNodes().stream()
                        .map(n -> contentConverter.getAsContent(n))
                        .collect(Collectors.toList())
                        .toArray(new Content[0])
                );

                return result;
            } catch (BadRequestException e) {
                throw new InvalidParameterException(e.getMessage());
            } catch (NotFoundException e) {
                throw new NoSuchNodeException("ANY");
            } catch (ForbiddenException e) {
                throw new PermissionDeniedException();
            } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
                throw e;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SystemException(e.getMessage());
        }
    }

    //TODO: il contentStore non viene fornito all'esterno perché potrebbe essere diverso da macchina a macchina
    public Tenant[] getAllTenants(MtomOperationContext context)
        throws InvalidParameterException, EcmEngineTransactionException, InvalidCredentialsException,
        NoDataExtractedException, PermissionDeniedException, EcmEngineException, RemoteException {
        try {
            handleContext(context);

            return catalogService.getAllTenants().stream()
                .map(s -> {
                    Tenant t = new Tenant();
                    t.setDomain(s);
                    t.setEnabled(true);
                    return t;
                })
                .collect(Collectors.toList())
                .toArray(new Tenant[0]);
        } catch (BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public boolean tenantExists(Tenant tenant, MtomOperationContext context)
        throws InvalidParameterException, EcmEngineTransactionException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineException, RemoteException {

        try {
            Objects.requireNonNull(tenant, "Tenant must not be null");
            Objects.requireNonNull(tenant.getDomain(), "Tenant domain must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            return catalogService.tenantExists(tenant.getDomain());
        } catch (BadRequestException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (NotFoundException e) {
            throw new NoSuchNodeException("ANY");
        } catch (ForbiddenException e) {
            throw new PermissionDeniedException();
        } catch (InvalidParameterException | InvalidCredentialsException | PermissionDeniedException | NoSuchNodeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EcmEngineTransactionException(e.getMessage());
        }
    }

    public Mimetype[] getMimetype(Mimetype mimetype) throws InvalidParameterException, RemoteException {
        boolean x = StringUtils.isBlank(mimetype.getFileExtension());
        boolean y = StringUtils.isBlank(mimetype.getMimetype());
        if ((x && y) || (!x && !y)) {
            throw new InvalidParameterException(
                String.format("EXTENSION: %s - MIMETYPE: %s - ONLY ONE PARAMETER MUST BE NOT NULL",
                    mimetype.getFileExtension(), mimetype.getMimetype()));
        }

        return mimeTypeManager
            .list(modelMapper.map(mimetype, MimeTypeDTO.class))
            .stream()
            .map(m -> modelMapper.map(m, Mimetype.class))
            .collect(Collectors.toList())
            .toArray(new Mimetype[0]);
    }

    public Node createContent(Node parent, Content content, MtomOperationContext context)
        throws InvalidParameterException, InsertException, NoSuchNodeException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {

        try {
            Objects.requireNonNull(parent, "Parent node must not be null");
            Objects.requireNonNull(parent.getUid(), "Parent UUID must not be null");
            Objects.requireNonNull(content, "Content node must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            ContentNode contentNode = contentConverter.getAsContentNode(content);
            contentNode.getPrimaryParent().setTargetUUID(parent.getUid());
            CatalogResponse<NodeData> createdNode = catalogService.createNode(contentNode, content.getContent());

            Node result = new Node();
            result.setUid(createdNode.getResult().getUuid());
            return result;
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

    public Node createRichContent(Node parent, Content content, AclRecord[] acls, boolean inherits, MtomOperationContext context)
        throws InvalidParameterException, InsertException, NoSuchNodeException,
        RemoteException, InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {
        try {
            Objects.requireNonNull(parent, "Parent node must not be null");
            Objects.requireNonNull(parent.getUid(), "Parent UUID must not be null");
            Objects.requireNonNull(content, "Content node must not be null");
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

            ContentNode contentNode = contentConverter.getAsContentNode(content);
            contentNode.getPrimaryParent().setTargetUUID(parent.getUid());
            AclData acl = contentConverter.convertACLs(acls, inherits);
            CatalogResponse<NodeData> createdNode = catalogService.createNode(contentNode, acl, content.getContent());

            Node result = new Node();
            result.setUid(createdNode.getResult().getUuid());
            return result;
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

    public Node[] massiveCreateContent(Node[] parents, Content[] contents, MassiveParameter massiveParameter,
                                       MtomOperationContext context) throws InvalidParameterException, InsertException, NoSuchNodeException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            Objects.requireNonNull(parents, "Parent node must not be null");
            Objects.requireNonNull(contents, "Content array must not be null");
            if (parents.length != contents.length) {
                throw new InvalidParameterException(
                    String.format("Parents and Contents must have the same size. #parents = %d, #contents = %d", parents.length, contents.length));
            }

            for (Node n : parents) {
                Objects.requireNonNull(n.getUid(), "Every parent UUID must not be null");
            }

        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            List<Pair<ContentNode,byte[]>> list = new ArrayList<>();
            for (int i = 0; i < parents.length; i++) {
                ContentNode contentNode = contentConverter.getAsContentNode(contents[i]);
                contentNode.getPrimaryParent().setTargetUUID(parents[i].getUid());
                list.add(new ImmutablePair<>(contentNode, contents[i].getContent()));
            }

            CatalogResponse<List<NodeData>> createdNodes = catalogService.createNodes(list);
            return createdNodes.getResult()
                .stream()
                .map(n -> {
                    Node node = new Node();
                    node.setUid(n.getUuid());
                    return node;
                })
                .toArray(Node[]::new);

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

    public void renameContent(Node source, String nameValue, String propertyPrefixedName,
                              boolean onlyPrimaryAssociation, MtomOperationContext context)
        throws InvalidParameterException, InsertException, UpdateException, NoSuchNodeException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            Objects.requireNonNull(source, "Source node must not be null");
            Objects.requireNonNull(source.getUid(), "Source UUID must not be null");
            Objects.requireNonNull(nameValue, "NameValue must not be null");

        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            catalogService.renameNode(source.getUid(), propertyPrefixedName, nameValue, onlyPrimaryAssociation);
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

    public void updateMetadata(Node node, Content newContent, MtomOperationContext context)
        throws InvalidParameterException, UpdateException, NoSuchNodeException, RemoteException,
        InvalidCredentialsException, PermissionDeniedException, EcmEngineTransactionException {
        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
            Objects.requireNonNull(newContent, "Content node must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            ContentMetadata cm = contentConverter.getAsContentMetadata(newContent);
            cm.setUuid(node.getUid());
            catalogService.updateNode(cm);
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

    public void massiveUpdateMetadata(Node[] nodes, Content[] newContents, MtomOperationContext context)
        throws InvalidParameterException, UpdateException, NoSuchNodeException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {
        try {
            Objects.requireNonNull(nodes, "Nodes must not be null");
            Objects.requireNonNull(newContents, "Content nodes must not be null");
            if (nodes.length != newContents.length) {
                throw new InvalidParameterException(
                    String.format("Nodes and Contents must have the same size. #nodes = %d, #newContents = %d", nodes.length, newContents.length));
            }

            for (Node n : nodes) {
                Objects.requireNonNull(n.getUid(), "Every node UUID must not be null");
            }
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            List<ContentMetadata> list = new ArrayList<>();
            for (int i = 0; i < newContents.length; i++) {
                Content c = newContents[i];
                ContentMetadata cm = contentConverter.getAsContentMetadata(c);
                cm.setUuid(nodes[i].getUid());
                list.add(cm);
            }

            catalogService.updateNodes(list);
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

    public void linkContent(Node source, Node destination, Association association, MtomOperationContext context)
        throws InvalidParameterException, InsertException, NoSuchNodeException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            Objects.requireNonNull(source, "Source node must not be null");
            Objects.requireNonNull(source.getUid(), "Source UUID must not be null");
            Objects.requireNonNull(destination, "Destination node must not be null");
            Objects.requireNonNull(destination.getUid(), "Destination UUID must not be null");
            Objects.requireNonNull(association, "Association must not be null");
            Objects.requireNonNull(association.getTypePrefixedName(), "Association type prefixed name must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);

            if (association.isChildAssociation()) {
                // parent child relationship
                catalogService.linkParent(source.getUid(), destination.getUid(), association.getTypePrefixedName(), association.getPrefixedName());
            } else {
                // association
                catalogService.linkNode(source.getUid(), destination.getUid(), association.getTypePrefixedName());
            }

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

    public void deleteContent(Node node, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, DeleteException, InvalidCredentialsException,
        PermissionDeniedException, RemoteException, EcmEngineTransactionException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        try {
            handleContext(context);
            catalogService.deleteNodes(List.of(node.getUid()), DeleteMode.DELETE);

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

    public void deleteNode(Node node, int mode, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, DeleteException, InvalidCredentialsException,
        PermissionDeniedException, RemoteException, EcmEngineTransactionException {

        try {
            Objects.requireNonNull(node, "Node must not be null");
            Objects.requireNonNull(node.getUid(), "Node UUID must not be null");
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        DeleteMode deleteMode;
        switch (mode) {
            case 0:
                deleteMode = DeleteMode.DELETE;
                break;
            case 1:
                deleteMode = DeleteMode.PURGE;
                break;
            case 2:
                deleteMode = DeleteMode.PURGE_COMPLETE;
                break;
            default:
                throw new InvalidParameterException("Invalid mode value " + mode);
        }

        try {
            handleContext(context);
            catalogService.deleteNodes(List.of(node.getUid()), deleteMode);

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

    public void massiveDeleteContent(Node[] nodes, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, DeleteException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {
        massiveDeleteNode(nodes, 0, context);
    }

    public void massiveDeleteNode(Node[] nodes, int mode, MtomOperationContext context)
        throws InvalidParameterException, NoSuchNodeException, DeleteException, InvalidCredentialsException,
        PermissionDeniedException, EcmEngineTransactionException, RemoteException {

        try {
            Objects.requireNonNull(nodes, "Nodes must not be null");
            Objects.requireNonNull(nodes.length == 0, "Nodes must not be empty");

            for (Node node : nodes) {
                Objects.requireNonNull(node.getUid(), "Node uid must not be null");
            }
        } catch (NullPointerException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        DeleteMode deleteMode;
        switch (mode) {
            case 0:
                deleteMode = DeleteMode.DELETE;
                break;
            case 1:
                deleteMode = DeleteMode.PURGE;
                break;
            case 2:
                deleteMode = DeleteMode.PURGE_COMPLETE;
                break;
            default:
                throw new InvalidParameterException("Invalid mode value " + mode);
        }

        try {
            handleContext(context);
            List<String> uuids = Arrays.stream(nodes)
                .map(n -> n.getUid())
                .collect(Collectors.toList());
            catalogService.deleteNodes(uuids, deleteMode);

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

    public Repository[] getRepositories(MtomOperationContext context)
        throws InvalidParameterException, InvalidCredentialsException, EcmEngineException, RemoteException {
        Repository primary = new Repository();
        primary.setId("primary");

        Repository[] repositories = new Repository[1];
        repositories[0] = primary;
        return repositories;
    }

    public boolean testResources() throws EcmEngineException, RemoteException {
        return integrityService.systemStatus() == 0;
    }
}
