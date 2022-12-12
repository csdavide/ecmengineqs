package it.doqui.index.ecmengineqs.integration.indexing;

import it.doqui.index.ecmengineqs.business.services.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.concurrent.*;

@ApplicationScoped
@Slf4j
public class IndexerDelegate {

    @ConfigProperty(name = "solr.indexer.pool.concurrency", defaultValue = "8")
    int concurrency;

    @Inject
    Indexer indexer;

    @Inject
    TransactionService transactionService;

    private ThreadPoolExecutor syncExecutor;
    private ExecutorService asyncExecutor;

    public IndexerDelegate() {
        syncExecutor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        asyncExecutor = null;
    }

    @PostConstruct
    void init() {
        syncExecutor.setCorePoolSize(0);
        syncExecutor.setMaximumPoolSize(Integer.MAX_VALUE);
        syncExecutor.setKeepAliveTime(60, TimeUnit.SECONDS);
        asyncExecutor = Executors.newFixedThreadPool(concurrency);
    }

    public Future<Boolean> submit(IndexingOperationSet operationSet, boolean async) {
        Callable<Boolean> task = () -> {
            try {
                return indexer.index(operationSet, async);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        };

        if (async) {
            asyncExecutor.submit(task);
            return ConcurrentUtils.constantFuture(false);
        }

        return syncExecutor.submit(task);
    }

    public void submitIndexAll(String tenant) {
        asyncExecutor.submit(() -> indexAll(tenant));
    }

    @Transactional
    void indexAll(String tenant) {
        log.debug("Indexing all pending transaction at {}", tenant);
        transactionService
            .streamTransactionsToIndex(tenant)
            .map(tx -> new IndexingOperationSet().setTenant(tenant).setTxId(tx.getUuid()))
            .forEach(op -> submit(op, true));
    }
}
