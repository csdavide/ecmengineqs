package it.doqui.index.ecmengineqs.business.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "ix_roots")
@Getter
@Setter
public class RootRef {

    @Id
    private String tenant;

    @OneToOne
    @JoinColumn(name = "root_id", nullable = false)
    private NodeData root;

}
