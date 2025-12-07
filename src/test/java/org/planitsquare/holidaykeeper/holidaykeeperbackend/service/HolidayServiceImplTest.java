package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.ErrorCode;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayDeleteRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayRefreshRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayQueryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("HolidayService 단위 테스트")
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private HolidayQueryRepository holidayQueryRepository;
    @Mock
    private CountryService countryService;
    @Mock
    private NagerApiClient nagerApiClient;
    @Mock
    private HolidayConverter holidayConverter;

    @InjectMocks
    private HolidayServiceImpl holidayService;

    private Country korea;
    private Holiday h1, h2;
    private HolidayResponse r1, r2;

    @BeforeEach
    void setUp() {

        korea = Country.builder()
            .id(1L).code("KR").name("South Korea")
            .build();

        h1 = Holiday.builder()
            .id(1L)
            .date(LocalDate.of(2025, 1, 1))
            .localName("신정")
            .name("New Year")
            .types("Public")
            .country(korea)
            .build();

        h2 = Holiday.builder()
            .id(2L)
            .date(LocalDate.of(2025, 3, 1))
            .localName("삼일절")
            .name("Independence Day")
            .types("Public")
            .country(korea)
            .build();

        r1 = new HolidayResponse(
            "2025-01-01", "신정", "New Year", "KR",
            true, true, null, 1948, new String[] { "Public" }
        );

        r2 = new HolidayResponse(
            "2025-03-01", "삼일절", "Independence Day", "KR",
            true, true, null, 1948, new String[] { "Public" }
        );
    }

    // ===========================================================
    // SEARCH TESTS
    // ===========================================================
    @Nested
    @DisplayName("searchHolidays 메서드는")
    class Describe_search {

        @Test
        @DisplayName("정상 검색 시 HolidaySearchResponse 반환")
        void search_success() {

            HolidaySearchRequest req = new HolidaySearchRequest(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "KR",
                null,
                0,
                20
            );

            when(countryService.getCountryByCode("KR")).thenReturn(korea);
            when(holidayQueryRepository.searchWithPaging(any(), any(), any(), any(), anyInt(),
                anyInt()))
                .thenReturn(List.of(h1, h2));
            when(holidayQueryRepository.count(any(), any(), any(), any()))
                .thenReturn(2L);

            when(holidayConverter.toSearchResponseWithPaging(any(), anyInt(), anyInt(), anyLong()))
                .thenReturn(
                    HolidaySearchResponse.builder()
                        .pageInfo(
                            HolidaySearchResponse.PageInfo.builder()
                                .currentPage(0)
                                .pageSize(20)
                                .totalElements(2)
                                .totalPages(1)
                                .isFirst(true)
                                .isLast(true)
                                .isEmpty(false)
                                .build()
                        )
                        .holidays(Collections.emptyList())
                        .build()
                );

            HolidaySearchResponse res = holidayService.searchHolidays(req);

            assertThat(res.pageInfo().totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("유효하지 않은 국가 코드 → BusinessException(INVALID_COUNTRY_CODE)")
        void search_invalid_country() {

            when(countryService.getCountryByCode("XX"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_COUNTRY_CODE));

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
        @DisplayName("startDate > endDate 이면 INVALID_DATE_RANGE")
        void search_invalid_date_range() {

            assertThatThrownBy(() ->
                new HolidaySearchRequest(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 1, 1),
                    "KR",
                    null,
                    0,
                    20
                )
            ).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("endDate가 미래면 FUTURE_YEAR_NOT_ALLOWED")
        void search_future_date() {

            assertThatThrownBy(() ->
                new HolidaySearchRequest(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2030, 1, 1),
                    "KR",
                    null, 0, 20
                )
            ).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
        }
    }

    // ===========================================================
    // REFRESH TESTS (PRIVATE syncByYear 간접 테스트)
    // ===========================================================
    @Nested
    @DisplayName("refreshHolidays 메서드는")
    class Describe_refresh {

        @Test
        @DisplayName("정상적인 재동기화 시 newCount 정상 반환")
        void refresh_success() {

            when(countryService.getCountryByCode("KR")).thenReturn(korea);
            when(nagerApiClient.fetchPublicHolidays("KR", 2025))
                .thenReturn(List.of(r1));

            when(holidayRepository.findByCountryAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());
            when(holidayConverter.toEntity(any(), eq(korea)))
                .thenReturn(h1);
            when(holidayRepository.saveAll(any()))
                .thenReturn(List.of(h1));

            HolidayRefreshRequest req = new HolidayRefreshRequest("KR", 2025);

            var res = holidayService.refreshHolidays(req);

            assertThat(res.newCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("미래 연도 요청 → FUTURE_YEAR_NOT_ALLOWED")
        void refresh_future_year() {

            assertThatThrownBy(() ->
                new HolidayRefreshRequest("KR", 2030)
            ).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
        }
    }

    // ===========================================================
    // DELETE TESTS
    // ===========================================================
    @Nested
    @DisplayName("deleteHolidays 메서드는")
    class Describe_delete {

        @Test
        @DisplayName("정상 삭제 시 deletedCount 반환")
        void delete_success() {

            when(countryService.getCountryByCode("KR")).thenReturn(korea);
            when(holidayRepository.deleteByCountryAndDateBetween(any(), any(), any()))
                .thenReturn(5);

            HolidayDeleteRequest req = new HolidayDeleteRequest("KR", 2025);

            var res = holidayService.deleteHolidays(req);

            assertThat(res.deletedCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("미래 연도 요청 → FUTURE_YEAR_NOT_ALLOWED")
        void delete_future_year() {

            assertThatThrownBy(() ->
                new HolidayDeleteRequest("KR", 2030)
            ).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUTURE_YEAR_NOT_ALLOWED);
        }
    }
}