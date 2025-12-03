package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NagerApiClient 통합 테스트")
class NagerApiClientIntegrationTest {

    @Autowired
    private NagerApiClient nagerApiClient;

    @Test
    @DisplayName("실제 API 호출 - 전체 국가 조회")
    void fetchAvailableCountries_realApiCall() {
        // when - 실제 API 호출
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then - 실제 데이터 검증
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThan(100);  // 실제로는 200+ 개국

        // 알려진 국가 코드 확인
        assertThat(result).extracting(CountryResponse::countryCode)
            .contains("KR", "US", "JP", "CN", "DE");
    }

    @Test
    @DisplayName("실제 API 응답 구조 검증")
    void fetchAvailableCountries_validateStructure() {
        // when
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then - 모든 국가 데이터가 올바른 형식인지 확인
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(country ->
            country.countryCode() != null &&
                country.countryCode().length() == 2 &&
                country.name() != null &&
                !country.name().isBlank()
        );
    }

    @Test
    @DisplayName("한국 데이터가 포함되어 있는지 확인")
    void fetchAvailableCountries_containsKorea() {
        // when
        List<CountryResponse> result = nagerApiClient.fetchAvailableCountries();

        // then
        assertThat(result)
            .extracting(CountryResponse::countryCode)
            .contains("KR");

        CountryResponse korea = result.stream()
            .filter(c -> "KR".equals(c.countryCode()))
            .findFirst()
            .orElseThrow();

        assertThat(korea.name()).containsIgnoringCase("Korea");
    }
}