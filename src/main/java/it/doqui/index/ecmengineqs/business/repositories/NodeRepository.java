package it.doqui.index.ecmengineqs.business.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.doqui.index.ecmengineqs.business.entities.NodeData;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NodeRepository implements PanacheRepository<NodeData> {
}
