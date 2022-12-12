package it.doqui.index.ecmengineqs.business.search;

public class SolrQueryElement {
    private int start;
    private int end;
    private int separator;
    private String conversion;
    private boolean wildCard;

    public SolrQueryElement() {
        this.conversion = null;
    }

    public SolrQueryElement(int start, int end, int separator, boolean wildCard) {
        this.start = start;
        this.end = end;
        this.separator = separator;
        this.wildCard = wildCard;
        this.conversion = null;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getSeparator() {
        return separator;
    }

    public void setSeparator(int separator) {
        this.separator = separator;
    }

    public String getConversion() {
        return conversion;
    }

    public void setConversion(String conversion) {
        this.conversion = conversion;
    }

    public boolean isWildCard() {
        return wildCard;
    }

    public void setWildCard(boolean wildCard) {
        this.wildCard = wildCard;
    }
}
