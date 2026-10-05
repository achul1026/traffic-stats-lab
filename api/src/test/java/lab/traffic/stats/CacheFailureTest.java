package lab.traffic.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import lab.traffic.config.CacheBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Redis 장애 상황 가정: 캐시 읽기·쓰기가 예외를 던져도 조회는 DB 로 대체되어 정상 응답한다. */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Import(CacheFailureTest.BrokenCacheConfig.class)
class CacheFailureTest {
    @Autowired StatsService service;
    @Autowired CacheBreaker breaker;
    @MockitoBean StatsRepository repo;

    @TestConfiguration
    static class BrokenCacheConfig {
        @Bean @Primary
        CacheManager brokenCacheManager() {
            Cache broken = new ConcurrentMapCache("stats") {
                @Override public ValueWrapper get(Object key) { throw new IllegalStateException("redis down"); }
                @Override public void put(Object key, Object value) { throw new IllegalStateException("redis down"); }
            };
            return new CacheManager() {
                @Override public Cache getCache(String name) { return broken; }
                @Override public Collection<String> getCacheNames() { return List.of("stats"); }
            };
        }
    }

    @Test
    void 캐시가_고장나도_DB_결과를_반환한다() {
        var rows = List.of(new Rows.RegionVolume(1, 100));
        when(repo.volumeRanking(any(), any(), any())).thenReturn(rows);

        var result = service.volumeRanking(Source.ROLLUP, 2024, true);

        assertThat(result).isEqualTo(rows);
        verify(repo).volumeRanking(eq(Source.ROLLUP), any(), any());
    }

    @Test
    void 캐시_오류_뒤에는_브레이커가_열려_캐시를_건너뛴다() {
        when(repo.volumeRanking(any(), any(), any())).thenReturn(List.of());

        service.volumeRanking(Source.ROLLUP, 2023, true);   // 캐시 오류 발생 → 브레이커 열림
        assertThat(breaker.isOpen()).isTrue();

        service.volumeRanking(Source.ROLLUP, 2023, true);   // 캐시를 건너뛰고 DB 로 직접
        verify(repo, times(2)).volumeRanking(eq(Source.ROLLUP), any(), any());
    }
}
