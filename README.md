# Distributed Cache Design Patterns

A Java implementation of a distributed in-memory cache demonstrating SOLID principles and classic design patterns including Builder, Factory, Decorator, Strategy, Observer, and Consistent Hashing.

---

## Features

- **LRU Eviction** — doubly-linked list + hash map for O(1) eviction
- **TTL Expiry** — per-entry expiration with optional background sweep
- **Distributed Mode** — consistent hashing across virtual nodes
- **Metrics** — hit rate, miss rate, evictions, expiries via Decorator
- **Events** — pluggable listener system (Observer pattern)
- **Fluent Builder API** — `CacheBuilder` + `CacheFactory` presets

---

## Class Diagram

```mermaid
classDiagram

    %% ─── API Interfaces ───────────────────────────────────────────────────────

    class ICache {
        <<interface>>
        +get(key) Optional~V~
        +put(key, value)
        +put(key, value, ttlMillis)
        +invalidate(key)
        +invalidateAll()
        +size() long
        +containsKey(key) boolean
    }

    class ICacheMetrics {
        <<interface>>
        +hitRate() double
        +missRate() double
        +totalRequests() long
        +totalHits() long
        +totalMisses() long
        +totalEvictions() long
        +totalExpiries() long
        +resetMetrics()
    }

    class ICacheEvents {
        <<interface>>
        +addListener(listener)
        +removeListener(listener)
    }

    %% ─── Service Layer ────────────────────────────────────────────────────────

    class AbstractBaseCache {
        <<abstract>>
        #storage IStorage
        #ttlManager TTLManager
        #listeners List~ICacheListener~
        +get(key) Optional~V~
        +put(key, value)
        +put(key, value, ttlMillis)
        +invalidate(key)
        +invalidateAll()
        +size() long
        +containsKey(key) boolean
        +addListener(listener)
        +removeListener(listener)
        #onHitHook(key, entry)
        #onMissHook(key)
        #onInsertHook(key, entry)
        #onRemoveHook(key)
        #evictIfNeeded()
        #defaultTTLMillis() long
    }

    class LocalCache {
        -evictionPolicy IEvictionPolicy
        -defaultTTLMillis long
        +startTTLSweep(intervalMillis)
        #onHitHook(key, entry)
        #onInsertHook(key, entry)
        #onRemoveHook(key)
        #evictIfNeeded()
        #defaultTTLMillis() long
    }

    class DistributedCache {
        -nodeMapper INodeMapper
        -nodeCaches Map~String, ICache~
        +get(key) Optional~V~
        +put(key, value)
        +invalidate(key)
        +invalidateAll()
        +size() long
        +addNode(address, factory)
        +removeNode(address)
        -cacheFor(key) ICache
    }

    class MetricsCache {
        -delegate ICache
        -hits LongAdder
        -misses LongAdder
        -evictions LongAdder
        -expiries LongAdder
        +get(key) Optional~V~
        +hitRate() double
        +missRate() double
        +totalHits() long
        +totalMisses() long
        +recordEviction()
        +recordExpiry()
    }

    %% ─── Storage Layer ────────────────────────────────────────────────────────

    class IStorage {
        <<interface>>
        +get(key) Optional~CacheEntry~
        +put(key, entry)
        +remove(key) Optional~CacheEntry~
        +containsKey(key) boolean
        +keys() Set~K~
        +entries() Collection~CacheEntry~
        +size() long
        +clear()
    }

    class ConcurrentStorage {
        -map ConcurrentHashMap
        +get(key) Optional~CacheEntry~
        +put(key, entry)
        +remove(key) Optional~CacheEntry~
        +keys() Set~K~
        +entries() Collection~CacheEntry~
        +size() long
        +clear()
    }

    class CacheEntry {
        -key K
        -value V
        -createdAt Instant
        -expiresAt Instant
        +withoutTTL(key, value)$
        +withTTL(key, value, ttlMillis)$
        +isExpired() boolean
        +isImmortal() boolean
        +remainingTTLMillis() long
    }

    %% ─── Eviction Policy ──────────────────────────────────────────────────────

    class IEvictionPolicy {
        <<interface>>
        +onAccess(key)
        +onInsert(key)
        +onRemove(key)
        +evict() Optional~K~
        +getCapacity() int
    }

    class LRUEvictionPolicy {
        -capacity int
        -map Map~K, Node~
        -head Node
        -tail Node
        +onAccess(key)
        +onInsert(key)
        +onRemove(key)
        +evict() Optional~K~
        +getCapacity() int
        -moveToHead(node)
        -addToHead(node)
        -removeNode(node)
    }

    %% ─── Distribution Layer ───────────────────────────────────────────────────

    class INodeMapper {
        <<interface>>
        +getNode(key) String
        +getNodes(key, replicationFactor) List~String~
        +addNode(node)
        +removeNode(node)
        +allNodes() List~String~
    }

    class ConsistentHashMapper {
        -virtualNodes int
        -ring ConcurrentSkipListMap
        -nodePositions Map~String, List~Long~~
        +getNode(key) String
        +getNodes(key, replicationFactor) List~String~
        +addNode(node)
        +removeNode(node)
        +allNodes() List~String~
        -hash(key) long
    }

    %% ─── TTL Layer ────────────────────────────────────────────────────────────

    class TTLManager {
        -storage IStorage
        -listeners List~ICacheListener~
        -scheduler ScheduledExecutorService
        +startScheduledSweep(intervalMillis)
        +checkAndExpire(key) boolean
        +sweep()
        +close()
        -notifyExpiry(entry)
    }

    %% ─── Events Layer ─────────────────────────────────────────────────────────

    class ICacheListener {
        <<interface>>
        +onHit(key, value)
        +onMiss(key)
        +onPut(key, value)
        +onEviction(key, value, cause)
        +onExpiry(key, value)
    }

    class NoOpCacheListener {
        +onHit(key, value)
        +onMiss(key)
        +onPut(key, value)
        +onEviction(key, value, cause)
        +onExpiry(key, value)
    }

    class LoggingCacheListener {
        -log Logger
        +onHit(key, value)
        +onMiss(key)
        +onPut(key, value)
        +onEviction(key, value, cause)
        +onExpiry(key, value)
    }

    class EvictionCause {
        <<enumeration>>
        CAPACITY
        EXPLICIT
        EXPIRED
    }

    %% ─── Factory Layer ────────────────────────────────────────────────────────

    class CacheBuilder {
        -capacity int
        -defaultTTLMillis long
        -enableMetrics boolean
        -evictionPolicy IEvictionPolicy
        -storage IStorage
        -nodeMapper INodeMapper
        -nodes List~String~
        +newBuilder()$
        +capacity(int) CacheBuilder
        +ttl(long) CacheBuilder
        +addNode(String) CacheBuilder
        +metrics(boolean) CacheBuilder
        +evictionPolicy(policy) CacheBuilder
        +buildLocal() ICache
        +buildDistributed() ICache
        -wrap(cache) ICache
    }

    class CacheFactory {
        +lruLocal(capacity, ttlMillis)$  ICache
        +distributed(nodeCapacity, nodes)$  ICache
        +unbounded()$  ICache
    }

    class CacheConfig {
        -capacity int
        -defaultTTLMillis long
        -virtualNodes int
        -enableMetrics boolean
        -enableLogging boolean
        +capacity() int
        +defaultTTLMillis() long
        +virtualNodes() int
        +enableMetrics() boolean
        +enableLogging() boolean
    }

    %% ─── Relationships ────────────────────────────────────────────────────────

    ICache <|.. AbstractBaseCache
    ICacheEvents <|.. AbstractBaseCache
    AbstractBaseCache <|-- LocalCache
    ICache <|.. DistributedCache
    ICache <|.. MetricsCache
    ICacheMetrics <|.. MetricsCache

    IStorage <|.. ConcurrentStorage
    IEvictionPolicy <|.. LRUEvictionPolicy
    INodeMapper <|.. ConsistentHashMapper
    ICacheListener <|.. NoOpCacheListener
    NoOpCacheListener <|-- LoggingCacheListener

    AbstractBaseCache *-- IStorage
    AbstractBaseCache *-- TTLManager
    AbstractBaseCache o-- ICacheListener

    LocalCache *-- IEvictionPolicy
    DistributedCache *-- INodeMapper
    DistributedCache o-- ICache

    MetricsCache o-- ICache

    TTLManager o-- IStorage
    TTLManager o-- ICacheListener

    ConcurrentStorage ..> CacheEntry

    CacheBuilder ..> LocalCache : creates
    CacheBuilder ..> DistributedCache : creates
    CacheBuilder ..> MetricsCache : wraps
    CacheBuilder ..> LRUEvictionPolicy : uses
    CacheBuilder ..> ConsistentHashMapper : uses
    CacheBuilder ..> ConcurrentStorage : uses

    CacheFactory ..> CacheBuilder : uses

    ICacheListener ..> EvictionCause
```

