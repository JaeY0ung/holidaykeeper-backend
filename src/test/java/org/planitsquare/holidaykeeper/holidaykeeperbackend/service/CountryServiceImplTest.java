package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.CountryConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CountryService 단위 테스트")
class CountryServiceImplTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private NagerApiClient nagerApiClient;

    @Mock
    private CountryConverter countryConverter;

    @InjectMocks
    private CountryServiceImpl countryService;

    private Country country1;
    private Country country2;
    private CountryResponse countryResponse1;
    private CountryResponse countryResponse2;

    @BeforeEach
    void setUp() {

        country1 = Country.builder()
            .id(1L)
            .code("KR")
            .name("South Korea")
            .build();

        country2 = Country.builder()
            .id(2L)
            .code("US")
            .name("United States")
            .build();

        countryResponse1 = new CountryResponse("KR", "South Korea");
        countryResponse2 = new CountryResponse("US", "United States");
    }

    @Nested
    @DisplayName("getCountryList 메서드는")
    class Describe_getCountryList {

        @Nested
        @DisplayName("DB에 국가 데이터가 있으면")
        class Context_with_existing_data {

            @BeforeEach
            void setUp() {

                when(countryRepository.findAll())
                    .thenReturn(List.of(country1, country2));
            }

            @Test
            @DisplayName("DB에서 조회한 국가 목록을 반환한다")
            void it_returns_countries_from_db() {
                // when
                List<Country> result = countryService.getCountryList();

                // then
                assertThat(result).hasSize(2);
                assertThat(result).containsExactly(country1, country2);
                verify(countryRepository, times(1)).findAll();
                verify(nagerApiClient, never()).fetchAvailableCountries();
            }
        }

        @Nested
        @DisplayName("DB에 국가 데이터가 없으면")
        class Context_without_existing_data {

            @BeforeEach
            void setUp() {

                when(countryRepository.findAll())
                    .thenReturn(Collections.emptyList())  // 첫 번째 호출: 비어있음
                    .thenReturn(List.of(country1, country2));  // 두 번째 호출: 저장 후

                when(nagerApiClient.fetchAvailableCountries())
                    .thenReturn(List.of(countryResponse1, countryResponse2));

                when(countryConverter.toEntity(countryResponse1))
                    .thenReturn(country1);
                when(countryConverter.toEntity(countryResponse2))
                    .thenReturn(country2);

                when(countryRepository.saveAll(any()))
                    .thenReturn(List.of(country1, country2));
            }

            @Test
            @DisplayName("외부 API를 호출하여 국가 데이터를 저장하고 반환한다")
            void it_fetches_from_api_and_saves() {
                // when
                List<Country> result = countryService.getCountryList();

                // then
                assertThat(result).hasSize(2);
                verify(countryRepository, times(2)).findAll();  // 저장 전후 2번 호출
                verify(nagerApiClient, times(1)).fetchAvailableCountries();
                verify(countryRepository, times(1)).deleteAll();
                verify(countryRepository, times(1)).saveAll(any());
            }
        }

        @Nested
        @DisplayName("외부 API 호출이 실패하면")
        class Context_with_api_failure {

            @BeforeEach
            void setUp() {

                when(countryRepository.findAll())
                    .thenReturn(Collections.emptyList());

                when(nagerApiClient.fetchAvailableCountries())
                    .thenThrow(new RuntimeException("API 호출 실패"));
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> countryService.getCountryList())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("API 호출 실패");

                verify(nagerApiClient, times(1)).fetchAvailableCountries();
            }
        }

        @Nested
        @DisplayName("외부 API 응답이 비어있으면")
        class Context_with_empty_api_response {

            @BeforeEach
            void setUp() {

                when(countryRepository.findAll())
                    .thenReturn(Collections.emptyList());

                when(nagerApiClient.fetchAvailableCountries())
                    .thenReturn(Collections.emptyList());
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> countryService.getCountryList())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("국가 목록 API 호출 실패");

                verify(nagerApiClient, times(1)).fetchAvailableCountries();
                verify(countryRepository, never()).saveAll(any());
            }
        }
    }

    @Nested
    @DisplayName("getCountryByCode 메서드는")
    class Describe_getCountryByCode {

        @Nested
        @DisplayName("존재하는 국가 코드가 주어지면")
        class Context_with_existing_code {

            @BeforeEach
            void setUp() {

                when(countryRepository.findByCode("KR"))
                    .thenReturn(Optional.of(country1));
            }

            @Test
            @DisplayName("해당 국가를 반환한다")
            void it_returns_country() {
                // when
                Country result = countryService.getCountryByCode("KR");

                // then
                assertThat(result).isNotNull();
                assertThat(result.getCode()).isEqualTo("KR");
                assertThat(result.getName()).isEqualTo("South Korea");
                verify(countryRepository, times(1)).findByCode("KR");
            }
        }

        @Nested
        @DisplayName("존재하지 않는 국가 코드가 주어지면")
        class Context_with_non_existing_code {

            @BeforeEach
            void setUp() {

                when(countryRepository.findByCode("XX"))
                    .thenReturn(Optional.empty());
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void it_throws_exception() {
                // when & then
                assertThatThrownBy(() -> countryService.getCountryByCode("XX"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("잘못된 국가 코드입니다");

                verify(countryRepository, times(1)).findByCode("XX");
            }
        }
    }
}