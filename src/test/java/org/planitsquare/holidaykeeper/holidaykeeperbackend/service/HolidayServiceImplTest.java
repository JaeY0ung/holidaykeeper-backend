package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("HolidayService 단위 테스트")
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private CountryService countryService;

    @Mock
    private NagerApiClient nagerApiClient;

    @Mock
    private HolidayConverter holidayConverter;

    @InjectMocks
    private HolidayServiceImpl holidayService;

    private Country korea;
    private Country usa;
    private Holiday holiday1;
    private Holiday holiday2;
    private HolidayResponse holidayResponse1;
    private HolidayResponse holidayResponse2;

    @BeforeEach
    void setUp() {

        korea = Country.builder()
            .id(1L)
            .code("KR")
            .name("South Korea")
            .build();

        usa = Country.builder()
            .id(2L)
            .code("US")
            .name("United States")
            .build();

        holiday1 = Holiday.builder()
            .id(1L)
            .date(LocalDate.of(2025, 1, 1))
            .localName("신정")
            .name("New Year's Day")
            .country(korea)
            .fixed(true)
            .global(true)
            .build();

        holiday2 = Holiday.builder()
            .id(2L)
            .date(LocalDate.of(2025, 3, 1))
            .localName("삼일절")
            .name("Independence Movement Day")
            .country(korea)
            .fixed(true)
            .global(true)
            .build();

        holidayResponse1 = new HolidayResponse(
            "2025-01-01",
            "신정",
            "New Year's Day",
            "KR",
            true,
            true,
            null,
            null,
            new String[] { "Public" }
        );

        holidayResponse2 = new HolidayResponse(
            "2025-03-01",
            "삼일절",
            "Independence Movement Day",
            "KR",
            true,
            true,
            null,
            null,
            new String[] { "Public" }
        );
    }

    @Nested
    @DisplayName("getHolidayList 메서드는")
    class Describe_getHolidayList {

        @Nested
        @DisplayName("DB에 공휴일 데이터가 있으면")
        class Context_with_existing_data {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryByCode("KR"))
                    .thenReturn(korea);

                when(holidayRepository.findByCountryAndDateBetween(
                    eq(korea),
                    eq(LocalDate.of(2025, 1, 1)),
                    eq(LocalDate.of(2025, 12, 31))
                ))
                    .thenReturn(List.of(holiday1, holiday2));
            }

            @Test
            @DisplayName("DB에서 조회한 공휴일 목록을 반환한다")
            void it_returns_holidays_from_db() {
                // when
                List<Holiday> result = holidayService.getHolidayList("KR", 2025);

                // then
                assertThat(result).hasSize(2);
                assertThat(result).containsExactly(holiday1, holiday2);
                verify(countryService, times(1)).getCountryByCode("KR");
                verify(holidayRepository, times(1)).findByCountryAndDateBetween(any(), any(),
                    any());
                verify(nagerApiClient, never()).fetchPublicHolidays(anyString(), anyInt());
            }
        }

        @Nested
        @DisplayName("DB에 공휴일 데이터가 없으면")
        class Context_without_existing_data {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryByCode("KR"))
                    .thenReturn(korea);

                when(holidayRepository.findByCountryAndDateBetween(
                    eq(korea),
                    eq(LocalDate.of(2025, 1, 1)),
                    eq(LocalDate.of(2025, 12, 31))
                ))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(List.of(holiday1, holiday2));

                when(countryService.getCountryList())
                    .thenReturn(List.of(korea));

                when(nagerApiClient.fetchPublicHolidays(anyString(), anyInt()))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(holidayConverter.toEntity(any(HolidayResponse.class), any(Country.class)))
                    .thenReturn(holiday1)
                    .thenReturn(holiday2);
            }

            @Test
            @DisplayName("최근 6년 데이터를 동기화하고 반환한다")
            void it_syncs_and_returns() {
                // when
                List<Holiday> result = holidayService.getHolidayList("KR", 2025);

                // then
                assertThat(result).hasSize(2);
                verify(holidayRepository, times(2)).findByCountryAndDateBetween(any(), any(),
                    any());
                verify(countryService, times(1)).getCountryList();
                verify(nagerApiClient, times(6)).fetchPublicHolidays(anyString(),
                    anyInt()); // ✅ 6년 (2020-2025)
            }
        }

        @Nested
        @DisplayName("존재하지 않는 국가 코드가 주어지면")
        class Context_with_invalid_country_code {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryByCode("XX"))
                    .thenThrow(new IllegalArgumentException("잘못된 국가 코드입니다"));
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> holidayService.getHolidayList("XX", 2025))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("잘못된 국가 코드입니다");

                verify(holidayRepository, never()).findByCountryAndDateBetween(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("syncHolidaysForRecentYears 메서드는")
    class Describe_syncHolidaysForRecentYears {

        @Nested
        @DisplayName("정상 동작 시")
        class Context_with_normal_operation {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryList())
                    .thenReturn(List.of(korea, usa));

                when(nagerApiClient.fetchPublicHolidays(anyString(), anyInt()))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(holidayConverter.toEntity(any(HolidayResponse.class), any(Country.class)))
                    .thenReturn(holiday1)
                    .thenReturn(holiday2);
            }

            @Test
            @DisplayName("모든 국가의 최근 6년(2020-2025) 공휴일을 동기화한다")
            void it_syncs_all_holidays() {
                // when
                holidayService.syncHolidaysFor6Years();  // ✅ 메서드명 변경

                // then
                // 2개 국가 × 6년(2020~2025) = 12번 API 호출
                verify(nagerApiClient, times(12)).fetchPublicHolidays(anyString(), anyInt());
                verify(holidayRepository, times(12)).deleteByCountryAndDateBetween(any(), any(),
                    any());
                verify(holidayRepository, times(12)).saveAll(any());
            }
        }

        @Nested
        @DisplayName("국가 목록이 비어있으면")
        class Context_with_empty_country_list {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryList())
                    .thenReturn(Collections.emptyList());
            }

            @Test
            @DisplayName("API를 호출하지 않는다")
            void it_does_not_call_api() {
                // when
                holidayService.syncHolidaysFor6Years();  // ✅ 메서드명 변경

                // then
                verify(nagerApiClient, never()).fetchPublicHolidays(anyString(), anyInt());
                verify(holidayRepository, never()).saveAll(any());
            }
        }

        @Nested
        @DisplayName("일부 API 호출이 실패해도")
        class Context_with_partial_api_failure {

            @BeforeEach
            void setUp() {

                when(countryService.getCountryList())
                    .thenReturn(List.of(korea));

                // ✅ 수정: 2020년만 실패, 나머지는 성공
                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2020)))
                    .thenThrow(new RuntimeException("API 호출 실패"));

                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2021)))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2022)))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2023)))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2024)))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(nagerApiClient.fetchPublicHolidays(eq("KR"), eq(2025)))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(holidayConverter.toEntity(any(HolidayResponse.class), any(Country.class)))
                    .thenReturn(holiday1);
            }

            @Test
            @DisplayName("나머지 데이터는 계속 처리한다")
            void it_continues_processing() {
                // when
                holidayService.syncHolidaysFor6Years();  // ✅ 메서드명 변경

                // then
                // 6번 호출 시도 (2020~2025)
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2020));
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2021));
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2022));
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2023));
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2024));
                verify(nagerApiClient, times(1)).fetchPublicHolidays(eq("KR"), eq(2025));

                // 5번 성공 (2020 제외, 2021-2025 성공)
                verify(holidayRepository, times(5)).saveAll(any());
            }
        }
    }

    @Nested
    @DisplayName("syncHolidaysByYear 메서드는")
    class Describe_syncHolidaysByYear {

        @Nested
        @DisplayName("정상 동작 시")
        class Context_with_normal_operation {

            @BeforeEach
            void setUp() {

                when(nagerApiClient.fetchPublicHolidays("KR", 2025))
                    .thenReturn(List.of(holidayResponse1, holidayResponse2));

                when(holidayConverter.toEntity(any(HolidayResponse.class), eq(korea)))
                    .thenReturn(holiday1)
                    .thenReturn(holiday2);
            }

            @Test
            @DisplayName("특정 국가의 특정 연도 공휴일을 동기화한다")
            void it_syncs_holidays() {
                // when
                holidayService.syncHolidaysByYear(korea, 2025);

                // then
                verify(nagerApiClient, times(1)).fetchPublicHolidays("KR", 2025);
                verify(holidayRepository, times(1)).deleteByCountryAndDateBetween(
                    eq(korea),
                    eq(LocalDate.of(2025, 1, 1)),
                    eq(LocalDate.of(2025, 12, 31))
                );
                verify(holidayRepository, times(1)).saveAll(any());
            }
        }

        @Nested
        @DisplayName("API 응답이 null이면")
        class Context_with_null_response {

            @BeforeEach
            void setUp() {

                when(nagerApiClient.fetchPublicHolidays("KR", 2025))
                    .thenReturn(null);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> holidayService.syncHolidaysByYear(korea, 2025))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("공휴일 목록 API 호출 실패");

                verify(holidayRepository, never()).saveAll(any());
            }
        }

        @Nested
        @DisplayName("API 응답이 비어있으면")
        class Context_with_empty_response {

            @BeforeEach
            void setUp() {

                when(nagerApiClient.fetchPublicHolidays("KR", 2025))
                    .thenReturn(Collections.emptyList());
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> holidayService.syncHolidaysByYear(korea, 2025))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("공휴일 목록 API 호출 실패");

                verify(holidayRepository, never()).saveAll(any());
            }
        }

        @Nested
        @DisplayName("API 호출이 실패하면")
        class Context_with_api_failure {

            @BeforeEach
            void setUp() {

                when(nagerApiClient.fetchPublicHolidays("KR", 2025))
                    .thenThrow(new RuntimeException("네트워크 오류"));
            }

            @Test
            @DisplayName("예외를 전파한다")
            void it_propagates_exception() {
                // when & then
                assertThatThrownBy(() -> holidayService.syncHolidaysByYear(korea, 2025))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("네트워크 오류");

                verify(holidayRepository, never()).deleteByCountryAndDateBetween(any(), any(),
                    any());
                verify(holidayRepository, never()).saveAll(any());
            }
        }
    }
}