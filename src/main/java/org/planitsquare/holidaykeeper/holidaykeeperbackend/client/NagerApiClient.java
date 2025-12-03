package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class NagerApiClient {

    private final RestTemplate restTemplate;

    @Value("${nager.api.base-url}")
    private String baseUrl;

    public NagerApiClient(@Qualifier("nagerRestTemplate") RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    /**
     * 국가 목록 조회
     */
    public List<CountryResponse> fetchAvailableCountries() {

        String url = baseUrl + "/AvailableCountries";

        try {
            log.debug("국가 목록 API 호출: {}", url);
            CountryResponse[] response = restTemplate.getForObject(url, CountryResponse[].class);

            if (response == null || response.length == 0) {
                log.warn("국가 목록 응답이 비어있습니다.");
                return List.of();
            }

            log.info("국가 목록 조회 성공: {} 개국", response.length);
            return Arrays.asList(response);

        } catch (Exception e) {
            log.error("국가 목록 API 호출 실패", e);
            throw new RuntimeException("국가 목록 조회 실패", e);
        }
    }

    /**
     * 특정 국가의 특정 연도 공휴일 조회
     */
    public List<HolidayResponse> fetchPublicHolidays(String countryCode, int year) {

        String url = String.format("%s/PublicHolidays/%d/%s", baseUrl, year, countryCode);

        try {
            log.debug("공휴일 API 호출: {} - {}", countryCode, year);
            HolidayResponse[] response = restTemplate.getForObject(url, HolidayResponse[].class);

            if (response == null || response.length == 0) {
                log.debug("공휴일 데이터 없음: {} - {}", countryCode, year);
                return List.of();
            }

            log.debug("공휴일 조회 성공: {} - {} ({} 건)", countryCode, year, response.length);
            return Arrays.asList(response);

        } catch (Exception e) {
            log.error("공휴일 API 호출 실패: {} - {}", countryCode, year, e);
            throw new RuntimeException(
                String.format("공휴일 조회 실패: %s - %d", countryCode, year), e);
        }
    }
}