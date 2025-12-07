package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.ErrorCode;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayDeleteRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayRefreshRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayDeleteResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayRefreshResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.enums.HolidayType;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
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
    void setUp() {

        holidayRepository.deleteAll();
        countryRepository.deleteAll();

        Country korea = countryRepository.save(
            Country.builder()
                .code("KR")
                .name("South Korea")
                .build()
        );
    }

    // ---------------------------------------------------------
    // 1) 검색 기능 테스트
    // ---------------------------------------------------------

    @Test
    @DisplayName("검색: DB 비어있으면 자동 동기화 후 반환")
    void search_autoSync() {

        HolidaySearchRequest req = new HolidaySearchRequest(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
            "KR",
            null,
            0,
            20
        );

        HolidaySearchResponse res = holidayService.searchHolidays(req);

        assertThat(res.holidays()).isNotEmpty();
        assertThat(res.pageInfo()).isNotNull();
        assertThat(res.holidays()).allMatch(h -> h.date().getYear() == 2025);
    }

    @Test
    @DisplayName("검색: DB 데이터 있으면 DB에서 조회")
    void search_returnsFromDbWhenExists() {

        holidayService.refreshHolidays(new HolidayRefreshRequest("KR", 2025));
        long before = holidayRepository.count();

        HolidaySearchRequest req = new HolidaySearchRequest(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
            "KR",
            null,
            0,
            20
        );

        HolidaySearchResponse res = holidayService.searchHolidays(req);

        assertThat(res.holidays()).isNotEmpty();
        assertThat(holidayRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("검색: 페이징 정보 검증")
    void search_paging() {

        holidayService.refreshHolidays(new HolidayRefreshRequest("KR", 2025));

        HolidaySearchRequest req = new HolidaySearchRequest(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
            "KR",
            null,
            0,
            5
        );

        HolidaySearchResponse res = holidayService.searchHolidays(req);

        assertThat(res.pageInfo().pageSize()).isEqualTo(5);
        assertThat(res.pageInfo().currentPage()).isEqualTo(0);
        assertThat(res.pageInfo().totalElements()).isGreaterThan(5);
        assertThat(res.pageInfo().totalPages()).isGreaterThan(1);
    }

    // ---------------------------------------------------------
    // 2) 검색 예외 테스트
    // ---------------------------------------------------------

    @Test
    @DisplayName("예외: 국가 코드 잘못됨")
    void search_invalidCountry() {

        HolidaySearchRequest req = new HolidaySearchRequest(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
            "XX",
            null,
            0,
            20
        );

        assertThatThrownBy(() -> holidayService.searchHolidays(req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_COUNTRY_CODE);
    }

    @Test
    @DisplayName("예외: startDate > endDate")
    void search_invalidDateRange() {

        assertThatThrownBy(() ->
            new HolidaySearchRequest(
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 1, 1),
                "KR",
                null,
                0,
                20
            )
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("예외: 타입 개수 너무 많음")
    void search_typeTooMany() {

        List<HolidayType> invalid = List.of(
            HolidayType.PUBLIC,
            HolidayType.BANK,
            HolidayType.SCHOOL,
            HolidayType.OPTIONAL,
            HolidayType.AUTHORITIES,
            HolidayType.OBSERVANCE,
            HolidayType.PUBLIC // 7개
        );

        assertThatThrownBy(() ->
            new HolidaySearchRequest(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "KR",
                invalid,
                0,
                20
            )
        )
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("예외: endDate가 미래")
    void search_futureEndDate() {

        assertThatThrownBy(() ->
            new HolidaySearchRequest(
                LocalDate.of(2025, 1, 1),
                LocalDate.now().plusDays(1),
                "KR",
                null,
                0,
                20
            )
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
    }

    // ---------------------------------------------------------
    // 3) 삭제 기능 테스트
    // ---------------------------------------------------------

    @Test
    @DisplayName("삭제: 미래 연도 삭제 시 예외")
    void delete_futureYear() {

        HolidayDeleteRequest req = new HolidayDeleteRequest("KR", 2030);

        assertThatThrownBy(() -> holidayService.deleteHolidays(req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
    }

    @Test
    @DisplayName("삭제: 정상 삭제")
    void delete_success() {

        holidayService.refreshHolidays(new HolidayRefreshRequest("KR", 2025));

        HolidayDeleteRequest req = new HolidayDeleteRequest("KR", 2025);

        HolidayDeleteResponse res = holidayService.deleteHolidays(req);

        assertThat(res.deletedCount()).isGreaterThan(0);
    }

    // ---------------------------------------------------------
    // 4) 재동기화 기능 테스트
    // ---------------------------------------------------------

    @Test
    @DisplayName("재동기화: 정상 동작")
    void refresh_success() {

        HolidayRefreshRequest req = new HolidayRefreshRequest("KR", 2025);

        HolidayRefreshResponse res = holidayService.refreshHolidays(req);

        assertThat(res.newCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("재동기화: 미래 연도 요청 시 예외")
    void refresh_futureYear() {

        HolidayRefreshRequest req = new HolidayRefreshRequest("KR", 2030);

        assertThatThrownBy(() -> holidayService.refreshHolidays(req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
    }

    // ---------------------------------------------------------
    // 5) 대량 동기화 테스트
    // ---------------------------------------------------------

    @Test
    @DisplayName("6년 전체 동기화 정상 동작")
    void syncSixYears_success() {

        HolidaySyncResponse res = holidayService.syncHolidaysFor6Years();

        assertThat(res.successCount()).isGreaterThan(0);
        assertThat(res.totalCount()).isGreaterThan(0);
    }
}