package com.frost.testehcache3.cache;

import java.util.function.Function;

import com.frost.testehcache3.user.User;
import org.ehcache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CustomCacheContextConfig {

    @Bean
    public CustomCacheManager<String> messageCacheManager(
        CacheManager ehcacheManager,
        Function<String, String> cacheKeyGenerator
    ) {
        return createCacheManager(
            ehcacheManager,
            "sampleMessageCache",
            String.class,
            Ordered.HIGHEST_PRECEDENCE,
            cacheKeyGenerator
        );
    }

    @Bean
    public CustomCacheManager<User> userCacheManager(
        CacheManager ehcacheManager,
        Function<String, String> cacheKeyGenerator
    ) {
        return createCacheManager(
            ehcacheManager,
            "sampleUserCache",
            User.class,
            Ordered.HIGHEST_PRECEDENCE + 1,
            cacheKeyGenerator
        );
    }

    private <T> CustomCacheManager<T> createCacheManager(
        CacheManager ehcacheManager,
        String cacheName,
        Class<T> valueType,
        int order,
        Function<String, String> cacheKeyGenerator
    ) {
        return new CustomCacheManagerImpl<>(
            ehcacheManager,
            cacheName,
            valueType,
            order,
            cacheKeyGenerator
        );
    }
}
