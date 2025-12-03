package org.planitsquare.holidaykeeper.holidaykeeperbackend.converter;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.springframework.stereotype.Component;

@Component
public class CountryConverter {

    public Country toEntity(CountryResponse response) {

        return Country.builder()
            .code(response.countryCode())
            .name(response.name())
            .build();
    }
}