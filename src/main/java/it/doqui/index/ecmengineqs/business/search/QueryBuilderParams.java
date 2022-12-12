package it.doqui.index.ecmengineqs.business.search;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO interno contenente i parametri da passare al {@link QueryBuilder} per la
 * generazione della query.
 *
 * @author DoQui
 */
public class QueryBuilderParams implements Serializable {

    private static final long serialVersionUID = 7082528747772541928L;

    private String contentType;
    private String mimeType;
    private Map<String, String> attributes;
    private String fullTextQuery;
    private boolean fullTextAllWords;

    /** Costruttore predefinito. */
    public QueryBuilderParams() {
        this.attributes = new LinkedHashMap<String, String>();
    }

    public void addAttribute(String name, String value) {
        this.attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFullTextQuery() {
        return this.fullTextQuery;
    }

    public void setFullTextQuery(String fullTextQuery) {
        this.fullTextQuery = fullTextQuery;
    }

    public boolean isFullTextAllWords() {
        return this.fullTextAllWords;
    }

    public void setFullTextAllWords(boolean fullTextAllWords) {
        this.fullTextAllWords = fullTextAllWords;
    }
}
