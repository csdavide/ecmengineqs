package it.doqui.index.ecmengineqs.business.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogResponse<T> {

    private T result;
    private boolean indexed;

}
