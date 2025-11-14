package com.isfx.shim.repository;

import com.isfx.shim.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // [추가] 409 닉네임 중복 검사
    boolean existsByName(String name);

    // [추가] 논리 삭제(is_active)를 고려한 사용자 조회
    Optional<User> findByIdAndIsActiveTrue(Long userId);
}