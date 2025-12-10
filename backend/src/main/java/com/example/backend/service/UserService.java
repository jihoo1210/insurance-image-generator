package com.example.backend.service;

import com.example.backend.dto.ImageListResponse;
import com.example.backend.dto.UserResponseDto;
import com.example.backend.entity.Image;
import com.example.backend.entity.User;
import com.example.backend.entity.UserSaveImages;
import com.example.backend.repository.ImageRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserSaveImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final UserSaveImagesRepository userSaveImagesRepository;
    private final ImgService imgService;

    public UserResponseDto createUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .build()
                ));
        return UserResponseDto.builder()
                .email(user.getEmail())
                .build();
    }

    /**
     * S3 이미지를 DB에 저장
     *
     * @param s3Key S3 객체 키
     * @param email 사용자 이메일
     * @return 저장된 Image 객체
     */
    @Transactional
    public Image saveImage(String s3Key, String email) {
        Image image = imageRepository.findByS3Key(s3Key).orElse(null);
        User user = userRepository.findByEmail(email).orElse(null);
        if(image != null && user != null) {
            if(userSaveImagesRepository.existsByUserAndImage(user, image)) {
             userSaveImagesRepository.deleteByUserAndImage(user, image);
            } else {
                UserSaveImages userSaveImages = UserSaveImages.builder()
                        .image(image)
                        .user(user)
                        .build();
                UserSaveImages response = userSaveImagesRepository.save(userSaveImages);
            }
            return image;
        }
        return null;
    }

    /**
     * 사용자의 즐겨찾기 목록 조회
     *
     * @param email 사용자 이메일
     * @return 즐겨찾기 이미지 목록
     */
    public List<ImageListResponse> getUserFavorites(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return List.of();
        }

        return user.getUserSaveImagesList().stream()
                .map(userSaveImages -> {
                    Image image = userSaveImages.getImage();
                    return ImageListResponse.builder()
                            .imageUrl(generatePresignedUrl(image.getS3Key()))
                            .s3Key(image.getS3Key())
                            .prompt(image.getPrompt())
                            .saveCount((long) (image.getUserSaveImages() != null ? image.getUserSaveImages().size() : 0))
                            .creatorEmail(image.getCreatorEmail())
                            .isFavorited(true)  // 즐겨찾기 페이지는 이미 저장된 것만 표시
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Presigned URL 생성 (ImgService 위임)
     */
    private String generatePresignedUrl(String s3Key) {
        String presignedUrl = imgService.generateS3Url(s3Key);
        return presignedUrl != null ? presignedUrl : "/download/" + s3Key;
    }

    /**
     * 즐겨찾기에서 이미지 제거
     *
     * @param s3Key S3 객체 키
     * @param email 사용자 이메일
     */
    @Transactional
    public void removeFavorite(String s3Key, String email) {
        Image image = imageRepository.findByS3Key(s3Key).orElse(null);
        User user = userRepository.findByEmail(email).orElse(null);

        if (image != null && user != null) {
            userSaveImagesRepository.deleteByUserAndImage(user, image);
        }
    }
}
