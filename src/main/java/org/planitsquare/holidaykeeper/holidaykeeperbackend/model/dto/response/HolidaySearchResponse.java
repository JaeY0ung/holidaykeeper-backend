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
    List<HolidayItemDto> holidays,

    @Schema(description = "페이지 정보")
    PageInfo pageInfo
) {

    @Schema(description = "페이지 메타데이터")
    @Builder
    public record PageInfo(

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int currentPage,

        @Schema(description = "한 페이지당 항목 수", example = "20")
        int pageSize,

        @Schema(description = "현재 페이지의 항목 수", example = "20")
        int numberOfElements,

        @Schema(description = "전체 항목 수", example = "150")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "8")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean isFirst,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean isLast,

        @Schema(description = "비어있는지 여부", example = "false")
        boolean isEmpty
    ) {

    }

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
