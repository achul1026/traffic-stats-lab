package lab.traffic.stats;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatsController.class)
class StatsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StatsService service;

    @Test
    void 월별_통행량_정상_응답() throws Exception {
        when(service.monthlyVolume(eq(Source.ROLLUP), eq(7), eq(YearMonth.of(2024, 10)), eq(YearMonth.of(2024, 12)), eq(true)))
                .thenReturn(List.of(new Rows.MonthlyVolume("2024-10", 123)));

        mvc.perform(get("/api/stats/regions/7/monthly-volume").param("from", "2024-10").param("to", "2024-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2024-10"))
                .andExpect(jsonPath("$[0].volume").value(123));
    }

    @Test
    void 기본값은_집계테이블_캐시사용() throws Exception {
        when(service.dailyAvgSpeed(any(), any(), anyBoolean())).thenReturn(List.of());
        mvc.perform(get("/api/stats/daily-avg-speed").param("month", "2024-11")).andExpect(status().isOk());
        org.mockito.Mockito.verify(service).dailyAvgSpeed(Source.ROLLUP, YearMonth.of(2024, 11), true);
    }

    @Test
    void 월_형식이_틀리면_400() throws Exception {
        mvc.perform(get("/api/stats/daily-avg-speed").param("month", "2024/11")).andExpect(status().isBadRequest());
    }

    @Test
    void 지역번호_범위를_벗어나면_400() throws Exception {
        mvc.perform(get("/api/stats/regions/99/monthly-volume").param("from", "2024-10").param("to", "2024-12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 기간이_거꾸로면_400() throws Exception {
        mvc.perform(get("/api/stats/regions/7/monthly-volume").param("from", "2024-12").param("to", "2024-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 알수없는_source_는_400() throws Exception {
        mvc.perform(get("/api/stats/volume-ranking").param("year", "2024").param("source", "WRONG"))
                .andExpect(status().isBadRequest());
    }
}
