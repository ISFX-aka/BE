package com.isfx.shim.repository;

import com.isfx.shim.entity.WeatherLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherLogRepository extends JpaRepository<WeatherLog, Long> {
}

