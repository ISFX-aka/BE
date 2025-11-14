package com.isfx.shim.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AirQualityResponseDto {
    private final Short pm10;
    private final Short pm25;
    private final Short airQualityIndex;
}
