package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "공휴일 데이터 적재 결과")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidaySyncResponse {

    @Schema(description = "처리 총 건수", example = "1200")
    private Integer totalCount;

    @Schema(description = "성공 건수", example = "1180")
    private Integer successCount;

    @Schema(description = "실패 건수", example = "20")
    private Integer failCount;

    @Schema(description = "처리된 국가 수", example = "200")
    private Integer countryCount;

    @Schema(description = "처리된 연도 범위", example = "2020-2025")
    private String yearRange;

    @Schema(description = "처리 시작 시간", example = "2025-01-15T10:30:00")
    private String startTime;

    @Schema(description = "처리 종료 시간", example = "2025-01-15T10:31:30")
    private String endTime;

    @Schema(description = "소요 시간 (초)", example = "90")
    private Long durationSeconds;
}