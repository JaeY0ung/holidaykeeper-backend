package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayQueryRepository holidayQueryRepository;

    private final NagerApiClient nagerApiClient;

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


    @Override
    @Transactional
    public HolidaySyncResponse syncHolidaysFor2Years() {

        int startYear = DateUtil.getTodayYear() - 1;
        int endYear = DateUtil.getTodayYear();

        return syncHolidays(startYear, endYear);
    }

    @Override
    public HolidayRefreshResponse refreshHolidays(HolidayRefreshRequest request) {

        Country country = countryService.getCountryByCode(request.countryCode());

        SyncResult syncResult = syncHolidaysByYear(country, request.year());

        return HolidayRefreshResponse.builder()
            .oldCount(syncResult.oldCount())
            .newCount(syncResult.newCount)
            .actualDeletedCount(syncResult.actualDeletedCount())
            .actualAddedCount(syncResult.actualAddedCount())
            .build();
    }

    @Override
    @Transactional
    public HolidaySyncResponse syncHolidaysFor6Years() {

        int startYear = DateUtil.getTodayYear() - 5;
        int endYear = DateUtil.getTodayYear();

        return syncHolidays(startYear, endYear);
    }

    private HolidaySyncResponse syncHolidays(int startYear, int endYear) {

        LocalDateTime startTime = LocalDateTime.now();

        log.info("공휴일 데이터 적재 시작: {}년 ~ {}년", startYear, endYear);

        List<Country> countryList = countryService.getCountryList();

        List<CompletableFuture<SyncResult>> futures = new java.util.ArrayList<>();

        for (Country country : countryList) {
            for (int year = startYear; year <= endYear; year++) {

                int finalYear = year;

                CompletableFuture<SyncResult> future =
                    syncHolidaysByYearAsync(country, year)
                        .exceptionally(ex -> {
                            log.warn("공휴일 동기화 실패: {} - {} ({})",
                                country.getCode(), finalYear, ex.getMessage());
                            return null;
                        });

                futures.add(future);
            }
        }

        // 모든 비동기 작업 완료 대기
        List<SyncResult> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        int successCount = (int) results.stream().filter(r -> r != null).count();
        int failCount = results.size() - successCount;

        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = ChronoUnit.SECONDS.between(startTime, endTime);

        return HolidaySyncResponse.builder()
            .totalCount(results.size())
            .successCount(successCount)
            .failCount(failCount)
            .countryCount(countryList.size())
            .yearRange(startYear + "-" + endYear)
            .startTime(startTime.toString())
            .endTime(endTime.toString())
            .durationSeconds(durationSeconds)
            .build();
    }
    
    @Transactional
    @Async("holidayExecutor")
    public CompletableFuture<SyncResult> syncHolidaysByYearAsync(Country country, Integer year) {

        SyncResult result = syncHolidaysByYear(country, year);

        return CompletableFuture.completedFuture(result);
    }

    private SyncResult syncHolidaysByYear(Country country, Integer year) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 기존 데이터 조회
        List<Holiday> oldHolidays = holidayRepository.findByCountryAndDateBetween(
            country, startDate, endDate);

        // API로 새 데이터 가져오기
        List<HolidayResponse> responses = nagerApiClient.fetchPublicHolidays(
            country.getCode(),
            year);

        if (responses == null || responses.isEmpty()) {
            throw new IllegalStateException("공휴일 목록 API 호출 실패: 응답이 비어있습니다");
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

        return new HolidayContent(
            holiday.getDate(),
            holiday.getLocalName(),
            holiday.getName(),
            holiday.getFixed(),
            holiday.getCounties(),
            holiday.getLaunchYear(),
            holiday.getTypes()
        );
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
}
