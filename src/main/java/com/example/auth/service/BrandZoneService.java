package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.Company;
import com.example.auth.dto.brandzone.BrandCampaignResponse;
import com.example.auth.dto.brandzone.BrandInfoResponse;
import com.example.auth.dto.brandzone.BrandListResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.CompanyRepository;
import com.example.auth.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 브랜드존 서비스
 * 브랜드별 캠페인 목록 및 브랜드 정보를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandZoneService {

    private final CompanyRepository companyRepository;
    private final CampaignRepository campaignRepository;
    private final LikeRepository likeRepository;

    /**
     * 모든 브랜드 목록 조회 (페이징, 필터링)
     */
    public PageResponse<BrandListResponse> getAllBrands(int page, int size, String brandName) {
        log.info("브랜드 목록 조회: page={}, size={}, brandName={}", page, size, brandName);

        Pageable pageable = PageRequest.of(page, size);
        Page<Company> companyPage;
        
        // 브랜드명 필터링 적용
        if (brandName != null && !brandName.trim().isEmpty()) {
            companyPage = companyRepository.findByCompanyNameContainingIgnoreCase(brandName.trim(), pageable);
        } else {
            companyPage = companyRepository.findAll(pageable);
        }

        List<BrandListResponse> responses = companyPage.getContent().stream()
                .map(company -> {
                    // 브랜드별 캠페인 통계 조회
                    long totalCampaigns = campaignRepository.countByCompany(company);
                    long activeCampaigns = campaignRepository.countByCompanyAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                            company, Campaign.ApprovalStatus.APPROVED, LocalDate.now());
                    
                    return BrandListResponse.fromCompany(company, totalCampaigns, activeCampaigns);
                })
                .collect(Collectors.toList());

        return PageResponse.<BrandListResponse>builder()
                .content(responses)
                .pageNumber(companyPage.getNumber() + 1)
                .pageSize(companyPage.getSize())
                .totalPages(companyPage.getTotalPages())
                .totalElements(companyPage.getTotalElements())
                .first(companyPage.isFirst())
                .last(companyPage.isLast())
                .build();
    }

    /**
     * 특정 브랜드 정보 조회
     */
    public BrandInfoResponse getBrandInfo(Long brandId) {
        log.info("브랜드 정보 조회: brandId={}", brandId);

        Company company = companyRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("브랜드를 찾을 수 없습니다. ID: " + brandId));

        // 브랜드 캠페인 통계 조회
        long totalCampaigns = campaignRepository.countByCompany(company);
        long activeCampaigns = campaignRepository.countByCompanyAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                company, Campaign.ApprovalStatus.APPROVED, LocalDate.now());

        return BrandInfoResponse.fromCompany(company, totalCampaigns, activeCampaigns);
    }



    // === Private Helper Methods ===

}
