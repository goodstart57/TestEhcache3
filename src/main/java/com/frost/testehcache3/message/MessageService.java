package com.frost.testehcache3.message;

import java.util.concurrent.atomic.AtomicInteger;

import com.frost.testehcache3.cache.CustomCacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final AtomicInteger invocationCounter = new AtomicInteger();

    private final CustomCacheManager<String> messageCacheManager;

    public MessageService(@Qualifier("messageCacheManager") CustomCacheManager<String> messageCacheManager) {
        this.messageCacheManager = messageCacheManager;
    }

    public String getMessage(String id) {
        return messageCacheManager.get(id).orElseGet(() -> {
            int sequence = invocationCounter.incrementAndGet();
            String message = "message-" + id + "-" + sequence;
            messageCacheManager.put(id, message);
            return message;
        });
    }

    public String putMessage(String id, String value) {
        messageCacheManager.put(id, value);
        return value;
    }

    public void evictMessage(String id) {
        messageCacheManager.evict(id);
    }

    public void clearMessages() {
        messageCacheManager.clear();
    }

    public int getInvocationCount() {
        return invocationCounter.get();
    }

    public void resetCounter() {
        invocationCounter.set(0);
    }
}
