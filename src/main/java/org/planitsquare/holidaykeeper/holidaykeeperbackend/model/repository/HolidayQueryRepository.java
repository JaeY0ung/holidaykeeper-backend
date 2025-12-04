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

    public List<Holiday> search(
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

        return queryFactory
            .selectFrom(h)
            .where(predicate)
            .fetch();
    }
}