package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
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
}