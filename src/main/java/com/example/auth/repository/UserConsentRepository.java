package com.example.auth.repository;

import com.example.auth.constant.ConsentType;
import com.example.auth.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    
    List<UserConsent> findByUserId(Long userId);
    
    Optional<UserConsent> findByUserIdAndConsentType(Long userId, ConsentType consentType);
    
    boolean existsByUserIdAndConsentTypeAndAgreed(Long userId, ConsentType consentType, Boolean agreed);
}
