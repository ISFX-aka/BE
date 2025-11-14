package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class RecordSummaryDto {

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("record_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @JsonProperty("energy_score")
    private Double energyScore;
}

