package it.doqui.index.ecmengineqs.business.search;

import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.dto.Pageable;
import it.doqui.index.ecmengineqs.business.exceptions.BadRequestException;
import it.doqui.index.ecmengineqs.business.services.CatalogService;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import it.doqui.index.ecmengineqs.integration.solr.SolrClient;
import it.doqui.index.ecmengineqs.integration.solr.SolrResponse;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class SearchService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    ModelService modelService;

    @Inject
    SolrQueryParser solrQueryParser;

    @Inject
    SolrClient solrClient;

    @Inject
    CatalogService catalogService;

    public SearchResult search(String luceneQuery, Collection<SortDefinition> sortFields, Pageable pageable, Long limit, boolean includeMetadata) {
        String escapedQuery;
        try {
            escapedQuery = QueryBuilder.escapeLuceneQuery(luceneQuery);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(String.format("Syntax error in query (%s): %s" + e.getMessage(), luceneQuery));
        }



        // search parameters:
        // * pageable -> limit = page.size, offset = page.index * page.size
        // * limit
        // * escapedQuery
        if (sortFields != null && !sortFields.isEmpty()) {
            sortFields = sortFields.stream()
                .map(f -> {
                    String fieldName = f.getFieldName();
                    if (fieldName.contains(":")) {
                        PrefixedQName p = PrefixedQName.valueOf(fieldName);
                        fieldName = "@" + new QName(
                            modelService.getNamespaceURI(p.getNamespaceURI())
                                .orElseThrow(() -> new RuntimeException("QName resolution exception (namespacePrefixResolver error): " + p.getNamespaceURI())),
                            p.getLocalPart());
                    }

                    return SortDefinition.builder().fieldName(fieldName).ascending(f.isAscending()).build();
                })
                .collect(Collectors.toList());
        }

        String sort = "";
        if (sortFields != null && !sortFields.isEmpty()) {
            sort += sortFields.stream()
                .map(f -> String.format("%s %s,", f.getFieldName(), f.isAscending() ? "asc" : "desc"))
                .collect(Collectors.joining());
        }
        sort += "DBID asc";

        String q = solrQueryParser.parse(new SolrQuery(escapedQuery));
        Long offset = pageable == null ? 0L : pageable.getPage() * pageable.getSize();
        Long rows = pageable == null ? limit : pageable.getSize();
        String cname = solrClient.collectionName();
        String fl = "ID";
        log.debug("QUERY: {} Offset: {} Rows: {} Coll: {}", q, offset, rows, cname);
        try {
            SearchResult result = new SearchResult();
            result.setPageable(pageable);

            if (userContextManager.getContext().isAdmin()) {
                SolrResponse response = solrClient.find(cname, q, fl, sort, offset, limit);
                result.setCount(response.getResponse().getNumFound());
                List<String> ids = listIDs(response);
                if (includeMetadata) {
                    if (!ids.isEmpty()) {
                        result.getNodes().addAll(listNodes(ids, true));
                    }
                } else {
                    result.getNodes().addAll(
                        ids.stream()
                            .map(uuid -> {
                                ContentNode node = new ContentNode();
                                node.setUuid(uuid);
                                return node;
                            })
                            .collect(Collectors.toList())
                    );
                }

            } else {
                List<ContentNode> allNodes = new ArrayList<>();
                long start = 0;
                long rowsPerBlock = 1000L;
                long numOfFoundRows = 0;
                do {
                    SolrResponse response = solrClient.find(cname, q, fl, sort, start, rowsPerBlock);
                    numOfFoundRows = response.getResponse().getNumFound();
                    List<String> ids = listIDs(response);
                    if (!ids.isEmpty()) {
                        Map<String,ContentNode> nodeMap = catalogService.mapAllowedContentNodes(ids, includeMetadata);
                        ids.stream()
                            .map(uuid -> nodeMap.get(uuid))
                            .filter(n -> n != null)
                            .forEach(allNodes::add);
                    }

                    start += rowsPerBlock;
                } while (start < numOfFoundRows);

                result.setCount(allNodes.size());
                int last = Integer.min(Long.valueOf(offset + rows).intValue(), allNodes.size());
                result.getNodes().addAll(allNodes.subList(offset.intValue(), last));
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> listIDs(SolrResponse response) {
        return response.getResponse().getDocs().stream()
            .map(x -> x.get("ID").toString())
            .map(x -> {
                if (x.contains("/")) {
                    try {
                        return new URI(x).getPath().substring(1);
                    } catch (URISyntaxException e) {
                        return x;
                    }
                }

                return x;
            }).collect(Collectors.toList());
    }

    private List<ContentNode> listNodes(List<String> ids, boolean includeMetadata) {
        Map<String,ContentNode> nodeMap = catalogService.mapAllowedContentNodes(ids, includeMetadata);
        return ids.stream()
            .map(uuid -> nodeMap.get(uuid))
            .filter(n -> n != null)
            .collect(Collectors.toList());
    }
}
