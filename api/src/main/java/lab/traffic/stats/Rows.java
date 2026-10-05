package lab.traffic.stats;

import java.io.Serializable;

/** 응답 행 모음. Redis 캐시에 저장되므로 Serializable. */
public final class Rows {
    private Rows() {}

    public record MonthlyVolume(String month, long volume) implements Serializable {}
    public record DailySpeed(String day, double avgSpeed) implements Serializable {}
    public record RegionVolume(int regionId, long volume) implements Serializable {}
}
