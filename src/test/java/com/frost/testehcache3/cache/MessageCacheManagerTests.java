package com.frost.testehcache3.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Qualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessageCacheManagerTests {

    @Autowired
    @Qualifier("messageCacheManager")
    private CustomCacheManager<String> messageCacheManager;

    @BeforeEach
    void setUp() {
        messageCacheManager.clear();
    }

    @Test
    void shouldPutGetEvictAndClearValues() {
        messageCacheManager.put("alpha", "value-1");

        assertThat(messageCacheManager.get("alpha")).contains("value-1");

        messageCacheManager.evict("alpha");
        assertThat(messageCacheManager.get("alpha")).isEmpty();

        messageCacheManager.put("beta", "value-2");
        messageCacheManager.clear();
        assertThat(messageCacheManager.get("beta")).isEmpty();
    }

    @Test
    void shouldUseConfiguredCacheKeyGenerator() {
        messageCacheManager.put("item-1", "value-3");

        assertThat(messageCacheManager.get("message::item-1")).isEmpty();
        assertThat(messageCacheManager.get("item-1")).contains("value-3");
    }
}
