package service;

import api.ICache;
import api.ICacheEvents;
import events.EvictionCause;
import events.ICacheListener;
import storage.CacheEntry;
import storage.IStorage;
import ttl.TTLManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractBaseCache<K, V>
        implements ICache<K, V>, ICacheEvents<K, V> {
    private static final Logger log = Logger.getLogger(AbstractBaseCache.class.getName());
    protected final IStorage<K, V> storage;
    protected final TTLManager<K, V> ttlManager;
    protected final List<ICacheListener<K, V>> listeners = new ArrayList<>();

    protected AbstractBaseCache(IStorage<K, V> storage) {
        this.storage    = storage;
        this.ttlManager = new TTLManager<>(storage, listeners);
    }
    @Override
    public final Optional<V> get(K key) {
        if (ttlManager.checkAndExpire(key)) {
            fireOnMiss(key);
            return Optional.empty();
        }
        Optional<CacheEntry<K, V>> entry = storage.get(key);
        if (entry.isEmpty()) {
            onMissHook(key);
            fireOnMiss(key);
            return Optional.empty();
        }
        onHitHook(key, entry.get());
        fireOnHit(key, entry.get().getValue());
        return Optional.of(entry.get().getValue());
    }

    @Override
    public final void put(K key, V value) {
        put(key, value, defaultTTLMillis());
    }

    @Override
    public final void put(K key, V value, long ttlMillis) {
        CacheEntry<K, V> entry = (ttlMillis <= 0)
                ? CacheEntry.withoutTTL(key, value)
                : CacheEntry.withTTL(key, value, ttlMillis);

        storage.put(key, entry);
        onInsertHook(key, entry);
        fireOnPut(key, value);
        evictIfNeeded();
    }
    @Override
    public final void invalidate(K key) {
        storage.remove(key).ifPresent(entry -> {
            onRemoveHook(key);
            fireOnEviction(key, entry.getValue(), EvictionCause.EXPLICIT);
        });
    }
    @Override
    public final void invalidateAll() {
        storage.keys().forEach(this::invalidate);
    }
    @Override
    public final long size() {
        return storage.size();
    }
    @Override
    public final boolean containsKey(K key) {
        if (ttlManager.checkAndExpire(key)) return false;
        return storage.containsKey(key);
    }
    @Override
    public void addListener(ICacheListener<K, V> listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }
    @Override
    public void removeListener(ICacheListener<K, V> listener) {
        listeners.remove(listener);
    }

    protected void onHitHook(K key, CacheEntry<K, V> entry) {}
    protected void onMissHook(K key) {}
    protected void onInsertHook(K key, CacheEntry<K, V> entry) {}
    protected void onRemoveHook(K key) {}
    protected void evictIfNeeded() {}
    protected long defaultTTLMillis() { return 0L; }
    private void fireOnHit(K key, V value) {
        for (ICacheListener<K, V> l : listeners) safeCall(() -> l.onHit(key, value));
    }

    private void fireOnMiss(K key) {
        for (ICacheListener<K, V> l : listeners) safeCall(() -> l.onMiss(key));
    }
    private void fireOnPut(K key, V value) {
        for (ICacheListener<K, V> l : listeners) safeCall(() -> l.onPut(key, value));
    }
    private void fireOnEviction(K key, V value, EvictionCause cause) {
        for (ICacheListener<K, V> l : listeners) safeCall(() -> l.onEviction(key, value, cause));
    }
    private void safeCall(Runnable r) {
        try { r.run(); } catch (Exception e) {
            log.log(Level.WARNING, "Cache listener threw an exception", e);
        }
    }
}

