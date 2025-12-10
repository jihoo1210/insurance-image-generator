package com.example.backend.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ImageListResponse {

    String imageUrl;
    String s3Key;
    String prompt;
    Long saveCount;
    String creatorEmail;
    Boolean isFavorited;
}
