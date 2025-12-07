package org.planitsquare.holidaykeeper.holidaykeeperbackend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.client.NagerApiClient;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.converter.CountryConverter;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.BusinessException;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.ErrorCode;
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
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_COUNTRY_CODE));
    }

    @Override
    @Transactional
    public List<Country> getCountryList() {

        List<Country> countryList = countryRepository.findAll();

        log.info("국가 목록 조회 완료: {} 개국", countryList.size());
        return countryList;
    }

    @Override
    @Transactional
    public void syncCountries() {
        // api로 국가 정보 가져오기
        List<CountryResponse> responses = nagerApiClient.fetchAvailableCountries();
        // 빈 응답은 에러 던지기
        if (responses == null || responses.isEmpty()) {
            throw new BusinessException(ErrorCode.HOLIDAY_API_CALL_FAILED);
        }
        // 변환
        List<Country> countries = responses.stream()
            .map(countryConverter::toEntity)
            .toList();

        // 가존에 저장되어 있던 국가 정보 모두 삭제
        countryRepository.deleteAll();
        // 저장
        countryRepository.saveAll(countries);
    }
}
