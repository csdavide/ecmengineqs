package it.doqui.index.ecmengineqs.business.search;

import it.doqui.index.ecmengineqs.business.dto.ContentNode;
import it.doqui.index.ecmengineqs.business.dto.Pageable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class SearchResult {
    private Pageable pageable;
    private long count;
    private final List<ContentNode> nodes;

    public SearchResult() {
        nodes = new ArrayList<>();
    }
}
