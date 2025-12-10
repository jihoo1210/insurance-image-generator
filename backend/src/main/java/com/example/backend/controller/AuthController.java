package com.example.backend.controller;

import com.example.backend.dto.UserResponseDto;
import com.example.backend.service.UserService;
import com.example.backend.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Map;

/**
 * OAuth2 인증 관련 컨트롤러
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthHelper authHelper;

    /**
     * OAuth2 로그인 성공 후 콜백 처리
     */
    @GetMapping("/oauth2-success")
    public String oauth2Success(Authentication authentication, Model model) {
        log.info("OAuth2 로그인 성공");

        if (authentication instanceof OAuth2AuthenticationToken token) {
            Map<String, Object> attributes = token.getPrincipal().getAttributes();

            String email = (String) attributes.getOrDefault("email", "");
            if (!email.isEmpty()) {
                UserResponseDto response = userService.createUser(email);
                log.info("사용자 생성/조회: {}", email);
            }
        }

        return "redirect:/";
    }

    /**
     * 현재 로그인한 사용자 정보 조회 (API)
     */
    @GetMapping("/user")
    @ResponseBody
    public Map<String, Object> getUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttributes();
        }
        return Collections.emptyMap();
    }

    /**
     * 로그아웃 성공 후 처리
     */
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        log.info("사용자 로그아웃");
        return "redirect:/";
    }
}
