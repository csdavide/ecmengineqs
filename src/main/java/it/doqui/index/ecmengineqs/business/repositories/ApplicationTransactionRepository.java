package it.doqui.index.ecmengineqs.business.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.doqui.index.ecmengineqs.business.entities.ApplicationTransaction;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationTransactionRepository implements PanacheRepository<ApplicationTransaction> {
}
