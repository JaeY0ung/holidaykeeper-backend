package org.planitsquare.holidaykeeper.holidaykeeperbackend.controller;

import lombok.RequiredArgsConstructor;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    @PostMapping("/all")
    public ResponseEntity<Void> loadHolidayData() {

        return ResponseEntity.ok().build();
    }

}
