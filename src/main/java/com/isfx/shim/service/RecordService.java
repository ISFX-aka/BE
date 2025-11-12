package com.isfx.shim.service;

import com.isfx.shim.dto.CreateRecordRequest;
import com.isfx.shim.dto.CreateRecordResponseDto;
import com.isfx.shim.entity.*;
import com.isfx.shim.entity.enums.*;
import com.isfx.shim.global.exception.CustomException;
import com.isfx.shim.global.exception.ErrorCode;
import com.isfx.shim.repository.AiPrescriptionsRepository;
import com.isfx.shim.repository.DailyRecordRepository;
import com.isfx.shim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final AiPrescriptionsRepository aiPrescriptionsRepository;
    private final UserRepository userRepository;
    private final WeatherService weatherService;

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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 현재 날짜 및 시간대 결정
        LocalDate recordDate = LocalDate.now();
        TimePeriod timePeriod = determineTimePeriod();

        // 3. TransportMode enum 변환
        TransportMode transportMode = convertTransportMode(request.getTransportMode());

        // 4. 날씨 정보 조회 (외부 API 연동)
        WeatherLog weatherLog = weatherService.fetchWeatherData(request.getLocation());

        // 5. 에너지 점수 계산
        double energyScore = calculateEnergyScore(request, transportMode, weatherLog);

        // 6. EnergyLevel 결정
        EnergyLevel energyLevel = determineEnergyLevel(energyScore);

        // 7. DailyRecord 생성 및 저장
        DailyRecord dailyRecord = createDailyRecord(
                user, recordDate, timePeriod, request, transportMode, energyScore, energyLevel
        );
        dailyRecord = dailyRecordRepository.save(dailyRecord);

        // 8. AI 처방 생성 (Upstage API 연동)
        AiPrescriptions aiPrescription = generateAiPrescription(dailyRecord, request, energyScore);

        // 9. 응답 DTO 생성
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
     * 전체 에너지 점수 계산
     * 
     * 에너지 점수는 세 가지 요소의 가중 평균으로 계산됩니다:
     * - 사회적 점수 (40%): 감정 수준, 대화 수준, 만남 횟수
     * - 이동 점수 (30%): 교통수단 종류와 혼잡도
     * - 날씨 점수 (30%): 온도, 날씨 조건, 대기질(PM10, PM25)
     * 
     * @param request 기록 생성 요청 DTO
     * @param transportMode 이동 수단 (WALK, SUBWAY, BUS 등)
     * @param weatherLog 날씨 로그 정보
     * @return 0~100 사이의 에너지 점수
     */
    private double calculateEnergyScore(CreateRecordRequest request, TransportMode transportMode, WeatherLog weatherLog) {
        // 각 요소별 점수 계산 (각각 0~100점)
        double socialScore = calculateSocialScore(request);
        double movementScore = calculateMovementScore(transportMode, request.getCongestionLevel());
        double weatherScore = calculateWeatherScore(weatherLog);

        // 가중 평균 계산: 사회적(40%) + 이동(30%) + 날씨(30%)
        double energyScore = (0.4 * socialScore) + (0.3 * movementScore) + (0.3 * weatherScore);
        
        // 점수를 0~100 범위로 제한
        return Math.max(0, Math.min(100, energyScore));
    }

    /**
     * 사회적 점수 계산
     * 
     * 사회적 상호작용을 기반으로 에너지 점수를 계산합니다.
     * 세 가지 요소로 구성되며, 총 100점 만점입니다:
     * 
     * 1. 감정 수준 점수 (최대 40점)
     *    - 감정 수준(1~5)을 5로 나눈 비율에 40을 곱함
     *    - 예: 감정 수준 5 → 40점, 감정 수준 3 → 24점
     * 
     * 2. 대화 수준 점수 (최대 30점)
     *    - 대화 수준(1~5)을 5로 나눈 비율에 30을 곱함
     *    - 예: 대화 수준 5 → 30점, 대화 수준 2 → 12점
     * 
     * 3. 만남 횟수 점수 (최대 30점)
     *    - 만남 횟수 × 10점 (최대 30점으로 제한)
     *    - 예: 만남 3회 → 30점, 만남 1회 → 10점
     * 
     * @param request 기록 생성 요청 DTO
     * @return 0~100 사이의 사회적 점수
     */
    private double calculateSocialScore(CreateRecordRequest request) {
        // null 값 처리: 기본값 0 사용
        int emotionLevel = request.getEmotionLevel() != null ? request.getEmotionLevel() : 0;
        int conversationLevel = request.getConversationLevel() != null ? request.getConversationLevel() : 0;
        int meetingCount = request.getMeetingCount() != null ? request.getMeetingCount() : 0;

        // 감정 수준 점수: (감정 수준 / 5) × 40점
        double emotionScore = (emotionLevel / 5.0) * 40.0;
        
        // 대화 수준 점수: (대화 수준 / 5) × 30점
        double conversationScore = (conversationLevel / 5.0) * 30.0;
        
        // 만남 횟수 점수: 만남 횟수 × 10점 (최대 30점)
        double meetingScore = Math.min(30.0, meetingCount * 10.0);

        // 총점 계산 및 0~100 범위로 제한
        return Math.max(0, Math.min(100, emotionScore + conversationScore + meetingScore));
    }

    /**
     * 이동 점수 계산
     * 
     * 교통수단과 혼잡도를 기반으로 에너지 점수를 계산합니다.
     * 
     * 계산 방식:
     * 1. 교통수단별 기본 점수 설정
     *    - 도보(WALK): 100점 (가장 에너지 소모가 적음)
     *    - 지하철(SUBWAY): 85점
     *    - 버스(BUS): 80점
     * 
     * 2. 혼잡도에 따른 감점 적용
     *    - 혼잡도 1: 감점 0점 (여유로움)
     *    - 혼잡도 2: 감점 5점
     *    - 혼잡도 3: 감점 10점
     *    - 혼잡도 4: 감점 15점
     *    - 혼잡도 5: 감점 20점 (매우 혼잡)
     *    - 공식: (혼잡도 - 1) × 5점
     * 
     * 최종 점수 = 기본 점수 - 혼잡도 감점 (최소 0점)
     * 
     * @param transportMode 교통수단 (WALK, SUBWAY, BUS)
     * @param congestionLevelInput 혼잡도 (1~5, null인 경우 기본값 3)
     * @return 0~100 사이의 이동 점수
     */
    private double calculateMovementScore(TransportMode transportMode, Integer congestionLevelInput) {
        // 혼잡도 null 처리: 기본값 3 사용
        int congestionLevel = congestionLevelInput != null ? congestionLevelInput : 3;
        // 혼잡도를 1~5 범위로 제한
        congestionLevel = Math.max(1, Math.min(5, congestionLevel));

        // 교통수단별 기본 점수
        double baseTransportScore = switch (transportMode) {
            case WALK -> 100.0;   // 도보: 최고 점수
            case SUBWAY -> 85.0; // 지하철
            case BUS -> 80.0;     // 버스
        };

        // 혼잡도에 따른 감점 계산: (혼잡도 - 1) × 5점
        double congestionPenalty = (congestionLevel - 1) * 5.0;
        
        // 최종 점수 = 기본 점수 - 감점 (최소 0점)
        return Math.max(0.0, baseTransportScore - congestionPenalty);
    }

    /**
     * 날씨 점수 계산
     * 
     * 날씨 정보를 기반으로 에너지 점수를 계산합니다.
     * 세 가지 요소로 구성되며, 총 100점 만점입니다:
     * 
     * 1. 온도 점수 (최대 33점)
     *    - 이상적인 온도(21°C)를 기준으로 가우시안 분포 함수 사용
     *    - 공식: 33 × exp(-0.03 × (온도 - 21)²)
     *    - 21°C에서 최대 33점, 온도가 벗어날수록 점수 감소
     *    - 예: 21°C → 33점, 15°C → 약 23점, 30°C → 약 15점
     * 
     * 2. 날씨 조건 점수 (최대 33점)
     *    - 맑음(clear): 33점
     *    - 구름(clouds): 25점
     *    - 비(rain): 18점
     *    - 눈(snow): 15점
     *    - 기타(other): 25점
     * 
     * 3. 대기질 점수 (최대 33점)
     *    - PM10 기준: 30 이하 → 감점 없음, 30 초과 시 감점
     *    - PM25 기준: 15 이하 → 감점 없음, 15 초과 시 감점
     *    - 공식: 33 - (PM10 감점 + PM25 감점)
     *    - PM10 감점: max(0, (PM10 - 30) / 70) × 13점
     *    - PM25 감점: max(0, (PM25 - 15) / 35) × 20점
     *    - 예: PM10=30, PM25=15 → 33점
     *          PM10=100, PM25=50 → 약 0점
     * 
     * @param weatherLog 날씨 로그 정보 (null인 경우 기본값 70점 반환)
     * @return 0~100 사이의 날씨 점수
     */
    private double calculateWeatherScore(WeatherLog weatherLog) {
        // 날씨 정보가 없는 경우 기본값 반환
        if (weatherLog == null) {
            return 70.0;
        }

        // null 값 처리: 기본값 사용
        double temperature = weatherLog.getTemperature() != null ? weatherLog.getTemperature() : 21.0;
        WeatherCondition condition = weatherLog.getCondition();
        double pm10 = weatherLog.getPm10() != null ? weatherLog.getPm10() : 30.0;
        double pm25 = weatherLog.getPm25() != null ? weatherLog.getPm25() : 15.0;

        // 1. 온도 점수: 가우시안 분포 함수 사용 (21°C 기준)
        // exp(-0.03 × (온도 - 21)²)로 21°C에서 최대값, 멀어질수록 감소
        double temperatureScore = 33 * Math.exp(-0.03 * Math.pow(temperature - 21, 2));
        
        // 2. 날씨 조건 점수
        double conditionScore = mapConditionScore(condition);

        // 3. 대기질 점수 계산
        // PM10 기준: 30 이하 → 감점 없음, 30 초과 시 비례 감점
        double pm10Factor = Math.max(0, (pm10 - 30) / 70.0);
        // PM25 기준: 15 이하 → 감점 없음, 15 초과 시 비례 감점
        double pm25Factor = Math.max(0, (pm25 - 15) / 35.0);
        // 대기질 점수 = 33점 - (PM10 감점 + PM25 감점)
        double airQualityScore = 33 - (pm10Factor * 13 + pm25Factor * 20);
        airQualityScore = Math.max(0, airQualityScore);

        // 총점 계산: 온도 + 조건 + 대기질
        double weatherScore = temperatureScore + conditionScore + airQualityScore;
        
        // 점수를 0~100 범위로 제한
        return Math.max(0, Math.min(100, weatherScore));
    }

    /**
     * 날씨 조건별 점수 매핑
     * 
     * 날씨 조건에 따라 점수를 반환합니다.
     * 맑은 날씨일수록 높은 점수를 부여합니다.
     * 
     * 점수 체계:
     * - 맑음(clear): 33점 (최고 점수)
     * - 구름(clouds): 25점
     * - 비(rain): 18점
     * - 눈(snow): 15점 (최저 점수)
     * - 기타(other): 25점
     * - null: 25점 (기본값)
     * 
     * @param condition 날씨 조건 enum
     * @return 날씨 조건 점수 (15~33점)
     */
    private double mapConditionScore(WeatherCondition condition) {
        if (condition == null) {
            return 25.0; // 기본값
        }
        return switch (condition) {
            case clear -> 33.0;  // 맑음: 최고 점수
            case clouds -> 25.0; // 구름
            case rain -> 18.0;   // 비
            case snow -> 15.0;   // 눈: 최저 점수
            case other -> 25.0;  // 기타
        };
    }

    /**
     * 에너지 레벨 결정
     * 
     * 에너지 점수에 따라 LOW, MEDIUM, HIGH 중 하나를 반환합니다.
     * 
     * 레벨 구분:
     * - LOW: 0점 이상 ~ 33점 미만 (에너지가 낮음)
     * - MEDIUM: 33점 이상 ~ 67점 미만 (에너지가 보통)
     * - HIGH: 67점 이상 ~ 100점 (에너지가 높음)
     * 
     * @param energyScore 계산된 에너지 점수 (0~100)
     * @return 에너지 레벨 (LOW, MEDIUM, HIGH)
     */
    private EnergyLevel determineEnergyLevel(double energyScore) {
        if (energyScore < 33) {
            return EnergyLevel.LOW;    // 0~32점: 낮은 에너지
        } else if (energyScore < 67) {
            return EnergyLevel.MEDIUM; // 33~66점: 보통 에너지
        } else {
            return EnergyLevel.HIGH;   // 67~100점: 높은 에너지
        }
    }

    /**
     * DailyRecord 생성
     */
    private DailyRecord createDailyRecord(
            User user, LocalDate recordDate, TimePeriod timePeriod,
            CreateRecordRequest request, TransportMode transportMode,
            double energyScore, EnergyLevel energyLevel) {
        
        // meetingCount가 null이면 기본값 0으로 설정
        Integer meetingCount = request.getMeetingCount() != null ? request.getMeetingCount() : 0;
        
        return DailyRecord.builder()
                .user(user)
                .recordDate(recordDate)
                .timePeriod(timePeriod)
                .emotionLevel(request.getEmotionLevel())
                .conversationLevel(request.getConversationLevel())
                .meetingCount(meetingCount)
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
     */
    private AiPrescriptions generateAiPrescription(
            DailyRecord dailyRecord, CreateRecordRequest request, double energyScore) {
        
        // TODO: Upstage API 호출하여 자연어 처방 생성
        // 현재는 더미 데이터 생성
        AiPrescriptions prescription = AiPrescriptions.builder()
                .record(dailyRecord)
                .category(AiPrescriptionCategory.recovery)
                .recommendationText("mock데이터 - 오늘은 사회적 에너지가 많이 소모되었네요. 저녁에는 좋아하는 음악을 들으며 휴식하는 것을 추천합니다.") // TODO: Upstage API 응답으로 대체
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
                        .condition(weatherLog.getCondition() != null ? weatherLog.getCondition().name() : null)
                        .temperature(weatherLog.getTemperature())
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

