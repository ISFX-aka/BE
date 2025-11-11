package com.isfx.shim.global.security;

import com.isfx.shim.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    // (Spring Security가 관리할 유저 정보)

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
    }

    // (UserDetails 인터페이스의 나머지 메서드 구현)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 권한 관련 로직
    }

    @Override
    public String getPassword() {
        return null; // (JWT 방식에서는 null일 수 있음)
    }

    @Override
    public String getUsername() {
        return this.email; // (ID로 email 사용)
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}