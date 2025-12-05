package org.planitsquare.holidaykeeper.holidaykeeperbackend.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.utility.DateUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

    private final HolidayService holidayService;

    /**
     * 매년 1월 2일 01:00 KST에 전년도·금년도 공휴일 데이터 자동 동기화 cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 1 2 1 *", zone = "Asia/Seoul")
    public void syncHolidaysAutomatically() {

        log.info("========================================");
        log.info("공휴일 자동 동기화 시작: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        log.info("========================================");

        int startYear = DateUtil.getTodayYear() - 1;
        int endYear = DateUtil.getTodayYear();

        try {
            HolidaySyncResponse response = holidayService.syncHolidaysFor2Years();

            log.info("========================================");
            log.info("공휴일 자동 동기화 완료");
            log.info("처리 총 건수: {}", response.totalCount());
            log.info("성공 건수: {}", response.successCount());
            log.info("실패 건수: {}", response.failCount());
            log.info("처리된 국가 수: {}", response.countryCount());
            log.info("처리된 연도 범위: {}", response.yearRange());
            log.info("소요 시간: {}초", response.durationSeconds());
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("공휴일 자동 동기화 실패: {}", e.getMessage(), e);
            log.error("========================================");
        }
    }
}
