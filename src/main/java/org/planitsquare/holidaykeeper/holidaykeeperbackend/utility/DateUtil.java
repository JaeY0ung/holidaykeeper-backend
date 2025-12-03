package org.planitsquare.holidaykeeper.holidaykeeperbackend.utility;

import java.time.LocalDate;

public class DateUtil {

    public static Integer getTodayYear() {

        return LocalDate.now().getYear();
    }

    public static Integer getYearBefore(int year) {

        return LocalDate.now().minusYears(year).getYear();
    }
}
