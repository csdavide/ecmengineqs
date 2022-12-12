package it.doqui.index.ecmengineqs.business.services;

import it.doqui.index.ecmengineqs.business.entities.ApplicationTransaction;
import it.doqui.index.ecmengineqs.business.repositories.ApplicationTransactionRepository;
import it.doqui.index.ecmengineqs.business.repositories.RemovedNodeRepository;
import it.doqui.index.ecmengineqs.foundation.UserContext;
import it.doqui.index.ecmengineqs.foundation.UserContextManager;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class TransactionService {

    @Inject
    UserContextManager userContextManager;

    @Inject
    ApplicationTransactionRepository txRepository;

    @Inject
    RemovedNodeRepository changeRepository;

    public ApplicationTransaction createTransaction() {
        ApplicationTransaction tx = new ApplicationTransaction();
        UserContext ctx = userContextManager.getContext();
        tx.setTenant(ctx.getTenant());
        tx.setUuid(ctx.getOperationId());
        txRepository.persist(tx);
        return tx;
    }

    public void setTransactionIndexedNow(String tenant, String txId) {
        txRepository.update("set indexedAt = ?1 where tenant = ?2 and uuid = ?3", ZonedDateTime.now(), tenant, txId);
    }

    public Stream<ApplicationTransaction> streamTransactionsToIndex(String tenant) {
        return txRepository.find("tenant = ?1 and (indexedAt is null or indexedAt <= createdAt)", tenant).stream();
    }
}
