package com.frost.testehcache3.cache;

import java.io.IOException;
import java.util.function.Function;

import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CustomCacheManagerConfig {

    private final CacheProperties cacheProperties;

    public CustomCacheManagerConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean(destroyMethod = "close")
    public CacheManager ehcacheManager() throws IOException {
        Resource ehcacheResource = cacheProperties.resolveConfigLocation(cacheProperties.getJcache().getConfig());
        Assert.notNull(
            ehcacheResource,
            "spring.cache.jcache.config must be configured for native Ehcache initialization"
        );
        org.ehcache.config.Configuration configuration = new XmlConfiguration(ehcacheResource.getURL());
        CacheManager cacheManager = CacheManagerBuilder.newCacheManager(configuration);
        if (cacheManager.getStatus() == Status.UNINITIALIZED) {
            cacheManager.init();
        }
        return cacheManager;
    }

    @Bean
    public Function<String, String> cacheKeyGenerator() {
        return key -> "message::" + key;
    }
}
