package com.example.backend.controller;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.entity.Image;
import com.example.backend.service.UserService;
import com.example.backend.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 관련 컨트롤러 (즐겨찾기 등)
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthHelper authHelper;

    /**
     * 사용자 즐겨찾기 목록 조회 페이지
     */
    @GetMapping("/favorites")
    public String getFavorites(Model model) {
        authHelper.checkLogin(model);
        String email = authHelper.getCurrentUserEmail();

        List<ImageListResponse> favorites = userService.getUserFavorites(email);
        model.addAttribute("images", favorites);
        model.addAttribute("pageTitle", "내 즐겨찾기");

        return "favorites";
    }

    /**
     * 이미지 즐겨찾기 저장
     */
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveImage(@RequestBody Map<String, String> request) {
        String s3Key = request.get("s3Key");
        String email = request.get("email");

        if (s3Key == null || s3Key.isEmpty()) {
            log.warn("S3 키가 없습니다");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "S3 키가 필요합니다"
            ));
        }

        try {
            Image savedImage = userService.saveImage(s3Key, email);

            if (savedImage == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미지를 찾을 수 없습니다"
                ));
            }

            log.info("이미지 저장 완료 - ID: {}, S3Key: {}", savedImage.getId(), s3Key);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageId", savedImage.getId());
            response.put("s3Key", savedImage.getS3Key());
            response.put("message", "이미지가 저장되었습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 저장 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이미지 저장에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 즐겨찾기에서 이미지 제거
     */
    @PostMapping("/remove-favorite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFavorite(@RequestBody Map<String, String> request) {
        String s3Key = request.get("s3Key");
        String email = request.get("email");

        if (s3Key == null || s3Key.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "S3 키가 필요합니다"
            ));
        }

        try {
            userService.removeFavorite(s3Key, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "즐겨찾기에서 제거되었습니다"
            ));

        } catch (Exception e) {
            log.error("즐겨찾기 제거 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "제거에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}
