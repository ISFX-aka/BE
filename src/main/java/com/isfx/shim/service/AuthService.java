package com.isfx.shim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isfx.shim.dto.KakaoLoginRequestDto;
import com.isfx.shim.dto.KakaoUserInfoResponseDto;
import com.isfx.shim.entity.enums.Role;
import com.isfx.shim.entity.User;
import com.isfx.shim.global.exception.CustomException;
import com.isfx.shim.global.exception.ErrorCode;
import com.isfx.shim.global.security.JwtUtil;
import com.isfx.shim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate(); // 외부 API 통신용
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱용

    // application.yml에서 카카오 설정값 읽어오기
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String kakaoTokenUri;
    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUri;


    @Transactional
    public String kakaoLogin(KakaoLoginRequestDto requestDto) {
        // 1. 인가 코드로 카카오 액세스 토큰 받기
        String accessToken = getKakaoAccessToken(requestDto.getCode());

        // 2. 액세스 토큰으로 카카오 사용자 정보 받기
        KakaoUserInfoResponseDto userInfoDto = getKakaoUserInfo(accessToken);

        // 3. 사용자 정보로 DB 조회 및 신규 회원 가입
        User user = registerOrLoginUser(userInfoDto);

        // 4. 우리 서비스의 JWT 토큰 발급
        return jwtUtil.createToken(user.getEmail()); // (중요) 토큰의 Subject는 email로
    }

    // 1. 카카오 액세스 토큰 요청
    private String getKakaoAccessToken(String code) {
        log.info("인가 코드로 액세스 토큰 요청: {}", code);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", code);
        body.add("client_secret", kakaoClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    kakaoTokenUri, HttpMethod.POST, request, String.class);

            // JSON 응답에서 access_token만 파싱
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 받기 실패", e);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 2. 카카오 사용자 정보 요청
    private KakaoUserInfoResponseDto getKakaoUserInfo(String accessToken) {
        log.info("액세스 토큰으로 사용자 정보 요청: {}", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    kakaoUserInfoUri, HttpMethod.GET, request, String.class);

            // JSON 응답을 DTO로 파싱
            return objectMapper.readValue(response.getBody(), KakaoUserInfoResponseDto.class);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 받기 실패", e);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 3. 사용자 등록 또는 로그인 처리
    private User registerOrLoginUser(KakaoUserInfoResponseDto userInfo) {
        String email = userInfo.getKakaoAccount().getEmail();
        if (email == null) {
            log.error("카카오 응답에 이메일이 없습니다. 동의 항목을 확인하세요.");
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String nickname = userInfo.getKakaoAccount().getProfile().getNickname();
        String profileImageUrl = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();

        // 1. 이메일로 기존 회원인지 확인
        return userRepository.findByEmail(email).orElseGet(() -> {
            // 2. 신규 회원이면 DB에 저장 (자동 회원가입)
            log.info("신규 회원 자동 가입: {}", email);
            User newUser = User.builder()
                    .email(email)
                    .name(nickname)
                    .profileImageUrl(profileImageUrl)
                    .role(Role.ROLE_USER) // 기본 권한
                    .build();
            return userRepository.save(newUser);
        });
    }
}