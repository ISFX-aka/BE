package com.isfx.shim.repository;

import com.isfx.shim.entity.AiPrescriptions;
import com.isfx.shim.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPrescriptionsRepository extends JpaRepository<AiPrescriptions, Long> {

    Optional<AiPrescriptions> findByRecord(DailyRecord record);
}

