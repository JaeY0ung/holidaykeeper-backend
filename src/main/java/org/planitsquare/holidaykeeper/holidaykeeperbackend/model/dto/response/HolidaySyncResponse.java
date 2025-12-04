package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "공휴일 데이터 적재 결과")
@Builder
public record HolidaySyncResponse(

    @Schema(description = "처리 총 건수", example = "1200")
    Integer totalCount,

    @Schema(description = "성공 건수", example = "1180")
    Integer successCount,

    @Schema(description = "실패 건수", example = "20")
    Integer failCount,

    @Schema(description = "처리된 국가 수", example = "200")
    Integer countryCount,

    @Schema(description = "처리된 연도 범위", example = "2020-2025")
    String yearRange,

    @Schema(description = "처리 시작 시간", example = "2025-01-15T10:30:00")
    String startTime,

    @Schema(description = "처리 종료 시간", example = "2025-01-15T10:31:30")
    String endTime,

    @Schema(description = "소요 시간 (초)", example = "90")
    Long durationSeconds
) {

}