---

## Package Structure

```
src/
├── Main.java
├── api/
│   ├── ICache.java            # Core cache interface
│   ├── ICacheMetrics.java     # Metrics interface
│   └── ICacheEvents.java      # Listener registration interface
├── service/
│   ├── AbstractBaseCache.java # Template Method base
│   ├── LocalCache.java        # LRU + TTL local cache
│   ├── DistributedCache.java  # Consistent-hash sharding
│   └── MetricsCache.java      # Decorator: metrics tracking
├── storage/
│   ├── IStorage.java          # Storage abstraction
│   ├── ConcurrentStorage.java # ConcurrentHashMap impl
│   └── CacheEntry.java        # Value wrapper with TTL metadata
├── eviction/
│   ├── IEvictionPolicy.java   # Eviction strategy interface
│   └── LRUEvictionPolicy.java # Doubly-linked list LRU
├── distribution/
│   ├── INodeMapper.java       # Node routing interface
│   └── ConsistentHashMapper.java # MD5-based virtual ring
├── ttl/
│   └── TTLManager.java        # TTL check + scheduled sweep
├── events/
│   ├── ICacheListener.java    # Observer interface
│   ├── EvictionCause.java     # Eviction reason enum
│   ├── NoOpCacheListener.java # No-op base
│   └── LoggingCacheListener.java # java.util.logging impl
└── factory/
    ├── CacheBuilder.java      # Fluent builder
    ├── CacheFactory.java      # Static preset factory
    └── CacheConfig.java       # Immutable config value object
```

