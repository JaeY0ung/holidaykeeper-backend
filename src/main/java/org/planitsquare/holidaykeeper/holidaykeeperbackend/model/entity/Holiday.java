package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "country_holiday")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    // 현지 언어로 된 공휴일 명칭
    @Column(name = "holiday_local_name", nullable = false)
    private String localName;

    // 영문 공휴일 명칭
    @Column(name = "holiday_name", nullable = false)
    private String name;

    // 국가
    @JoinColumn(name = "country_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

    // 고정 공휴일 여부 (true: 매년 같은 날짜, false: 변동 가능)
    @Column(name = "fixed", nullable = false)
    private Boolean fixed;

    // 전국 단위 공휴일 여부 (true: 전국, false: 지역 한정)
    @Column(name = "global", nullable = false)
    private Boolean global;

    // 적용 지역 목록 (global이 false일 경우 배열, 전국일 경우 null)
    // 저장 예시: "US-CA,US-CT,US-IL,US-IN,US-KY,US-MI,US-NY,US-MO,US-OH"
    @Column(name = "counties")
    private String counties;

    // 편의 메서드
    @Transient
    public List<String> getCountiesList() {

        return counties == null || counties.isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(counties.split(","));
    }

    // 공휴일 제정 연도 (없으면 null)
    @Column(name = "holiday_launch_year")
    private Integer launchYear;

    // 공휴일 유형 배열 (Public, Bank, School, Authorities, Optional, Observance)
    // 저장 예시: "School,Authorities"
    @Column(name = "holiday_types")
    private String types;

    @Transient
    public List<String> getTypesList() {

        return types == null || types.isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(types.split(","));
    }

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
