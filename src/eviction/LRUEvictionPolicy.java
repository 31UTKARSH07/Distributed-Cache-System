package eviction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LRUEvictionPolicy<K, V> implements IEvictionPolicy<K, V> {
    private final int capacity;
    private final Map<K, Node<K>> map;
    private final Node<K> head, tail;
    public LRUEvictionPolicy(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null);
        this.tail = new Node<>(null);
        head.next = tail;
        tail.prev = head;
    }
    @Override
    public void onAccess(K key) {
        Node<K> node = map.get(key);
        if (node != null) {
            moveToHead(node);
        }
    }

    @Override
    public void onInsert(K key) {
        if (map.containsKey(key)) {
            moveToHead(map.get(key));
        } else {
            Node<K> newNode = new Node<>(key);
            map.put(key, newNode);
            addToHead(newNode);
        }
    }
    @Override
    public void onRemove(K key) {
        Node<K> node = map.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }
    @Override
    public Optional<K> evict() {
        if (map.size() <= capacity) {
            return Optional.empty();
        }
        Node<K> lruNode = tail.prev;
        K key = lruNode.key;
        onRemove(key);
        return Optional.of(key);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
    private void moveToHead(Node<K> node) {
        removeNode(node);
        addToHead(node);
    }
    private void addToHead(Node<K> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    private void removeNode(Node<K> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    private static class Node<K> {
        K key;
        Node<K> prev, next;
        Node(K key) { this.key = key; }
    }
}