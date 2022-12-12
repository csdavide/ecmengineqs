package it.doqui.index.ecmengine.mtom.dto;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlMimeType;

/**
 * Classe DTO che rappresenta un contenuto fisico.
 * 
 * @author DoQui
 *
 */
public class Attachment extends MtomEngineDto {
    private static final long serialVersionUID = -1433123104947549789L;

    /**
     * Il nome del file fisico.
     */
    public String fileName;

    /**
     * Il mimetype del file fisico.
     */
    public String fileType;

    /**
     * La dimensione del file fisico.
     */
    public long fileSize;

    @XmlMimeType("application/octet-stream")

    /**
     * Il contenuto fisico.
     */
    public DataHandler attachmentDataHandler;

    public Attachment() {
    }
}
