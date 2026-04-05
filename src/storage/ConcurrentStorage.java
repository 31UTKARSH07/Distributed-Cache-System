package storage;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentStorage<K, V> implements IStorage<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<K, V>> map;

    public ConcurrentStorage() {
        this.map = new ConcurrentHashMap<>();
    }
    public ConcurrentStorage(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }
    @Override
    public Optional<CacheEntry<K, V>> get(K key) {
        return Optional.ofNullable(map.get(key));
    }
    @Override
    public void put(K key, CacheEntry<K, V> entry) {
        map.put(key, entry);
    }
    @Override
    public Optional<CacheEntry<K, V>> remove(K key) {
        return Optional.ofNullable(map.remove(key));
    }
    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }
    @Override
    public Set<K> keys() {
        return map.keySet();
    }
    @Override
    public Collection<CacheEntry<K, V>> entries() {
        return map.values();
    }
    @Override
    public long size() {
        return map.size();
    }
    @Override
    public void clear() {
        map.clear();
    }
}
