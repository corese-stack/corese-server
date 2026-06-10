package fr.inria.corese.server.service;

import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.store.TripleStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background service that periodically dumps the triplestore to a NTriples file.
 */
public class ScheduledDumpService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDumpService.class);

    private final TripleStoreManager store;
    private final ServerConfig config;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong dumpCount = new AtomicLong(0);

    private ScheduledFuture<?> task;

    /**
     * Creates a new ScheduledDumpService.
     *
     * @param store  the triplestore to dump
     * @param config the server configuration
     */
    public ScheduledDumpService(TripleStoreManager store, ServerConfig config) {
        this.store = store;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "corese-dump-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start the periodic dump scheduler.
     */
    public void start() {
        int interval = config.dumpIntervalSeconds();

        if (interval <= 0) {
            log.info("Scheduled dump disabled (CORESE_DUMP_INTERVAL=0)");
            return;
        }

        if (config.dumpPath() == null || config.dumpPath().isBlank()) {
            log.warn("Scheduled dump skipped — CORESE_DUMP_PATH not configured");
            return;
        }

        task = scheduler.scheduleAtFixedRate(
                this::runDump,
                interval,
                interval,
                TimeUnit.SECONDS
        );

        log.info("Scheduled dump started — every {} second(s) → {}",
                interval, config.dumpPath());
    }

    /**
     * Stop the periodic dump scheduler.
     * Does not trigger a final dump — the shutdown hook in ServerApplication handles that.
     */
    public void stop() {
        if (task != null) {
            task.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Scheduled dump stopped — {} dump(s) completed", dumpCount.get());
    }

    /**
     * Returns the number of successful dumps performed since start.
     *
     * @return dump count
     */
    public long getDumpCount() {
        return dumpCount.get();
    }

    private void runDump() {
        try {
            log.debug("Scheduled dump #{} starting...", dumpCount.get() + 1);
            store.dumpToFile(config.dumpPath());
            dumpCount.incrementAndGet();
            log.debug("Scheduled dump #{} complete", dumpCount.get());
        } catch (Exception e) {
            log.error("Scheduled dump failed: {}", e.getMessage(), e);
        }
    }
}
