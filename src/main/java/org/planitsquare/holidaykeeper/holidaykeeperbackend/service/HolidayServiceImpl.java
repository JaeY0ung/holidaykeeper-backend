package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
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
    public List<Holiday> getHolidayList(String countryCode, int year) {

        Country country = countryService.getCountryByCode(countryCode);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Holiday> holidayList = holidayRepository.findByCountryAndDateBetween(
            country,
            startDate,
            endDate);

        if (!holidayList.isEmpty()) {
            log.info("공휴일 목록 조회 완료: {} 개", holidayList.size());
            return holidayList;
        }

        syncHolidaysForRecentYears();

        return holidayRepository.findByCountryAndDateBetween(country, startDate, endDate);
    }

    @Override
    @Transactional
    public HolidaySyncResponse syncHolidaysForRecentYears() {

        LocalDateTime startTime = LocalDateTime.now();

        Integer startYear = DateUtil.getYearBefore(5);
        Integer endYear = DateUtil.getTodayYear();

        log.info("공휴일 데이터 적재 시작: {} ~ {}", startYear, endYear);

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

        // db 에서 공휴일 정보 조회
        List<Holiday> holidays;
        if (request.types() == null || request.types().isEmpty()) {
            // JPA 쿼리 메서드 기반 조회
            holidays = holidayRepository.findByCountryAndDateBetween(
                country,
                request.startDate(),
                request.endDate());

        } else {
            List<String> typeValues = request.types()
                .stream()
                .map(HolidayType::getValue) // → "School", "Authorities" 등
                .toList();
            // QueryDSL 동적 쿼리 기반 조회
            holidays = holidayQueryRepository.search(
                country,
                request.startDate(),
                request.endDate(),
                typeValues
            );
        }
        // 변환
        return holidayConverter.toSearchResponse(holidays);
    }
}
