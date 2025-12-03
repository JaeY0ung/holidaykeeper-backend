package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.CountryConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.entity.Country;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    private final NagerApiClient nagerApiClient;

    private final CountryConverter countryConverter;

    @Override
    public Country getCountryByCode(String countryCode) {

        return countryRepository.findByCode(countryCode)
            .orElseThrow(() -> new IllegalArgumentException("잘못된 국가 코드입니다."));
    }

    @Override
    public List<Country> getCountryList() {

        List<Country> countryList = countryRepository.findAll();

        if (!countryList.isEmpty()) {
            log.info("국가 목록 조회 완료: {} 개국", countryList.size());
            return countryList;
        }
        log.info("국가 데이터가 없습니다. 외부 API에서 데이터를 가져옵니다.");
        syncCountries();
        return countryRepository.findAll();
    }

    /**
     * 싱크 맞추기 (기존 db에 저장되어 있는 국가 정보 제거하고 api로 다시 호출하여 저장)
     */
    private void syncCountries() {
        // 가존에 저장되어 있던 국가 정보 모두 삭제
        countryRepository.deleteAll();
        // api로 국가 정보 가져오기
        List<CountryResponse> responses = nagerApiClient.fetchAvailableCountries();
        // 변환
        List<Country> countries = responses.stream()
            .map(countryConverter::toEntity)
            .toList();
        // 저장
        countryRepository.saveAll(countries);
    }
}
