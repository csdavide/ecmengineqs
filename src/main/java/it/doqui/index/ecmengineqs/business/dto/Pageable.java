package it.doqui.index.ecmengineqs.business.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Pageable {
    private int page;
    private int size;
}
