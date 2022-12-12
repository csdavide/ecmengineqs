package it.doqui.index.ecmengineqs.business.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.doqui.index.ecmengineqs.business.entities.RootRef;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RootRefRepository implements PanacheRepositoryBase<RootRef,String> {
}
