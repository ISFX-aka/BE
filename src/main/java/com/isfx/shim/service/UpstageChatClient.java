package com.isfx.shim.service;

import com.isfx.shim.dto.UpstageChatRequestDto;
import com.isfx.shim.dto.UpstageChatResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class UpstageChatClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private static final String API_URL = "https://api.upstage.ai/v1/chat/completions";
    private static final String MODEL = "solar-1-mini-chat";

    public UpstageChatClient(RestTemplateBuilder restTemplateBuilder,
                            @Value("${api.upstage.key}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    /**
     * Upstage Chat API를 호출하여 AI 응답을 받습니다.
     * 
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @return AI가 생성한 응답 텍스트
     */
    public String generateChatResponse(String systemPrompt, String userPrompt) {
        log.info("[Upstage API] Chat API 호출 시작");
        
        try {
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 메시지 구성
            UpstageChatRequestDto.Message systemMessage = UpstageChatRequestDto.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build();
            
            UpstageChatRequestDto.Message userMessage = UpstageChatRequestDto.Message.builder()
                    .role("user")
                    .content(userPrompt)
                    .build();
            
            // 요청 본문 구성
            UpstageChatRequestDto request = UpstageChatRequestDto.builder()
                    .model(MODEL)
                    .messages(List.of(systemMessage, userMessage))
                    .build();
            
            HttpEntity<UpstageChatRequestDto> entity = new HttpEntity<>(request, headers);
            
            // API 호출
            ResponseEntity<UpstageChatResponseDto> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    UpstageChatResponseDto.class
            );
            
            if (response.getBody() == null) {
                log.error("[Upstage API] 응답 본문이 null입니다");
                throw new RuntimeException("Upstage API response body is null");
            }
            
            String content = response.getBody().getContent();
            if (content == null || content.trim().isEmpty()) {
                log.error("[Upstage API] 응답 내용이 비어있습니다");
                throw new RuntimeException("Upstage API response content is empty");
            }
            
            log.info("[Upstage API] Chat API 호출 성공");
            return content.trim();
            
        } catch (Exception e) {
            log.error("[Upstage API] Chat API 호출 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Upstage Chat API: " + e.getMessage(), e);
        }
    }
}

