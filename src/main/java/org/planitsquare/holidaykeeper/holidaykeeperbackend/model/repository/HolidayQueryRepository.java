package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Holiday;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.QHoliday;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HolidayQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 공휴일 검색 (페이징 적용)
     */
    public List<Holiday> searchWithPaging(
        Country country,
        LocalDate start,
        LocalDate end,
        List<String> types,
        int page,
        int size
    ) {

        QHoliday h = QHoliday.holiday;

        BooleanExpression predicate = h.country.eq(country)
            .and(h.date.between(start, end));

        if (types != null && !types.isEmpty()) {
            BooleanExpression typeExpr = null;
            for (String type : types) {
                BooleanExpression containsType = h.types.containsIgnoreCase(type);
                typeExpr = (typeExpr == null) ? containsType : typeExpr.or(containsType);
            }
            predicate = predicate.and(typeExpr);
        }

        return queryFactory
            .selectFrom(h)
            .where(predicate)
            .orderBy(h.date.asc(), h.id.asc()) // 날짜 순으로 정렬, 같은 날짜는 ID 순
            .offset((long) page * size)
            .limit(size)
            .fetch();
    }

    /**
     * 공휴일 개수 조회 (페이징 메타데이터를 위해)
     */
    public long count(
        Country country,
        LocalDate start,
        LocalDate end,
        List<String> types
    ) {

        QHoliday h = QHoliday.holiday;

        BooleanExpression predicate = h.country.eq(country)
            .and(h.date.between(start, end));

        if (types != null && !types.isEmpty()) {
            BooleanExpression typeExpr = null;
            for (String type : types) {
                BooleanExpression containsType = h.types.containsIgnoreCase(type);
                typeExpr = (typeExpr == null) ? containsType : typeExpr.or(containsType);
            }
            predicate = predicate.and(typeExpr);
        }

        Long count = queryFactory
            .select(h.count())
            .from(h)
            .where(predicate)
            .fetchOne();

        return count != null ? count : 0L;
    }
}