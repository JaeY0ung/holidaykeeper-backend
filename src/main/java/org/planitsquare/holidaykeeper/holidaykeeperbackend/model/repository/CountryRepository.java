package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository;

import java.util.Optional;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByCode(String code);
}
