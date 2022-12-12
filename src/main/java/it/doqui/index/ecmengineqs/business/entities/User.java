package it.doqui.index.ecmengineqs.business.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "ix_users", indexes = {
    @Index(columnList = "tenant,uuid", unique = true),
    @Index(columnList = "tenant,username", unique = true)
})
@Getter
@Setter
@ToString
@TypeDef(
    name = "json",
    typeClass = JsonType.class
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenant;

    @Column(nullable = false)
    private String uuid;

    @Column(nullable = false)
    private String username;

    @Type(type = "json")
    @Column(name = "data", columnDefinition = "JSON")
    private final Map<String, Object> data;

    public User() {
        data = new HashMap<>();
    }

}
