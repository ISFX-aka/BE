package com.isfx.shim.controller;

import com.isfx.shim.dto.CreateRecordRequest;
import com.isfx.shim.dto.CreateRecordResponseDto;
import com.isfx.shim.dto.UpdateRecordRequest;
import com.isfx.shim.global.common.ApiResponse;
import com.isfx.shim.global.security.UserDetailsImpl;
import com.isfx.shim.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    /**
     * 오늘의 기록 생성 API
     * @param userDetails JWT 토큰에서 추출된 사용자 정보
     * @param request 기록 생성 요청 DTO
     * @return 생성된 기록 응답 DTO
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateRecordResponseDto> createRecord(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateRecordRequest request) {
        
        Long userId = userDetails.getUser().getId();
        CreateRecordResponseDto response = recordService.createRecord(userId, request);
        
        return ApiResponse.created(response);
    }

    /**
     * 기록 수정 API
     */
    @PutMapping("/{recordId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateRecordResponseDto> updateRecord(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("recordId") Long recordId,
            @Valid @RequestBody UpdateRecordRequest request) {

        Long userId = userDetails.getUser().getId();
        CreateRecordResponseDto response = recordService.updateRecord(userId, recordId, request);

        return ApiResponse.success(response);
    }
}
