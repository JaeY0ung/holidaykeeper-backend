package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql(scripts = "/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("HolidayService 통합 테스트 전체")
class HolidayServiceIntegrationTest {

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

    @Nested
    @DisplayName("syncHolidaysFor6Years 동작 검증")
    class SyncHolidaysFor6YearsTest {

        @Test
        @DisplayName("최초 실행 시 휴일 데이터가 정상적으로 저장된다")
        void syncHolidaysFor6Years_firstTime_insertsData() {
            // given
            assertThat(holidayRepository.count()).isZero();

            // when
            assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());

            // then
            assertThat(holidayRepository.count())
                .as("최초 동기화 후 휴일 데이터 개수는 0보다 커야 한다")
                .isGreaterThan(0);
        }

        @Test
        @DisplayName("중복 API 응답이 있더라도 예외 없이 동일 결과로 재실행된다 (idempotent)")
        void syncHolidaysFor6Years_isIdempotent_evenWhenApiReturnsDuplicates() {
            // given
            // 1차 동기화
            assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
            long firstCount = holidayRepository.count();

            // when
            // 2차 동기화 (Nager.Date API가 중복 데이터를 내려줘도 예외 없이 처리되어야 함)
            assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());

            // then
            long secondCount = holidayRepository.count();

            // upsert 로직 + 중복 제거 키가 잘 동작한다면,
            // 재동기화 이후에도 전체 개수는 동일해야 함
            assertThat(secondCount)
                .as("중복 응답이 있어도 upsert 이후 전체 개수는 변하지 않아야 한다")
                .isEqualTo(firstCount);
        }

        @Test
        @DisplayName("일부 데이터가 삭제된 상태에서도 재동기화 시 원래 개수로 복구된다")
        void syncHolidaysFor6Years_recoversAfterManualDelete() {
            // given
            assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());
            long originalCount = holidayRepository.count();

            Holiday one = holidayRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("휴일 데이터가 존재해야 합니다."));

            holidayRepository.delete(one);
            long afterDeleteCount = holidayRepository.count();
            assertThat(afterDeleteCount)
                .as("임의 삭제 후에는 전체 개수가 1 줄어야 한다")
                .isEqualTo(originalCount - 1);

            // when
            // 재동기화 수행
            assertDoesNotThrow(() -> holidayService.syncHolidaysFor6Years());

            // then
            long afterResyncCount = holidayRepository.count();
            assertThat(afterResyncCount)
                .as("재동기화 후에는 기존 데이터 수가 복구되어야 한다")
                .isEqualTo(originalCount);
        }
    }
}