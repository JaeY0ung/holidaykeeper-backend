package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("HolidayService 통합 테스트")
class HolidayServiceIntegrationTest {

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private CountryRepository countryRepository;

    private Country korea;

    @BeforeEach
    void setUp() {

        holidayRepository.deleteAll();
        countryRepository.deleteAll();

        // 한국 데이터 생성
        korea = countryRepository.save(
            Country.builder()
                .code("KR")
                .name("South Korea")
                .build()
        );
    }

    @Test
    @DisplayName("getHolidayList - DB에 데이터가 없으면 자동 동기화 후 반환")
    void getHolidayList_autoSyncWhenEmpty() {
        // given
        assertThat(holidayRepository.count()).isZero();

        // when
        List<Holiday> result = holidayService.getHolidayList("KR", 2025);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(h -> h.getCountry().getCode().equals("KR"));
        assertThat(result).allMatch(h -> h.getDate().getYear() == 2025);
    }

    @Test
    @DisplayName("getHolidayList - DB에 데이터가 있으면 DB에서 조회")
    void getHolidayList_returnsFromDbWhenExists() {
        // given
        holidayService.syncHolidaysByYear(korea, 2025);
        long countBefore = holidayRepository.count();

        // when
        List<Holiday> result = holidayService.getHolidayList("KR", 2025);

        // then
        assertThat(result).isNotEmpty();
        assertThat(holidayRepository.count()).isEqualTo(countBefore); // 추가 동기화 안 됨
    }

    @Test
    @DisplayName("syncHolidaysForRecentYears - 전체 국가 최근 6년 동기화")
    void syncHolidaysForRecentYears_syncsAllCountries() {
        // given
        assertThat(holidayRepository.count()).isZero();

        // when
        holidayService.syncHolidaysForRecentYears();

        // then
        assertThat(holidayRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("syncHolidaysByYear - 특정 국가/연도 공휴일 동기화")
    void syncHolidaysByYear_syncsSpecificYear() {
        // given
        assertThat(holidayRepository.count()).isZero();

        // when
        holidayService.syncHolidaysByYear(korea, 2025);

        // then
        List<Holiday> holidays = holidayRepository.findAll();
        assertThat(holidays).isNotEmpty();
        assertThat(holidays).allMatch(h -> h.getCountry().getCode().equals("KR"));
        assertThat(holidays).allMatch(h -> h.getDate().getYear() == 2025);
    }

    @Test
    @DisplayName("syncHolidaysByYear - 기존 데이터 삭제 후 재저장")
    void syncHolidaysByYear_replacesExistingData() {
        // given
        holidayService.syncHolidaysByYear(korea, 2025);
        long countBefore = holidayRepository.count();
        assertThat(countBefore).isGreaterThan(0);

        // when - 재동기화
        holidayService.syncHolidaysByYear(korea, 2025);

        // then
        long countAfter = holidayRepository.count();
        assertThat(countAfter).isGreaterThan(0);
        // 같은 연도 데이터는 덮어써짐
    }

    @Test
    @DisplayName("syncHolidaysByYear - 연도별 데이터 분리 저장")
    void syncHolidaysByYear_separatesDataByYear() {
        // given - 2024년과 2025년 데이터 저장
        holidayService.syncHolidaysByYear(korea, 2024);
        holidayService.syncHolidaysByYear(korea, 2025);

        // when - 전체 조회
        List<Holiday> all2024 = holidayRepository.findByCountryAndDateBetween(
            korea,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31)
        );
        List<Holiday> all2025 = holidayRepository.findByCountryAndDateBetween(
            korea,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)
        );

        // then
        assertThat(all2024).isNotEmpty();
        assertThat(all2025).isNotEmpty();
        assertThat(all2024).allMatch(h -> h.getDate().getYear() == 2024);
        assertThat(all2025).allMatch(h -> h.getDate().getYear() == 2025);
    }

    @Test
    @DisplayName("한국 신정(1월 1일) 데이터 검증")
    void verifyKoreaNewYearHoliday() {
        // given
        holidayService.syncHolidaysByYear(korea, 2025);

        // when
        List<Holiday> holidays = holidayRepository.findAll();
        Holiday newYear = holidays.stream()
            .filter(h -> h.getDate().equals(LocalDate.of(2025, 1, 1)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("신정 데이터가 없습니다"));

        // then
        assertThat(newYear.getName()).containsIgnoringCase("New Year");
        assertThat(newYear.getCountry().getCode()).isEqualTo("KR");
        assertThat(newYear.getGlobal()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 국가 코드로 조회 시 예외 발생")
    void getHolidayList_invalidCountryCode() {
        // when & then
        assertThatThrownBy(() -> holidayService.getHolidayList("XX", 2025))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("잘못된 국가 코드입니다");
    }

    @Test
    @DisplayName("여러 국가의 같은 날짜 공휴일 구분 저장")
    void syncMultipleCountriesSameDate() {
        // given
        Country usa = countryRepository.save(
            Country.builder()
                .code("US")
                .name("United States")
                .build()
        );

        // when
        holidayService.syncHolidaysByYear(korea, 2025);
        holidayService.syncHolidaysByYear(usa, 2025);

        // then
        List<Holiday> koreaHolidays = holidayRepository.findByCountryAndDateBetween(
            korea,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)
        );
        List<Holiday> usaHolidays = holidayRepository.findByCountryAndDateBetween(
            usa,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)
        );

        assertThat(koreaHolidays).isNotEmpty();
        assertThat(usaHolidays).isNotEmpty();
        assertThat(koreaHolidays).allMatch(h -> h.getCountry().getCode().equals("KR"));
        assertThat(usaHolidays).allMatch(h -> h.getCountry().getCode().equals("US"));
    }
}