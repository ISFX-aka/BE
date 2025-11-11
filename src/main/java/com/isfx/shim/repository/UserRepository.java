package com.isfx.shim.repository;

import com.isfx.shim.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // (GET /me, PUT /me, DELETE /me) 논리 삭제되지 않은 사용자 조회
    Optional<User> findByIdAndIsActiveTrue(Long userId);

    // (PUT /me) 닉네임 중복 검사 (409 Conflict)
    boolean existsByName(String name);
}