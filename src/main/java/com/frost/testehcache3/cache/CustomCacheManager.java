package com.frost.testehcache3.cache;

import java.util.Optional;

import org.springframework.core.Ordered;

public interface CustomCacheManager<T> extends Ordered {

    String getCacheName();

    Class<T> getValueType();

    Optional<T> get(String key);

    void put(String key, T value);

    void evict(String key);

    void clear();

    @Override
    int getOrder();
}
