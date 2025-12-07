package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.ErrorCode;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("NagerApiClient 단위 테스트")
class NagerApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NagerApiClient nagerApiClient;
    
    @Test
    @DisplayName("국가 목록 조회 성공 - 정상 응답 처리")
    void fetchAvailableCountries_success() {
        // given
        CountryResponse[] mockResponse = {
            new CountryResponse("KR", "South Korea"),
            new CountryResponse("US", "United States")
        };

        when(restTemplate.getForObject(anyString(), eq(CountryResponse[].class)))
            .thenReturn(mockResponse);

        // when
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).countryCode()).isEqualTo("KR");
        assertThat(result.get(1).countryCode()).isEqualTo("US");
    }

    @Test
    @DisplayName("API 호출 실패 시 RuntimeException 발생")
    void fetchAvailableCountries_failure() {
        // given
        when(restTemplate.getForObject(anyString(), eq(CountryResponse[].class)))
            .thenThrow(new RuntimeException("Connection timeout"));

        // when & then
        assertThatThrownBy(() -> nagerApiClient.fetchAvailableCountries())
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.COUNTRY_API_CALL_FAILED.getMessage());
    }

    @Test
    @DisplayName("빈 배열 반환 시 빈 리스트 반환")
    void whenRestTemplateReturnsEmptyArray_thenReturnEmptyList() {
        // given
        when(restTemplate.getForObject(anyString(), eq(CountryResponse[].class)))
            .thenReturn(new CountryResponse[0]);

        // when
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 반환 시 빈 리스트 반환 (NPE 방지)")
    void whenRestTemplateReturnsNull_thenReturnEmptyList() {
        // given
        when(restTemplate.getForObject(anyString(), eq(CountryResponse[].class)))
            .thenReturn(null);

        // when
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
    }
}