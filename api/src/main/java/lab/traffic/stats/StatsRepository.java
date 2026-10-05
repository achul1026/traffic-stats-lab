package lab.traffic.stats;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 통계 조회. 같은 질의를 원본(RAW)과 집계 테이블(ROLLUP)로 각각 제공한다.
 * 기간 경계는 한국 시간(+09:00) 자정 기준이다. (bench/queries.py 의 Q1, Q2, Q4 와 같은 질의)
 */
@Repository
public class StatsRepository {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime kst(LocalDate d) {
        return d.atStartOfDay().atOffset(KST);
    }

    /** from 포함, toExclusive 미포함 */
    public List<Rows.MonthlyVolume> monthlyVolume(Source src, int regionId, LocalDate from, LocalDate toExclusive) {
        if (src == Source.ROLLUP) {
            return jdbc.query("""
                    SELECT to_char(day, 'YYYY-MM') AS m, sum(total_volume) AS vol
                    FROM daily_region_stats
                    WHERE region_id = ? AND day >= ? AND day < ?
                    GROUP BY 1 ORDER BY 1""",
                    (rs, i) -> new Rows.MonthlyVolume(rs.getString("m"), rs.getLong("vol")),
                    regionId, from, toExclusive);
        }
        return jdbc.query("""
                SELECT to_char(obs_time AT TIME ZONE 'Asia/Seoul', 'YYYY-MM') AS m, sum(volume) AS vol
                FROM obs_part
                WHERE region_id = ? AND obs_time >= ? AND obs_time < ?
                GROUP BY 1 ORDER BY 1""",
                (rs, i) -> new Rows.MonthlyVolume(rs.getString("m"), rs.getLong("vol")),
                regionId, kst(from), kst(toExclusive));
    }

    public List<Rows.DailySpeed> dailyAvgSpeed(Source src, LocalDate from, LocalDate toExclusive) {
        if (src == Source.ROLLUP) {
            return jdbc.query("""
                    SELECT to_char(day, 'YYYY-MM-DD') AS d,
                           round((sum(sum_speed) / sum(samples))::numeric, 2) AS avg_speed
                    FROM daily_region_stats
                    WHERE day >= ? AND day < ?
                    GROUP BY 1 ORDER BY 1""",
                    (rs, i) -> new Rows.DailySpeed(rs.getString("d"), rs.getDouble("avg_speed")),
                    from, toExclusive);
        }
        return jdbc.query("""
                SELECT to_char(obs_time AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS d,
                       round(avg(speed_kmh)::numeric, 2) AS avg_speed
                FROM obs_part
                WHERE obs_time >= ? AND obs_time < ?
                GROUP BY 1 ORDER BY 1""",
                (rs, i) -> new Rows.DailySpeed(rs.getString("d"), rs.getDouble("avg_speed")),
                kst(from), kst(toExclusive));
    }

    public List<Rows.RegionVolume> volumeRanking(Source src, LocalDate from, LocalDate toExclusive) {
        if (src == Source.ROLLUP) {
            return jdbc.query("""
                    SELECT region_id, sum(total_volume) AS vol
                    FROM daily_region_stats
                    WHERE day >= ? AND day < ?
                    GROUP BY 1 ORDER BY 2 DESC, 1""",
                    (rs, i) -> new Rows.RegionVolume(rs.getInt("region_id"), rs.getLong("vol")),
                    from, toExclusive);
        }
        return jdbc.query("""
                SELECT region_id, sum(volume) AS vol
                FROM obs_part
                WHERE obs_time >= ? AND obs_time < ?
                GROUP BY 1 ORDER BY 2 DESC, 1""",
                (rs, i) -> new Rows.RegionVolume(rs.getInt("region_id"), rs.getLong("vol")),
                kst(from), kst(toExclusive));
    }
}
