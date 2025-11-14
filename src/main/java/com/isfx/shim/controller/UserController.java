package com.isfx.shim.controller;

import com.isfx.shim.dto.UserRequestDto.UserUpdateReqDto;
import com.isfx.shim.dto.UserResponseDto.UserMyInfoGetResDto;
import com.isfx.shim.dto.UserResponseDto.UserStatsGetResDto;
import com.isfx.shim.dto.UserResponseDto.UserUpdateResDto;
import com.isfx.shim.global.common.ApiResponse;
import com.isfx.shim.global.security.UserDetailsImpl;
import com.isfx.shim.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me") // default 경로
public class UserController {

    private final UserService userService;

    /**
     * 프로필 사진 수정 API
     * @param userDetails (중요) JWT 토큰에서 추출된 사용자 정보
     * @param imageFile   업로드할 이미지 파일
     * @return 새로 업로드된 이미지 URL
     */
    @PutMapping("/profile-image")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> updateProfileImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("image") MultipartFile imageFile) {
        // userDetails에서 사용자 ID(Long)를 가져옴
        Long userId = userDetails.getUser().getId();
        String newImageUrl = userService.updateProfileImage(userId, imageFile);
        return ApiResponse.success(Map.of("imageUrl", newImageUrl));
    }

    /**
     * 프로필 사진 삭제 API
     * @param userDetails JWT 토큰에서 추출된 사용자 정보
     * @return 성공 메시지
     */
    @DeleteMapping("/profile-image")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> deleteProfileImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        userService.deleteProfileImage(userId);
        return ApiResponse.success("프로필 이미지가 성공적으로 삭제되었습니다.");
    }

    // GET /api/users/me (내 정보 조회)
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> getMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        UserMyInfoGetResDto response = userService.getUserInfo(userId);
        return ApiResponse.success(response);
    }

    // PUT /api/users/me (내 정보 수정 - 닉네임)
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> updateMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserUpdateReqDto requestDto // @Valid로 DTO 유효성 검사
    ) {
        Long userId = userDetails.getUser().getId();
        UserUpdateResDto response = userService.updateUserInfo(userId, requestDto);
        return ApiResponse.success(response);
    }

    // DELETE /api/users/me (회원 탈퇴)
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK) // (API 명세는 204 No Content지만, 기존 컨트롤러 패턴(ApiResponse)을 따름)
    public ApiResponse<Object> deleteMyAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        userService.deleteUser(userId);
        return ApiResponse.success("회원 탈퇴가 성공적으로 처리되었습니다.");
    }

    // GET /api/users/me/status (내 활동 통계 조회)
    @GetMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> getMyStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("period") String period,
            @RequestParam(value = "date", required = false) String date
    ) {
        Long userId = userDetails.getUser().getId();
        UserStatsGetResDto response = userService.getUserStats(userId, period, date);
        return ApiResponse.success(response);
    }
}