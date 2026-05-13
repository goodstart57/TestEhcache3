package com.frost.testehcache3.cache;

import java.util.Optional;
import java.util.function.Function;

import org.ehcache.Cache;
import org.springframework.util.Assert;

public class CustomCacheManagerImpl<T> implements CustomCacheManager<T> {

    private final org.ehcache.CacheManager cacheManager;

    private final String cacheName;

    private final Class<T> valueType;

    private final int order;

    private final Function<String, String> cacheKeyGenerator;

    public CustomCacheManagerImpl(
        org.ehcache.CacheManager cacheManager,
        String cacheName,
        Class<T> valueType,
        int order,
        Function<String, String> cacheKeyGenerator
    ) {
        Assert.notNull(cacheManager, "Cache manager must not be null");
        Assert.hasText(cacheName, "Cache name must not be blank");
        Assert.notNull(valueType, "Value type must not be null");
        Assert.notNull(cacheKeyGenerator, "Cache key generator must not be null");
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.valueType = valueType;
        this.order = order;
        this.cacheKeyGenerator = cacheKeyGenerator;
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
        return Optional.ofNullable(getCache().get(generateCacheKey(key)));
    }

    @Override
    public void put(String key, T value) {
        String cacheKey = generateCacheKey(key);
        Assert.notNull(value, "Cache value must not be null");
        getCache().put(cacheKey, value);
    }

    @Override
    public void evict(String key) {
        getCache().remove(generateCacheKey(key));
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

    protected String generateCacheKey(String rawKey) {
        Assert.hasText(rawKey, "Cache key must not be blank");
        String cacheKey = cacheKeyGenerator.apply(rawKey);
        Assert.hasText(cacheKey, "Generated cache key must not be blank");
        return cacheKey;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
