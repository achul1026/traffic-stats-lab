package lab.traffic.stats;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lab.traffic.config.CacheBreaker;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
    private final StatsRepository repo;
    private final CachedStats cached;
    private final CacheBreaker breaker;

    public StatsService(StatsRepository repo, CachedStats cached, CacheBreaker breaker) {
        this.repo = repo;
        this.cached = cached;
        this.breaker = breaker;
    }

    /** 요청이 캐시를 원하고 캐시가 살아 있을 때만 캐시를 쓴다. */
    private boolean viaCache(boolean useCache) {
        return useCache && !breaker.isOpen();
    }

    /** fromMonth, toMonth 모두 포함. */
    public List<Rows.MonthlyVolume> monthlyVolume(Source src, int regionId, YearMonth fromMonth, YearMonth toMonth, boolean useCache) {
        LocalDate from = fromMonth.atDay(1);
        LocalDate to = toMonth.plusMonths(1).atDay(1);
        return viaCache(useCache) ? cached.monthlyVolume(src, regionId, from, to) : repo.monthlyVolume(src, regionId, from, to);
    }

    public List<Rows.DailySpeed> dailyAvgSpeed(Source src, YearMonth month, boolean useCache) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.plusMonths(1).atDay(1);
        return viaCache(useCache) ? cached.dailyAvgSpeed(src, from, to) : repo.dailyAvgSpeed(src, from, to);
    }

    public List<Rows.RegionVolume> volumeRanking(Source src, int year, boolean useCache) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year + 1, 1, 1);
        return viaCache(useCache) ? cached.volumeRanking(src, from, to) : repo.volumeRanking(src, from, to);
    }
}
