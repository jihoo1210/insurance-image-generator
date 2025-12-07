package com.example.backend.service;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.entity.Image;
import com.example.backend.repository.ImageRepository;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Gemini API를 사용하여 이미지를 생성하고
 * AWS S3에 저장하는 서비스
 *
 * Gemini API 공식 문서: https://ai.google.dev/gemini-api/docs?hl=en
 * 이미지 생성 문서: https://ai.google.dev/gemini-api/docs/vision/generate-images
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ImgService {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ImageRepository imageRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${app.mock-mode:false}")
    private boolean mockMode;

    /**
     * 바이너리 파일을 AWS S3에 저장
     */
    private String saveBinaryFile(String fileName, byte[] fileContent) {
        try {
            log.debug("S3 저장 시작 - 파일명: {}, 크기: {} bytes", fileName, fileContent.length);

            String s3Key = UUID.randomUUID() + "_" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentLength((long) fileContent.length)
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                    new ByteArrayInputStream(fileContent),
                    fileContent.length
            ));

            log.info("✅ S3에 파일 저장 완료: {}", s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("❌ S3 파일 저장 중 오류: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * S3에 저장된 객체의 Presigned URL을 생성
     * 퍼블릭 접근이 비활성화된 S3도 일시적으로 접근 가능하게 함
     *
     * @param s3Key S3 객체 키
     * @return Presigned URL (1시간 유효)
     */
    public String generateS3Url(String s3Key) {
        try {
            log.debug("S3 Presigned URL 생성 중 - 키: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))  // 1시간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("✅ S3 Presigned URL 생성 완료: {}", presignedUrl);
            return presignedUrl;

        } catch (Exception e) {
            log.error("❌ S3 Presigned URL 생성 중 오류: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 에러 메시지에서 재시도 대기 시간 추출
     */
    private double extractRetryAfterSeconds(String errorMessage) {
        if (errorMessage == null) return -1;
        Pattern pattern = Pattern.compile("Please retry in ([\\d.]+)s");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return -1;
    }

    /**
     * 프롬프트와 시스템 인스트럭션으로 이미지 생성
     *
     * @param prompt 이미지 생성 프롬프트
     * @param creatorEmail 이미지 제작자 이메일
     * @return 실제 S3 키
     */
    public String generateImage(String prompt, String creatorEmail) throws QuotaExceededException {

        if (prompt == null) return null;

        Client client = Client.builder()
                .apiKey(googleApiKey)
                .build();

        try {
            String model = "gemini-3-pro-image-preview";

            var userContent = Content.builder()
                    .role("user")
                    .parts(ImmutableList.of(
                            Part.fromText(prompt)
                    ))
                    .build();

            var configBuilder = GenerateContentConfig.builder();

            var systemContent = Content.builder()
                    .role("user")
                    .parts(ImmutableList.of(
                            Part.fromText("당신은 KB 손해보험 홍보 이미지 제작 전문가입니다. "
                                    + "사용자의 요청에 기반하여 반드시 이미지를 생성해야 합니다. "
                                    + "사용자는 텍스트 응답이 아닌 이미지 파일 받기를 기대하고 있습니다. "
                                    + "항상 이미지로 응답해야 합니다. "
                                    + "어떤 텍스트 응답도 제공하지 마십시오. "
                                    + "오직 이미지만 생성하고 출력하십시오. "
                                    + "반드시 인물이 들어갈 필요는 없습니다. "
                                    + "고객의 요구사항을 저장하여 이후에 제작할 이미지의 참고자료로 활용할 수 있도록 하세요. "
                                    + "고객이 입력한 프롬프트를 바탕으로 창의적이고 매력적인 이미지를 생성하세요. "
                                    + "이미지의 해상도가 매우 좋을 필요는 없습니다. "
                                    + "이미지는 KB 손해보험의 브랜드 이미지에 부합해야 합니다. "
                                    + "KB 손해보험 다이렉트에 관한 내용은 절대 추가하지 마세요. "
                                    + "색상은 주로 KB 손해보험과 관련된 색(주 색상: #FFCA00, 보조 색상: #000000, 배경/배색: #FFFFFF)을 사용하고, 신뢰감을 주는 느낌을 강조하세요. "
                                    + "최종 이미지는 고객의 요구를 충족시키면서도 KB 손해보험의 브랜드 가치를 효과적으로 전달해야 합니다. "
                                    + "사용자는 한국인이며 소비자 또한 한국인입니다. 오탈자나 어색한 문구가 없도록 주의하세요. "
                                    + "이미지는 매우 강렬해야 하며 소비자에의 소비욕을 불러 일으켜야 합니다. "
                                    + "강렬한 이미지를 위해서라면 주조색과 보조색을 과감하게 수정해도 무방합니다. "
                                    + "반드시 담당자와 상담을 해야만 하는 느낌을 제공해야 합니다. "
                                    + "이미지 요청에서 지시한 상세 정보는 최종 이미지에 절대 포함되면 안됩니다. "
                                    + "적당한 설명을 포함하여 직관성이 높게 제작하세요.")
                    ))
                    .build();
            configBuilder.systemInstruction(systemContent);

            GenerateContentConfig config = configBuilder.build();
            var contents = ImmutableList.of(userContent);

            var response = client.models.generateContent(model, contents, config);

            String savedS3Key = processResponse(response);

            if (savedS3Key != null) {
                Image image = Image.builder()
                        .s3Key(savedS3Key)
                        .prompt(prompt)
                        .creatorEmail(creatorEmail)
                        .build();
                imageRepository.save(image);
            }

            return savedS3Key;

        } catch (ApiException e) {
            handleApiException(e);
            return null;
        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════");
            log.error("❌ [예상치 못한 오류]");
            log.error("오류 클래스: {}", e.getClass().getName());
            log.error("오류 메시지: {}", e.getMessage());
            e.printStackTrace();
            log.error("═══════════════════════════════════════════════════════════");
            return null;
        }
    }

    /**
     * 첨부된 이미지를 기반으로 이미지 생성
     *
     * @param prompt 이미지 생성 프롬프트
     * @param creatorEmail 이미지 제작자 이메일
     * @param attachImage 첨부된 이미지 파일
     * @return 실제 S3 키
     */
    public String generateImageWithAttachment(String prompt, String creatorEmail, MultipartFile attachImage) throws QuotaExceededException {

        if (prompt == null || attachImage == null || attachImage.isEmpty()) return null;

        Client client = Client.builder()
                .apiKey(googleApiKey)
                .build();

        try {
            String model = "gemini-3-pro-image-preview";

            // 첨부된 이미지를 Base64로 인코딩
            byte[] imageBytes = attachImage.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = attachImage.getContentType() != null ? attachImage.getContentType() : "image/jpeg";

            // 이미지 + 텍스트 프롬프트 결합
            var userContent = Content.builder()
                    .role("user")
                    .parts(ImmutableList.of(
                            Part.fromText(prompt),
                            Part.fromBytes(imageBytes, mimeType)
                    ))
                    .build();

            var configBuilder = GenerateContentConfig.builder();

            var systemContent = Content.builder()
                    .role("user")
                    .parts(ImmutableList.of(
                            Part.fromText("당신은 KB 손해보험 홍보 이미지 제작 전문가입니다. "
                                    + "사용자가 첨부한 이미지와 요청을 기반으로 새로운 이미지를 생성해야 합니다. "
                                    + "사용자의 요청에 기반하여 반드시 이미지를 생성해야 합니다. "
                                    + "사용자는 텍스트 응답이 아닌 이미지 파일 받기를 기대하고 있습니다. "
                                    + "항상 이미지로 응답해야 합니다. "
                                    + "어떤 텍스트 응답도 제공하지 마십시오. "
                                    + "오직 이미지만 생성하고 출력하십시오. "
                                    + "반드시 인물이 들어갈 필요는 없습니다. "
                                    + "고객의 요구사항을 저장하여 이후에 제작할 이미지의 참고자료로 활용할 수 있도록 하세요. "
                                    + "고객이 입력한 프롬프트를 바탕으로 창의적이고 매력적인 이미지를 생성하세요. "
                                    + "이미지의 해상도가 매우 좋을 필요는 없습니다. "
                                    + "이미지는 KB 손해보험의 브랜드 이미지에 부합해야 합니다. "
                                    + "KB 손해보험 다이렉트에 관한 내용은 절대 추가하지 마세요. "
                                    + "색상은 주로 KB 손해보험과 관련된 색(주 색상: #FFCA00, 보조 색상: #000000, 배경/배색: #FFFFFF)을 사용하고, 신뢰감을 주는 느낌을 강조하세요. "
                                    + "최종 이미지는 고객의 요구를 충족시키면서도 KB 손해보험의 브랜드 가치를 효과적으로 전달해야 합니다. "
                                    + "사용자는 한국인이며 소비자 또한 한국인입니다. 오탈자나 어색한 문구가 없도록 주의하세요. "
                                    + "이미지는 매우 강렬해야 하며 소비자에의 소비욕을 불러 일으켜야 합니다. "
                                    + "강렬한 이미지를 위해서라면 주조색과 보조색을 과감하게 수정해도 무방합니다. "
                                    + "반드시 담당자와 상담을 해야만 하는 느낌을 제공해야 합니다. "
                                    + "이미지 요청에서 지시한 상세 정보는 최종 이미지에 절대 포함되면 안됩니다. "
                                    + "적당한 설명을 포함하여 직관성이 높게 제작하세요.")
                    ))
                    .build();
            configBuilder.systemInstruction(systemContent);

            GenerateContentConfig config = configBuilder.build();
            var contents = ImmutableList.of(userContent);

            var response = client.models.generateContent(model, contents, config);

            String savedS3Key = processResponse(response);

            if (savedS3Key != null) {
                Image image = Image.builder()
                        .s3Key(savedS3Key)
                        .prompt(prompt)
                        .creatorEmail(creatorEmail)
                        .build();
                imageRepository.save(image);
            }

            return savedS3Key;

        } catch (ApiException e) {
            handleApiException(e);
            return null;
        } catch (IOException e) {
            log.error("첨부 파일 처리 중 오류: {}", e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════");
            log.error("❌ [예상치 못한 오류]");
            log.error("오류 클래스: {}", e.getClass().getName());
            log.error("오류 메시지: {}", e.getMessage());
            e.printStackTrace();
            log.error("═══════════════════════════════════════════════════════════");
            return null;
        }
    }

    /**
     * API 응답 처리
     *
     * 응답 구조:
     * - response.candidates() -> Optional<List<Candidate>>
     * - candidate.content() -> Optional<Content>
     * - content.parts() -> Optional<List<Part>>
     * - part.inlineData() -> Optional<Blob>
     * - blob.data() -> Optional<byte[]>
     *
     * @return 실제 S3 키 (예: "uuid_generated_image.png")
     */
    private String processResponse(GenerateContentResponse response) {
        try {
            log.debug("응답 구조 검증 시작");

            // candidates 확인
            if (response.candidates() == null || response.candidates().isEmpty()) {
                log.error("❌ candidates가 없음");
                return null;
            }

            var candidates = response.candidates().get();
            log.info("  후보(candidates) 개수: {}", candidates.size());

            if (candidates.isEmpty()) {
                log.error("❌ candidates 리스트가 비어있음");
                return null;
            }

            // 첫 번째 candidate 처리
            var candidate = candidates.get(0);
            log.debug("  첫 번째 후보 선택");

            // content 확인
            if (candidate.content() == null || candidate.content().isEmpty()) {
                log.error("❌ content가 없음");
                return null;
            }

            var content = candidate.content().get();
            log.debug("  content 추출 완료");

            // parts 확인
            if (content.parts() == null || content.parts().isEmpty()) {
                log.error("❌ parts가 없음");
                return null;
            }

            var parts = content.parts().get();
            log.info("  파트(parts) 개수: {}", parts.size());

            // 각 파트 처리
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);

                // 이미지 데이터 확인
                if (part.inlineData() != null && part.inlineData().isPresent()) {
                    log.info("  ✓ 이미지 데이터 발견!");

                    Blob blob = part.inlineData().get();

                    // MIME 타입 확인
                    String fileExtension = ".png";
                    if (blob.mimeType() != null && blob.mimeType().isPresent()) {
                        String mimeType = blob.mimeType().get();
                        log.info("    MIME 타입: {}", mimeType);

                        if (mimeType.contains("jpeg")) {
                            fileExtension = ".jpg";
                        } else if (mimeType.contains("webp")) {
                            fileExtension = ".webp";
                        }
                    }

                    // 이미지 바이너리 데이터 추출
                    if (blob.data() != null && blob.data().isPresent()) {
                        byte[] imageData = blob.data().get();
                        log.info("    이미지 데이터 크기: {} bytes", imageData.length);

                        // S3 저장
                        String fileName = "generated_image" + fileExtension;
                        String s3Key = saveBinaryFile(fileName, imageData);

                        if (s3Key != null) {
                            log.info("  ✓ 이미지 저장 성공");
                            log.info("    S3 키: {}", s3Key);
                            // *** 중요: s3Key 반환 (imageUrl로 변환하지 않음) ***
                            return s3Key;
                        } else {
                            log.error("  ❌ 이미지 저장 실패");
                            return null;
                        }
                    } else {
                        log.error("  ❌ 이미지 데이터 없음");
                    }
                }
            }

            log.error("❌ 이미지 데이터를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("❌ 응답 처리 중 오류: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * API 예외 처리
     */
    private void handleApiException(ApiException e) {
        log.error("═══════════════════════════════════════════════════════════");
        log.error("❌ [Gemini API 에러]");
        log.error("═══════════════════════════════════════════════════════════");

        String errorMessage = e.getMessage();
        log.error("에러 메시지: {}", errorMessage);

        // 429: 할당량 초과
        if (errorMessage != null &&
            (errorMessage.contains("429") ||
             errorMessage.contains("quota") ||
             errorMessage.contains("Quota exceeded"))) {
            double retryAfterSeconds = extractRetryAfterSeconds(errorMessage);
            log.warn("⏱️  API 할당량 초과 - 재시도 대기: {}초", retryAfterSeconds);
        }

        log.error("═══════════════════════════════════════════════════════════");
    }

    /**
     * API 할당량 초과 커스텀 예외
     */
    public static class QuotaExceededException extends Exception {
        private final long retryAfterMillis;

        public QuotaExceededException(String message, long retryAfterMillis) {
            super(message);
            this.retryAfterMillis = retryAfterMillis;
        }

        public long getRetryAfterMillis() {
            return retryAfterMillis;
        }
    }

    /**
     * 모든 이미지 조회 (저장 수 기준 정렬)
     *
     * @param userEmail 현재 로그인한 사용자 이메일 (null 가능)
     * @return 이미지 목록
     */
    public List<ImageListResponse> getAllImages(String userEmail) {
        List<Image> images = imageRepository.findAll();

        // 저장 수 기준 내림차순 정렬
        images.sort((a, b) -> {
            Long countA = a.getUserSaveImages() != null ? (long) a.getUserSaveImages().size() : 0;
            Long countB = b.getUserSaveImages() != null ? (long) b.getUserSaveImages().size() : 0;
            return countB.compareTo(countA);
        });

        // DTO로 변환
        return images.stream()
                .map(image -> {
                    // 현재 사용자가 이 이미지를 즐겨찾기했는지 확인
                    Boolean isFavorited = false;
                    if (userEmail != null && !userEmail.trim().isEmpty()) {
                        isFavorited = image.getUserSaveImages() != null &&
                                image.getUserSaveImages().stream()
                                        .anyMatch(usi -> userEmail.equals(usi.getUser().getEmail()));
                    }

                    return ImageListResponse.builder()
                            .imageUrl(generateS3Url(image.getS3Key()))
                            .s3Key(image.getS3Key())
                            .prompt(image.getPrompt())
                            .saveCount((long) (image.getUserSaveImages() != null ? image.getUserSaveImages().size() : 0))
                            .creatorEmail(image.getCreatorEmail())
                            .isFavorited(isFavorited)
                            .build();
                })
                .toList();
    }
}
