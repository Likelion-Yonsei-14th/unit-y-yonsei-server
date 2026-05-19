package com.likelion.yonsei.daedongje.domain.performance.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LivePerformanceTest {

    @Test
    void singleton_creates_row_with_fixed_id_and_no_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();

        assertThat(livePerformance.getId()).isEqualTo(LivePerformance.SINGLETON_ID);
        assertThat(livePerformance.getPerformance()).isNull();
    }

    @Test
    void updatePerformance_pins_and_clears_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();
        Performance performance = BeanUtils.instantiateClass(Performance.class);
        ReflectionTestUtils.setField(performance, "id", 12L);

        livePerformance.updatePerformance(performance);
        assertThat(livePerformance.getPerformance()).isEqualTo(performance);

        livePerformance.updatePerformance(null);
        assertThat(livePerformance.getPerformance()).isNull();
    }
}
