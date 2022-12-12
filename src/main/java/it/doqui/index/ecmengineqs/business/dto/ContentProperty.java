package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.utils.I18NUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class ContentProperty {
    @Getter
    @Setter
    private String name;

    private final LinkedHashMap<String,String> attributes;

    public ContentProperty() {
        this.attributes = new LinkedHashMap<>();
    }

    public void parseAttributes(String value) {
        this.attributes.clear();
        String a[] = value.split("\\|");
        for (int i = 0; i < a.length; i++) {
            String b[] = a[i].split("=");
            if (b.length > 0) {
                String k = b[0];
                String v = b.length > 1 && !StringUtils.equals(b[1], "null") ? b[1] : null;
                this.attributes.put(k,v);
            }
        }
    }

    public String getAttribute(String key) {
        return this.attributes.get(key);
    }

    public void setAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public Locale getLocale() {
        return I18NUtils.parseLocale(this.attributes.get("locale"));
    }

    @Override
    public String toString() {
        return attributes.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("|"));
    }
}
