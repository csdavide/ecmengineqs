package it.doqui.index.ecmengineqs.integration.solr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrResponse {
    private SolrResponseHeader responseHeader;
    private SolrResponsePayload response;
}
