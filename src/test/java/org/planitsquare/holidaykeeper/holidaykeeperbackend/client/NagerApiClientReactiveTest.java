package org.planitsquare.holidaykeeper.holidaykeeperbackend.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.external_api_dto.response.CountryResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class NagerApiClientReactiveTest {

    private MockWebServer mockWebServer;
    private NagerApiClientReactive client;

    @BeforeEach
    void setUp() throws Exception {

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        client = new NagerApiClientReactive(webClient);
    }

    @AfterEach
    void tearDown() throws Exception {

        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("MockWebServer — 국가 목록 조회 성공 테스트")
    void fetchAvailableCountries_success() throws Exception {

        List<CountryResponse> mockList = List.of(
            new CountryResponse("KR", "Korea"),
            new CountryResponse("US", "United States")
        );

        String json = new ObjectMapper().writeValueAsString(mockList);

        mockWebServer.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.fetchAvailableCountries())
            .assertNext(list -> {
                assertThat(list).hasSize(2);
                assertThat(list.get(0).countryCode()).isEqualTo("KR");
            })
            .verifyComplete();
    }
}