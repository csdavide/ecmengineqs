package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "ix_mimetypes", indexes = {
    @Index(columnList = "file_extension")
})
@Getter
@Setter
@ToString
public class MimeTypeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_extension", nullable = false)
    private String fileExtension;

    @Column(name = "mimetype", nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private int priority;
}
