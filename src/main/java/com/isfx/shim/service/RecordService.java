package com.isfx.shim.service;

import com.isfx.shim.dto.CreateRecordRequest;
import com.isfx.shim.dto.CreateRecordResponseDto;
import com.isfx.shim.entity.*;
import com.isfx.shim.entity.enums.*;
import com.isfx.shim.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final WeatherLogRepository weatherLogRepository;
    private final AiPrescriptionsRepository aiPrescriptionsRepository;
    private final UserRepository userRepository;

    /**
     * 일일 기록 생성
     * @param userId 현재 로그인한 사용자의 ID
     * @param request 기록 생성 요청 DTO
     * @return 생성된 기록 응답 DTO
     */
    @Transactional
    public CreateRecordResponseDto createRecord(Long userId, CreateRecordRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")); // TODO: CustomException으로 변경

        // 2. 현재 날짜 및 시간대 결정
        LocalDate recordDate = LocalDate.now();
        TimePeriod timePeriod = determineTimePeriod();

        // 3. TransportMode enum 변환
        TransportMode transportMode = convertTransportMode(request.getTransportMode());

        // 4. 날씨 정보 조회 (외부 API 연동)
        WeatherLog weatherLog = fetchWeatherData(recordDate, request.getLocation());

        // 5. 에너지 점수 계산
        double energyScore = calculateEnergyScore(request, transportMode, weatherLog);

        // 6. EnergyLevel 결정
        EnergyLevel energyLevel = determineEnergyLevel(energyScore);

        // 7. DailyRecord 생성 및 저장
        DailyRecord dailyRecord = createDailyRecord(
                user, recordDate, timePeriod, request, transportMode, energyScore, energyLevel
        );
        dailyRecord = dailyRecordRepository.save(dailyRecord);

        // 8. WeatherLog 저장
        weatherLog = weatherLogRepository.save(weatherLog);

        // 9. AI 처방 생성 (Upstage API 연동)
        AiPrescriptions aiPrescription = generateAiPrescription(dailyRecord, request, energyScore);

        // 10. 응답 DTO 생성
        return buildResponseDto(dailyRecord, aiPrescription, weatherLog);
    }

    /**
     * 현재 시간대 결정
     */
    private TimePeriod determineTimePeriod() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        if (hour >= 6 && hour < 12) {
            return TimePeriod.MORNING;
        } else if (hour >= 12 && hour < 18) {
            return TimePeriod.HOON; // 오후
        } else if (hour >= 18 && hour < 22) {
            return TimePeriod.EVENING;
        } else {
            return TimePeriod.NIGHT;
        }
    }

    /**
     * TransportMode 문자열을 enum으로 변환
     */
    private TransportMode convertTransportMode(String transportMode) {
        if (transportMode == null) {
            return TransportMode.SUBWAY;
        }
        return switch (transportMode.toLowerCase()) {
            case "subway" -> TransportMode.SUBWAY;
            case "bus" -> TransportMode.BUS;
            case "walk" -> TransportMode.WALK;
            default -> TransportMode.WALK;
        };
    }

    /**
     * 날씨 정보 조회 (외부 API 연동)
     * TODO: 서울시 실시간 자치구별 대기환경 현황 API 연동
     * TODO: 기상청 단기예보 API 연동
     * 참고: https://data.seoul.go.kr/dataList/OA-1200/S/1/datasetView.do
     * 참고: https://www.data.go.kr/data/15084084/openapi.do
     */
    private WeatherLog fetchWeatherData(LocalDate date, String location) {
        // TODO: 외부 API 호출하여 날씨 정보 가져오기
        // 현재는 더미 데이터 반환
        return WeatherLog.builder()
                .location(location)
                .observed_at(LocalDateTime.now())
                .temperature(BigDecimal.valueOf(12.5)) // TODO: 외부 API 데이터로 대체
                .condition(WeatherCondition.clear) // TODO: 외부 API 데이터로 대체
                .pm10((short) 35) // TODO: 외부 API 데이터로 대체
                .build();
    }

    /**
     * 에너지 점수 계산
     */
    private double calculateEnergyScore(CreateRecordRequest request, TransportMode transportMode, WeatherLog weatherLog) {
        double socialScore = calculateSocialScore(request);
        double movementScore = calculateMovementScore(transportMode, request.getCongestionLevel());
        double weatherScore = calculateWeatherScore(weatherLog);

        double energyScore = (0.4 * socialScore) + (0.3 * movementScore) + (0.3 * weatherScore);
        return Math.max(0, Math.min(100, energyScore));
    }

    private double calculateSocialScore(CreateRecordRequest request) {
        int emotionLevel = request.getEmotionLevel() != null ? request.getEmotionLevel() : 0;
        int conversationLevel = request.getConversationLevel() != null ? request.getConversationLevel() : 0;
        int meetingCount = request.getMeetingCount() != null ? request.getMeetingCount() : 0;

        double emotionScore = (emotionLevel / 5.0) * 40.0;
        double conversationScore = (conversationLevel / 5.0) * 30.0;
        double meetingScore = Math.min(30.0, meetingCount * 10.0);

        return Math.max(0, Math.min(100, emotionScore + conversationScore + meetingScore));
    }

    private double calculateMovementScore(TransportMode transportMode, Integer congestionLevelInput) {
        int congestionLevel = congestionLevelInput != null ? congestionLevelInput : 3;
        congestionLevel = Math.max(1, Math.min(5, congestionLevel));

        double baseTransportScore = switch (transportMode) {
            case WALK -> 100.0;
            case SUBWAY -> 85.0;
            case BUS -> 80.0;
        };

        double congestionPenalty = (congestionLevel - 1) * 5.0;
        return Math.max(0.0, baseTransportScore - congestionPenalty);
    }

    private double calculateWeatherScore(WeatherLog weatherLog) {
        if (weatherLog == null) {
            return 70.0; // 기본값
        }

        double temperature = weatherLog.getTemperature() != null ? weatherLog.getTemperature().doubleValue() : 21.0;
        WeatherCondition condition = weatherLog.getCondition();
        double pm10 = weatherLog.getPm10() != null ? weatherLog.getPm10() : 30.0;
        double pm25 = weatherLog.getPm25() != null ? weatherLog.getPm25() : 15.0;

        double temperatureScore = 33 * Math.exp(-0.03 * Math.pow(temperature - 21, 2));
        double conditionScore = mapConditionScore(condition);

        double pm10Factor = Math.max(0, (pm10 - 30) / 70.0);
        double pm25Factor = Math.max(0, (pm25 - 15) / 35.0);
        double airQualityScore = 33 - (pm10Factor * 13 + pm25Factor * 20);
        airQualityScore = Math.max(0, airQualityScore);

        double weatherScore = temperatureScore + conditionScore + airQualityScore;
        return Math.max(0, Math.min(100, weatherScore));
    }

    private double mapConditionScore(WeatherCondition condition) {
        if (condition == null) {
            return 25.0;
        }
        return switch (condition) {
            case clear -> 33.0;
            case clouds -> 25.0;
            case rain -> 18.0;
            case snow -> 15.0;
            case other -> 25.0;
        };
    }

    /**
     * EnergyLevel 결정
     */
    private EnergyLevel determineEnergyLevel(double energyScore) {
        if (energyScore < 33) {
            return EnergyLevel.LOW;
        } else if (energyScore < 67) {
            return EnergyLevel.MEDIUM;
        } else {
            return EnergyLevel.HIGH;
        }
    }

    /**
     * DailyRecord 생성
     */
    private DailyRecord createDailyRecord(
            User user, LocalDate recordDate, TimePeriod timePeriod,
            CreateRecordRequest request, TransportMode transportMode,
            double energyScore, EnergyLevel energyLevel) {
        
        return DailyRecord.builder()
                .user(user)
                .recordDate(recordDate)
                .timePeriod(timePeriod)
                .emotionLevel(request.getEmotionLevel())
                .conversationLevel(request.getConversationLevel())
                .meetingCount(request.getMeetingCount())
                .transportMode(transportMode)
                .congestionLevel(request.getCongestionLevel())
                .location(request.getLocation())
                .journal(request.getJournal())
                .energyScore(energyScore)
                .energyLevel(energyLevel)
                .build();
    }

    /**
     * AI 처방 생성 (Upstage API 연동)
     * TODO: Upstage API 연동하여 자연어 처방 생성
     */
    private AiPrescriptions generateAiPrescription(
            DailyRecord dailyRecord, CreateRecordRequest request, double energyScore) {
        
        // TODO: Upstage API 호출하여 자연어 처방 생성
        // 현재는 더미 데이터 생성
        AiPrescriptions prescription = AiPrescriptions.builder()
                .record(dailyRecord)
                .category(AiPrescriptionCategory.recovery) // TODO: 에너지 점수 기반 추천 로직으로 대체
                .recommendationText("오늘은 사회적 에너지가 많이 소모되었네요. 저녁에는 좋아하는 음악을 들으며 휴식하는 것을 추천합니다.") // TODO: Upstage API 응답으로 대체
                .build();
        
        return aiPrescriptionsRepository.save(prescription);
    }

    /**
     * 응답 DTO 생성
     */
    private CreateRecordResponseDto buildResponseDto(
            DailyRecord dailyRecord, AiPrescriptions aiPrescription, WeatherLog weatherLog) {
        
        CreateRecordResponseDto.AiPrescriptionDto aiPrescriptionDto =
                CreateRecordResponseDto.AiPrescriptionDto.builder()
                        .id(aiPrescription.getPrescription_id())
                        .category(aiPrescription.getCategory().name())
                        .recommendationText(aiPrescription.getRecommendationText())
                        .build();

        CreateRecordResponseDto.WeatherLogDto weatherLogDto =
                CreateRecordResponseDto.WeatherLogDto.builder()
                        .id(weatherLog.getWeather_log_id())
                        .location(weatherLog.getLocation())
                        .condition(weatherLog.getCondition().name())
                        .temperature(weatherLog.getTemperature().doubleValue())
                        .pm10(weatherLog.getPm10() != null ? weatherLog.getPm10().intValue() : null)
                        .build();

        return CreateRecordResponseDto.builder()
                .recordId(dailyRecord.getId())
                .userId(dailyRecord.getUser().getId())
                .recordDate(dailyRecord.getRecordDate())
                .journal(dailyRecord.getJournal())
                .energyScore(dailyRecord.getEnergyScore())
                .createdAt(dailyRecord.getCreatedAt())
                .aiPrescription(aiPrescriptionDto)
                .weatherLog(weatherLogDto)
                .build();
    }
}

