package api;

import events.ICacheListener;

public interface ICacheEvents<K,V> {
    void addListener(ICacheListener<K,V>listener);
    void removeListener(ICacheListener<K,V>listener);
}
