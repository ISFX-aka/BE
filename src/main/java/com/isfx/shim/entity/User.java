package com.isfx.shim.entity;

import com.isfx.shim.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.ROLE_USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String name, String email, String profileImageUrl, Role role) {
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.role = (role != null) ? role : Role.ROLE_USER;
        this.isActive = true;
    }

    // 닉네임 수정
    public void updateName(String name) {
        this.name = name;
    }

    // 회원탈퇴
    public void softDelete() {
        this.isActive = false;
    }

    // 프로필 사진 수정
    public void updateProfileImage(String newImageUrl) {
        this.profileImageUrl = newImageUrl;
    }
}