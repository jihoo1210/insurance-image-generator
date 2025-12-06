package com.example.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * 메인 페이지 및 로그인 상태 확인 처리
 */
@Slf4j
@Controller
public class MainController {

    /**
     * 메인 페이지 표시
     *
     * @param model 뷰에 전달할 데이터
     * @return index.mustache
     */
    @GetMapping("/")
    public String index(Model model) {
        checkLogin(model);
        model.addAttribute("message", "보험 이미지 생성 서비스에 오신 것을 환영합니다!");
        return "index";
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok().build();
    }

    /**
     * *** checkLogin 메서드 ***
     * 페이지 요청마다 현재 로그인 상태를 확인하고 Model에 User 객체를 추가합니다.
     *
     * - 로그인: User 객체 추가 (이메일 + 로그아웃 버튼 표시)
     * - 비로그인: User 객체 미추가 (구글 로그인 버튼 표시)
     *
     * @param model 뷰에 전달할 데이터 모델
     */
    public void checkLogin(Model model) {
        try {
            // SecurityContext에서 현재 요청의 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증 여부 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            // OAuth2User 인스턴스 확인
            if (!(authentication.getPrincipal() instanceof OAuth2User)) {
                return;
            }

            // OAuth2User에서 이메일 추출
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");

            if (email == null || email.trim().isEmpty()) {
                return;
            }

            // ✅ 로그인 상태 - User 객체 생성 및 Model에 추가
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("email", email);
            userMap.put("name", oauth2User.getAttribute("name"));

            model.addAttribute("User", userMap);

        } catch (Exception e) {
            // 에러 무시
        }
    }
}
