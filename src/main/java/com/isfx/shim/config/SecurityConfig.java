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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. CSRF 비활성화
        http.csrf((csrf) -> csrf.disable());

        // 2. CORS 설정 추가
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 3. Session 비활성화
        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 4. URL별 권한 설정
        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
        );

        // 5. 필터 추가
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 6. CORS 허용 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 주소 (리스트로 담을 수 있음)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",       // 로컬 React
                "http://3.36.228.115:8080",     // 배포된 서버 (필요시),
                "http://localhost:5173",      // 로컬 Vite
                "https://2025-ganzi-thon-isfx-fe.vercel.app" // 배포 프론트
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 쿠키/인증 정보 포함 허용 (JWT 사용 시 필수)
        configuration.setAllowCredentials(true);

        // 브라우저가 이 헤더를 읽을 수 있게 허용 (JWT 토큰 등을 헤더로 보낼 때 필요)
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}