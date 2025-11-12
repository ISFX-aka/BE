package com.isfx.shim.service;

import com.isfx.shim.entity.User;
import com.isfx.shim.global.exception.CustomException;
import com.isfx.shim.global.exception.ErrorCode;
import com.isfx.shim.global.util.S3Util;
import com.isfx.shim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final S3Util s3Util;

    /**
     * 프로필 사진 수정
     * @param userId 현재 로그인한 사용자의 ID
     * @param imageFile 새로 업로드할 이미지 파일
     * @return 새로 업로드된 이미지의 URL
     */
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile imageFile) {
        // 1. 사용자 조회
        User user = findUserById(userId);

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
        User user = findUserById(userId);

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
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}