package com.example.auth.repository;

import com.example.auth.domain.Company;
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
     * 사업자등록번호 존재 여부를 확인합니다.
     * @param businessRegistrationNumber 사업자등록번호
     * @return 존재 여부
     */
    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
