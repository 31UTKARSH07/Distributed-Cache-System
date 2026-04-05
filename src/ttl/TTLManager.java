package ttl;
import events.ICacheListener;
import storage.CacheEntry;
import storage.IStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TTLManager<K, V> implements AutoCloseable {
    private static final Logger log = Logger.getLogger(TTLManager.class.getName());

    private final IStorage<K, V> storage;
    private final List<ICacheListener<K, V>> listeners;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> sweepTask;

    public TTLManager(IStorage<K, V> storage, List<ICacheListener<K, V>> listeners) {
        this.storage   = storage;
        this.listeners = listeners;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-ttl-sweep");
            t.setDaemon(true);
            return t;
        });
    }

    public void startScheduledSweep(long intervalMillis) {
        if (sweepTask != null) sweepTask.cancel(false);
        sweepTask = scheduler.scheduleAtFixedRate(
                this::sweep, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS
        );
        log.fine("TTL sweep scheduled every " + intervalMillis + "ms");
    }
    public boolean checkAndExpire(K key) {
        return storage.get(key)
                .filter(CacheEntry::isExpired)
                .map(entry -> {
                    storage.remove(key);
                    notifyExpiry(entry);
                    return true;
                })
                .orElse(false);
    }
    public void sweep() {
        List<K> expired = new ArrayList<>();
        for (CacheEntry<K, V> entry : storage.entries()) {
            if (entry.isExpired()) expired.add(entry.getKey());
        }
        for (K key : expired) {
            storage.remove(key).ifPresent(this::notifyExpiry);
        }
        if (!expired.isEmpty()) {
            log.fine(() -> "TTL sweep removed " + expired.size() + " expired entries");
        }
    }
    private void notifyExpiry(CacheEntry<K, V> entry) {
        for (ICacheListener<K, V> listener : listeners) {
            try {
                listener.onExpiry(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.log(Level.WARNING, "Listener threw on onExpiry", e);
            }
        }
    }

    @Override
    public void close() {
        if (sweepTask != null) sweepTask.cancel(false);
        scheduler.shutdownNow();
    }
}
