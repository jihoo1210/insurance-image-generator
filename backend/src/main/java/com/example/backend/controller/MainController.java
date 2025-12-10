package com.example.backend.controller;

import com.example.backend.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 메인 페이지 컨트롤러
 */
@RequiredArgsConstructor
@Controller
public class MainController {

    private final AuthHelper authHelper;

    /**
     * 메인 페이지 표시
     *
     * @param model 뷰에 전달할 데이터
     * @return index.mustache
     */
    @GetMapping("/")
    public String index(Model model) {
        authHelper.checkLogin(model);
        model.addAttribute("message", "보험 이미지 생성 서비스에 오신 것을 환영합니다!");
        return "index";
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }
}
