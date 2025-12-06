package com.example.backend.dto;

import com.example.backend.entity.Image;
import com.example.backend.entity.User;
import com.example.backend.entity.UserSaveImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSaveImagesRepository extends JpaRepository<UserSaveImages, Long> {
    void deleteByUserAndImage(User user, Image image);
    boolean existsByUserAndImage(User user, Image image);
}
