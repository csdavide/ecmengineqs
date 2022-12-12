package it.doqui.index.ecmengineqs.business.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ix_transactions", indexes = {
    @Index(columnList = "tenant,uuid", unique = true)
})
@Getter
@Setter
@ToString
public class ApplicationTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenant;

    @Column(nullable = false)
    private String uuid;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "tx", fetch = FetchType.LAZY)
    private final List<NodeData> nodes;

    @JsonIgnore
    @OneToMany(mappedBy = "tx", fetch = FetchType.LAZY)
    private final List<RemovedNode> removedRefs;

    @Column(name = "indexed_at")
    private ZonedDateTime indexedAt;

    public ApplicationTransaction() {
        nodes = new ArrayList<>();
        removedRefs = new ArrayList<>();
    }

    @PrePersist
    private void prePersist() {
        createdAt = ZonedDateTime.now();
    }
}
