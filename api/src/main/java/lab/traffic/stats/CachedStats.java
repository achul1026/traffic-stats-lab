package lab.traffic.stats;

import java.time.LocalDate;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 캐시가 적용된 조회. 프록시를 거쳐야 하므로 StatsService 와 분리된 빈으로 둔다.
 * 키는 (질의, 대상, 파라미터) 전체다. TTL 은 application.yml(spring.cache.redis.time-to-live).
 */
@Component
public class CachedStats {
    private final StatsRepository repo;

    public CachedStats(StatsRepository repo) {
        this.repo = repo;
    }

    @Cacheable(cacheNames = "stats", key = "'monthly:' + #src + ':' + #regionId + ':' + #from + ':' + #to")
    public List<Rows.MonthlyVolume> monthlyVolume(Source src, int regionId, LocalDate from, LocalDate to) {
        return repo.monthlyVolume(src, regionId, from, to);
    }

    @Cacheable(cacheNames = "stats", key = "'speed:' + #src + ':' + #from + ':' + #to")
    public List<Rows.DailySpeed> dailyAvgSpeed(Source src, LocalDate from, LocalDate to) {
        return repo.dailyAvgSpeed(src, from, to);
    }

    @Cacheable(cacheNames = "stats", key = "'rank:' + #src + ':' + #from + ':' + #to")
    public List<Rows.RegionVolume> volumeRanking(Source src, LocalDate from, LocalDate to) {
        return repo.volumeRanking(src, from, to);
    }
}
