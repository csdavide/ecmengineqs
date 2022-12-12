package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.repositories.RootRefRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TenantService {

    @Inject
    RootRefRepository rootRefRepository;

    public List<String> getAllTenants() {
        return rootRefRepository.findAll().stream().map(r -> r.getTenant()).collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rootRefRepository.findByIdOptional(name).isPresent();
    }
}
