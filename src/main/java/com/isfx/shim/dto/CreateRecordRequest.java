package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRecordRequest {

    @JsonProperty("emotion_level")
    @Min(value = 1, message = "감정 수준은 1 이상이어야 합니다.")
    @Max(value = 5, message = "감정 수준은 5 이하여야 합니다.")
    @NotNull(message = "emotion_level은 필수 입력값입니다.")
    private Integer emotionLevel; // 감정 수준 (1~5)

    @JsonProperty("conversation_level")
    @Min(value = 1, message = "대화 수준은 1 이상이어야 합니다.")
    @Max(value = 5, message = "대화 수준은 5 이하여야 합니다.")
    @NotNull(message = "conversation_level은 필수 입력값입니다.")
    private Integer conversationLevel; // 대화 수준 (1~5)

    @JsonProperty("meeting_count")
    @Min(value = 0, message = "만남 횟수는 0 이상이어야 합니다.")
    @NotNull(message = "meeting_count는 필수 입력값입니다.")
    private Integer meetingCount; // 만남 횟수

    @JsonProperty("transport_mode")
    @NotBlank(message = "transport_mode는 필수 입력값입니다.")
    @Pattern(regexp = "subway|bus|car|walk|bike|none",
             message = "이동 수단은 subway, bus, car, walk, bike, none 중 하나여야 합니다.")
    private String transportMode; // 이동 수단

    @JsonProperty("congestion_level")
    @Min(value = 1, message = "혼잡도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "혼잡도는 5 이하여야 합니다.")
    @NotNull(message = "congestion_level은 필수 입력값입니다.")
    private Integer congestionLevel; // 혼잡도 (1~5)

    @JsonProperty("location")
    @NotBlank(message = "location은 필수 입력값입니다.")
    private String location; // 위치 정보 (예: 강남구)

    @JsonProperty("journal")
    private String journal; // 일기 내용 (선택)
}
