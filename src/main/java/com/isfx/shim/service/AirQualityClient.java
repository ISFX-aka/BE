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
        log.info("[대기오염 API] API 호출 시작: district={}", targetName);

        try {
            String url = UriComponentsBuilder
                    .fromUriString("http://openapi.seoul.go.kr:8088")
                    .pathSegment(apiKey, "json", "ListAirQualityByDistrictService", "1", "25")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toString();

            log.debug("[대기오염 API] API URL: {}", url);
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            log.debug("[대기오염 API] API 응답 상태: {}", response.getStatusCode());

            JsonNode body = response.getBody();
            if (body == null) {
                log.warn("[대기오염 API] API 응답 body가 null입니다: district={}, mock 데이터 사용", targetName);
                return AirQualityResponseDto.builder()
                        .pm10((short) 30)
                        .pm25((short) 15)
                        .airQualityIndex((short) 50)
                        .build();
            }

            JsonNode items = body
                    .path("ListAirQualityByDistrictService")
                    .path("row");

            if (!items.isArray() || items.size() == 0) {
                log.warn("[대기오염 API] API 응답에 지역 데이터가 없습니다: district={}, mock 데이터 사용", targetName);
                return AirQualityResponseDto.builder()
                        .pm10((short) 30)
                        .pm25((short) 15)
                        .airQualityIndex((short) 50)
                        .build();
            }

            log.debug("[대기오염 API] API 응답에서 받은 지역 데이터 개수: {}", items.size());
            
            for (JsonNode item : items) {
                String stationName = item.path("MSRSTENAME").asText(item.path("MSRSTN_NM").asText(""));
                log.debug("[대기오염 API] 비교 중: API 지역명='{}', 요청 지역명='{}'", stationName, targetName);
                if (stationName.trim().equals(targetName)) {
                    // API 응답의 모든 필드명 확인을 위한 로깅
                    log.debug("[대기오염 API] 매칭된 지역 데이터 발견: district={}, 전체 JSON={}", targetName, item.toString());
                    
                    // 서울시 API 실제 필드명: PM (PM10), FPM (PM2.5), CAI (대기질 지수)
                    JsonNode pm10Node = item.path("PM");  // PM → PM10 (미세먼지)
                    JsonNode pm25Node = item.path("FPM"); // FPM → PM2.5 (초미세먼지)
                    JsonNode caiNode = item.path("CAI");  // CAI → 대기질 지수
                    
                    // 값 추출 (null 체크 포함)
                    Short pm10 = null;
                    if (!pm10Node.isMissingNode() && !pm10Node.isNull()) {
                        String pm10Str = pm10Node.asText("");
                        if (!pm10Str.isEmpty() && !pm10Str.equals("-")) {
                            try {
                                int pm10Value = Integer.parseInt(pm10Str.trim());
                                if (pm10Value >= 0) {
                                    pm10 = (short) pm10Value;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("[대기오염 API] PM10 파싱 실패: value={}, district={}", pm10Str, targetName);
                            }
                        }
                    }
                    
                    Short pm25 = null;
                    if (!pm25Node.isMissingNode() && !pm25Node.isNull()) {
                        String pm25Str = pm25Node.asText("");
                        if (!pm25Str.isEmpty() && !pm25Str.equals("-")) {
                            try {
                                int pm25Value = Integer.parseInt(pm25Str.trim());
                                if (pm25Value >= 0) {
                                    pm25 = (short) pm25Value;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("[대기오염 API] PM25 파싱 실패: value={}, district={}", pm25Str, targetName);
                            }
                        }
                    }
                    
                    Short cai = null;
                    if (!caiNode.isMissingNode() && !caiNode.isNull()) {
                        String caiStr = caiNode.asText("");
                        if (!caiStr.isEmpty() && !caiStr.equals("-")) {
                            try {
                                int caiValue = Integer.parseInt(caiStr.trim());
                                if (caiValue >= 0) {
                                    cai = (short) caiValue;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("[대기오염 API] CAI 파싱 실패: value={}, district={}", caiStr, targetName);
                            }
                        }
                    }
                    
                    log.info("[대기오염 API] 파싱된 값: district={}, pm10={}, pm25={}, cai={}", targetName, pm10, pm25, cai);

                    AirQualityResponseDto result = AirQualityResponseDto.builder()
                            .pm10(pm10)
                            .pm25(pm25)
                            .airQualityIndex(cai)
                            .build();
                    
                    log.info("[대기오염 API] 서울시 대기질 정보 API 호출 성공: district={}, pm10={}, pm25={}, airQualityIndex={}", 
                            targetName, result.getPm10(), result.getPm25(), result.getAirQualityIndex());
                    return result;
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
