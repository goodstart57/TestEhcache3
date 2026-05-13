package com.frost.testehcache3.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.frost.testehcache3.cache.CustomCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessageServiceCacheTests {

    @Autowired
    private MessageService messageService;

    @Autowired
    @Qualifier("messageCacheManager")
    private CustomCacheManager<String> messageCacheManager;

    @BeforeEach
    void setUp() {
        messageService.clearMessages();
        messageService.resetCounter();
    }

    @Test
    void shouldUseSpringCacheBackedByEhcache() {
        String first = messageService.getMessage("item-1");
        String second = messageService.getMessage("item-1");

        assertThat(first).isEqualTo(second);
        assertThat(messageService.getInvocationCount()).isEqualTo(1);
        assertThat(messageCacheManager.get("item-1")).contains(first);
    }

    @Test
    void shouldLoadAgainAfterManualEviction() {
        String first = messageService.getMessage("item-2");
        messageService.evictMessage("item-2");

        String second = messageService.getMessage("item-2");

        assertThat(second).isNotEqualTo(first);
        assertThat(messageService.getInvocationCount()).isEqualTo(2);
    }
}
