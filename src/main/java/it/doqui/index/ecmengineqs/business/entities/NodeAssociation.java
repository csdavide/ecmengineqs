package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "ix_associations", indexes = {
    @Index(columnList = "source_id"),
    @Index(columnList = "target_id")
})
@Getter
@Setter
public class NodeAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private NodeData source;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private NodeData target;

    @Column(name = "type_name", nullable = false)
    private String typeName;

}
