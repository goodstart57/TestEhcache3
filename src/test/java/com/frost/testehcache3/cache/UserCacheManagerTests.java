package com.frost.testehcache3.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.frost.testehcache3.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserCacheManagerTests {

    @Autowired
    @Qualifier("userCacheManager")
    private CustomCacheManager<User> userCacheManager;

    @BeforeEach
    void setUp() {
        userCacheManager.clear();
    }

    @Test
    void shouldPutUserValue() {
        User user = new User("u-1", "User One");

        userCacheManager.put("u-1", user);

        assertThat(userCacheManager.get("u-1")).contains(user);
    }

    @Test
    void shouldGetCachedUserValue() {
        User user = new User("u-2", "User Two");
        userCacheManager.put("u-2", user);

        assertThat(userCacheManager.get("u-2")).contains(user);
    }

    @Test
    void shouldEvictUserValue() {
        userCacheManager.put("u-3", new User("u-3", "User Three"));

        userCacheManager.evict("u-3");

        assertThat(userCacheManager.get("u-3")).isEmpty();
    }

    @Test
    void shouldClearUserValues() {
        userCacheManager.put("u-4", new User("u-4", "User Four"));

        userCacheManager.clear();

        assertThat(userCacheManager.get("u-4")).isEmpty();
    }
}
