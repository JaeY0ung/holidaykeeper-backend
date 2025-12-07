package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("HolidayService - 최초 실행 테스트")
class HolidayServiceSyncFirstTest {

    @Autowired
    private HolidayService holidayService;
    @Autowired
    private HolidayRepository holidayRepository;
    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setup() {

        countryRepository.save(Country.builder().code("KR").name("South Korea").build());
        countryRepository.save(Country.builder().code("US").name("United States").build());
        countryRepository.save(Country.builder().code("AL").name("Albania").build());
    }

    @Test
    @DisplayName("최초 syncHolidaysFor6Years 실행 시 데이터가 정상 저장된다")
    void syncHolidaysFor6Years_firstTime() {

        assertThat(holidayRepository.count()).isZero();

        assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());

        assertThat(holidayRepository.count())
            .as("초기 동기화 후 저장된 데이터는 0보다 커야 한다")
            .isGreaterThan(0);
    }
}