package com.isfx.shim.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 헤더에서 토큰 가져오기
        String tokenValue = jwtUtil.getTokenFromHeader(request);

        // 2. 토큰 유효성 검사
        if (StringUtils.hasText(tokenValue)) { // 토큰이 존재할 때만 검증
            if (jwtUtil.validateToken(tokenValue)) { // 토큰이 유효할 때
                Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

                try {
                    // 3. 토큰에서 사용자 정보(email)를 가져와 UserDetails 객체 생성
                    UserDetails userDetails = userDetailsService.loadUserByUsername(info.getSubject());

                    // 4. 인증(Authentication) 객체 생성 및 SecurityContext에 저장
                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);

                } catch (Exception e) {
                    log.error("사용자를 찾을 수 없습니다.");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}