# Ehcache 2에서 3으로 전환 가이드

## 개요
이 문서는 Ehcache 2 개발자가 현재 `sample-ehcache3` 프로젝트를 기준으로 Ehcache 3 전환 포인트를 이해할 수 있도록 정리한 가이드입니다.

설명 대상은 현재 프로젝트에 있는 다음 구조입니다.
- `src/main/resources/ehcache.xml` 기반의 Ehcache 3 XML 설정
- 네이티브 캐시 매니저 초기화와 캐시 컨텍스트 배선을 분리한 Spring 설정
- 서비스와 컨트롤러에서 사용하는 커스텀 캐시 매니저 추상화
- `message`, `user` 두 개의 캐시 도메인

## 1. 먼저 달라지는 점

### Ehcache 2 관점
Ehcache 2 프로젝트는 보통 다음 구조를 중심으로 작성되었습니다.
- `net.sf.ehcache.CacheManager`
- 이름으로 조회하는 `Ehcache` 또는 `Cache`
- key/value를 감싸는 `Element`
- Ehcache 2 전용 XML 스키마와 레거시 설정 방식

### Ehcache 3 관점
현재 프로젝트에서 대응되는 구조는 다음과 같습니다.
- `org.ehcache.CacheManager`
- `org.ehcache.Cache<K, V>`
- `Element` 없이 바로 사용하는 타입 안전한 key/value 접근
- Ehcache 3 스키마 기반 XML

핵심 변화는 캐시 접근이 제네릭 기반의 타입 안전한 방식으로 바뀐다는 점입니다.

```java
Cache<String, T> cache = cacheManager.getCache(cacheName, String.class, valueType);
```

현재 `CustomCacheManagerImpl`은 이 방식으로 캐시를 조회합니다.  
즉, Ehcache 2에서 흔했던 "raw cache 조회 -> `Element` 반환 -> `getObjectValue()` 추출" 패턴이 사라졌습니다.

## 2. 이 프로젝트에서 Ehcache 3를 부팅하는 방식

### 네이티브 캐시 매니저 생성
Ehcache 3의 네이티브 `CacheManager`는 `CustomCacheManagerConfig`에서 생성됩니다.

현재 프로젝트의 핵심 포인트는 다음과 같습니다.
- `application.yaml`의 `spring.cache.jcache.config` 값을 읽음
- Spring `CacheProperties`로 XML 리소스 위치를 해석함
- 그 리소스로 `XmlConfiguration`을 생성함
- `CacheManagerBuilder.newCacheManager(...)`로 네이티브 매니저를 만듦
- 상태가 `UNINITIALIZED`일 때만 `init()`을 호출함

현재 코드 패턴은 다음과 같습니다.

```java
org.ehcache.config.Configuration configuration = new XmlConfiguration(ehcacheResource.getURL());
CacheManager cacheManager = CacheManagerBuilder.newCacheManager(configuration);
if (cacheManager.getStatus() == Status.UNINITIALIZED) {
    cacheManager.init();
}
```

### Ehcache 2 개발자가 여기서 봐야 할 점
Ehcache 2 프로젝트에서는 매니저 초기화 책임이 숨겨져 있거나 여러 군데에 흩어져 있는 경우가 많았습니다.  
이 프로젝트는 초기화 책임을 한 곳으로 모읍니다.

이유는 분명합니다. 같은 Ehcache 3 `CacheManager`에 `init()`를 여러 번 호출하면 런타임 예외가 발생합니다.

실제로 이 프로젝트는 리팩터링 중 아래 에러를 겪었습니다.

```text
Init not supported from AVAILABLE
```

해결 방식은 다음과 같습니다.
- 네이티브 캐시 매니저 lifecycle은 `CustomCacheManagerConfig`만 담당
- `CustomCacheContextConfig`에서 개별 캐시 bean을 만들 때는 `init()`을 다시 호출하지 않음

## 3. XML 관점에서의 전환

### 현재 Ehcache 3 XML
현재 프로젝트는 두 개의 캐시를 정의합니다.
- `sampleMessageCache`
- `sampleUserCache`

예시는 다음과 같습니다.

```xml
<cache alias="sampleMessageCache">
    <key-type>java.lang.String</key-type>
    <value-type>java.lang.String</value-type>
    <expiry>
        <ttl unit="seconds">10</ttl>
    </expiry>
    <resources>
        <heap unit="entries">100</heap>
    </resources>
</cache>
```

