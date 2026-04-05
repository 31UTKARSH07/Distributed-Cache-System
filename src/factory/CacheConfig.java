package factory;

public final class CacheConfig {
    public static final long NO_TTL = 0L;
    public static final int DEFAULT_CAPACITY = 1_000;
    public static final int DEFAULT_VIRT_NODES = 150;

    private final int capacity;
    private final long defaultTTLMillis;
    private final int virtualNodes;
    private final boolean enableMetrics;
    private final boolean enableLogging;

    CacheConfig(int capacity,
                long defaultTTLMillis,
                int virtualNodes,
                boolean enableMetrics,
                boolean enableLogging) {
        this.capacity = capacity;
        this.defaultTTLMillis = defaultTTLMillis;
        this.virtualNodes = virtualNodes;
        this.enableMetrics = enableMetrics;
        this.enableLogging = enableLogging;
    }

    public int capacity() { return capacity; }
    public long defaultTTLMillis() { return defaultTTLMillis; }
    public int virtualNodes() { return virtualNodes; }
    public boolean enableMetrics() { return enableMetrics; }
    public boolean enableLogging() { return enableLogging; }

    @Override
    public String toString() {
        return String.format("CacheConfig[Cap=%d, TTL=%dms, Nodes=%d, Metrics=%b, Log=%b]",
                capacity, defaultTTLMillis, virtualNodes, enableMetrics, enableLogging);
    }
}