package com.example.auth.repository;

import com.example.auth.domain.CampaignCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 캠페인 카테고리 저장소 인터페이스
 */
@Repository
public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Long> {
    
    /**
     * 카테고리 타입별로 카테고리 목록을 조회합니다.
     * @param categoryType 카테고리 타입
     * @return 카테고리 목록
     */
    List<CampaignCategory> findByCategoryType(CampaignCategory.CategoryType categoryType);
    
    /**
     * 카테고리 타입과 이름으로 카테고리를 조회합니다.
     * @param categoryType 카테고리 타입
     * @param categoryName 카테고리 이름
     * @return 카테고리 정보
     */
    Optional<CampaignCategory> findByCategoryTypeAndCategoryName(
            CampaignCategory.CategoryType categoryType, String categoryName);
    
    /**
     * 카테고리 타입과 이름의 존재 여부를 확인합니다.
     * @param categoryType 카테고리 타입
     * @param categoryName 카테고리 이름
     * @return 존재 여부
     */
    boolean existsByCategoryTypeAndCategoryName(
            CampaignCategory.CategoryType categoryType, String categoryName);
}
