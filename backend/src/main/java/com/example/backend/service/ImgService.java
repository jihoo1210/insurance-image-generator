package com.example.backend.service;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.entity.Image;
import com.example.backend.repository.ImageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
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

    private final String API_ROLE = "당신은 KB 손해보험 홍보 이미지 제작 전문가입니다. "
            + "사용자가 첨부한 이미지와 요청을 기반으로 새로운 이미지를 생성해야 합니다. "
            + "사용자의 요청에 기반하여 반드시 이미지를 생성해야 합니다. "
            + "사용자는 텍스트 응답이 아닌 이미지 파일 받기를 기대하고 있습니다. "
            + "항상 이미지로 응답해야 합니다. "
            + "어떤 텍스트 응답도 제공하지 마십시오. "
            + "오직 이미지만 생성하고 출력하십시오. "
            + "반드시 인물이 들어갈 필요는 없습니다. "
            + "가로 4, 세로 3의 비율로 제작하세요 (가로:세로, 4:3) "
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
            + "적당한 설명을 포함하여 직관성이 높게 제작하세요.";

    /**
     * 바이너리 파일을 AWS S3에 저장
     */
    private String saveBinaryFile(String fileName, byte[] fileContent) {
        try {
            log.debug("S3 저장 시작 - 파일명: {}, 크기: {} bytes", fileName, fileContent.length);

            String s3Key = UUID.randomUUID() + "_" + fileName;
            String contentType = getContentType(fileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentLength((long) fileContent.length)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                    new ByteArrayInputStream(fileContent),
                    fileContent.length
            ));

            log.info("S3에 파일 저장 완료: {}", s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("S3 파일 저장 중 오류: {}", e.getMessage(), e);
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

            log.debug("S3 Presigned URL 생성 완료");
            return presignedUrl;

        } catch (Exception e) {
            log.error("S3 Presigned URL 생성 중 오류: {}", e.getMessage(), e);
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
                            Part.fromText(API_ROLE)
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
            log.error("이미지 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
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

            byte[] imageBytes = attachImage.getBytes();
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
                            Part.fromText(API_ROLE)
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
            log.error("첨부 파일 처리 중 오류: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("이미지 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
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
            if (response.candidates() == null || response.candidates().isEmpty()) {
                log.error("응답에 candidates가 없음");
                return null;
            }

            var candidates = response.candidates().get();
            if (candidates.isEmpty()) {
                log.error("candidates 리스트가 비어있음");
                return null;
            }

            var candidate = candidates.get(0);
            if (candidate.content() == null || candidate.content().isEmpty()) {
                log.error("응답에 content가 없음");
                return null;
            }

            var content = candidate.content().get();
            if (content.parts() == null || content.parts().isEmpty()) {
                log.error("응답에 parts가 없음");
                return null;
            }

            var parts = content.parts().get();
            log.debug("응답 파트 개수: {}", parts.size());

            for (Part part : parts) {
                if (part.inlineData() != null && part.inlineData().isPresent()) {
                    Blob blob = part.inlineData().get();

                    String fileExtension = ".png";
                    if (blob.mimeType() != null && blob.mimeType().isPresent()) {
                        String mimeType = blob.mimeType().get();
                        if (mimeType.contains("jpeg")) {
                            fileExtension = ".jpg";
                        } else if (mimeType.contains("webp")) {
                            fileExtension = ".webp";
                        }
                    }

                    if (blob.data() != null && blob.data().isPresent()) {
                        byte[] imageData = blob.data().get();
                        log.debug("이미지 데이터 크기: {} bytes", imageData.length);

                        String fileName = "generated_image" + fileExtension;
                        String s3Key = saveBinaryFile(fileName, imageData);

                        if (s3Key != null) {
                            log.info("이미지 저장 성공 - S3 키: {}", s3Key);
                            return s3Key;
                        } else {
                            log.error("이미지 저장 실패");
                            return null;
                        }
                    }
                }
            }

            log.error("응답에서 이미지 데이터를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("응답 처리 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * API 예외 처리 및 QuotaExceededException 발생
     */
    private void handleApiException(ApiException e) throws QuotaExceededException {
        String errorMessage = e.getMessage();
        log.error("Gemini API 에러: {}", errorMessage);

        if (errorMessage != null &&
            (errorMessage.contains("429") ||
             errorMessage.contains("quota") ||
             errorMessage.contains("Quota exceeded"))) {
            double retryAfterSeconds = extractRetryAfterSeconds(errorMessage);
            long retryAfterMillis = retryAfterSeconds > 0 ? (long) (retryAfterSeconds * 1000) : 60000;
            log.warn("API 할당량 초과 - 재시도 대기: {}초", retryAfterSeconds);
            throw new QuotaExceededException("API 할당량이 초과되었습니다. 잠시 후 다시 시도해주세요.", retryAfterMillis);
        }
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
     * 페이지네이션으로 이미지 조회
     * Spring Data JPA Page 객체 직접 반환
     *
     * @param pageable 페이지네이션 정보
     * @param userEmail 현재 로그인한 사용자 이메일 (null 가능)
     * @return Page<ImageListResponse>
     */
    public Page<ImageListResponse> getPagedImages(Pageable pageable, String userEmail) {
        Page<Image> imagePage = imageRepository.findAll(pageable);

        return imagePage.map(image -> convertToImageListResponse(image, userEmail));
    }

    /**
     * Image 엔티티를 ImageListResponse DTO로 변환
     */
    private ImageListResponse convertToImageListResponse(Image image, String userEmail) {
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
    }

    /**
     * 파일명에서 Content-Type 추출
     */
    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".webp")) return "image/webp";
        if (lowerName.endsWith(".gif")) return "image/gif";

        return "image/png"; // 기본값
    }
}
