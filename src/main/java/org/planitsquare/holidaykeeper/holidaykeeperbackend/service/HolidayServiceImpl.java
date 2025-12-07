package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClientReactive;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
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
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.enums.HolidayType;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayQueryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.utility.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayQueryRepository holidayQueryRepository;

    private final NagerApiClient nagerApiClient;
    private final NagerApiClientReactive nagerApiClientReactive;

    private final CountryService countryService;

    private final HolidayConverter holidayConverter;

    @Builder
    private record SyncResult(
        int oldCount,            // 이전에 저장되었던 레코드 수
        int newCount,            // 새롭게 재동기화된 후의 레코드 수
        int actualDeletedCount,  // 실제로 DB에서 삭제된 레코드 수
        int actualAddedCount    // 실제로 DB에 저장된 레코드 수
    ) {

    }

    /**
     * 공휴일 내용 비교를 위한 record
     */
    @Builder
    private record HolidayContent(
        LocalDate date,
        String localName,
        String name,
        Boolean fixed,
        String counties,
        Integer launchYear,
        String types
    ) {

    }

    /**
     * (국가, 연도, 응답 리스트)를 한 번에 담는 record
     */
    @Builder
    private record CountryYearHolidays(
        Country country,
        int year,
        List<HolidayResponse> responses
    ) {

    }

    @Override
    @Transactional
    public HolidaySyncResponse syncHolidaysFor2Years() {

        int startYear = DateUtil.getTodayYear() - 1;
        int endYear = DateUtil.getTodayYear();

        return syncHolidays(startYear, endYear);
    }

    @Override
    @Transactional
    public HolidaySyncResponse syncHolidaysFor6Years() {

        int startYear = DateUtil.getTodayYear() - 5;
        int endYear = DateUtil.getTodayYear();

        return syncHolidays(startYear, endYear);
    }

    @Override
    @Transactional
    public HolidayRefreshResponse refreshHolidays(HolidayRefreshRequest request) {

        Country country = countryService.getCountryByCode(request.countryCode());

        SyncResult syncResult = syncHolidaysByYear(country, request.year());

        return HolidayRefreshResponse.builder()
            .oldCount(syncResult.oldCount())
            .newCount(syncResult.newCount())
            .actualDeletedCount(syncResult.actualDeletedCount())
            .actualAddedCount(syncResult.actualAddedCount())
            .build();
    }

    @Override
    public HolidaySearchResponse searchHolidays(HolidaySearchRequest request) {

        Country country = countryService.getCountryByCode(request.countryCode());

        List<String> typeValues = (request.types() == null || request.types().isEmpty())
            ? null
            : request.types().stream()
                .map(HolidayType::getValue)
                .toList();

        // 1. 페이징 적용하여 데이터 조회
        List<Holiday> holidays = holidayQueryRepository.searchWithPaging(
            country,
            request.startDate(),
            request.endDate(),
            typeValues,
            request.page(),
            request.size()
        );

        // 2. 전체 개수 조회 (페이지 정보를 위해)
        long totalElements = holidayQueryRepository.count(
            country,
            request.startDate(),
            request.endDate(),
            typeValues
        );

        // 3. 변환 및 반환
        return holidayConverter.toSearchResponseWithPaging(
            holidays,
            request.page(),
            request.size(),
            totalElements
        );
    }

    @Override
    @Transactional
    public HolidayDeleteResponse deleteHolidays(HolidayDeleteRequest request) {

        // 삭제할 국가
        Country country = countryService.getCountryByCode(request.countryCode());

        // 삭제할 기간
        LocalDate startDate = LocalDate.of(request.year(), 1, 1);
        LocalDate endDate = LocalDate.of(request.year(), 12, 31);

        // 삭제
        int deletedCount = holidayRepository.deleteByCountryAndDateBetween(country, startDate,
            endDate);

        return HolidayDeleteResponse.builder()
            .deletedCount(deletedCount)
            .build();
    }

    private HolidaySyncResponse syncHolidays(int startYear, int endYear) {

        LocalDateTime startTime = LocalDateTime.now();

        log.info("공휴일 데이터 적재 시작: {}년 ~ {}년", startYear, endYear);

        List<Country> countryList = countryService.getCountryList();

        // 1. 외부 API 병렬 호출 → 모든 응답을 한 번에 가져옴
        List<CountryYearHolidays> allResponses = fetchAllHolidaysReactive(
            countryList, startYear, endYear
        ).block(); // <- 여기까지만 Reactor 사용

        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        // 2. 이후는 전부 동기 + JPA + @Transactional 안에서 수행
        for (CountryYearHolidays cyh : allResponses) {
            totalCount++;

            Country country = cyh.country();
            int year = cyh.year();
            List<HolidayResponse> responses = cyh.responses();

            try {
                if (responses == null || responses.isEmpty()) {
                    log.warn("공휴일 응답 없음: {} - {}", country.getCode(), year);
                    failCount++;
                    continue;
                }

                SyncResult result = syncHolidaysByYearSync(country, year, responses);
                successCount++;

                log.debug("동기화 성공: {} - {} (old: {}, new: {}, +{}, -{})",
                    country.getCode(),
                    year,
                    result.oldCount(),
                    result.newCount(),
                    result.actualAddedCount(),
                    result.actualDeletedCount()
                );

            } catch (Exception e) {
                failCount++;
                log.warn("동기화 실패: {} - {} ({})",
                    country.getCode(), year, e.getMessage(), e);
            }
        }

        LocalDateTime end = LocalDateTime.now();
        long duration = ChronoUnit.SECONDS.between(startTime, end);

        return HolidaySyncResponse.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .failCount(failCount)
            .countryCount(countryList.size())
            .yearRange(startYear + "-" + endYear)
            .startTime(startTime.toString())
            .endTime(end.toString())
            .durationSeconds(duration)
            .build();
    }

    private Mono<List<CountryYearHolidays>> fetchAllHolidaysReactive(
        List<Country> countryList,
        int startYear,
        int endYear
    ) {

        return Flux.fromIterable(countryList)
            .flatMap(country ->
                Flux.range(startYear, endYear - startYear + 1)
                    .flatMap(year ->
                        nagerApiClientReactive.fetchPublicHolidays(country.getCode(), year)
                            .onErrorResume(ex -> {
                                log.warn("공휴일 API 실패: {} - {} ({})",
                                    country.getCode(), year, ex.getMessage());
                                // 실패한 경우 빈 리스트로 대체
                                return Mono.just(List.of());
                            })
                            .map(responses ->
                                CountryYearHolidays.builder()
                                    .country(country)
                                    .year(year)
                                    .responses(responses)
                                    .build()
                            )
                    )
            )
            .collectList();
    }

    private SyncResult syncHolidaysByYearSync(
        Country country,
        int year,
        List<HolidayResponse> responses
    ) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 기존 데이터 조회
        List<Holiday> oldHolidays = holidayRepository.findByCountryAndDateBetween(
            country, startDate, endDate);

        // Response → Entity 변환
        List<Holiday> newHolidays = responses.stream()
            .map(r -> holidayConverter.toEntity(r, country))
            .toList();

        // Set 비교
        Set<HolidayContent> oldSet = oldHolidays.stream()
            .map(this::toHolidayContent)
            .collect(Collectors.toSet());

        Set<HolidayContent> newSet = newHolidays.stream()
            .map(this::toHolidayContent)
            .collect(Collectors.toSet());

        Set<HolidayContent> actualDeleted = new HashSet<>(oldSet);
        actualDeleted.removeAll(newSet);

        Set<HolidayContent> actualAdded = new HashSet<>(newSet);
        actualAdded.removeAll(oldSet);

        // 삭제
        int deletedCount = holidayRepository.deleteByCountryAndDateBetween(
            country, startDate, endDate
        );

        // 저장
        int saved = holidayRepository.saveAll(newHolidays).size();

        return SyncResult.builder()
            .oldCount(deletedCount)
            .newCount(saved)
            .actualDeletedCount(actualDeleted.size())
            .actualAddedCount(actualAdded.size())
            .build();
    }

    private SyncResult syncHolidaysByYear(Country country, Integer year) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 기존 데이터 조회
        List<Holiday> oldHolidays = holidayRepository.findByCountryAndDateBetween(
            country, startDate, endDate);

        // API로 새 데이터 가져오기
        List<HolidayResponse> responses = nagerApiClient.fetchPublicHolidays(
            country.getCode(), year);

        if (responses == null || responses.isEmpty()) {
            throw new BusinessException(ErrorCode.HOLIDAY_API_CALL_FAILED);
        }

        // 변환
        List<Holiday> newHolidays = responses.stream()
            .map(response -> holidayConverter.toEntity(response, country))
            .toList();

        // Set으로 변환하여 비교
        Set<HolidayContent> oldSet = oldHolidays.stream()
            .map(this::toHolidayContent)
            .collect(Collectors.toSet());

        Set<HolidayContent> newSet = newHolidays.stream()
            .map(this::toHolidayContent)
            .collect(Collectors.toSet());

        // 실제 삭제된 것 (old에는 있는데 new에는 없음)
        Set<HolidayContent> actualDeleted = new HashSet<>(oldSet);
        actualDeleted.removeAll(newSet);

        // 실제 추가된 것 (new에는 있는데 old에는 없음)
        Set<HolidayContent> actualAdded = new HashSet<>(newSet);
        actualAdded.removeAll(oldSet);

        // 기존 데이터 삭제
        int deletedCount = holidayRepository.deleteByCountryAndDateBetween(
            country,
            startDate,
            endDate);

        // 저장
        List<Holiday> saved = holidayRepository.saveAll(newHolidays);

        return SyncResult.builder()
            .oldCount(deletedCount)
            .newCount(saved.size())
            .actualAddedCount(actualAdded.size())
            .actualDeletedCount(actualDeleted.size())
            .build();
    }

    /**
     * Holiday를 비교 가능한 객체로 변환
     */
    private HolidayContent toHolidayContent(Holiday holiday) {

        return HolidayContent.builder()
            .date(holiday.getDate())
            .localName(holiday.getLocalName())
            .name(holiday.getName())
            .fixed(holiday.getFixed())
            .counties(holiday.getCounties())
            .launchYear(holiday.getLaunchYear())
            .types(holiday.getTypes())
            .build();

    }
}
