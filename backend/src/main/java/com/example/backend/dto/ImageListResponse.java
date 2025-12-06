package com.example.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
@Getter
public class ImageListResponse {

    private String imageUrl;
    private String s3Key;
    private String prompt;
    private Long saveCount;
    private String creatorEmail;
    private Boolean isFavorited;  // 현재 사용자가 즐겨찾기했는지 여부
}

