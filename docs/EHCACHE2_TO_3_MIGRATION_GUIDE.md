# Ehcache 2 to 3 Migration Guide

## Overview
This guide explains how an Ehcache 2 developer should read the current `TestEhcache3` project and what to change mentally when moving to Ehcache 3.

The examples here are based on the current project structure:
- native Ehcache 3 XML configuration in `src/main/resources/ehcache.xml`
- Spring configuration split into native cache bootstrap and cache-context wiring
- custom cache manager abstraction used by service and controller layers
- two cache domains: `message` and `user`

## 1. What Changes First

### Ehcache 2 mindset
In Ehcache 2, many projects were built around:
- `net.sf.ehcache.CacheManager`
- `Ehcache` or `Cache` instances looked up by name
- `Element` objects wrapping key and value
- XML with Ehcache 2 schema and legacy configuration style

### Ehcache 3 mindset
In this project, the equivalent model is:
- `org.ehcache.CacheManager`
- `org.ehcache.Cache<K, V>`
- typed key/value access without `Element`
- XML based on the Ehcache 3 schema

The biggest conceptual change is that cache access is now type-safe and generic:

```java
Cache<String, T> cache = cacheManager.getCache(cacheName, String.class, valueType);
```

That line in `CustomCacheManagerImpl` replaces the old Ehcache 2 habit of retrieving a raw cache and manually unpacking values from `Element`.

## 2. How This Project Boots Ehcache 3

### Native cache manager creation
The native Ehcache 3 manager is created in `CustomCacheManagerConfig`.

Key points:
- Spring reads `spring.cache.jcache.config` from `application.yaml`
- the XML file path is resolved through Spring `CacheProperties`
- `XmlConfiguration` is created from that resource
- the native manager is built with `CacheManagerBuilder.newCacheManager(...)`
- initialization is performed once when the manager is still `UNINITIALIZED`

Relevant pattern:

```java
org.ehcache.config.Configuration configuration = new XmlConfiguration(ehcacheResource.getURL());
CacheManager cacheManager = CacheManagerBuilder.newCacheManager(configuration);
if (cacheManager.getStatus() == Status.UNINITIALIZED) {
    cacheManager.init();
}
```

### Why this matters to an Ehcache 2 developer
In Ehcache 2 projects, lifecycle handling was often hidden or scattered. In this project, initialization responsibility is explicitly centralized in one place.

This is important because calling `init()` multiple times on the same Ehcache 3 manager causes runtime failure. During the recent refactor, the project hit exactly this error:

```text
Init not supported from AVAILABLE
```

The fix was to ensure:
- native cache manager lifecycle is owned only by `CustomCacheManagerConfig`
- cache-specific bean creation in `CustomCacheContextConfig` does not call `init()` again

## 3. XML Migration View

### Current Ehcache 3 XML
The project currently defines two caches:
- `sampleMessageCache`
- `sampleUserCache`

Example structure:

```xml
<cache alias="sampleMessageCache">
    <key-type>java.lang.String</key-type>
    <value-type>java.lang.String</value-type>
    <expiry>
        <ttl unit="seconds">10</ttl>
    </expiry>
    <resources>
        <heap unit="entries">100</heap>
    </resources>
</cache>
```

### How to read this if you come from Ehcache 2
Map the concepts like this:

| Ehcache 2 concept | Ehcache 3 concept in this project |
| --- | --- |
| cache name | `alias` |
| raw cache values | explicit `value-type` |
| key extraction from `Element` | typed key access via `Cache<K, V>` |
| time-to-live config | `<expiry><ttl .../></expiry>` |
| in-memory size limits | `<resources><heap .../></resources>` |

What to watch closely:
- Ehcache 3 requires explicit key and value typing
- value classes used in XML must match actual runtime types exactly
- adding a new cache means updating both XML and Spring bean wiring

## 4. How Cache Access Is Structured Here

### Custom abstraction instead of direct Ehcache calls
The public interface used by services is `CustomCacheManager<T>`.

It exposes:
- `get`
- `put`
- `evict`
- `clear`
- `getCacheName`
- `getValueType`

This is intentionally simpler than exposing native Ehcache APIs directly.

### Typed implementation
`CustomCacheManagerImpl<T>` wraps the native Ehcache 3 cache access:
- resolves the cache by alias
- enforces `String` key type
- enforces generic value type
- applies a shared cache key generator before reads and writes

Example:

```java
public Optional<T> get(String key) {
    return Optional.ofNullable(getCache().get(generateCacheKey(key)));
}
```

For an Ehcache 2 developer, this is the replacement for:
- cache lookup by name
- `get(key)` returning an `Element`
- extracting `element.getObjectValue()`

