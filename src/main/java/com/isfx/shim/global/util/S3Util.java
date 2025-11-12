package com.isfx.shim.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.isfx.shim.global.exception.CustomException;
import com.isfx.shim.global.exception.ErrorCode;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Util {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 파일 업로드
     */
    public String uploadFile(MultipartFile multipartFile, String dirName) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        String originalFileName = multipartFile.getOriginalFilename();
        String uniqueFileName = dirName + "/" + UUID.randomUUID() + "_" + originalFileName;

        try (InputStream inputStream = multipartFile.getInputStream()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(uniqueFileName)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, multipartFile.getSize())
            );

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return generateFileUrl(uniqueFileName);
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // bucket/key 형식으로 파싱
            String bucketDomain = "amazonaws.com/" + bucket + "/";
            int index = fileUrl.indexOf(bucketDomain);
            if (index == -1) {
                log.error("잘못된 S3 URL 형식: {}", fileUrl);
                return;
            }

            String fileKey = fileUrl.substring(index + bucketDomain.length());

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류: {}", fileUrl, e);
        }
    }

    private String generateFileUrl(String fileKey) {
        String encodedKey = URLEncoder.encode(fileKey, StandardCharsets.UTF_8);
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, encodedKey);
    }
}
