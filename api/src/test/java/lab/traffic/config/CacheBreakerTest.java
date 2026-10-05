package lab.traffic.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CacheBreakerTest {
    /** 시간을 마음대로 움직이는 시계 */
    static class MutableClock extends Clock {
        long now = 1_000_000;
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
        @Override public long millis() { return now; }
    }

    @Test
    void 처음에는_닫혀_있다() {
        assertThat(new CacheBreaker(new MutableClock()).isOpen()).isFalse();
    }

    @Test
    void 실패하면_열리고_시간이_지나면_다시_닫힌다() {
        var clock = new MutableClock();
        var b = new CacheBreaker(clock);

        b.recordFailure();
        assertThat(b.isOpen()).isTrue();

        clock.now += CacheBreaker.OPEN_MILLIS - 1;
        assertThat(b.isOpen()).isTrue();

        clock.now += 1;
        assertThat(b.isOpen()).isFalse();   // 다음 요청이 캐시를 다시 시도한다
    }
}
