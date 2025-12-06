package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "공휴일 재동기화 결과")
public record HolidayRefreshResponse(
    @Schema(description = "이전에 저장되었던 레코드 수", example = "15")
    int oldCount,

    @Schema(description = "새롭게 재동기화된 후의 레코드 수", example = "15")
    int newCount,

    @Schema(description = "실제로 DB에서 삭제된 레코드 수", example = "1")
    int actualDeletedCount,

    @Schema(description = "실제로 DB에 추가된 레코드 수", example = "12")
    int actualAddedCount
) {

}