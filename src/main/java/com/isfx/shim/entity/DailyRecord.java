package com.isfx.shim.entity;

import com.isfx.shim.entity.enums.EnergyLevel;
import com.isfx.shim.entity.enums.TimePeriod;
import com.isfx.shim.entity.enums.TransportMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "daily_records")
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // (WeatherLog 엔티티가 있다는 가정 하에)
    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "weather_log_id", nullable = false)
    // private WeatherLog weatherLog;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_period", nullable = false)
    private TimePeriod timePeriod;

    @Column(name = "emotion_level", nullable = false)
    private Integer emotionLevel;

    @Column(name = "conversation_level", nullable = false)
    private Integer conversationLevel;

    @Column(name = "meeting_count", nullable = false)
    private Integer meetingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TransportMode transportMode;

    @Column(name = "congestion_level")
    private Integer congestionLevel;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String journal;

    @Column(name = "energy_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal energyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_level", nullable = false)
    private EnergyLevel energyLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}