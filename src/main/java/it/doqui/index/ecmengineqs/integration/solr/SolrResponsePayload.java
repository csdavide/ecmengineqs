package it.doqui.index.ecmengineqs.integration.solr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrResponsePayload {
    private Long numFound;
    private Long start;
    private Boolean numFoundExact;
    private List<Map<String, Object>> docs;
}
