package com.isfx.shim.config;

import com.isfx.shim.global.security.JwtAuthenticationFilter;
import com.isfx.shim.global.security.JwtUtil;
import com.isfx.shim.global.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    // 1. 인증 필터 Bean으로 등록
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    // 2. SecurityFilterChain 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 설정 비활성화 (JWT 사용 시 불필요)
        http.csrf((csrf) -> csrf.disable());

        // Session 정책 설정: STATELESS (세션 사용 안 함)
        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 3. URL별 인가(Authorization) 설정
        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        // 소셜 로그인 API 모두 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger UI 관련 경로 모두 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 그 외 모든 요청은 인증(토큰) 필요
                        .anyRequest().authenticated()
        );

        // 4. JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter (기본 로그인 필터) 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}