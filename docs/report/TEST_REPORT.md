# Test Report

## Overview
- Project: `TestEhcache3`
- Date: `2026-05-12`
- Target: JDK 17, Maven Wrapper 3.9.9, Ehcache 3.10.9
- Purpose: verify direct cache control through `messageCacheManager` without Spring Cache abstraction

## Test Command
```powershell
cmd /c "pushd \\ucloudnas.corp.lgcns.com\S2UN03\HOMEDIR\82744\workspace\AX\rewrite\TestEhcache3 && mvnw.cmd test"
```

## Test Code

### 1. Cache manager direct control
File: `src/test/java/com/frost/testehcache3/cache/MessageCacheManagerTests.java`

```java
@SpringBootTest
class MessageCacheManagerTests {

    @Autowired
    @Qualifier("messageCacheManager")
    private CustomCacheManager<String> messageCacheManager;

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
}
```

Verified behavior:
- `put` stores a value in Ehcache
- `get` returns the cached value
- `evict` removes a single key
- `clear` removes all entries

### 2. Service-level cache hit/miss flow
File: `src/test/java/com/frost/testehcache3/message/MessageServiceCacheTests.java`

```java
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
```

Verified behavior:
- second call with the same key is served from cache
- explicit eviction forces recomputation
- service and `messageCacheManager` share the same native Ehcache instance

### 3. Spring Boot context load
File: `src/test/java/com/frost/testehcache3/TestEhcache3ApplicationTests.java`

```java
@SpringBootTest
class TestEhcache3ApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Verified behavior:
- application context starts successfully with current Ehcache configuration

## Execution Result
Latest execution summary:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.cache.MessageCacheManagerTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.message.MessageServiceCacheTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.TestEhcache3ApplicationTests

Total: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Conclusion
- Direct cache control through `messageCacheManager` works as intended.
- Native Ehcache XML configuration loads correctly.
- No test failures or application context issues were observed in the latest run.
