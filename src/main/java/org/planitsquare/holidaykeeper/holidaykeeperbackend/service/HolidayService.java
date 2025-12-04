package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;

public interface HolidayService {

    /**
     * 나라 코드에 맞는 공휴일 목록 조회
     *
     * @param countryCode
     */
    List<Holiday> getHolidayList(String countryCode, int year);

    /**
     * 최근 2년 내 모든 나라의 공휴일들 저장하기
     */
    HolidaySyncResponse syncHolidaysFor2Years();

    /**
     * 최근 5년 내 모든 나라의 공휴일들 저장하기
     */
    HolidaySyncResponse syncHolidaysFor6Years();

    /**
     * 해당 연도의 싱크 맞추기 (기존 db에 저장되어 있는 국가 공휴일 정보 제거하고 api로 다시 호출하여 저장)
     *
     * @param country
     * @param year
     */
    void syncHolidaysByYear(Country country, Integer year);

    /**
     * 공휴일 정보 조회
     *
     * @param request
     * @return
     */
    HolidaySearchResponse searchHolidays(HolidaySearchRequest request);
}
