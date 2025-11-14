package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.isfx.shim.entity.enums.EnergyLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreateRecordResponseDto {

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("record_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @JsonProperty("emotion_level")
    private Integer emotionLevel;

    @JsonProperty("conversation_level")
    private Integer conversationLevel;

    @JsonProperty("meeting_count")
    private Integer meetingCount;

    @JsonProperty("transport_mode")
    private String transportMode;

    @JsonProperty("congestion_level")
    private Integer congestionLevel;

    @JsonProperty("location")
    private String location;

    @JsonProperty("journal")
    private String journal;

    @JsonProperty("energy_score")
    private Double energyScore;

    @JsonProperty("energy_level")
    private EnergyLevel energyLevel;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    @JsonProperty("ai_prescription")
    private AiPrescriptionDto aiPrescription;

    @JsonProperty("weather_log")
    private WeatherLogDto weatherLog;

    // AI 처방 정보
    @Getter
    @Builder
    public static class AiPrescriptionDto {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("category")
        private String category;

        @JsonProperty("recommendation_text")
        private String recommendationText;

        @JsonProperty("journal_explain")
        private String journalExplain;
    }

    // 날씨 로그 정보
    @Getter
    @Builder
    public static class WeatherLogDto {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("location")
        private String location;

        @JsonProperty("observed_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime observedAt;

        @JsonProperty("condition")
        private String condition;

        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("pm10")
        private Integer pm10;
    }
}
