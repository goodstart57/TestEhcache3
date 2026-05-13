package com.frost.testehcache3.cache;

import java.lang.reflect.Method;
import java.util.Optional;

import org.ehcache.Cache;
import org.springframework.core.Ordered;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.Assert;

public class CustomCacheManagerImpl<T> implements CustomCacheManager<T> {

    private static final Method GET_METHOD = resolveMethod("get", String.class);

    private static final Method PUT_METHOD = resolveMethod("put", String.class, Object.class);

    private static final Method EVICT_METHOD = resolveMethod("evict", String.class);

    private final org.ehcache.CacheManager cacheManager;

    private final String cacheName;

    private final Class<T> valueType;

    private final int order;

    private final KeyGenerator keyGenerator;

    public CustomCacheManagerImpl(
        org.ehcache.CacheManager cacheManager,
        String cacheName,
        Class<T> valueType,
        int order,
        KeyGenerator keyGenerator
    ) {
        Assert.notNull(cacheManager, "Cache manager must not be null");
        Assert.hasText(cacheName, "Cache name must not be blank");
        Assert.notNull(valueType, "Value type must not be null");
        Assert.notNull(keyGenerator, "Key generator must not be null");
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.valueType = valueType;
        this.order = order;
        this.keyGenerator = keyGenerator;
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
        return Optional.ofNullable(getCache().get(generateCacheKey(GET_METHOD, key)));
    }

    @Override
    public void put(String key, T value) {
        String cacheKey = generateCacheKey(PUT_METHOD, key);
        Assert.notNull(value, "Cache value must not be null");
        getCache().put(cacheKey, value);
    }

    @Override
    public void evict(String key) {
        getCache().remove(generateCacheKey(EVICT_METHOD, key));
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

    protected String generateCacheKey(Method method, String rawKey) {
        Assert.hasText(rawKey, "Cache key must not be blank");
        Object generatedKey = keyGenerator.generate(this, method, rawKey);
        Assert.notNull(generatedKey, "Generated cache key must not be null");
        String cacheKey = String.valueOf(generatedKey);
        Assert.hasText(cacheKey, "Generated cache key must not be blank");
        return cacheKey;
    }

    private static Method resolveMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return CustomCacheManager.class.getMethod(methodName, parameterTypes);
        }
        catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to resolve cache manager method: " + methodName, ex);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }
}
