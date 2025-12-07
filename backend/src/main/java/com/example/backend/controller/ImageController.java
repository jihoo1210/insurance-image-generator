package com.example.backend.controller;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.service.ImgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.List;

/**
 * 이미지 생성 요청을 처리하는 컨트롤러
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ImageController {

    private final ImgService imgService;
    private final S3Client s3Client;
    private final MainController mainController;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * 이미지 생성 요청 처리 (POST)
     * 텍스트만으로 생성 또는 이미지 첨부하여 생성
     *
     * @param prompt 이미지 생성을 위한 프롬프트
     * @param email 사용자 이메일
     * @param attachImage 첨부된 이미지 파일 (선택사항)
     * @param model 뷰에 전달할 데이터
     * @return index.mustache (결과 포함)
     */
    @PostMapping("/generate")
    public String generateImage(
            @RequestParam(name = "prompt") String prompt,
            @RequestParam(name = "email", required = false, defaultValue = "") String email,
            @RequestParam(name = "attachImage", required = false) MultipartFile attachImage,
            Model model) {

        log.info("이미지 생성 요청 - Prompt: {}, Email: {}, 첨부파일: {}",
                prompt, email, attachImage != null && !attachImage.isEmpty() ? attachImage.getOriginalFilename() : "없음");

        // *** 로그인 상태 확인 및 User 객체 추가 ***
        mainController.checkLogin(model);

        try {
            String s3Key;
            String userEmail = StringUtils.hasText(email) ? email : "비회원";

            // 첨부된 이미지가 있으면 해당 이미지를 기반으로 생성, 없으면 텍스트만으로 생성
            if (attachImage != null && !attachImage.isEmpty()) {
                s3Key = imgService.generateImageWithAttachment(prompt, userEmail, attachImage);
                log.info("첨부 이미지 기반 이미지 생성 완료");
            } else {
                s3Key = imgService.generateImage(prompt, userEmail);
                log.info("텍스트 기반 이미지 생성 완료");
            }

            // 결과 처리 - 성공/실패 여부에 따라
            if (s3Key != null) {
                // Presigned URL 생성 (S3에서 직접 이미지 제공)
                String presignedUrl = imgService.generateS3Url(s3Key);

                model.addAttribute("success", true);
                model.addAttribute("s3Key", s3Key);
                model.addAttribute("imageUrl", presignedUrl != null ? presignedUrl : "/download/" + s3Key);
                model.addAttribute("message", "이미지 생성 성공!");
                log.info("✅ 이미지 생성 성공 - S3 키: {}", s3Key);
            } else {
                model.addAttribute("success", false);
                model.addAttribute("message", "이미지 생성에 실패했습니다.");
                log.warn("❌ 이미지 생성 실패 - S3 키가 null");
            }

        } catch (ImgService.QuotaExceededException e) {
            log.warn("⚠️ API 할당량 초과: {}", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", "⚠️ " + e.getMessage());
            model.addAttribute("isQuotaExceeded", true);
            model.addAttribute("retryAfterMillis", e.getRetryAfterMillis());

        } catch (Exception e) {
            log.error("❌ 이미지 생성 중 예외 발생: {}", e.getClass().getSimpleName(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "오류 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        model.addAttribute("prompt", prompt);

        return "index";
    }

    /**
     * S3에서 파일 다운로드
     *
     * @param s3Key S3 객체 키
     * @return 파일 바이너리 데이터
     */
    @GetMapping("/download/{s3Key}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String s3Key) {
        try {
            log.info("이미지 다운로드 요청 - S3 Key: {}", s3Key);

            // S3에서 객체 읽기
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                byte[] imageData = inputStream.readAllBytes();
                log.info("이미지 다운로드 성공 - 크기: {} bytes", imageData.length);

                return ResponseEntity.ok()
                        .header("Content-Type", "image/png")
                        .header("Content-Disposition", "inline; filename=\"" + s3Key + "\"")
                        .body(imageData);
            }

        } catch (Exception e) {
            log.error("이미지 다운로드 중 오류 - S3 Key: {}, 오류: {}", s3Key, e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    public String showImageList(Model model) {
        mainController.checkLogin(model);

        // 현재 로그인한 사용자 이메일 추출
        String userEmail = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                userEmail = oauth2User.getAttribute("email");
            }
        }

        List<ImageListResponse> imageList = imgService.getAllImages(userEmail);
        model.addAttribute("images", imageList);
        return "imagelist";
    }
}

