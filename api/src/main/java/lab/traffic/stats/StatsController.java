package lab.traffic.stats;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    /** 특정 지역의 월별 통행량 (예: from=2024-10&to=2024-12) */
    @GetMapping("/regions/{regionId}/monthly-volume")
    public List<Rows.MonthlyVolume> monthlyVolume(
            @PathVariable @Min(1) @Max(31) int regionId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "ROLLUP") Source source,
            @RequestParam(defaultValue = "true") boolean cache) {
        YearMonth f = parse(from), t = parse(to);
        if (t.isBefore(f) || f.plusMonths(36).isBefore(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기간은 from ≤ to, 최대 36개월");
        }
        return service.monthlyVolume(source, regionId, f, t, cache);
    }

    /** 전 지역 일별 평균 속도 (예: month=2024-11) */
    @GetMapping("/daily-avg-speed")
    public List<Rows.DailySpeed> dailyAvgSpeed(
            @RequestParam String month,
            @RequestParam(defaultValue = "ROLLUP") Source source,
            @RequestParam(defaultValue = "true") boolean cache) {
        return service.dailyAvgSpeed(source, parse(month), cache);
    }

    /** 지역별 연간 통행량 순위 (예: year=2024) */
    @GetMapping("/volume-ranking")
    public List<Rows.RegionVolume> volumeRanking(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "ROLLUP") Source source,
            @RequestParam(defaultValue = "true") boolean cache) {
        return service.volumeRanking(source, year, cache);
    }

    private static YearMonth parse(String s) {
        try {
            return YearMonth.parse(s);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "월 형식은 yyyy-MM: " + s);
        }
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<String> badParam(jakarta.validation.ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
