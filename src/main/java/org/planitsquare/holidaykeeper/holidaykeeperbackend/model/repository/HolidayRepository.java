package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

}
