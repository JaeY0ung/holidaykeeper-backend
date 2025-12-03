package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {

}
