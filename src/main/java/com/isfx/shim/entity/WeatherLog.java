package com.isfx.shim.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

import com.isfx.shim.entity.enums.WeatherCondition;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "weather_logs")
public class WeatherLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "weather_log_id")
  private Long weather_log_id;

  @Column(name = "location", nullable = false, length = 255)
  private String location;

  @Column(name = "observed_at", nullable = false)
  private LocalDateTime observed_at;

  @Column(name = "temperature", nullable = false)
  private Double temperature;

  @Enumerated(EnumType.STRING)
  @Column(name = "weather_condition", nullable = false, columnDefinition = "ENUM('clear', 'clouds', 'rain', 'snow', 'other')")
  private WeatherCondition condition;

  @Column(name = "pm10", nullable = false)
  private Short pm10;

  @Column(name = "pm25", nullable = false)
  private Short pm25;

  @Column(name = "air_quality_index", nullable = false)
  private Short air_quality_index;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  public WeatherLog(String location, LocalDateTime observed_at, Double temperature,
                   WeatherCondition condition, Short pm10, Short pm25, Short air_quality_index) {
    this.location = location;
    this.observed_at = observed_at;
    this.temperature = temperature;
    this.condition = condition;
    this.pm10 = pm10;
    this.pm25 = pm25;
    this.air_quality_index = air_quality_index;
  }
}
