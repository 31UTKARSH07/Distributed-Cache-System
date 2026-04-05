package storage;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;


public interface IStorage<K, V> {
    Optional<CacheEntry<K, V>> get(K key);
    void put(K key, CacheEntry<K, V> entry);
    Optional<CacheEntry<K, V>> remove(K key);
    boolean containsKey(K key);
    Set<K> keys();
    Collection<CacheEntry<K, V>> entries();
    long size();
    void clear();
}
