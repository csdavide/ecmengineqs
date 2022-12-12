package it.doqui.index.ecmengineqs.business.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.doqui.index.ecmengineqs.business.entities.NodePath;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NodePathRepository implements PanacheRepository<NodePath> {
}
