package org.planitsquare.holidaykeeper.holidaykeeperbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.service.HolidayService;
import org.springframework.http.ResponseEntity;
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
        @ApiResponse(
            responseCode = "200",
            description = "공휴일 데이터 적재 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HolidaySyncResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content
        )
    })
    @PostMapping("/sync/all")
    public ResponseEntity<HolidaySyncResponse> loadHolidayData() {

        HolidaySyncResponse response = holidayService.syncHolidaysForRecentYears();
        return ResponseEntity.ok(response);
    }

}
