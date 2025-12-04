package org.planitsquare.holidaykeeper.holidaykeeperbackend.utility;

import java.time.LocalDate;

public class DateUtil {

    public static Integer getTodayYear() {

        return LocalDate.now().getYear();
    }
}
