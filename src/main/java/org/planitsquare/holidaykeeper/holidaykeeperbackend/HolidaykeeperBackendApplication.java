package org.planitsquare.holidaykeeper.holidaykeeperbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class HolidaykeeperBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(HolidaykeeperBackendApplication.class, args);
    }

}