---

## Design Patterns Used

| Pattern | Where |
|---|---|
| **Builder** | `CacheBuilder` — fluent API for constructing any cache variant |
| **Factory Method** | `CacheFactory` — static presets (`lruLocal`, `distributed`, `unbounded`) |
| **Decorator** | `MetricsCache` wraps any `ICache` to add metrics without changing behaviour |
| **Template Method** | `AbstractBaseCache` defines the algorithm skeleton; subclasses override hooks |
| **Strategy** | `IEvictionPolicy` — swap LRU for any eviction algorithm at construction time |
| **Observer** | `ICacheListener` / `ICacheEvents` — fire-and-forget event callbacks |
| **Consistent Hashing** | `ConsistentHashMapper` — MD5 virtual node ring for stable key routing |

---

## Quick Start

```java
// Local LRU cache with TTL and metrics
ICache<String, String> cache = CacheBuilder.<String, String>newBuilder()
        .capacity(1000)
        .ttl(60_000)          // 60s TTL
        .metrics(true)
        .buildLocal();

cache.put("key", "value");
cache.get("key");             // Optional.of("value")

// Distributed across 3 nodes
ICache<String, String> dist = CacheBuilder.<String, String>newBuilder()
        .capacity(500)
        .addNode("node1:6379")
        .addNode("node2:6379")
        .addNode("node3:6379")
        .metrics(true)
        .buildDistributed();

// One-liner presets
ICache<String, String> lru  = CacheFactory.lruLocal(1_000, 60_000);
ICache<String, String> dist = CacheFactory.distributed(500, "node1", "node2");
```

---

## Running

```bash
javac -sourcepath src -d out src/Main.java
java -cp out Main
```
