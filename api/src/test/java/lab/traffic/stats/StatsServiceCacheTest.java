package lab.traffic.stats;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 캐시 동작: 같은 요청은 DB 를 한 번만 조회하고, cache=false 는 매번 조회한다. DB·Redis 없이 실행된다. */
@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class StatsServiceCacheTest {
    @Autowired StatsService service;
    @Autowired CacheManager cacheManager;
    @MockitoBean StatsRepository repo;

    private final LocalDate from = LocalDate.of(2024, 10, 1), to = LocalDate.of(2025, 1, 1);
    private final List<Rows.MonthlyVolume> rows = List.of(new Rows.MonthlyVolume("2024-10", 10));

    @BeforeEach
    void stub() {
        cacheManager.getCache("stats").clear();   // 테스트끼리 캐시가 섞이지 않게
        when(repo.monthlyVolume(any(), anyInt(), any(), any())).thenReturn(rows);
    }

    @Test
    void 같은_요청은_DB를_한번만_조회한다() {
        service.monthlyVolume(Source.ROLLUP, 7, YearMonth.of(2024, 10), YearMonth.of(2024, 12), true);
        var second = service.monthlyVolume(Source.ROLLUP, 7, YearMonth.of(2024, 10), YearMonth.of(2024, 12), true);

        assertThat(second).isEqualTo(rows);
        verify(repo, times(1)).monthlyVolume(Source.ROLLUP, 7, from, to);
    }

    @Test
    void 파라미터가_다르면_별도로_조회한다() {
        service.monthlyVolume(Source.ROLLUP, 7, YearMonth.of(2024, 10), YearMonth.of(2024, 12), true);
        service.monthlyVolume(Source.ROLLUP, 8, YearMonth.of(2024, 10), YearMonth.of(2024, 12), true);
        service.monthlyVolume(Source.RAW, 7, YearMonth.of(2024, 10), YearMonth.of(2024, 12), true);

        verify(repo, times(3)).monthlyVolume(any(), anyInt(), any(), any());
    }

    @Test
    void cache_false_는_캐시를_거치지_않는다() {
        for (int i = 0; i < 3; i++) {
            service.monthlyVolume(Source.ROLLUP, 9, YearMonth.of(2024, 10), YearMonth.of(2024, 12), false);
        }
        verify(repo, times(3)).monthlyVolume(Source.ROLLUP, 9, from, to);
    }

    @Test
    void 월_범위는_다음달_1일_미만으로_변환된다() {
        service.dailyAvgSpeed(Source.RAW, YearMonth.of(2024, 12), false);
        verify(repo).dailyAvgSpeed(Source.RAW, LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 1));
    }
}
