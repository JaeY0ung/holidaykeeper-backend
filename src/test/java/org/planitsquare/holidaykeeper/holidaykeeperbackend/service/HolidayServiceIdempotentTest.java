package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("HolidayService - 중복 실행(Idempotent) 테스트")
class HolidayServiceIdempotentTest {

    @Autowired
    private HolidayService holidayService;
    @Autowired
    private HolidayRepository holidayRepository;

    @BeforeEach
    void setup() {

    }

    @Test
    @DisplayName("syncHolidaysFor6Years는 중복 호출해도 전체 개수가 동일해야 한다")
    void syncHolidays_isIdempotent() {

        assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
        long firstCount = holidayRepository.count();

        assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
        long secondCount = holidayRepository.count();

        assertThat(secondCount)
            .as("중복 호출에도 결과는 바뀌지 않아야 한다")
            .isEqualTo(firstCount);
    }
}