## 5. Multi-Cache Wiring Pattern

### Current project pattern
`CustomCacheContextConfig` creates one Spring bean per domain cache:
- `messageCacheManager`
- `userCacheManager`

Both are created through the same helper:

```java
private <T> CustomCacheManager<T> createCacheManager(...)
```

This means the project treats each business cache as:
1. an XML cache alias
2. a typed Spring bean
3. a service-level dependency

### What you should copy when migrating an Ehcache 2 project
When adding a new cache in Ehcache 3:
1. define the cache alias in `ehcache.xml`
2. define `key-type` and `value-type`
3. add a Spring bean that binds alias + value type
4. inject that bean into a domain service
5. add tests for cache manager behavior, service behavior, and controller behavior

In this project, `sampleUserCache` is the clearest example of that end-to-end extension path.

## 6. Service and Controller Usage

### Service layer
Services do not call the native Ehcache API directly.

`MessageService` and `UserService` both:
- attempt a typed cache read first
- compute a fallback value when the key is missing
- write the computed value back to cache
- expose explicit `put`, `evict`, and `clear` methods for tests and controller actions

This is a useful migration pattern if your Ehcache 2 code currently mixes:
- cache access
- object creation
- controller logic

### Controller layer
Both domains now expose:
- `GET /messages/{id}` and `GET /users/{id}`
- `PUT /messages/{id}` and `PUT /users/{id}`
- `DELETE /messages/{id}` and `DELETE /users/{id}`

This makes cache behavior visible at the HTTP level and helps prove that the new Ehcache 3 wiring works end to end.

## 7. Testing Strategy You Should Reuse

The project validates Ehcache 3 behavior at three levels.

### 1. Cache manager tests
Examples:
- `MessageCacheManagerTests`
- `UserCacheManagerTests`

These verify direct cache behavior:
- `put`
- `get`
- `evict`
- `clear`

### 2. Service tests
Examples:
- `MessageServiceCacheTests`
- `UserServiceCacheTests`

These verify:
- first call computes a value
- second call hits cache
- eviction forces recomputation

### 3. Controller tests
Examples:
- `MessageControllerTests`
- `UserControllerTests`

These verify:
- HTTP `GET`
- HTTP `PUT`
- HTTP `DELETE`
- real Spring wiring through `MockMvc`

For an Ehcache 2 migration, this layered test structure is valuable because it separates:
- cache configuration failures
- service integration failures
- web contract failures

## 8. Migration Pitfalls Seen in This Project

### 1. Double initialization
Symptom:
- `Init not supported from AVAILABLE`

Cause:
- calling `init()` more than once on the same `org.ehcache.CacheManager`

Fix:
- initialize only once in the native cache configuration

### 2. Type mismatch between XML and Java
Symptom:
- cache lookup failure or runtime type mismatch

Cause:
- `value-type` in `ehcache.xml` not matching the class used in `getCache(...)`

Fix:
- keep XML `value-type` and Java `Class<T>` exactly aligned

### 3. Carrying over Ehcache 2-style untyped design
Symptom:
- code compiles but loses the benefit of Ehcache 3 typing

Cause:
- treating Ehcache 3 as if it were still an untyped key/value map

Fix:
- model each cache with an explicit value type and inject typed cache managers

### 4. Mixing cache bootstrap and business cache wiring
Symptom:
- configuration becomes hard to extend when new caches are added

Fix in this project:
- `CustomCacheManagerConfig` owns native bootstrap
- `CustomCacheContextConfig` owns domain cache bean assembly

## 9. Recommended Migration Sequence

If you are converting an Ehcache 2 application, this project suggests the following order:

1. Replace Ehcache 2 dependency and imports with Ehcache 3 equivalents.
2. Rewrite XML into Ehcache 3 schema with explicit `key-type` and `value-type`.
3. Centralize native `CacheManager` creation and lifecycle.
4. Introduce a typed cache abstraction if business code currently depends on raw cache objects.
5. Migrate one cache domain first, such as `message`.
6. Add a second cache domain, such as `user`, to prove the pattern scales.
7. Add direct cache tests, service tests, and controller tests.
8. Only then migrate the rest of the application caches.

## 10. Current Project Takeaway

From an Ehcache 2 developer’s perspective, the most important lesson from this project is:

- Ehcache 3 should be treated as a typed cache system, not a drop-in replacement for old `Element`-based access.
- Cache lifecycle must be centralized.
- Each business cache should be represented consistently across XML, Spring bean wiring, service code, and tests.

If you follow the patterns used here for `sampleMessageCache` and `sampleUserCache`, the migration path becomes predictable and testable.
