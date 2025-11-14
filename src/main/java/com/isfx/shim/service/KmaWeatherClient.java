package com.isfx.shim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.isfx.shim.dto.KmaWeatherResponseDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KmaWeatherClient {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public KmaWeatherClient(RestTemplateBuilder restTemplateBuilder,
                            @Value("${api.kma.key}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    public KmaWeatherResponseDto getWeather(int nx, int ny) {
        String baseDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 기상청 초단기실황은 매 정시에 발표되므로, 현재 시간에 가장 가까운 정시 데이터 요청
        String baseTime = calculateBaseTime();

        String url = UriComponentsBuilder
                .fromUriString("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 1000)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build()
                .toString();

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode responseBody = response.getBody();
            
            if (responseBody == null) {
                log.error("[날씨 API] 기상청 초단기실황 API 응답 본문이 null: nx={}, ny={}, baseDate={}, baseTime={}", nx, ny, baseDate, baseTime);
                throw new RuntimeException("KMA API response body is null");
            }

            // API 응답 구조 검증
            JsonNode responseNode = responseBody.path("response");
            if (responseNode.isMissingNode()) {
                log.error("[날씨 API] 기상청 초단기실황 API 응답 구조 오류 (response 노드 없음): nx={}, ny={}, responseBody={}", nx, ny, responseBody.toString());
                throw new RuntimeException("Invalid KMA API response structure: missing 'response' node");
            }

            // resultCode 확인
            String resultCode = responseNode.path("header").path("resultCode").asText("");
            String resultMsg = responseNode.path("header").path("resultMsg").asText("");
            
            if (!"00".equals(resultCode)) {
                log.error("[날씨 API] 기상청 초단기실황 API 오류 응답: nx={}, ny={}, resultCode={}, resultMsg={}", nx, ny, resultCode, resultMsg);
                throw new RuntimeException(String.format("KMA API error: resultCode=%s, resultMsg=%s", resultCode, resultMsg));
            }

            JsonNode bodyNode = responseNode.path("body");
            if (bodyNode.isMissingNode()) {
                log.error("[날씨 API] 기상청 초단기실황 API 응답 구조 오류 (body 노드 없음): nx={}, ny={}", nx, ny);
                throw new RuntimeException("Invalid KMA API response structure: missing 'body' node");
            }

            // totalCount 확인
            int totalCount = bodyNode.path("totalCount").asInt(0);
            if (totalCount == 0) {
                log.warn("[날씨 API] 기상청 초단기실황 API 데이터 없음: nx={}, ny={}, baseDate={}, baseTime={}, totalCount=0", nx, ny, baseDate, baseTime);
                throw new RuntimeException("No weather data available from KMA API");
            }

            JsonNode itemsNode = bodyNode.path("items");
            if (itemsNode.isMissingNode()) {
                log.error("[날씨 API] 기상청 초단기실황 API 응답 구조 오류 (items 노드 없음): nx={}, ny={}", nx, ny);
                throw new RuntimeException("Invalid KMA API response structure: missing 'items' node");
            }

            JsonNode itemNode = itemsNode.path("item");
            if (itemNode.isMissingNode()) {
                log.error("[날씨 API] 기상청 초단기실황 API 응답 구조 오류 (item 노드 없음): nx={}, ny={}", nx, ny);
                throw new RuntimeException("Invalid KMA API response structure: missing 'item' node");
            }

            // item이 배열인지 단일 객체인지 확인
            JsonNode items = itemNode.isArray() ? itemNode : itemNode;
            
            double temperature = Double.NaN;
            int sky = 1;
            int pty = 0;

            if (items.isArray()) {
                // 배열인 경우
                for (JsonNode item : items) {
                    String category = item.path("category").asText("");
                    switch (category) {
                        case "T1H" -> temperature = item.path("obsrValue").asDouble(Double.NaN);
                        case "SKY" -> sky = item.path("obsrValue").asInt(1);
                        case "PTY" -> pty = item.path("obsrValue").asInt(0);
                        default -> {
                            // ignore others
                        }
                    }
                }
            } else {
                // 단일 객체인 경우
                String category = items.path("category").asText("");
                switch (category) {
                    case "T1H" -> temperature = items.path("obsrValue").asDouble(Double.NaN);
                    case "SKY" -> sky = items.path("obsrValue").asInt(1);
                    case "PTY" -> pty = items.path("obsrValue").asInt(0);
                    default -> {
                        // ignore others
                    }
                }
            }

            if (Double.isNaN(temperature)) {
                log.warn("[날씨 API] 기상청 초단기실황 API 응답에 온도 데이터(T1H) 없음: nx={}, ny={}, baseDate={}, baseTime={}, totalCount={}, items={}", 
                        nx, ny, baseDate, baseTime, totalCount, items.toString());
                throw new RuntimeException("Temperature data (T1H) missing in KMA response");
            }

            log.debug("[날씨 API] 기상청 초단기실황 API 파싱 성공: nx={}, ny={}, temperature={}, sky={}, pty={}", nx, ny, temperature, sky, pty);
            return new KmaWeatherResponseDto(temperature, sky, pty);
        } catch (Exception e) {
            log.error("[날씨 API] 기상청 초단기실황 API 호출 실패: nx={}, ny={}, baseDate={}, baseTime={}, error={}", 
                    nx, ny, baseDate, baseTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 현재 시간에 가장 가까운 정시를 계산
     */
    private String calculateBaseTime() {
        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        
        // 1시간 전 정시 데이터 요청 (0시인 경우 23시로)
        int baseHour = (currentHour - 1 + 24) % 24;
        
        return String.format("%02d00", baseHour);
    }

}
