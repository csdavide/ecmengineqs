package it.doqui.index.ecmengineqs.business.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RenditionDTO {

    private String uuid;
    private String description;
    private String genMimeType;
    private String renditionId;
    private String mimeType;
    private byte[] binaryData;

}
