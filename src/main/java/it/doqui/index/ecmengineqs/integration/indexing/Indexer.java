package it.doqui.index.ecmengineqs.integration.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import it.doqui.index.ecmengineqs.business.converters.NodeMapper;
import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.dto.ContentProperty;
import it.doqui.index.ecmengineqs.business.dto.MLTextProperty;
import it.doqui.index.ecmengineqs.business.dto.NodeRef;
import it.doqui.index.ecmengineqs.business.entities.NodeData;
import it.doqui.index.ecmengineqs.business.search.ISO9075;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import it.doqui.index.ecmengineqs.business.services.NodeService;
import it.doqui.index.ecmengineqs.business.services.TransactionService;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.foundation.UserContext;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.integration.solr.SolrClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.xml.namespace.QName;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static it.doqui.index.ecmengineqs.business.converters.PropertyConverter.TYPE_CONTENT;
import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_ECMSYS_DISABLED_FULLTEXT;
import static it.doqui.index.ecmengineqs.business.schema.SchemaConstants.ASPECT_ECMSYS_ENCRYPTED;
import static it.doqui.index.ecmengineqs.foundation.Constants.ARCHIVE;

@ApplicationScoped
@Slf4j
public class Indexer {

    @ConfigProperty(name = "solr.fullTextSizeThreshold", defaultValue = "10485760")
    long fullTextSizeThreshold;

    @ConfigProperty(name = "solr.asynchronousReindexSizeThreshold", defaultValue = "4194304")
    long asynchronousReindexSizeThreshold;

    @ConfigProperty(name = "solr.fakeIndexModeEnabled", defaultValue = "false")
    boolean fakeIndexModeEnabled;

    @Inject
    NodeService nodeService;

    @Inject
    TransactionService transactionService;

    @Inject
    SolrClient solrClient;

    @Inject
    NodeMapper nodeMapper;

    @Inject
    ModelService modelService;

    @Inject
    UserContextManager userContextManager;

