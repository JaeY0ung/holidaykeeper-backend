package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.CountryConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.ErrorCode;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;


@ExtendWith(MockitoExtension.class)
@DisplayName("CountryService 기본 단위 테스트")
public class CountryServiceImplTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private NagerApiClient nagerApiClient;

    @Mock
    private CountryConverter countryConverter;

    @InjectMocks
    private CountryServiceImpl countryService;

    private Country korea;
    private CountryResponse koreaResponse;

    @BeforeEach
    void setUp() {

        korea = Country.builder()
            .id(1L)
            .code("KR")
            .name("South Korea")
            .build();

        koreaResponse = new CountryResponse("KR", "South Korea");
    }

    // ----------------------------------------------------------------
    // 1) getCountryByCode 테스트
    // ----------------------------------------------------------------
    @Test
    @DisplayName("국가 코드 조회 - 존재하는 경우 반환")
    void getCountryByCode_success() {

        when(countryRepository.findByCode("KR")).thenReturn(Optional.of(korea));

        Country result = countryService.getCountryByCode("KR");

        assertThat(result).isEqualTo(korea);
    }

    @Test
    @DisplayName("국가 코드 조회 - 존재하지 않는 경우 BusinessException")
    void getCountryByCode_fail() {

        when(countryRepository.findByCode("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countryService.getCountryByCode("XX"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_COUNTRY_CODE);
    }

    // ----------------------------------------------------------------
    // 2) getCountryList 테스트
    // ----------------------------------------------------------------
    @Test
    @DisplayName("국가 목록 조회 - DB 데이터 반환")
    void getCountryList_success() {

        when(countryRepository.findAll()).thenReturn(List.of(korea));

        List<Country> result = countryService.getCountryList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(korea);
        verify(countryRepository, times(1)).findAll();
    }

    // ----------------------------------------------------------------
    // 3) syncCountries 테스트
    // ----------------------------------------------------------------
    @Test
    @DisplayName("syncCountries - 정상적으로 API 받아서 저장")
    void syncCountries_success() {

        when(nagerApiClient.fetchAvailableCountries())
            .thenReturn(List.of(koreaResponse));

        when(countryConverter.toEntity(koreaResponse))
            .thenReturn(korea);

        countryService.syncCountries();

        verify(countryRepository, times(1)).deleteAll();
        verify(countryRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("syncCountries - API 응답 null → 예외 발생")
    void syncCountries_nullFail() {

        when(nagerApiClient.fetchAvailableCountries())
            .thenReturn(null);

        assertThatThrownBy(() -> countryService.syncCountries())
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.HOLIDAY_API_CALL_FAILED);

        verify(countryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("syncCountries - API 응답 empty → 예외 발생")
    void syncCountries_emptyFail() {

        when(nagerApiClient.fetchAvailableCountries())
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> countryService.syncCountries())
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.HOLIDAY_API_CALL_FAILED);

        verify(countryRepository, never()).saveAll(any());
    }
}