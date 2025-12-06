package com.example.backend.controller;

import com.example.backend.dto.ImageListResponse;
import java.util.List;

import com.example.backend.entity.Image;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final MainController mainController;

    /**
     * 사용자 즐겨찾기 목록 조회 페이지
     * 로그인된 사용자만 접근 가능
     *
     * @param model 뷰에 전달할 데이터
     * @return 즐겨찾기 목록 페이지
     */
    @GetMapping("/favorites")
    public String getFavorites(org.springframework.ui.Model model) {

        // 로그인 상태 확인
        mainController.checkLogin(model);

        // SecurityContext에서 이메일 추출 (파라미터 email이 비어있을 경우)
        String email = "";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                email = oauth2User.getAttribute("email");
            }

        // 사용자 즐겨찾기 조회
        List<ImageListResponse> favorites = userService.getUserFavorites(email);
        model.addAttribute("images", favorites);
        model.addAttribute("pageTitle", "내 즐겨찾기");

        return "favorites";
    }
    /**
     * 이미지 즐겨찾기 저장
     *
     * @param request S3 키와 이메일
     * @return 저장 결과
     */
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveImage(@RequestBody Map<String, String> request) {
        try {
            String s3Key = request.get("s3Key");
            String email = request.get("email");

            if (s3Key == null || s3Key.isEmpty()) {
                log.warn("S3 키가 없습니다");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "S3 키가 필요합니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 이미지 저장
            Image savedImage = userService.saveImage(s3Key, email);

            log.info("이미지 저장 완료 - ID: {}, S3Key: {}", savedImage.getId(), s3Key);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageId", savedImage.getId());
            response.put("s3Key", savedImage.getS3Key());
            response.put("message", "이미지가 저장되었습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 저장 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "이미지 저장에 실패했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 즐겨찾기에서 이미지 제거
     *
     * @param request S3 키와 이메일
     * @return 제거 결과
     */
    @PostMapping("/remove-favorite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFavorite(@RequestBody Map<String, String> request) {
        try {
            String s3Key = request.get("s3Key");
            String email = request.get("email");

            if (s3Key == null || s3Key.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "S3 키가 필요합니다");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            userService.removeFavorite(s3Key, email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "즐겨찾기에서 제거되었습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 제거 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "제거에 실패했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
