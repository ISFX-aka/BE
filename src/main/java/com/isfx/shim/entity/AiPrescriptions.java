package com.isfx.shim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.isfx.shim.entity.enums.AiPrescriptionCategory;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_prescriptions")
public class AiPrescriptions {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "prescription_id")
  private Long prescription_id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_id", nullable = false)
  private DailyRecord record;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private AiPrescriptionCategory category;

  @Column(name = "recommendation_text", nullable = false, columnDefinition = "TEXT")
  private String recommendationText;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  
}
