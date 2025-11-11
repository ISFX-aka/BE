package com.isfx.shim.controller;

import com.isfx.shim.global.common.ApiResponse;
import com.isfx.shim.global.security.UserDetailsImpl;
import com.isfx.shim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
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
}