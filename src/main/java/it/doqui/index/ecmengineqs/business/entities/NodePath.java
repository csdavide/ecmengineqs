package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "ix_paths", indexes = {
    @Index(columnList = "lev"),
    @Index(columnList = "node_id")
})
@Getter
@Setter
public class NodePath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "node_id", nullable = false)
    private NodeData node;

    @Column(name = "node_path", nullable = false, columnDefinition = "LONGTEXT")
    private String path;

    @Column(name = "file_path", nullable = false, columnDefinition = "LONGTEXT")
    private String filePath;

    @Column(name = "lev", nullable = false)
    private int lev;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
