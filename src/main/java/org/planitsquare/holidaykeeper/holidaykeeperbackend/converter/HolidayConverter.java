package org.planitsquare.holidaykeeper.holidaykeeperbackend.converter;

import java.time.LocalDate;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.HolidayResponse;
import org.springframework.stereotype.Component;

@Component
public class HolidayConverter {

    public Holiday toEntity(HolidayResponse response, Country country) {

        return Holiday.builder()
            .date(LocalDate.parse(response.date()))
            .localName(response.localName())
            .name(response.name())
            .country(country)
            .fixed(response.fixed())
            .global(response.global())
            .counties(convertArrayToString(response.counties()))
            .launchYear(response.launchYear())
            .types(convertArrayToString(response.types()))
            .build();
    }

    private String convertArrayToString(String[] array) {

        if (array == null || array.length == 0) {
            return null;
        }
        return String.join(",", array);
    }
}