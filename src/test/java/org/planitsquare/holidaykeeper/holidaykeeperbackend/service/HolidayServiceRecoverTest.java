package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("HolidayService - 삭제 후 복구 테스트")
class HolidayServiceRecoverTest {

    @Autowired
    private HolidayService holidayService;
    @Autowired
    private HolidayRepository holidayRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setup() {

        countryRepository.save(Country.builder().code("KR").name("South Korea").build());
        countryRepository.save(Country.builder().code("US").name("United States").build());
        countryRepository.save(Country.builder().code("AL").name("Albania").build());
    }

    @Test
    @Transactional
    @DisplayName("수동 삭제 후 재동기화하면 원래 개수로 복구된다")
    void recoverAfterManualDelete() {

        assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
        long originalCount = holidayRepository.count();

        Holiday one = holidayRepository.findAll().stream()
            .findFirst()
            .orElseThrow();

        holidayRepository.delete(one);
        entityManager.flush();
        entityManager.clear();

        assertThat(holidayRepository.count())
            .isEqualTo(originalCount - 1);

        assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
        entityManager.flush();
        entityManager.clear();

        assertThat(holidayRepository.count())
            .isEqualTo(originalCount);
    }
}