package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("journal")
    private String journal;

    @JsonProperty("energy_score")
    private Double energyScore;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

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
    }

    // 날씨 로그 정보
    @Getter
    @Builder
    public static class WeatherLogDto {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("location")
        private String location;

        @JsonProperty("condition")
        private String condition;

        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("pm10")
        private Integer pm10;
    }
}
