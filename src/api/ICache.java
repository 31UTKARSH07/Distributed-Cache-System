package api;

import java.util.Optional;

public interface ICache <K,V>{

    Optional<V>get(K key);

    void put(K key , V value);
    void put(K key , V value , long ttlMillis);
    void invalidate(K key);
    void invalidateAll();
    long size();
    boolean containsKey(K key);

}
