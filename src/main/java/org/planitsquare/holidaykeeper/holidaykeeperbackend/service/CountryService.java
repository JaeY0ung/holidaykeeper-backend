package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;

public interface CountryService {

    /**
     * 국가 코드로 국가 조회
     *
     * @param countryCode
     * @return
     */
    Country getCountryByCode(String countryCode);

    /**
     * 나라 목록 조회
     */
    List<Country> getCountryList();
}
