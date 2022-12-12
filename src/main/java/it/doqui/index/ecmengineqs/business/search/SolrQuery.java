package it.doqui.index.ecmengineqs.business.search;

import java.util.ArrayList;

public class SolrQuery {

    private char[] originalQuery;
    private ArrayList<SolrQueryElement> elements;

    public SolrQuery(String query) {
        this.originalQuery = query.toCharArray();
        elements = new ArrayList<>();
    }

    public char[] getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(char[] originalQuery) {
        this.originalQuery = originalQuery;
    }

    public void addElem(int startElem, int endElem, int separator, boolean wildCard) {
        elements.add(new SolrQueryElement(startElem, endElem, separator, wildCard));
    }

    public ArrayList<SolrQueryElement> getElements() {
        return elements;
    }

    public void setElements(ArrayList<SolrQueryElement> elements) {
        this.elements = elements;
    }
}
