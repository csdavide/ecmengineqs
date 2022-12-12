package it.doqui.index.ecmengineqs.integration.solr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrResponseHeader {
    private int status;
    private Integer QTime;
    private String rf;
    private Boolean zkConnected;
    private Map<String, String> params;
}
