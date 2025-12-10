package com.example.backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 공통 유틸리티
 */
@Component
public class AuthHelper {

    /**
     * 현재 로그인 상태를 확인하고 Model에 User 객체를 추가
     *
     * @param model 뷰에 전달할 데이터 모델
     */
    public void checkLogin(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        if (!(authentication.getPrincipal() instanceof OAuth2User)) {
            return;
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        if (email == null || email.trim().isEmpty()) {
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("name", oauth2User.getAttribute("name"));

        model.addAttribute("User", userMap);
    }

    /**
     * 현재 로그인한 사용자의 이메일 반환
     *
     * @return 로그인한 사용자 이메일, 없으면 null
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                return oauth2User.getAttribute("email");
            }
        }

        return null;
    }
}
