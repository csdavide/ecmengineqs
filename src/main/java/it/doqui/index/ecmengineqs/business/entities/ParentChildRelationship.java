package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity
@Table(name = "ix_children", indexes = {
    @Index(columnList = "parent_id"),
    @Index(columnList = "child_id")
})
@Getter
@Setter
@Accessors(chain = true)
public class ParentChildRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = false)
    private NodeData parent;

    @ManyToOne
    @JoinColumn(name = "child_id", nullable = false)
    private NodeData child;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

}
