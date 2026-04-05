package service;

import api.ICache;
import distribution.INodeMapper;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedCache <K,V> implements ICache<K,V> {
    private final INodeMapper<K> nodeMapper;

    private final Map<String, ICache<K, V>> nodeCaches;
    public interface NodeCacheFactory<K, V> {
        ICache<K, V> create(String nodeAddress);
    }

    public DistributedCache(INodeMapper<K> nodeMapper,
                            NodeCacheFactory<K, V> factory) {
        this.nodeMapper  = nodeMapper;
        this.nodeCaches  = new ConcurrentHashMap<>();
        nodeMapper.allNodes().forEach(node ->
                nodeCaches.put(node, factory.create(node))
        );
    }
    @Override
    public Optional<V> get(K key) {
        return cacheFor(key).get(key);
    }

    @Override
    public void put(K key, V value) {
        cacheFor(key).put(key, value);
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        cacheFor(key).put(key, value, ttlMillis);
    }

    @Override
    public void invalidate(K key) {
        cacheFor(key).invalidate(key);
    }
    @Override
    public void invalidateAll() {
        nodeCaches.values().forEach(ICache::invalidateAll);
    }
    @Override
    public long size() {
        return nodeCaches.values().stream().mapToLong(ICache::size).sum();
    }
    @Override
    public boolean containsKey(K key) {
        return cacheFor(key).containsKey(key);
    }

    public void addNode(String nodeAddress, NodeCacheFactory<K, V> factory) {
        nodeMapper.addNode(nodeAddress);
        nodeCaches.put(nodeAddress, factory.create(nodeAddress));
    }

    public void removeNode(String nodeAddress) {
        nodeMapper.removeNode(nodeAddress);
        nodeCaches.remove(nodeAddress);
    }

    private ICache<K, V> cacheFor(K key) {
        String node = nodeMapper.getNode(key);
        ICache<K, V> cache = nodeCaches.get(node);
        if (cache == null) {
            throw new IllegalStateException("No cache found for node: " + node);
        }
        return cache;
    }
}
