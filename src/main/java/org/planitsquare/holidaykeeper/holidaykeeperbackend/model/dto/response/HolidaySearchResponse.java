package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Schema(description = "공휴일 검색 응답")
@Builder
public record HolidaySearchResponse(

    @ArraySchema(
        arraySchema = @Schema(description = "검색된 공휴일 목록"),
        schema = @Schema(implementation = HolidayItemDto.class)
    )
    List<HolidayItemDto> holidays
) {

    @Schema(description = "공휴일 검색 결과 상세 DTO")
    @Builder
    public record HolidayItemDto(

        @Schema(description = "공휴일 ID", example = "101")
        Long id,

        @Schema(description = "날짜", example = "2025-12-25")
        LocalDate date,

        @Schema(description = "공휴일 지역별 이름", example = "크리스마스")
        String localName,

        @Schema(description = "공휴일 이름(영문)", example = "Christmas Day")
        String name,

        @Schema(description = "국가 ID", example = "1")
        Long countryId,

        @Schema(description = "국가명", example = "South Korea")
        String countryName,

        @ArraySchema(
            schema = @Schema(description = "공휴일 타입", example = "School"),
            arraySchema = @Schema(description = "공휴일 타입 리스트")
        )
        List<String> types
    ) {

    }
}
