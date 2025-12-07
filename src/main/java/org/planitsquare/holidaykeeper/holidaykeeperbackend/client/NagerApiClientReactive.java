package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NagerApiClientReactive {

    private final WebClient nagerWebClient;

    public Mono<List<CountryResponse>> fetchAvailableCountries() {

        return nagerWebClient.get()
            .uri("/AvailableCountries")
            .retrieve()
            .bodyToFlux(CountryResponse.class)
            .collectList()
            .doOnNext(list ->
                log.info("국가 목록 조회 성공: {} 개국", list.size())
            )
            .doOnError(ex ->
                log.error("국가 목록 API 호출 실패", ex)
            );
    }

    public Mono<List<HolidayResponse>> fetchPublicHolidays(String countryCode, int year) {

        return nagerWebClient.get()
            .uri("/PublicHolidays/{year}/{code}", year, countryCode)
            .retrieve()
            .bodyToFlux(HolidayResponse.class)
            .collectList()
            .doOnNext(list ->
                log.debug("공휴일 조회 성공: {} - {} ({} 건)", countryCode, year, list.size())
            )
            .doOnError(ex ->
                log.error("공휴일 조회 실패: {} - {}", countryCode, year, ex)
            );
    }
}