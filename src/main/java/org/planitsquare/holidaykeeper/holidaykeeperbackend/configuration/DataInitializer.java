package org.planitsquare.holidaykeeper.holidaykeeperbackend.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.CountryService;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CountryRepository countryRepository;
    private final HolidayRepository holidayRepository;
    private final CountryService countryService;
    private final HolidayService holidayService;

    @Override
    public void run(String... args) {

        log.info("=== 데이터 초기화 시작 ===");

        initializeCountries();
        initializeHolidays();

        log.info("=== 데이터 초기화 완료 ===");
    }

    /**
     * 국가 데이터 초기화
     */
    private void initializeCountries() {

        if (countryRepository.count() > 0) {
            log.info("국가 데이터가 이미 존재합니다. ({}개)", countryRepository.count());
            return;
        }

        log.info("국가 데이터가 없습니다. 외부 API 동기화를 시작합니다.");

        try {
            countryService.syncCountries();
            log.info("국가 데이터 동기화 완료: 총 {}개 국가 저장됨", countryRepository.count());

        } catch (BusinessException e) {
            // ErrorCode 기반 예외 처리
            log.error("[국가 동기화 실패] {} - {}", e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 오류
            log.error("[국가 동기화 중 시스템 오류] {}", e.getMessage(), e);
            throw new RuntimeException("국가 데이터 초기화 실패", e); // 앱 실행 중단 여부 선택 가능
        }
    }

    /**
     * 공휴일 데이터 초기화
     */
    private void initializeHolidays() {

        if (holidayRepository.count() > 0) {
            log.info("공휴일 데이터가 이미 존재합니다. ({}개)", holidayRepository.count());
            return;
        }

        log.info("공휴일 데이터가 없습니다. 6년치 API 동기화를 시작합니다.");

        try {
            holidayService.syncHolidaysFor6Years();
            log.info("공휴일 데이터 동기화 완료: 총 {}개 공휴일 저장됨", holidayRepository.count());

        } catch (BusinessException e) {
            log.error("[공휴일 동기화 실패] {} - {}", e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("[공휴일 동기화 중 시스템 오류] {}", e.getMessage(), e);
            throw new RuntimeException("공휴일 데이터 초기화 실패", e);
        }
    }
}