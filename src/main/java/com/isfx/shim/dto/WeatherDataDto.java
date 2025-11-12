package com.isfx.shim.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherDataDto {
  private String location;
  private String condition;
  private Double temperature;
  private Short pm10;
  private Short pm25;
  private Short airQualityIndex;
}