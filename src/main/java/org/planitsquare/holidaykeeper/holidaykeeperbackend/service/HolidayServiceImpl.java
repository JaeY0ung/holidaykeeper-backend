package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.HolidayConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
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

    /**
     * 최근 5년 내 모든 나라의 공휴일들 저장하기
     */
    private void syncHolidaysForRecentYears() {

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

        log.info("공휴일 데이터 적재 완료 - 총: {}, 성공: {}, 실패: {}",
            totalCount, successCount, failCount);
    }

    /**
     * 해당 연도의 싱크 맞추기 (기존 db에 저장되어 있는 국가 공휴일 정보 제거하고 api로 다시 호출하여 저장)
     *
     * @param country
     * @param year
     */
    private void syncHolidaysByYear(Country country, Integer year) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        // api로 해당 국가의 5년간 공휴일 정보 가져오기
        List<HolidayResponse> responses = nagerApiClient.fetchPublicHolidays(country.getCode(),
            year);
        // 변환
        List<Holiday> holidays = responses.stream()
            .map(response -> holidayConverter.toEntity(response, country))
            .toList();
        // 기존에 저장되어 있던 해당 국가의 5년간 공휴일 모두 삭제
        holidayRepository.deleteByCountryAndDateBetween(country, startDate, endDate);
        // 저장
        holidayRepository.saveAll(holidays);
    }

}