### Ehcache 2와 개념 대응

| Ehcache 2 개념 | 현재 프로젝트의 Ehcache 3 개념 |
| --- | --- |
| cache name | `alias` |
| raw value 저장 | 명시적 `value-type` |
| `Element`에서 값 추출 | `Cache<K, V>`의 타입 기반 접근 |
| TTL 설정 | `<expiry><ttl .../></expiry>` |
| 메모리 크기 제한 | `<resources><heap .../></resources>` |

특히 주의할 점:
- Ehcache 3는 key/value 타입을 명시적으로 요구함
- XML의 `value-type`은 실제 Java 타입과 정확히 일치해야 함
- 새 캐시를 추가하려면 XML 수정과 Spring bean 배선을 함께 해야 함

## 4. 현재 프로젝트의 캐시 접근 구조

### 네이티브 Ehcache를 바로 노출하지 않는 구조
서비스 레이어가 직접 사용하는 공개 인터페이스는 `CustomCacheManager<T>`입니다.

제공 메서드는 다음과 같습니다.
- `get`
- `put`
- `evict`
- `clear`
- `getCacheName`
- `getValueType`

즉, 비즈니스 코드가 네이티브 Ehcache API에 직접 묶이지 않도록 단순한 추상화를 둔 구조입니다.

### 타입 기반 구현체
`CustomCacheManagerImpl<T>`는 내부에서 Ehcache 3 네이티브 캐시를 감쌉니다.

역할은 다음과 같습니다.
- alias로 캐시를 찾음
- key 타입을 `String`으로 고정함
- value 타입을 제네릭으로 강제함
- 공통 key generator를 적용한 뒤 읽기/쓰기를 수행함

예시:

```java
public Optional<T> get(String key) {
    return Optional.ofNullable(getCache().get(generateCacheKey(key)));
}
```

Ehcache 2 개발자 기준으로 보면 이것은 다음의 대체 구조입니다.
- 이름으로 캐시 조회
- `get(key)` 호출 시 `Element` 반환
- `element.getObjectValue()`로 실제 값 추출

## 5. 멀티 캐시 배선 패턴

### 현재 프로젝트 패턴
`CustomCacheContextConfig`는 도메인별 캐시 bean을 생성합니다.
- `messageCacheManager`
- `userCacheManager`

두 bean 모두 아래 공통 헬퍼를 통해 만들어집니다.

```java
private <T> CustomCacheManager<T> createCacheManager(...)
```

즉, 이 프로젝트는 각 비즈니스 캐시를 다음 3단계로 다룹니다.
1. XML의 cache alias
2. 타입이 고정된 Spring bean
3. 서비스에서 주입받는 의존성

### Ehcache 2 프로젝트 전환 시 그대로 가져가야 할 흐름
Ehcache 3에서 새 캐시를 추가할 때는 다음 순서가 안전합니다.
1. `ehcache.xml`에 cache alias 추가
2. `key-type`, `value-type` 지정
3. alias와 value type을 바인딩하는 Spring bean 추가
4. 해당 bean을 도메인 서비스에 주입
5. 캐시 매니저 테스트, 서비스 테스트, 컨트롤러 테스트 추가

현재 프로젝트에서는 `sampleUserCache`가 이 확장 흐름을 가장 잘 보여주는 예시입니다.

## 6. 서비스와 컨트롤러 사용 방식

### 서비스 레이어
서비스는 네이티브 Ehcache API를 직접 호출하지 않습니다.

현재 `MessageService`, `UserService`는 공통적으로 다음 구조를 따릅니다.
- 먼저 타입 기반 캐시 조회
- 값이 없으면 fallback 값 계산
- 계산한 값을 다시 캐시에 저장
- 테스트와 컨트롤러를 위해 `put`, `evict`, `clear` 메서드를 별도로 노출

이 방식은 Ehcache 2 코드에서 아래 관심사가 섞여 있는 경우 특히 유용합니다.
- 캐시 접근
- 객체 생성
- 컨트롤러 처리

### 컨트롤러 레이어
현재 두 도메인은 모두 다음 엔드포인트를 가집니다.
- `GET /messages/{id}`, `GET /users/{id}`
- `PUT /messages/{id}`, `PUT /users/{id}`
- `DELETE /messages/{id}`, `DELETE /users/{id}`

