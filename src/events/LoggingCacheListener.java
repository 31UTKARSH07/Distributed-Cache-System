package events;

import java.util.logging.Logger;

public class LoggingCacheListener <K,V> extends NoOpCacheListener<K,V>{
    private static final Logger log = Logger.getLogger(LoggingCacheListener.class.getName());
    @Override
    public void onHit(K key, V value) {
        log.fine(() -> "[CACHE HIT]  key=" + key);
    }
    @Override
    public void onMiss(K key) {
        log.fine(() -> "[CACHE MISS] key=" + key);
    }
    @Override
    public void onPut(K key, V value) {
        log.fine(() -> "[CACHE PUT]  key=" + key);
    }
    @Override
    public void onEviction(K key, V value, EvictionCause cause) {
        log.info(() -> "[EVICTION]   key=" + key + " cause=" + cause);
    }
    @Override
    public void onExpiry(K key, V value) {
        log.info(() -> "[EXPIRED]    key=" + key);
    }
}
