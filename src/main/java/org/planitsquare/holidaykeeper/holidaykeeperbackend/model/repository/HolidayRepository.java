package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository;

import java.time.LocalDate;
import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByCountryAndDateBetween(
        Country country,
        LocalDate startDate,
        LocalDate endDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Holiday h " +
        "WHERE h.country = :country " +
        "AND h.date BETWEEN :startDate AND :endDate")
    int deleteByCountryAndDateBetween(
        @Param("country") Country country,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
