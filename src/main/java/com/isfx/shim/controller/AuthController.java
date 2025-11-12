package com.isfx.shim.controller;

import com.isfx.shim.dto.KakaoLoginRequestDto;
import com.isfx.shim.global.common.ApiResponse;
import com.isfx.shim.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 소셜 로그인 API
     * @param requestDto 클라이언트가 보낸 '인가 코드'
     * @return 우리 서비스의 JWT 토큰
     */
    @PostMapping("/social/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> kakaoLogin(@RequestBody KakaoLoginRequestDto requestDto) {
        String jwtToken = authService.kakaoLogin(requestDto);

        // JWT 토큰을 JSON 형태로 반환
        return ApiResponse.success(Map.of("token", jwtToken));
    }
}