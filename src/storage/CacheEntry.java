package storage;

import java.time.Instant;
public final class CacheEntry<K, V> {

    private final K key;
    private final V value;
    private final Instant createdAt;
    private final Instant expiresAt;

    private CacheEntry(K key, V value, Instant createdAt, Instant expiresAt) {
        this.key  = key;
        this.value = value;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    public static <K, V> CacheEntry<K, V> withoutTTL(K key, V value) {
        return new CacheEntry<>(key, value, Instant.now(), null);
    }
    public static <K, V> CacheEntry<K, V> withTTL(K key, V value, long ttlMillis) {
        Instant now = Instant.now();
        return new CacheEntry<>(key, value, now, now.plusMillis(ttlMillis));
    }

    public K getKey(){ return key; }
    public V getValue(){ return value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    public boolean isImmortal() {
        return expiresAt == null;
    }

    public long remainingTTLMillis() {
        if (isImmortal()) return -1L;
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    @Override
    public String toString() {
        return "CacheEntry{key=" + key +
                ", expiresAt=" + (expiresAt != null ? expiresAt : "never") + "}";
    }
}
