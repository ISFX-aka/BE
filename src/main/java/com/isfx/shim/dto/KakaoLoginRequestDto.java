package com.isfx.shim.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {
    private String code; // 카카오로부터 받은 인가 코드
}