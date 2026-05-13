package com.frost.testehcache3.cache;

import java.util.Optional;

import org.ehcache.Cache;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

public class CustomCacheManagerImpl<T> implements CustomCacheManager<T> {

    private final org.ehcache.CacheManager cacheManager;

    private final String cacheName;

    private final Class<T> valueType;

    private final int order;

    public CustomCacheManagerImpl(org.ehcache.CacheManager cacheManager, String cacheName, Class<T> valueType) {
        this(cacheManager, cacheName, valueType, Ordered.LOWEST_PRECEDENCE);
    }

    public CustomCacheManagerImpl(
        org.ehcache.CacheManager cacheManager,
        String cacheName,
        Class<T> valueType,
        int order
    ) {
        Assert.notNull(cacheManager, "Cache manager must not be null");
        Assert.hasText(cacheName, "Cache name must not be blank");
        Assert.notNull(valueType, "Value type must not be null");
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.valueType = valueType;
        this.order = order;
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    @Override
    public Class<T> getValueType() {
        return valueType;
    }

    @Override
    public Optional<T> get(String key) {
        Assert.hasText(key, "Cache key must not be blank");
        return Optional.ofNullable(getCache().get(key));
    }

    @Override
    public void put(String key, T value) {
        Assert.hasText(key, "Cache key must not be blank");
        Assert.notNull(value, "Cache value must not be null");
        getCache().put(key, value);
    }

    @Override
    public void evict(String key) {
        Assert.hasText(key, "Cache key must not be blank");
        getCache().remove(key);
    }

    @Override
    public void clear() {
        getCache().clear();
    }

    protected Cache<String, T> getCache() {
        Cache<String, T> cache = cacheManager.getCache(cacheName, String.class, valueType);
        Assert.notNull(cache, () -> "Cache not found: " + cacheName);
        return cache;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
