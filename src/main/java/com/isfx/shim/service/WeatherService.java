package com.isfx.shim.service;

import com.isfx.shim.dto.AirQualityResponseDto;
import com.isfx.shim.dto.KmaWeatherResponseDto;
import com.isfx.shim.dto.WeatherDataDto;
import com.isfx.shim.entity.WeatherLog;
import com.isfx.shim.entity.enums.WeatherCondition;
import com.isfx.shim.repository.WeatherLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final AirQualityClient airQualityClient;
    private final KmaWeatherClient kmaWeatherClient;
    private final WeatherLogRepository weatherLogRepository;

    @Transactional
    public WeatherLog fetchWeatherData(String location) {
        String normalizedLocation = location != null ? location.trim() : "서울특별시";
        boolean isMockData = false;

        // 대기오염 API 호출 (서울시 대기질 정보 API)
        log.info("[대기오염 API] 서울시 대기질 정보 API 호출 시작: location={}", normalizedLocation);
        AirQualityResponseDto airQuality;
        try {
            airQuality = airQualityClient.getAirQuality(normalizedLocation);
            log.info("[대기오염 API] API 호출 완료, 받은 데이터: pm10={}, pm25={}, airQualityIndex={}", 
                    airQuality.getPm10(), airQuality.getPm25(), airQuality.getAirQualityIndex());
            
            // Mock 데이터 확인 (기본 mock 값: pm10=30, pm25=15, cai=50)
            if (airQuality.getPm10() != null && airQuality.getPm10() == 30 && 
                airQuality.getPm25() != null && airQuality.getPm25() == 15 &&
                airQuality.getAirQualityIndex() != null && airQuality.getAirQualityIndex() == 50) {
                log.warn("[대기오염 API] 서울시 대기질 정보 API 호출 실패 또는 데이터 없음, mock 데이터 사용: location={}", normalizedLocation);
                isMockData = true;
            } else {
                log.info("[대기오염 API] 실제 API 데이터 사용: pm10={}, pm25={}, airQualityIndex={}", 
                        airQuality.getPm10(), airQuality.getPm25(), airQuality.getAirQualityIndex());
            }
        } catch (Exception e) {
            log.error("[대기오염 API] 서울시 대기질 정보 API 호출 실패, mock 데이터 사용: location={}, error={}", normalizedLocation, e.getMessage(), e);
            airQuality = AirQualityResponseDto.builder()
                    .pm10((short) 30)
                    .pm25((short) 15)
                    .airQualityIndex((short) 50)
                    .build();
            isMockData = true;
        }

        // 날씨 API 호출 (기상청 초단기실황 API)
        Coordinate coordinate = CoordinateMapper.toGridXY(normalizedLocation);
        Double temperature = null;
        WeatherCondition condition = null;
        try {
            KmaWeatherResponseDto weather = kmaWeatherClient.getWeather(coordinate.nx(), coordinate.ny());
            temperature = weather.getTemperature();
            condition = mapCondition(weather.getSkyCode(), weather.getPrecipitationType());
            log.info("[날씨 API] 기상청 초단기실황 API 호출 성공: location={}, temperature={}, condition={}", normalizedLocation, temperature, condition);
        } catch (Exception e) {
            log.error("[날씨 API] 기상청 초단기실황 API 호출 실패, null로 저장: location={}, nx={}, ny={}, error={}", 
                    normalizedLocation, coordinate.nx(), coordinate.ny(), e.getMessage(), e);
            // 날씨 API 호출 실패 시 null로 저장
            temperature = null;
            condition = null;
        }

        // Mock 데이터인 경우 location에 표시
        String locationWithMock = isMockData ? "mock데이터 - " + normalizedLocation : normalizedLocation;

        // null 값에 대한 기본값 설정
        WeatherCondition finalCondition = condition != null ? condition : WeatherCondition.other;
        Double finalTemperature = temperature != null ? temperature : 21.0; // temperature가 null이면 기본값 21.0 사용
        Short finalPm10 = airQuality.getPm10() != null ? airQuality.getPm10() : (short) 30; // pm10이 null이면 기본값 30 사용
        Short finalPm25 = airQuality.getPm25() != null ? airQuality.getPm25() : (short) 15; // pm25가 null이면 기본값 15 사용
        Short finalAirQualityIndex = airQuality.getAirQualityIndex() != null ? airQuality.getAirQualityIndex() : (short) 50; // air_quality_index가 null이면 기본값 50 사용

        WeatherLog weatherLog = WeatherLog.builder()
                .location(locationWithMock)
                .observed_at(LocalDateTime.now())
                .temperature(finalTemperature)
                .condition(finalCondition)
                .pm10(finalPm10)
                .pm25(finalPm25)
                .air_quality_index(finalAirQualityIndex)
                .build();

        return weatherLogRepository.save(weatherLog);
    }

    @Transactional(readOnly = true)
    public WeatherDataDto getLatestWeather(String location) {
        WeatherLog weatherLog = fetchWeatherData(location);
        return WeatherDataDto.builder()
                .location(weatherLog.getLocation())
                .condition(weatherLog.getCondition() != null ? weatherLog.getCondition().name() : null)
                .temperature(weatherLog.getTemperature())
                .pm10(weatherLog.getPm10())
                .pm25(weatherLog.getPm25())
                .airQualityIndex(weatherLog.getAir_quality_index())
                .build();
    }

    private WeatherCondition mapCondition(int skyCode, int precipitationType) {
        if (precipitationType == 1 || precipitationType == 2) {
            return WeatherCondition.rain;
        }
        if (precipitationType == 3) {
            return WeatherCondition.snow;
        }
        return switch (skyCode) {
            case 1 -> WeatherCondition.clear;
            case 3, 4 -> WeatherCondition.clouds;
            default -> WeatherCondition.other;
        };
    }
}
