package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;

public interface CountryService {

    /**
     * 나라 목록 조회
     */
    List<Country> getCountryList();
}
