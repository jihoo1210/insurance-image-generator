package com.example.backend.controller;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.service.ImgService;
import com.example.backend.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이미지 생성 요청을 처리하는 컨트롤러
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ImageController {

    private final ImgService imgService;
    private final S3Client s3Client;
    private final AuthHelper authHelper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * 이미지 생성 요청 처리 (POST)
     */
    @PostMapping("/generate")
    public String generateImage(
            @RequestParam(name = "prompt") String prompt,
            @RequestParam(name = "email", required = false, defaultValue = "") String email,
            @RequestParam(name = "attachImage", required = false) MultipartFile attachImage,
            Model model) {

        log.info("이미지 생성 요청 - Prompt: {}, Email: {}, 첨부파일: {}",
                prompt, email, attachImage != null && !attachImage.isEmpty() ? attachImage.getOriginalFilename() : "없음");

        authHelper.checkLogin(model);

        try {
            String s3Key;
            String userEmail = StringUtils.hasText(email) ? email : "비회원";

            if (attachImage != null && !attachImage.isEmpty()) {
                s3Key = imgService.generateImageWithAttachment(prompt, userEmail, attachImage);
                log.info("첨부 이미지 기반 이미지 생성 완료");
            } else {
                s3Key = imgService.generateImage(prompt, userEmail);
                log.info("텍스트 기반 이미지 생성 완료");
            }

            if (s3Key != null) {
                String presignedUrl = imgService.generateS3Url(s3Key);

                model.addAttribute("success", true);
                model.addAttribute("s3Key", s3Key);
                model.addAttribute("imageUrl", presignedUrl != null ? presignedUrl : "/download/" + s3Key);
                model.addAttribute("message", "이미지 생성 성공!");
                log.info("이미지 생성 성공 - S3 키: {}", s3Key);
            } else {
                model.addAttribute("success", false);
                model.addAttribute("message", "이미지 생성에 실패했습니다.");
                log.warn("이미지 생성 실패 - S3 키가 null");
            }

        } catch (ImgService.QuotaExceededException e) {
            log.warn("API 할당량 초과: {}", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
            model.addAttribute("isQuotaExceeded", true);
            model.addAttribute("retryAfterMillis", e.getRetryAfterMillis());

        } catch (Exception e) {
            log.error("이미지 생성 중 예외 발생: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "오류 발생: " + e.getMessage());
        }

        model.addAttribute("prompt", prompt);

        return "index";
    }

    /**
     * S3에서 파일 다운로드
     */
    @GetMapping("/download/{s3Key}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String s3Key) {
        try {
            log.info("이미지 다운로드 요청 - S3 Key: {}", s3Key);

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
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    public String showImageList(
            @PageableDefault(size = 8, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        authHelper.checkLogin(model);
        String userEmail = authHelper.getCurrentUserEmail();

        Page<ImageListResponse> imagePage = imgService.getPagedImages(pageable, userEmail);

        // Spring Page 객체의 메서드 직접 활용
        int currentPageIndex = imagePage.getNumber(); // 0-based (Spring Pageable용)
        int currentPageDisplay = currentPageIndex + 1; // 1-based (화면 표시용)
        int totalPages = imagePage.getTotalPages();

        // 5개 페이지 버튼 계산 (표시용 1-based)
        List<Map<String, Object>> pageNumbers = new ArrayList<>();
        int startPage = Math.max(1, currentPageDisplay - 2);
        int endPage = Math.min(totalPages, startPage + 4);

        // startPage 재조정 (끝에서 5개 미만일 경우)
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        for (int i = startPage; i <= endPage; i++) {
            Map<String, Object> pageNum = new HashMap<>();
            pageNum.put("pageNumber", i - 1); // 0-based for URL
            pageNum.put("displayNumber", i); // 1-based for display
            pageNum.put("isCurrentPage", i == currentPageDisplay);
            pageNumbers.add(pageNum);
        }

        model.addAttribute("images", imagePage.getContent());
        model.addAttribute("currentPage", currentPageDisplay); // 화면 표시용
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", imagePage.getTotalElements());
        model.addAttribute("hasPrevious", imagePage.hasPrevious());
        model.addAttribute("hasNext", imagePage.hasNext());
        model.addAttribute("previousPage", currentPageIndex - 1); // 0-based
        model.addAttribute("nextPage", currentPageIndex + 1); // 0-based
        model.addAttribute("firstPage", 0); // 0-based
        model.addAttribute("lastPage", totalPages - 1); // 0-based
        model.addAttribute("showFirstPage", true);
        model.addAttribute("showLastPage", true);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("hasPagination", totalPages > 1);

        return "imagelist";
    }
}
