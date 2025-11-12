package com.isfx.shim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.isfx.shim.dto.AirQualityResponseDto;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class AirQualityClient {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public AirQualityClient(RestTemplateBuilder restTemplateBuilder,
                            @Value("${api.seoul.air.key}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    public AirQualityResponseDto getAirQuality(String districtName) {
        String targetName = districtName != null ? districtName.trim() : "종로구";

        try {
            String url = UriComponentsBuilder
                    .fromUriString("http://openapi.seoul.go.kr:8088")
                    .pathSegment(apiKey, "json", "ListAirQualityByDistrictService", "1", "25")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toString();

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

            JsonNode items = response.getBody()
                    .path("ListAirQualityByDistrictService")
                    .path("row");

            for (JsonNode item : items) {
                String stationName = item.path("MSRSTENAME").asText(item.path("MSRSTN_NM").asText(""));
                if (stationName.trim().equals(targetName)) {
                    short pm10 = (short) item.path("PM10").asInt(-1);
                    short pm25 = (short) item.path("PM25").asInt(-1);
                    short cai = (short) item.path("CAI").asInt(-1);

                    return AirQualityResponseDto.builder()
                            .pm10(pm10 >= 0 ? pm10 : null)
                            .pm25(pm25 >= 0 ? pm25 : null)
                            .airQualityIndex(cai >= 0 ? cai : null)
                            .build();
                }
            }

            log.warn("[대기오염 API] 서울시 대기질 정보 API에서 해당 지역 데이터 없음: district={}, mock 데이터 사용", targetName);
        } catch (Exception e) {
            log.error("[대기오염 API] 서울시 대기질 정보 API 호출 실패: district={}, error={}, mock 데이터 사용", targetName, e.getMessage(), e);
        }

        // Mock 데이터 반환
        return AirQualityResponseDto.builder()
                .pm10((short) 30)
                .pm25((short) 15)
                .airQualityIndex((short) 50)
                .build();
    }
}
