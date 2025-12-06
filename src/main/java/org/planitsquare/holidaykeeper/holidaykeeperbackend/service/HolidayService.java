package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayDeleteRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayRefreshRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayDeleteResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayRefreshResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;

public interface HolidayService {

    /**
     * 최근 2년 내 모든 국가의 공휴일들 저장하기
     */
    HolidaySyncResponse syncHolidaysFor2Years();

    /**
     * 국가의 공휴일들 재동기화하기
     *
     * @param request
     * @return
     */
    HolidayRefreshResponse refreshHolidays(HolidayRefreshRequest request);

    /**
     * 최근 5년 내 모든 국가의 공휴일들 저장하기
     */
    HolidaySyncResponse syncHolidaysFor6Years();

    /**
     * 공휴일 정보 조회
     *
     * @param request
     * @return
     */
    HolidaySearchResponse searchHolidays(HolidaySearchRequest request);

    /**
     * 공휴일 삭제
     *
     * @param request
     * @return
     */
    HolidayDeleteResponse deleteHolidays(HolidayDeleteRequest request);
}