    @Transactional
    public boolean index(IndexingOperationSet operationSet, boolean async) {
        log.debug("Indexing transaction {} (async = {})", operationSet.getTxId(), async);
        userContextManager.setContext(
            UserContext.builder()
                .repository("primary")
                .tenant(operationSet.getTenant())
                .username("admin").build()
        );

        operationSet.getOperations()
            .stream()
            .forEach(op -> log.debug("Operation {} node {}", op.getType(), op.getRef() == null ? null : op.getRef().getUuid()));

        log.debug("Loading...");

        if (operationSet.getOperations().isEmpty()) {
            operationSet.getOperations().addAll(
                nodeService
                    .findNodesHavingTx(operationSet.getTenant(), operationSet.getTxId())
                    .stream()
                    .map(n -> {
                        IndexingOperation op = new IndexingOperation();
                        if (StringUtils.equals(n.getKind(), ARCHIVE)) {
                            op.setType(IndexingOperationType.REMOVE);
                        } else {
                            op.setType(IndexingOperationType.REINDEX);
                        }

                        op.setRef(n);
                        return op;
                    })
                    .collect(Collectors.toList()));
        } else {
            Map<String, NodeData> nodeMap = nodeService.mapNodesInUUIDs(operationSet.getTenant(),
                operationSet
                    .getOperations()
                    .stream()
                    .filter(op -> op.getRef() instanceof NodeRef)
                    .filter(op -> op.getRef().getTenant() == null || StringUtils.equals(op.getRef().getTenant(), operationSet.getTenant()))
                    .map(op -> op.getRef().getUuid())
                    .collect(Collectors.toList()), null);

            List<IndexingOperation> operations = operationSet
                .getOperations()
                .stream()
                .map(op -> {
                    if (op.getRef() instanceof NodeRef) {
                        NodeData n = nodeMap.get(op.getRef().getUuid());
                        if (n != null) {
                            IndexingOperation x = new IndexingOperation();
                            x.setType(op.getType());
                            x.setRef(n);
                        } else {
                            log.warn("Unable to find node %s in tenant %s", op.getRef().getUuid(), operationSet.getTenant());
                        }
                    }

                    return op;
                })
                .collect(Collectors.toList());

            operationSet.getOperations().clear();
            operationSet.getOperations().addAll(operations);
        }

        AtomicBoolean requireReindex = new AtomicBoolean(false);
        List<Map<String,Object>> documents = new ArrayList<>();
        operationSet.getOperations()
            .stream()
            .filter(op -> op.getType() != IndexingOperationType.REMOVE)
            .map(op -> createDocument(operationSet.getTxId(), op, async))
            .filter(res -> res != null)
            .map(res -> {
                if (BooleanUtils.isTrue(res.getLeft())) {
                    requireReindex.set(true);
                }

                return res.getRight();
            })
            .filter(doc -> doc != null && !doc.isEmpty())
            .forEach(documents::add);

        List<String> removedDocuments = new ArrayList<>();
        operationSet.getOperations()
            .stream()
            .filter(op -> op.getType() == IndexingOperationType.REMOVE)
            .map(op -> deleteDocument(operationSet.getTxId(), op, async))
            .filter(doc -> doc != null && !doc.isEmpty())
            .forEach(removedDocuments::add);

        if (documents.isEmpty() && removedDocuments.isEmpty()) {
            transactionService.setTransactionIndexedNow(operationSet.getTenant(), operationSet.getTxId());
            log.debug("No data to index for transaction {}@{}", operationSet.getTxId(), operationSet.getTenant());
            return false;
        }

        try {
            String cname = solrClient.collectionName();
            log.debug("Using solr collection {}", cname);
            if (fakeIndexModeEnabled) {
                log.debug("Generated documents: {}", new ObjectMapper().writeValueAsString(documents));
                log.debug("Removed documents: {}", new ObjectMapper().writeValueAsString(removedDocuments));
                return false;
            }

            if (!documents.isEmpty()) {
                solrClient.update(cname, documents);
            }

            if (!removedDocuments.isEmpty()) {
                Map<String, Object> cmdMap = new HashMap<>();
                cmdMap.put("delete", removedDocuments);
                solrClient.delete(cname, cmdMap);
            }

            if (!requireReindex.get()) {
                transactionService.setTransactionIndexedNow(operationSet.getTenant(), operationSet.getTxId());
                log.info("Transaction {}@{} fully indexed", operationSet.getTxId(), operationSet.getTenant());
            } else {
                log.info("Transaction {}@{} require a reindex", operationSet.getTxId(), operationSet.getTenant());
            }

            return true;
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Pair<Boolean,Map<String, Object>> createDocument(String txId, IndexingOperation op, boolean async) {
        log.info("Indexing tx: {}, op: {}, tenant: {}, node: {}, async: {}", txId, op.getType(), op.getRef().getTenant(), op.getRef().getUuid(), async);

        // il parametro async è necessario per sapere se indicizzare anche la parte full text
        // nel caso di chiamata sync il full text verrà indicizzato successivamente
        // in una chiamata asincrona se la dimensione del contenuto supera una certa soglia.
        // Verificare le due soglie asynchronousReindexSizeThreshold e fullTextSizeThreshold.
        // Il fulltext viene salvato in TEXT.
        // Se tra gli aspect è presente {http://www.doqui.it/model/ecmengine/system/1.0}localizable
        // allora si cerca il locale nella property
        // {http://www.doqui.it/model/ecmengine/system/1.0}locale e si salva il testo anche in
        // TEXT_locale_{locale}
        // Per il full text si usa Tika. Vedi OLD fullTextIndex in SolrIndexerComponent.

        // La cancellazione di un nodo cancella tutti i discendenti.
        // Cerca i figli con query PRIMARYPARENT:{id} e per ciascuno cancella
        // ricorsivamente i discendenti, infine cancella il nodo.

        if (!(op.getRef() instanceof NodeData)) {
            log.warn("Expected NodeData instead of class {}", op.getRef().getClass().getName());
            return null;
        }


        NodeData n = (NodeData) op.getRef();

        switch (op.getType()) {
            case UPDATE_ACL:
                // Le ACL non sono indicizzate
                return null;
            case REMOVE: {
                return null;
//                // Cancellazione nodo da Solr
//                Map<String,String> delete = new HashMap<>();
//                delete.put("id", n.getUuid());
//
//                Map<String,Object> resultMap = new HashMap<>();
//                resultMap.put("delete", delete);
//
//                return new ImmutablePair<>(false, resultMap);
            }
            default:
                break;
        }

        Multimap<String,Object> multimap = ArrayListMultimap.create();
        boolean requireReindex = false;
        multimap.put("ID", n.getUuid());
        multimap.put("TX", txId);
        multimap.put("DBID", n.getId());

        ContentNode c = nodeMapper.map(n);

        // aspects TODO solo se il type è indexed
        c.getAspects()
            .values()
            .stream()
            .map(a -> {
                PrefixedQName pq = PrefixedQName.valueOf(a.getName());
                QName q = modelService
                    .getNamespaceURI(pq.getNamespaceURI())
                    .map(ns -> new QName(ns, pq.getLocalPart()))
                    .orElseThrow(() -> new RuntimeException(String.format("Unable to find prefix %s in the model", pq.getNamespaceURI())));
                return q.toString();
            })
            .forEach(a -> multimap.put("ASPECT", a));

        // properties
        c.getProperties()
            .values()
            .stream()
            .filter(pc -> pc.getValue() != null && !StringUtils.isBlank(pc.getValue().toString()))
            .forEach(pc -> {
                if (!pc.getDescriptor().isIndexed()) {
                    return;
                }

                String typeName = pc.getDescriptor().getTypeName();
                List<Object> convertedValues = new ArrayList<>();
                if (pc.getDescriptor().isMultiple()) {
                    for (Object v : (List<Object>) pc.getValue()) {
                        Object convertedValue = format(typeName, v, c.getLocale());
                        if (convertedValue != null) {
                            convertedValues.add(convertedValue);
                        }
                    }
                } else {
                    Object convertedValue = format(typeName, pc.getValue(), c.getLocale());
                    if (convertedValue != null) {
                        convertedValues.add(convertedValue);
                    }
                }

                if (!convertedValues.isEmpty()) {
                    PrefixedQName pq = PrefixedQName.valueOf(pc.getDescriptor().getName());
                    QName q = modelService
                        .getNamespaceURI(pq.getNamespaceURI())
                        .map(ns -> new QName(ns, pq.getLocalPart()))
                        .orElseThrow(() -> new RuntimeException(String.format("Unable to find prefix %s in the model", pq.getNamespaceURI())));
                    multimap.putAll("@" + q.toString(), convertedValues);
                }
            });

        // content
        if (c.getContentProperty() != null) {
            if (!c.getAspects().keySet().contains(ASPECT_ECMSYS_DISABLED_FULLTEXT) && !c.getAspects().keySet().contains(ASPECT_ECMSYS_ENCRYPTED)) {
                long size = NumberUtils.toLong(c.getContentProperty().getAttribute("size"), 0);
                if (size > 0 && size < fullTextSizeThreshold) {
                    if (size > asynchronousReindexSizeThreshold && !async) {
                        //TODO: schedule async job to reindex this node
                        requireReindex = true;
                    } else {
                        String text = fullText(c.getContentProperty());
                        if (!StringUtils.isBlank(text)) {
                            multimap.put("TEXT", text);
                            Locale locale = c.getLocale();
                            if (locale != null) {
                                multimap.put("TEXT_locale_" + locale, text);
                            }
                        }
                    }
                }
            }
        }

        //Determinare se il nodo è root
        //se root il vecchio index creava il core, lista vuota di path
        //altrimenti calcola i paths. Cosa sono i category paths?

        // recupera gli antenati
        Map<Long,NodeData> nodeMap = nodeService.mapAncestors(List.of(n));

        n.getParents().stream().forEach(p -> {
            multimap.put("PARENT", p.getParent().getURI().toString());
            multimap.put("ASSOCTYPEQNAME", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf(p.getTypeName()))));
            multimap.put("QNAME", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf(p.getName()))));
            //TODO: verificare significato LINKASPECT

            if (p.isPrimary()) {
                multimap.put("PRIMARYPARENT", p.getParent().getURI().toString());
                multimap.put("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf((p.getTypeName())))));
                //TODO: completare
            }
        });

