package it.doqui.index.ecmengineqs.business.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class SortDefinition {
    private String fieldName;
    boolean ascending;
}
