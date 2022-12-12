package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "ix_models", indexes = {
    @Index(columnList = "tenant")
})
@Getter
@Setter
@ToString
public class CustomModelData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenant;

    @Column(name = "model_filename", nullable = false)
    private String filename;

    @Column(name = "model_name")
    private String name;

    @Column(name = "model_title")
    private String title;

    @Column(name = "model_desc", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "data", columnDefinition = "LONGTEXT")
    private String data;
}
