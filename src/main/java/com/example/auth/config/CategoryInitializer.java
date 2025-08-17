package com.example.auth.config;

import com.example.auth.domain.CampaignCategory;
import com.example.auth.repository.CampaignCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 기본 카테고리 데이터를 초기화하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryInitializer implements CommandLineRunner {

    private final CampaignCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeCategories();
    }

    /**
     * 기본 카테고리 데이터를 초기화합니다.
     */
    private void initializeCategories() {
        log.info("캠페인 카테고리 초기화를 시작합니다.");

        // 방문형 카테고리 생성
        createCategoryIfNotExists(CampaignCategory.CategoryType.방문, "카페");
        createCategoryIfNotExists(CampaignCategory.CategoryType.방문, "맛집");
        createCategoryIfNotExists(CampaignCategory.CategoryType.방문, "뷰티");
        createCategoryIfNotExists(CampaignCategory.CategoryType.방문, "숙박");

        // 배송형 카테고리 생성
        createCategoryIfNotExists(CampaignCategory.CategoryType.배송, "식품");
        createCategoryIfNotExists(CampaignCategory.CategoryType.배송, "화장품");
        createCategoryIfNotExists(CampaignCategory.CategoryType.배송, "생활용품");
        createCategoryIfNotExists(CampaignCategory.CategoryType.배송, "패션");
        createCategoryIfNotExists(CampaignCategory.CategoryType.배송, "잡화");

        log.info("캠페인 카테고리 초기화가 완료되었습니다.");
    }

    /**
     * 카테고리가 존재하지 않으면 생성합니다.
     */
    private void createCategoryIfNotExists(CampaignCategory.CategoryType type, String name) {
        if (!categoryRepository.existsByCategoryTypeAndCategoryName(type, name)) {
            CampaignCategory category = CampaignCategory.builder()
                    .categoryType(type)
                    .categoryName(name)
                    .build();
            
            categoryRepository.save(category);
            log.info("카테고리 생성: {} - {}", type.getDisplayName(), name);
        } else {
            log.debug("카테고리 이미 존재: {} - {}", type.getDisplayName(), name);
        }
    }
}
