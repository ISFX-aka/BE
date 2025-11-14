package com.isfx.shim.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KmaWeatherResponseDto {
    private final double temperature;
    private final int skyCode;
    private final int precipitationType;
}
