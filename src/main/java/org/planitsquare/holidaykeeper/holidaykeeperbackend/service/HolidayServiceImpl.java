package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayDeleteRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayDeleteResponse;
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

    private HolidaySyncResponse syncHolidays(int startYear, int endYear) {

        LocalDateTime startTime = LocalDateTime.now();

        log.info("공휴일 데이터 적재 시작: {}년 ~ {}년", startYear, endYear);

        List<Country> countryList = countryService.getCountryList();

        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        for (Country country : countryList) {
            for (Integer year = startYear; year <= endYear; year++) {
                totalCount++;
                try {
                    syncHolidaysByYear(country, year);
                    successCount++;
                    log.debug("성공: {} - {}", country.getCode(), year);
                } catch (Exception e) {
                    failCount++;
                    log.warn("실패: {} - {} ({})", country.getCode(), year, e.getMessage());
                }
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = ChronoUnit.SECONDS.between(startTime, endTime);

        log.info("공휴일 데이터 적재 완료 - 총: {}, 성공: {}, 실패: {}, 소요: {}초",
            totalCount, successCount, failCount, durationSeconds);

        return HolidaySyncResponse.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .failCount(failCount)
            .countryCount(countryList.size())
            .yearRange(startYear + "-" + endYear)
            .startTime(startTime.toString())
            .endTime(endTime.toString())
            .durationSeconds(durationSeconds)
            .build();
    }

    @Override
    @Transactional
    public void syncHolidaysByYear(Country country, Integer year) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // api로 해당 국가의 해당 연도의 공휴일 정보 가져오기
        List<HolidayResponse> responses = nagerApiClient.fetchPublicHolidays(
            country.getCode(),
            year);

        if (responses == null || responses.isEmpty()) {
            throw new IllegalStateException("공휴일 목록 API 호출 실패: 응답이 비어있습니다");
        }

        // 변환
        List<Holiday> holidays = responses.stream()
            .map(response -> holidayConverter.toEntity(response, country))
            .toList();

        // 기존에 저장되어 있던 해당 국가의 해당 연도의 공휴일 모두 삭제
        holidayRepository.deleteByCountryAndDateBetween(country, startDate, endDate);

        // 저장
        holidayRepository.saveAll(holidays);
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
