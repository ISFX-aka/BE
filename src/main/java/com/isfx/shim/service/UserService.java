package com.isfx.shim.service;

import com.isfx.shim.dto.UserRequestDto.UserUpdateReqDto;
import com.isfx.shim.dto.UserResponseDto.UserMyInfoGetResDto;
import com.isfx.shim.dto.UserResponseDto.UserStatsGetResDto;
import com.isfx.shim.dto.UserResponseDto.UserUpdateResDto;
import com.isfx.shim.dto.UserResponseDto.EnergyTrendDto;
import com.isfx.shim.entity.DailyRecord;
import com.isfx.shim.entity.User;
import com.isfx.shim.global.exception.CustomException;
import com.isfx.shim.global.exception.ErrorCode;
import com.isfx.shim.global.util.S3Util;
import com.isfx.shim.repository.DailyRecordRepository;
import com.isfx.shim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final S3Util s3Util;
    private final DailyRecordRepository dailyRecordRepository; // [추가] 통계용

    /**
     * 프로필 사진 수정
     * @param userId 현재 로그인한 사용자의 ID
     * @param imageFile 새로 업로드할 이미지 파일
     * @return 새로 업로드된 이미지의 URL
     */
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile imageFile) {
        // 1. 사용자 조회
        // [수정] findById -> findActiveUserById
        User user = findActiveUserById(userId);

        // 2. 기존 프로필 이미지가 있다면 S3에서 삭제
        String oldImageUrl = user.getProfileImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            s3Util.deleteFile(oldImageUrl);
        }

        // 3. 새 이미지를 S3에 업로드
        String newImageUrl = s3Util.uploadFile(imageFile, "profile");

        // 4. 사용자의 profileImageUrl 필드를 새 URL로 업데이트 (DB)
        user.updateProfileImage(newImageUrl);

        log.info("프로필 사진 업데이트 성공: userId={}, newImageUrl={}", userId, newImageUrl);
        return newImageUrl;
    }

    /**
     * 프로필 사진 삭제
     * @param userId 현재 로그인한 사용자의 ID
     */
    @Transactional
    public void deleteProfileImage(Long userId) {
        // 1. 사용자 조회
        // [수정] findById -> findActiveUserById
        User user = findActiveUserById(userId);

        // 2. 기존 프로필 이미지가 있다면 S3에서 삭제
        String oldImageUrl = user.getProfileImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            s3Util.deleteFile(oldImageUrl);
        }

        // 3. 사용자의 profileImageUrl 필드를 null로 업데이트 (DB)
        user.updateProfileImage(null);

        log.info("프로필 사진 삭제 성공: userId={}", userId);
    }

    // (공통 메서드) ID로 사용자 찾기
    // [수정] 404 처리를 위해 findByIdAndIsActiveTrue 사용
    private User findActiveUserById(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // GET /api/users/me (내 정보 조회)
    public UserMyInfoGetResDto getUserInfo(Long userId) {
        User user = findActiveUserById(userId);
        return UserMyInfoGetResDto.fromEntity(user);
    }

    // PUT /api/users/me (내 정보 수정 - 닉네임)
    @Transactional
    public UserUpdateResDto updateUserInfo(Long userId, UserUpdateReqDto requestDto) {
        // 닉네임 중복 검사 (409 Conflict)
        if (userRepository.existsByName(requestDto.getName())) {
            throw new CustomException(ErrorCode.CONFLICT_NICKNAME);
        }

        // 사용자 조회 (404 Not Found)
        User user = findActiveUserById(userId);

        // 닉네임 변경 (Entity의 비즈니스 메서드 호출)
        user.updateName(requestDto.getName());
        // (@Transactional로 인해 자동 저장)

        return UserUpdateResDto.fromEntity(user);
    }

    // DELETE /api/users/me (회원 탈퇴 - 논리 삭제)

    @Transactional
    public void deleteUser(Long userId) {
        User user = findActiveUserById(userId);
        user.softDelete(); // Entity의 논리 삭제 메서드 호출
        // (@Transactional로 인해 자동 저장)
    }

    // GET /api/users/me/status (내 활동 통계 조회)

    public UserStatsGetResDto getUserStats(Long userId, String period, String dateStr) {
        // 사용자 조회
        User user = findActiveUserById(userId);

        // 날짜 파라미터 처리
        LocalDate date = (dateStr == null || dateStr.isEmpty()) ? LocalDate.now() : LocalDate.parse(dateStr);
        LocalDate startDate, endDate;

        if ("week".equals(period)) {
            startDate = date.with(DayOfWeek.MONDAY);
            endDate = date.with(DayOfWeek.SUNDAY);
        } else if ("month".equals(period)) {
            startDate = date.withDayOfMonth(1);
            endDate = date.withDayOfMonth(date.lengthOfMonth());
        } else {
            // 400 Bad Request
            throw new CustomException(ErrorCode.INVALID_PERIOD_REQUEST);
        }

        // Repository에서 데이터 조회
        List<DailyRecord> records = dailyRecordRepository.findAllByUserAndRecordDateBetween(user, startDate, endDate);

        // 통계 계산 (BigDecimal 사용)
        int recordCount = records.size();
        // [수정] double 기반으로 수정
        double totalScore = records.stream()
                .mapToDouble(DailyRecord::getEnergyScore) // (Fix 1)
                .sum();

        double averageEnergyScore;
        if (recordCount > 0) {
            // [수정] API 명세(소수점 2자리)를 맞추기 위해 BigDecimal로 '계산만' 수행
            BigDecimal totalDecimal = BigDecimal.valueOf(totalScore);
            BigDecimal countDecimal = BigDecimal.valueOf(recordCount);
            averageEnergyScore = totalDecimal.divide(countDecimal, 2, RoundingMode.HALF_UP).doubleValue();
        } else {
            averageEnergyScore = 0.0;
        }

        // DTO로 변환
        List<EnergyTrendDto> energyTrend = records.stream()
                .map(record -> EnergyTrendDto.builder()
                        .recordId(record.getId())
                        .recordDate(record.getRecordDate().toString())
                        .energyScore(record.getEnergyScore()) // (Fix 2) .doubleValue() 삭제
                        .build())
                .collect(Collectors.toList());

        // 최종 응답 DTO 빌드
        return UserStatsGetResDto.builder()
                .userId(userId)
                .period(period)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .averageEnergyScore(averageEnergyScore)
                .recordCount(recordCount)
                .energyTrend(energyTrend)
                .build();
    }
}