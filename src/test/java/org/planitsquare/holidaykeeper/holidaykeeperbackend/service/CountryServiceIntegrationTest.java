package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("CountryService 통합 테스트")
class CountryServiceIntegrationTest {

    @Autowired
    private CountryService countryService;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {

        countryRepository.deleteAll();
    }

    @Test
    @DisplayName("국가 목록 전체 플로우 테스트 - DB 비어있을 때 API 호출하여 저장")
    void getCountryList_fullFlow() {
        // given
        assertThat(countryRepository.count()).isZero();

        // when
        List<Country> result = countryService.getCountryList();

        // then
        assertThat(result).isNotEmpty();
        assertThat(countryRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("국가 코드로 조회 - 존재하는 경우")
    void getCountryByCode_existing() {
        // given
        Country savedCountry = countryRepository.save(
            Country.builder()
                .code("KR")
                .name("South Korea")
                .build()
        );

        // when
        Country result = countryService.getCountryByCode("KR");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedCountry.getId());
        assertThat(result.getCode()).isEqualTo("KR");
    }
}