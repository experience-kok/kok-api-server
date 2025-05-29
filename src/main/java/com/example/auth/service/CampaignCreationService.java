package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.User;
import com.example.auth.domain.VisitLocation;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignCategoryRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.VisitLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 생성 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * 클라이언트(광고주)가 새로운 캠페인을 등록하는 기능을 제공합니다.
 * 사용자 권한 확인, 카테고리 유효성 검증, 캠페인 및 방문 위치 정보 저장 등의
 * 과정을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCreationService {

    private final CampaignRepository campaignRepository;
    private final CampaignCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final VisitLocationRepository visitLocationRepository;

    /**
     * 캠페인 생성 메서드
     * 
     * 요청된 캠페인 정보를 검증하고 데이터베이스에 저장합니다.
     * CLIENT 권한을 가진 사용자만 캠페인을 생성할 수 있으며,
     * 캠페인은 기본적으로 'PENDING' 상태로 생성되어 관리자의 승인이 필요합니다.
     * 
     * @param userId 캠페인을 생성하려는 사용자의 ID (클라이언트)
     * @param request 캠페인 생성 요청 데이터
     * @return 생성된 캠페인 정보를 담은 응답 객체
     * @throws ResourceNotFoundException 사용자나 카테고리가 존재하지 않는 경우
     * @throws AccessDeniedException 사용자가 CLIENT 권한이 없는 경우
     */
    @Transactional
    public CreateCampaignResponse createCampaign(Long userId, CreateCampaignRequest request) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 사용자 권한 확인 (CLIENT 권한만 가능)
        if (!"CLIENT".equals(user.getRole())) {
            throw new AccessDeniedException("캠페인 등록은 클라이언트 권한을 가진 사용자만 가능합니다.");
        }
        
        // 카테고리 조회 - 이름과 타입으로 찾기
        CampaignCategory category = categoryRepository.findByCategoryNameAndCategoryType(
                request.getCategory().getName(), request.getCategory().getType())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("카테고리를 찾을 수 없습니다. 이름: %s, 타입: %s", 
                                request.getCategory().getName(), request.getCategory().getType())));
        
        // Campaign 엔티티 생성
        Campaign campaign = request.toEntity(user, category);
        
        // 캠페인 저장
        Campaign savedCampaign = campaignRepository.save(campaign);
        
        // 방문 위치 저장 (있는 경우)
        if (request.getVisitLocations() != null && !request.getVisitLocations().isEmpty()) {
            List<VisitLocation> locations = request.getVisitLocations().stream()
                    .map(loc -> VisitLocation.builder()
                            .campaign(savedCampaign)
                            .address(loc.getAddress())
                            .latitude(loc.getLatitude())
                            .longitude(loc.getLongitude())
                            .additionalInfo(loc.getAdditionalInfo())
                            .build())
                    .collect(Collectors.toList());
            
            List<VisitLocation> savedLocations = visitLocationRepository.saveAll(locations);
            savedCampaign.getVisitLocations().addAll(savedLocations);
        }
        
        log.info("캠페인이 성공적으로 생성되었습니다. ID: {}, 제목: {}", savedCampaign.getId(), savedCampaign.getTitle());
        
        return CreateCampaignResponse.fromEntity(savedCampaign);
    }
}
