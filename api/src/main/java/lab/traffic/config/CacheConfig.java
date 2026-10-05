package lab.traffic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시(Redis) 장애가 조회 실패로 번지지 않게 한다.
 * 캐시 읽기·쓰기에서 예외가 나면 로그만 남기고 DB 조회로 넘어간다. (캐시는 최적화일 뿐 정답의 원천이 아니다)
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);
    private final CacheBreaker breaker;

    public CacheConfig(CacheBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object k) { breaker.recordFailure(); log.warn("캐시 읽기 실패, DB로 대체: {}", e.toString()); }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) { breaker.recordFailure(); log.warn("캐시 쓰기 실패: {}", e.toString()); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object k) { breaker.recordFailure(); log.warn("캐시 삭제 실패: {}", e.toString()); }
            @Override public void handleCacheClearError(RuntimeException e, Cache c) { breaker.recordFailure(); log.warn("캐시 비우기 실패: {}", e.toString()); }
        };
    }
}
