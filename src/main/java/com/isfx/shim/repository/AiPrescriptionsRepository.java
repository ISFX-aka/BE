package com.isfx.shim.repository;

import com.isfx.shim.entity.AiPrescriptions;
import com.isfx.shim.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiPrescriptionsRepository extends JpaRepository<AiPrescriptions, Long> {

    Optional<AiPrescriptions> findByRecord(DailyRecord record);
    // [추가] 회원 탈퇴 시, 해당 기록들에 연결된 AI 처방을 한 번에 삭제하는 메서드
    // (엔티티 필드명이 'record'라서 'ByRecordIn'이어야 함)
    void deleteAllByRecordIn(List<DailyRecord> records);
}

