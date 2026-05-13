# Test Report

## Overview
- Project: `TestEhcache3`
- Date: `2026-05-13`
- Target: JDK 17, Maven Wrapper 3.9.9, Ehcache 3.10.9
- Purpose: verify native Ehcache-backed custom cache managers, multi-cache context wiring, and controller-level `get`/`put`/`evict` behavior for both `message` and `user` domains

## Test Command
```powershell
cmd /c "pushd \\ucloudnas.corp.lgcns.com\S2UN03\HOMEDIR\82744\workspace\AX\rewrite\TestEhcache3 && mvnw.cmd test"
```

## Test Coverage

### 1. Message cache manager behavior
File: `src/test/java/com/frost/testehcache3/cache/MessageCacheManagerTests.java`

Verified behavior:
- `put` stores a message value
- `get` returns the cached message value
- `evict` removes a cached message entry
- `clear` removes all cached message entries
- shared key generation prefixes raw keys with `message::`

### 2. User cache manager behavior
File: `src/test/java/com/frost/testehcache3/cache/UserCacheManagerTests.java`

Verified behavior:
- `put` stores a `User` value in the second cache
- `get` returns the cached `User`
- `evict` removes a cached `User` entry
- `clear` removes all cached user entries

### 3. Service-level cache hit and eviction flow
Files:
- `src/test/java/com/frost/testehcache3/message/MessageServiceCacheTests.java`
- `src/test/java/com/frost/testehcache3/user/UserServiceCacheTests.java`

Verified behavior:
- repeated reads for the same id are served from cache
- explicit eviction forces recomputation on the next read
- `messageCacheManager` and `userCacheManager` each use their own Ehcache alias while preserving the same service contract pattern

### 4. Controller-level HTTP behavior
Files:
- `src/test/java/com/frost/testehcache3/message/MessageControllerTests.java`
- `src/test/java/com/frost/testehcache3/user/UserControllerTests.java`

Verified behavior:
- `GET /messages/{id}` returns a lazily generated cached message string
- `PUT /messages/{id}` stores the provided message value and returns it
- `DELETE /messages/{id}` evicts the cache entry and a later `GET` regenerates the value
- `GET /users/{id}` returns a lazily generated cached `User` JSON payload
- `PUT /users/{id}` stores the provided `User` display name and returns the saved JSON payload
- `DELETE /users/{id}` evicts the cache entry and a later `GET` regenerates the user payload

### 5. Spring Boot context load
File: `src/test/java/com/frost/testehcache3/TestEhcache3ApplicationTests.java`

Verified behavior:
- application context starts successfully with both cache aliases and both controller domains registered

## Execution Result
Latest execution summary:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.cache.MessageCacheManagerTests
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.cache.UserCacheManagerTests
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.message.MessageControllerTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.message.MessageServiceCacheTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.TestEhcache3ApplicationTests
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.user.UserControllerTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.frost.testehcache3.user.UserServiceCacheTests

Total: Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Conclusion
- Message and user domains both pass cache-manager, service, and controller-level verification.
- Multi-cache Ehcache XML wiring works for `sampleMessageCache` and `sampleUserCache`.
- HTTP `get`/`put`/`evict` behavior is verified end-to-end with `MockMvc`.
- No test failures or application context issues were observed in the latest run.
