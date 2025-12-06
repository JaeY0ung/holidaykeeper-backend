package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayDeleteRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidayRefreshRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request.HolidaySearchRequest;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayDeleteResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidayRefreshResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySearchResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.response.HolidaySyncResponse;

/**
 * 공휴일(Holiday) 데이터의 적재·재동기화·조회·삭제를 담당하는 서비스 인터페이스입니다. 외부 API 또는 공휴일 제공처에서 데이터를 가져와 저장하거나, 특정 국가·연도
 * 기준으로 재동기화 및 삭제 작업을 수행합니다.
 * <p>
 * 주요 기능: - 공휴일 데이터 대량 동기화(최근 연도 범위) - 특정 국가/연도 단위 재동기화 - 검색 조건 기반 조회 - 국가·연도 단위 삭제
 * <p>
 * 구현체(ServiceImpl)는 데이터 적재 로직, 외부 API 호출, DB 업데이트 로직 등을 포함합니다.
 */
public interface HolidayService {

    /**
     * 최근 2년 내 모든 국가의 공휴일 데이터를 외부 API로부터 조회해 DB에 저장합니다. 이미 존재하는 데이터는 갱신하거나 중복을 제거하며, 동기화 작업의 통계 정보를
     * {@link HolidaySyncResponse} 로 반환합니다.
     *
     * @return HolidaySyncResponse 공휴일 적재 결과(총 처리 건수, 성공·실패 건수, 소요 시간 등)
     */
    HolidaySyncResponse syncHolidaysFor2Years();

    /**
     * 특정 국가의 특정 연도 공휴일을 재동기화합니다. 기존 기록을 조회한 뒤 삭제/갱신/추가 필요한 데이터를 반영하고, 재동기화 결과(삭제·추가된 레코드 수 등)를
     * 반환합니다.
     *
     * @param request 재동기화 요청 DTO (국가 코드, 연도)
     * @return HolidayRefreshResponse 재동기화 결과(기존 건수, 신규 건수, 삭제/추가된 수량)
     */
    HolidayRefreshResponse refreshHolidays(HolidayRefreshRequest request);

    /**
     * 최근 6년 내(요구사항: 과거 5년 + 올해)의 모든 국가 공휴일 데이터를 대량 동기화합니다. 국가별 · 연도별 API 호출을 통해 전체 데이터를 적재하며, 작업 통계
     * 정보는 {@link HolidaySyncResponse} 형태로 제공합니다.
     *
     * @return HolidaySyncResponse 공휴일 대량 적재 결과
     */
    HolidaySyncResponse syncHolidaysFor6Years();

    /**
     * 날짜 범위, 국가 코드, 공휴일 타입 등의 조건에 따라 공휴일 정보를 조회합니다. 페이징 처리된 결과를 {@link HolidaySearchResponse} 로
     * 반환합니다.
     *
     * @param request 공휴일 검색 요청 DTO (기간, 국가 코드, 타입, 페이지 등)
     * @return HolidaySearchResponse 공휴일 검색 결과 목록 및 페이지 정보
     */
    HolidaySearchResponse searchHolidays(HolidaySearchRequest request);

    /**
     * 특정 국가의 특정 연도에 해당하는 공휴일 데이터를 삭제합니다. 삭제된 레코드 수는 {@link HolidayDeleteResponse} 로 반환됩니다.
     *
     * @param request 삭제 요청 DTO (국가 코드, 연도)
     * @return HolidayDeleteResponse 삭제된 레코드 수 정보
     */
    HolidayDeleteResponse deleteHolidays(HolidayDeleteRequest request);
}
