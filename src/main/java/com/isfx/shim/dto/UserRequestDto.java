package com.isfx.shim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRequestDto {

    // (PUT /api/users/me) 회원 정보 수정 요청 DTO
    @Getter
    @Setter // @RequestBody 매핑을 위해
    @NoArgsConstructor
    public static class UserUpdateReqDto {

        @NotBlank(message = "'name' 값이 비어있거나 유효하지 않습니다.")
        private String name;
    }
}