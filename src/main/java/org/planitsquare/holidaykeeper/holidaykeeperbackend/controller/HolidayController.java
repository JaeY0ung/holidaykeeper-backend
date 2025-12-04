package org.planitsquare.holidaykeeper.holidaykeeperbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Holiday", description = "공휴일 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(
        summary = "전체 공휴일 데이터 초기 적재",
        description = "최근 6년(2020-2025)의 전체 국가 공휴일 데이터를 동기화하고 처리 결과를 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "공휴일 데이터 적재 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/sync/all")
    public ResponseEntity<HolidaySyncResponse> loadHolidayData() {

        HolidaySyncResponse response = holidayService.syncHolidaysForRecentYears();
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "공휴일 검색",
        description = "날짜, 국가 코드, 타입으로 공휴일을 검색합니다"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 파라미터")
    })
    @GetMapping
    public ResponseEntity<HolidaySearchResponse> searchHolidayData(
        @Valid @ModelAttribute HolidaySearchRequest request
    ) {

        HolidaySearchResponse response = holidayService.searchHolidays(request);
        return ResponseEntity.ok(response);
    }

}
