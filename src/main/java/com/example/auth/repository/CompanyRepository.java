package com.example.auth.repository;

import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 업체 정보 저장소 인터페이스
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * 사업자등록번호로 업체를 조회합니다.
     * @param businessRegistrationNumber 사업자등록번호
     * @return 업체 정보
     */
    Optional<Company> findByBusinessRegistrationNumber(String businessRegistrationNumber);
    
    /**
     * 업체명으로 업체를 조회합니다.
     * @param companyName 업체명
     * @return 업체 정보
     */
    Optional<Company> findByCompanyName(String companyName);
    
    /**
     * 업체명으로 부분 검색 (대소문자 무시, 페이징)
     * @param companyName 업체명 (부분 일치)
     * @param pageable 페이징 정보
     * @return 업체 목록
     */
    Page<Company> findByCompanyNameContainingIgnoreCase(String companyName, Pageable pageable);
    
    /**
     * 사업자등록번호 존재 여부를 확인합니다.
     * @param businessRegistrationNumber 사업자등록번호
     * @return 존재 여부
     */
    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
    
    /**
     * 특정 사용자의 업체 정보를 조회합니다.
     * @param user 사용자 객체
     * @return 업체 정보
     */
    Optional<Company> findByUser(User user);
    
    /**
     * 특정 사용자 ID의 업체 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return 업체 정보
     */
    Optional<Company> findByUserId(Long userId);
    
    /**
     * 특정 사용자의 업체 정보를 삭제합니다.
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}

