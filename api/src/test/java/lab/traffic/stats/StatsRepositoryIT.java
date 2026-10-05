package lab.traffic.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 통합 테스트: 실제 PostgreSQL 에서 원본(RAW)과 집계 테이블(ROLLUP)이 같은 답을 내는지 확인한다.
 * 실행: LAB_IT=1 ./gradlew test   (scripts/api_dataset.sh 로 데이터셋 준비 필요)
 */
@SpringBootTest(properties = "spring.cache.type=none")
@EnabledIfEnvironmentVariable(named = "LAB_IT", matches = "1")
class StatsRepositoryIT {
    @Autowired StatsRepository repo;

    @Test
    void 월별_통행량이_일치한다() {
        var raw = repo.monthlyVolume(Source.RAW, 7, LocalDate.of(2024, 10, 1), LocalDate.of(2025, 1, 1));
        var roll = repo.monthlyVolume(Source.ROLLUP, 7, LocalDate.of(2024, 10, 1), LocalDate.of(2025, 1, 1));
        assertThat(raw).hasSize(3);
        assertThat(roll).isEqualTo(raw);
    }

    @Test
    void 일별_평균속도가_일치한다() {
        var raw = repo.dailyAvgSpeed(Source.RAW, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 1));
        var roll = repo.dailyAvgSpeed(Source.ROLLUP, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 1));
        assertThat(raw).hasSize(30);
        assertThat(roll).hasSameSizeAs(raw);
        for (int i = 0; i < raw.size(); i++) {
            assertThat(roll.get(i).day()).isEqualTo(raw.get(i).day());
            assertThat(roll.get(i).avgSpeed()).isCloseTo(raw.get(i).avgSpeed(), org.assertj.core.data.Offset.offset(0.011));
        }
    }

    @Test
    void 지역별_순위가_일치한다() {
        var raw = repo.volumeRanking(Source.RAW, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1));
        var roll = repo.volumeRanking(Source.ROLLUP, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1));
        assertThat(raw).hasSize(31);
        assertThat(roll).isEqualTo(raw);
    }
}
