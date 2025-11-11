package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoResponseDto {

    @JsonProperty("id")
    private Long id; // 카카오 고유 ID

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        @JsonProperty("email")
        private String email;

        @JsonProperty("profile")
        private Profile profile;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        @JsonProperty("nickname")
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }
}