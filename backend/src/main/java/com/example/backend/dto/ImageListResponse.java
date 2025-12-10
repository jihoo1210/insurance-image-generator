package com.example.backend.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ImageListResponse {

    String imageUrl;      // 다운로드용 URL (Content-Disposition: attachment)
    String displayUrl;    // 브라우저 표시용 URL (Content-Disposition: inline)
    String s3Key;
    String prompt;
    Long saveCount;
    String creatorEmail;
    Boolean isFavorited;
}
