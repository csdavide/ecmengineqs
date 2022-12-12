package it.doqui.index.ecmengineqs.business.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import it.doqui.index.ecmengineqs.foundation.NodeReferenceable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Table(name = "ix_nodes", indexes = {
    @Index(columnList = "tenant,uuid", unique = true)
})
@Getter
@Setter
@Accessors(chain = true)
@ToString
@TypeDef(
    name = "json",
    typeClass = JsonType.class
)
public class NodeData implements NodeReferenceable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String tenant;

    @Column(nullable = false)
    private String uuid;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Type(type = "json")
    @Column(name = "data", columnDefinition = "JSON")
    private final Map<String, Object> data;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "child", fetch = FetchType.LAZY)
    @OrderBy("primary DESC")
    private final List<ParentChildRelationship> parents;

    @JsonIgnore
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private final List<ParentChildRelationship> children;

    @JsonIgnore
    @OneToMany(mappedBy = "source", fetch = FetchType.LAZY)
    private final List<NodeAssociation> originatedAssociations;

    @JsonIgnore
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    private final List<NodeAssociation> terminatedAssociations;

    @JsonIgnore
    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private final List<NodePath> paths;

    @ManyToOne
    @JoinColumn(name = "tx")
    private ApplicationTransaction tx;

    public NodeData() {
        data = new HashMap<>();
        parents = new ArrayList<>();
        children = new ArrayList<>();
        originatedAssociations = new ArrayList<>();
        terminatedAssociations = new ArrayList<>();
        paths = new ArrayList<>();
    }

    public Object getProperty(String name) {
        switch (name) {
            case "sys:node-dbid":
                return id;
            case "sys:node-uuid":
                return uuid;
            case "sys:store-protocol":
                return kind;
            case "sys:store-identifier":
                return "SpacesStore";
            default:
                Map<String,Object> properties = (Map<String, Object>) data.get("properties");
                return properties.get(name);
        }
    }

    public void setProperty(String name, Object value) {
        Map<String,Object> properties = (Map<String, Object>) data.get("properties");
        properties.put(name, value);
    }

    @PrePersist
    private void prePersist() {
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeData nodeData = (NodeData) o;
        return Objects.equals(tenant, nodeData.tenant) && Objects.equals(uuid, nodeData.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, uuid);
    }

    @Override
    public URI getURI() {
        try {
            return new URI(String.format("%s://@%s@SpacesStore/%s", kind, tenant, uuid));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
