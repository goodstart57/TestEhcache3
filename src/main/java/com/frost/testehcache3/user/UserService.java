package com.frost.testehcache3.user;

import java.util.concurrent.atomic.AtomicInteger;

import com.frost.testehcache3.cache.CustomCacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AtomicInteger invocationCounter = new AtomicInteger();

    private final CustomCacheManager<User> userCacheManager;

    public UserService(@Qualifier("userCacheManager") CustomCacheManager<User> userCacheManager) {
        this.userCacheManager = userCacheManager;
    }

    public User getUser(String id) {
        return userCacheManager.get(id).orElseGet(() -> {
            int sequence = invocationCounter.incrementAndGet();
            User user = new User(id, "user-" + id + "-" + sequence);
            userCacheManager.put(id, user);
            return user;
        });
    }

    public void putUser(String id, User user) {
        userCacheManager.put(id, user);
    }

    public void evictUser(String id) {
        userCacheManager.evict(id);
    }

    public void clearUsers() {
        userCacheManager.clear();
    }

    public int getInvocationCount() {
        return invocationCounter.get();
    }

    public void resetCounter() {
        invocationCounter.set(0);
    }
}
