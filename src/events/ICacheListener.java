package events;

public interface ICacheListener <K,V>{
    void onHit(K key,V value);
    void onMiss(K key);
    void onPut(K key, V value);
    void onEviction(K key, V value, EvictionCause cause);
    void onExpiry(K key, V value);
}
