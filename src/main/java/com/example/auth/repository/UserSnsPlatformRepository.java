package com.example.auth.repository;

import com.example.auth.domain.UserSnsPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSnsPlatformRepository extends JpaRepository<UserSnsPlatform, Long> {

    List<UserSnsPlatform> findByUserId(Long userId);

    Optional<UserSnsPlatform> findByUserIdAndId(Long userId, Long platformId);

    Optional<UserSnsPlatform> findByUserIdAndPlatformTypeAndAccountUrl(Long userId, String platformType, String accountUrl);

    void deleteByUserIdAndId(Long userId, Long platformId);
}