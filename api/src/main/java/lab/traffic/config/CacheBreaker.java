package lab.traffic.config;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 간단한 서킷 브레이커. 캐시(Redis)에서 오류가 나면 일정 시간 캐시를 건너뛰고 DB 로 직접 조회한다.
 * 장애 중에 요청마다 타임아웃을 기다리는 문제(측정: 요청당 약 1초)를 막기 위한 것이다.
 * 열려 있는 시간이 지나면 다음 요청이 캐시를 다시 시도하고, 또 실패하면 다시 열린다.
 */
@Component
public class CacheBreaker {
    static final long OPEN_MILLIS = 10_000;
    private final AtomicLong openUntil = new AtomicLong(0);
    private final Clock clock;

    public CacheBreaker() {
        this(Clock.systemUTC());
    }

    CacheBreaker(Clock clock) {
        this.clock = clock;
    }

    public boolean isOpen() {
        return clock.millis() < openUntil.get();
    }

    public void recordFailure() {
        openUntil.set(clock.millis() + OPEN_MILLIS);
    }
}
