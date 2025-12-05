package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "공휴일 삭제 응답")
@Builder
public record HolidayDeleteResponse(

    @Schema(description = "삭제된 공휴일 레코드 수", example = "15")
    int deletedCount
) {

}
