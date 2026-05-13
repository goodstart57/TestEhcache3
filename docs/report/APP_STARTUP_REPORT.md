# Application Startup Report

## Overview
- Project: `TestEhcache3`
- Date: `2026-05-12`
- Verified at: `2026-05-12 17:26:58 +09:00`
- Purpose: verify that the Spring Boot application starts normally and that direct cache control via `messageCacheManager` works at runtime

## Startup Command
```powershell
cmd /c "pushd \\ucloudnas.corp.lgcns.com\S2UN03\HOMEDIR\82744\workspace\AX\rewrite\TestEhcache3 && mvnw.cmd spring-boot:run"
```

Reason for this form:
- the repository is located on a UNC path
- `pushd` maps the network path before running `mvnw.cmd`

## Runtime Verification

### 1. Application startup
Startup was considered successful after confirming these log lines:

```text
Tomcat initialized with port 8080 (http)
Cache 'sampleMessageCache' created in EhcacheManager.
Tomcat started on port 8080 (http) with context path '/'
Started TestEhcache3Application in 10.46 seconds
```

### 2. Endpoint check
Target endpoint:

```text
GET http://localhost:8080/messages/item-1
```

Observed responses:

```text
1st call: message-item-1-1
2nd call: message-item-1-1
```

Verification result:
- the endpoint returned HTTP success content
- the second call returned the same value as the first call
- this confirms a cache hit through direct `messageCacheManager` usage

### 3. Process cleanup
- the startup process was terminated after verification
- no additional long-running local process was left intentionally

## Conclusion
- application startup succeeded
- Ehcache initialized correctly
- `messageCacheManager` runtime cache behavior was verified through repeated endpoint calls
