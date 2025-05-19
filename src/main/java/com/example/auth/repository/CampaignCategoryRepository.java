package com.example.auth.repository;

import com.example.auth.domain.CampaignCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Integer> {
    
    /**
     * 카테고리 이름과 타입으로 카테고리를 찾습니다.
     * 
     * @param categoryName 카테고리 이름 (예: '맛집', '카페')
     * @param categoryType 카테고리 타입 (예: '방문', '배송')
     * @return 해당하는 카테고리를 Optional로 반환
     */
    Optional<CampaignCategory> findByCategoryNameAndCategoryType(String categoryName, String categoryType);
}