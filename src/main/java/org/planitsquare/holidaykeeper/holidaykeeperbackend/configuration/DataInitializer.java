package org.planitsquare.holidaykeeper.holidaykeeperbackend.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.HolidayRepository;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.CountryService;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 데이터 초기화 Country 데이터와 Holiday 데이터가 없을 경우 외부 API에서 데이터를 가져옵니다.
 */
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

        // 1. Country 데이터 체크 및 동기화
        if (countryRepository.count() == 0) {
            log.info("국가 데이터가 없습니다. 외부 API에서 데이터를 가져옵니다.");
            try {
                countryService.syncCountries();
                log.info("국가 데이터 동기화가 완료되었습니다. 총 {}개의 국가가 저장되었습니다.",
                    countryRepository.count());
            } catch (Exception e) {
                log.error("국가 데이터 동기화 중 오류가 발생했습니다: {}", e.getMessage(), e);
            }
        } else {
            log.info("국가 데이터가 이미 존재합니다. ({}개)", countryRepository.count());
        }

        // 2. Holiday 데이터 체크 및 동기화
        if (holidayRepository.count() == 0) {
            log.info("공휴일 데이터가 없습니다. 외부 API에서 데이터를 가져옵니다.");
            try {
                holidayService.syncHolidaysFor6Years();
                log.info("공휴일 데이터 동기화가 완료되었습니다. 총 {}개의 공휴일이 저장되었습니다.",
                    holidayRepository.count());
            } catch (Exception e) {
                log.error("공휴일 데이터 동기화 중 오류가 발생했습니다: {}", e.getMessage(), e);
            }
        } else {
            log.info("공휴일 데이터가 이미 존재합니다. ({}개)", holidayRepository.count());
        }

        log.info("=== 데이터 초기화 완료 ===");
    }
}
