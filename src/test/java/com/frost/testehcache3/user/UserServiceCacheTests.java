package com.frost.testehcache3.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.frost.testehcache3.cache.CustomCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceCacheTests {

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("userCacheManager")
    private CustomCacheManager<User> userCacheManager;

    @BeforeEach
    void setUp() {
        userService.clearUsers();
        userService.resetCounter();
    }

    @Test
    void shouldUseCacheForRepeatedUserReads() {
        User first = userService.getUser("u-1");
        User second = userService.getUser("u-1");

        assertThat(second).isEqualTo(first);
        assertThat(userService.getInvocationCount()).isEqualTo(1);
        assertThat(userCacheManager.get("u-1")).contains(first);
    }

    @Test
    void shouldLoadUserAgainAfterManualEviction() {
        User first = userService.getUser("u-2");
        userService.evictUser("u-2");

        User second = userService.getUser("u-2");

        assertThat(second).isNotEqualTo(first);
        assertThat(userService.getInvocationCount()).isEqualTo(2);
    }
}
