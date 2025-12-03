package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response;

public record HolidayResponse(
    String date,           // "2025-01-01"
    String localName,      // "Any nou"
    String name,           // "New Year's Day"
    String countryCode,    // "AD"
    Boolean fixed,
    Boolean global,
    String[] counties,     // null 또는 ["US-CA", "US-CT"]
    Integer launchYear,
    String[] types         // ["Public", "Bank"]
) {

}