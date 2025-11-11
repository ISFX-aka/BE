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
import com.isfx.shim.repository.DailyRecordRepository;
import com.isfx.shim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DailyRecordRepository dailyRecordRepository;

    // @param userId
    // @return User 엔티티
    // @throws CustomException (404 Not Found) (공통) 활성화된 사용자 조회
    private User findActiveUserById(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }


    // GET /api/users/me 내 정보 조회
    public UserMyInfoGetResDto getUserInfo(Long userId) {
        User user = findActiveUserById(userId);
        return UserMyInfoGetResDto.fromEntity(user);
    }


    // PUT /api/users/me 내 정보(닉네임) 수정
    @Transactional // 쓰기 작업
    public UserUpdateResDto updateUserInfo(Long userId, UserUpdateReqDto requestDto) {

        // 닉네임 중복 검사 (409 Conflict)
        if (userRepository.existsByName(requestDto.getName())) {
            throw new CustomException(ErrorCode.CONFLICT_NICKNAME);
        }

        // 사용자 조회 (404 Not Found)
        User user = findActiveUserById(userId);

        // 닉네임 변경 (Entity의 비즈니스 메서드 호출)
        user.updateName(requestDto.getName());

        // (변경 감지) @Transactional이 종료되면서 자동 save, 명시적으로 save를 호출할 필요 없음.
        return UserUpdateResDto.fromEntity(user);
    }


    /**
     * DELETE /api/users/me
     * 회원 탈퇴 (논리 삭제)
     */
    @Transactional // 쓰기 작업
    public void deleteUser(Long userId) {
        User user = findActiveUserById(userId);
        user.softDelete(); // Entity의 논리 삭제 메서드 호출
        // (변경 감지) 자동 save
    }


    /**
     * GET /api/users/me/status
     * 내 활동 통계 조회
     */
    public UserStatsGetResDto getUserStats(Long userId, String period, String dateStr) {

        // 1. 사용자 조회
        User user = findActiveUserById(userId);

        // 2. 날짜 파라미터 처리
        LocalDate date = (dateStr == null || dateStr.isEmpty()) ? LocalDate.now() : LocalDate.parse(dateStr);
        LocalDate startDate, endDate;

        if ("week".equals(period)) {
            // (간단한 예시: 해당 주의 월~일)
            startDate = date.with(java.time.DayOfWeek.MONDAY);
            endDate = date.with(java.time.DayOfWeek.SUNDAY);
        } else if ("month".equals(period)) {
            // (해당 월의 1일 ~ 말일)
            startDate = date.withDayOfMonth(1);
            endDate = date.withDayOfMonth(date.lengthOfMonth());
        } else {
            // 400 Bad Request
            throw new CustomException(ErrorCode.INVALID_PERIOD_REQUEST);
        }

        // 3. Repository에서 데이터 조회
        List<DailyRecord> records = dailyRecordRepository.findAllByUserAndRecordDateBetween(user, startDate, endDate);

        // 4. 통계 계산
        int recordCount = records.size();
        double averageEnergyScore = records.stream()
                .mapToDouble(DailyRecord::getEnergyScore)
                .average()
                .orElse(0.0);

        // 5. DTO로 변환
        List<EnergyTrendDto> energyTrend = records.stream()
                .map(EnergyTrendDto::fromEntity)
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 빌드
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