        if (n.getParents().isEmpty()) {
            // Nel caso root sopra non entra. Alcune chiavi devono essere impostate ad hoc.
            multimap.put("ISROOT", "T");
            multimap.put("QNAME", "");
            multimap.put("PRIMARYASSOCTYPEQNAME", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf("sys:children"))));
        } else {
            multimap.put("ISROOT", "F");
            multimap.put("ISPART", "F"); //TODO: verificare significato ISPART
        }

        multimap.put("ISNODE", "T");

        //TODO: verificare significato ISCONTAINER
        multimap.put("ISCONTAINER", "T");

        //TODO: verificare significato FTSSTATUS

        multimap.put("EXACTTYPE", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf(n.getTypeName()))));
        modelService
            .getHierarchyTypes(c.getType())
            .forEach(t -> multimap.put("TYPE", ISO9075.getXPathName(modelService.convertPrefixedQName(PrefixedQName.valueOf(t.getName())))));

        n.getPaths().stream()
            .filter(p -> p.isEnabled())
            .forEach(p -> {
            log.debug("Processing path '{}'", p.getFilePath());
            String[] s = p.getFilePath().split("/");

            List<String> elements = Arrays.stream(s)
                .filter(x -> StringUtils.isNotBlank(x))
                .map(x -> modelService.convertPrefixedString(x))
                .collect(Collectors.toList());

            multimap.put("PATH", "/" + String.join("/", elements));
            multimap.put("PARENTPATH", "/" + String.join("/", elements.subList(0, elements.size() - 1)));
        });

        multimap.putAll("ANCESTOR",
            nodeMap
                .values()
                .stream()
                .filter(x -> x.getId() != n.getId())
                .map(x -> x.getURI())
                .map(uri -> uri.toString())
                .collect(Collectors.toList())
        );

        //TODO: verificare significato ISCATEGORY

        Map<String,Object> resultMap = new HashMap<>();
        multimap
            .keySet()
            .stream()
            .forEach(k -> resultMap.put(k, multimap.get(k)));

        return new ImmutablePair<>(requireReindex, resultMap);
    }

    private Object format(String type, Object value, Locale locale) {
        Object result = null;
        if (value != null) {
            switch (type) {
                case TYPE_CONTENT:
                    result = value.toString();
                    break;
                case "d:date":
                case "d:datetime":
                    result = ((ZonedDateTime) value).format(DateTimeFormatter.ISO_INSTANT);
                    break;
                case "d:mltext": {
                    MLTextProperty ml = (MLTextProperty) value;
                    if (locale == null) {
                        result = ml.values().stream().findFirst().get();
                    } else {
                        result = ml.get(locale);
                    }
                    break;
                }
                default:
                    result = value;
                    break;
            }
        }
        return result;
    }

    private String fullText(ContentProperty cp) {
        log.warn("FullText Indexing not yet implemented");
        //TODO: implementare indicizzazione full text
        return null;
    }

    private String deleteDocument(String txId, IndexingOperation op, boolean async) {
        if (!(op.getRef() instanceof NodeData)) {
            log.warn("Expected NodeData instead of class {}", op.getRef().getClass().getName());
            return null;
        }

        switch (op.getType()) {
            case REMOVE: {
                // Cancellazione nodo da Solr
                NodeData n = (NodeData) op.getRef();
                return n.getUuid();
            }
            default:
                return null;
        }
    }

}
