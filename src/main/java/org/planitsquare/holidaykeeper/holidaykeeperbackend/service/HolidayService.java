package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;

public interface HolidayService {

    /**
     * 나라 코드에 맞는 공휴일 목록 조회
     *
     * @param countryCode
     */
    List<Holiday> getHolidayList(String countryCode, int year);
}
