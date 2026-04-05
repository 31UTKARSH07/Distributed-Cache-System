package eviction;

import java.util.Optional;

public interface IEvictionPolicy <K,V> {
    void onAccess(K key);
    void onInsert(K key);
    void onRemove(K key);
    Optional<K> evict();
    int getCapacity();
}
