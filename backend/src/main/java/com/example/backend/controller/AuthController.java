package com.example.backend.controller;

import com.example.backend.dto.UserResponseDto;
import com.example.backend.service.UserService;
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
 * OAuth2 인증 관련 엔드포인트를 처리하는 컨트롤러
 * Mustache 템플릿을 렌더링하므로 @Controller 사용
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final MainController mainController;

    /**
     * OAuth2 로그인 성공 후 콜백 처리
     * Google 로그인 완료 후 호출됨
     *
     * @param authentication 로그인한 사용자 정보
     * @param model 뷰에 전달할 데이터
     * @return 메인 페이지로 리다이렉트 (index 페이지)
     */
    @GetMapping("/oauth2-success")
    public String oauth2Success(Authentication authentication, Model model) {
        log.info("OAuth2 로그인 성공");

        // *** 로그인 상태 확인 및 User 객체 추가 ***
        mainController.checkLogin(model);

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            Map<String, Object> attributes = token.getPrincipal().getAttributes();

            String email = (String) attributes.getOrDefault("email", "");
            if (!email.isEmpty()) {
                UserResponseDto response = userService.createUser(email);
                model.addAttribute("message", response.getEmail() + "님 환영합니다!");
                log.info("사용자 생성/조회: {}", email);
            }
        }

        return "index";
    }

    /**
     * 현재 로그인한 사용자 정보 조회 (API)
     * JSON으로 반환하므로 @ResponseBody 사용
     *
     * @param authentication 로그인한 사용자 정보
     * @return 사용자 정보 (JSON)
     */
    @GetMapping("/user")
    @ResponseBody
    public Map<String, Object> getUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            return token.getPrincipal().getAttributes();
        }

        return Collections.emptyMap();
    }

    /**
     * 로그아웃 성공 후 처리
     * Spring Security가 자동으로 로그아웃 처리하므로 리다이렉트만 수행
     *
     * @return 홈 페이지로 리다이렉트
     */
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        log.info("사용자 로그아웃");
        return "redirect:/";
    }
}

