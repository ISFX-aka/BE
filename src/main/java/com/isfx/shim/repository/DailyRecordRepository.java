package com.isfx.shim.repository;

import com.isfx.shim.entity.DailyRecord;
import com.isfx.shim.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    // (GET /me/status) 통계 계산을 위한 특정 기간의 기록 조회
    List<DailyRecord> findAllByUserAndRecordDateBetween(User user, LocalDate startDate, LocalDate endDate);

    boolean existsByUserAndRecordDate(User user, LocalDate recordDate);

    Optional<DailyRecord> findByIdAndUser(Long recordId, User user);

    // [추가] 사용자의 모든 기록 조회 (이게 빠져서 에러가 났던 겁니다!)
    List<DailyRecord> findAllByUser(User user);

    // [추가] 회원 탈퇴 시 해당 사용자의 모든 기록 삭제
    void deleteAllByUser(User user);

}