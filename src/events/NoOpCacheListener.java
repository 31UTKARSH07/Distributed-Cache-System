package events;

public class NoOpCacheListener<K, V> implements ICacheListener<K, V> {
    @Override public void onHit(K key, V value) {}
    @Override public void onMiss(K key) {}
    @Override public void onPut(K key, V value) {}
    @Override public void onEviction(K key, V value, EvictionCause cause) {}
    @Override public void onExpiry(K key, V value) {}
}

