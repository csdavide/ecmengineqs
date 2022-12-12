package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "ix_removed_nodes", indexes = {
    @Index(columnList = "tenant,uuid", unique = true)
})
@Getter
@Setter
@ToString
public class RemovedNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenant;

    @Column(nullable = false)
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "tx", nullable = false)
    private ApplicationTransaction tx;

}
