package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.isfx.shim.entity.User;
import com.isfx.shim.entity.DailyRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserResponseDto {

    // (GET /api/users/me) 내 정보 조회 응답 DTO

    @Getter
    @Builder
    public static class UserMyInfoGetResDto {
        private Long userId;
        private String email;
        private String name;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;

        public static UserMyInfoGetResDto fromEntity(User user) {
            return UserMyInfoGetResDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    // (PUT /api/users/me) 내 정보 수정 응답 DTO

    @Getter
    @Builder
    public static class UserUpdateResDto {
        private Long userId;
        private String email;
        private String name;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;

        public static UserUpdateResDto fromEntity(User user) {
            return UserUpdateResDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    // (GET /api/users/me/status) 내 활동 통계 응답 DTO
    @Getter
    @Builder
    public static class UserStatsGetResDto {
        private Long userId;
        private String period;
        private String startDate;
        private String endDate;
        private double averageEnergyScore;
        private int recordCount;
        private List<EnergyTrendDto> energyTrend;
    }

    // UserStatsGetResDto 내부에 포함될 일별 에너지 DTO
    @Getter
    @Builder
    public static class EnergyTrendDto {
        private Long recordId;
        private String recordDate;
        private double energyScore;

        public static EnergyTrendDto fromEntity(DailyRecord record) {
            return EnergyTrendDto.builder()
                    .recordId(record.getId())
                    .recordDate(record.getRecordDate().toString())
                    .energyScore(record.getEnergyScore())
                    .build();
        }
    }
}