즉, 캐시 동작을 HTTP 레벨에서도 직접 확인할 수 있도록 구성했습니다.  
Ehcache 3 배선이 실제 애플리케이션 흐름에서 정상 동작하는지 검증하기 좋은 구조입니다.

## 7. 테스트 전략

현재 프로젝트는 Ehcache 3 동작을 세 단계로 검증합니다.

### 1. 캐시 매니저 테스트
예시:
- `MessageCacheManagerTests`
- `UserCacheManagerTests`

검증 항목:
- `put`
- `get`
- `evict`
- `clear`

### 2. 서비스 테스트
예시:
- `MessageServiceCacheTests`
- `UserServiceCacheTests`

검증 항목:
- 첫 호출은 값을 계산함
- 두 번째 호출은 캐시 히트
- eviction 이후 다시 계산됨

### 3. 컨트롤러 테스트
예시:
- `MessageControllerTests`
- `UserControllerTests`

검증 항목:
- HTTP `GET`
- HTTP `PUT`
- HTTP `DELETE`
- `MockMvc`를 통한 실제 Spring 배선

Ehcache 2에서 3으로 전환할 때 이 구조가 좋은 이유는 장애 지점을 분리해 주기 때문입니다.
- XML/캐시 설정 문제
- 서비스 통합 문제
- 웹 계약 문제

## 8. 이 프로젝트에서 실제로 드러난 전환 리스크

### 1. 중복 초기화
증상:
- `Init not supported from AVAILABLE`

원인:
- 같은 `org.ehcache.CacheManager`에 `init()`를 두 번 호출

해결:
- 네이티브 캐시 설정에서만 한 번 초기화

### 2. XML 타입과 Java 타입 불일치
증상:
- 캐시 조회 실패 또는 런타임 타입 문제

원인:
- `ehcache.xml`의 `value-type`과 `getCache(...)`에 넘기는 타입이 다름

해결:
- XML의 `value-type`과 Java의 `Class<T>`를 정확히 맞춤

### 3. Ehcache 2식 비정형 접근을 그대로 가져오는 경우
증상:
- 컴파일은 되지만 Ehcache 3의 타입 안정성 이점을 잃음

원인:
- Ehcache 3를 여전히 raw key/value map처럼 취급

해결:
- 캐시별 value type을 명시하고 타입 기반 캐시 매니저를 주입

### 4. 캐시 bootstrap과 비즈니스 캐시 배선이 섞이는 경우
증상:
- 캐시가 늘어날수록 설정이 복잡해짐

현재 프로젝트의 해결 방식:
- `CustomCacheManagerConfig`는 네이티브 bootstrap만 담당
- `CustomCacheContextConfig`는 도메인 캐시 bean 조립만 담당

## 9. 추천 전환 순서

Ehcache 2 애플리케이션을 전환할 때는 현재 프로젝트 기준으로 다음 순서를 추천합니다.

1. Ehcache 2 dependency와 import를 Ehcache 3 기준으로 교체
2. XML을 Ehcache 3 스키마와 `key-type`, `value-type` 기준으로 재작성
3. 네이티브 `CacheManager` 생성과 lifecycle을 한 곳으로 모음
4. 비즈니스 코드가 raw cache 객체에 의존한다면 타입 기반 추상화 도입
5. `message` 같은 한 개 도메인부터 먼저 전환
6. `user` 같은 두 번째 도메인을 추가해 패턴이 확장 가능한지 검증
7. 캐시 매니저 테스트, 서비스 테스트, 컨트롤러 테스트 추가
8. 그 이후 나머지 캐시를 순차적으로 전환

## 10. 현재 프로젝트에서 가져가야 할 핵심

Ehcache 2 개발자 관점에서 이 프로젝트가 주는 가장 중요한 메시지는 다음과 같습니다.

- Ehcache 3는 `Element` 기반 레거시 접근의 단순 치환이 아니라 타입 기반 캐시 시스템으로 받아들여야 합니다.
- 캐시 lifecycle은 반드시 중앙 집중적으로 관리해야 합니다.
- 각 비즈니스 캐시는 XML, Spring bean, 서비스 코드, 테스트에서 일관된 단위로 표현되어야 합니다.

현재 프로젝트의 `sampleMessageCache`, `sampleUserCache` 패턴을 그대로 따르면 Ehcache 3 전환 경로를 예측 가능하고 테스트 가능한 형태로 만들 수 있습니다.
