package com.example.auth.service;

import com.example.auth.domain.Company;
import com.example.auth.dto.company.CompanyRequest;
import com.example.auth.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업체 정보 관리 서비스 (내부 사용용)
 * 캠페인 생성 시에만 사용되는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 업체를 생성합니다. (캠페인 생성 시 내부 사용)
     * @param request 업체 등록 요청
     * @return 생성된 업체 엔티티
     */
    @Transactional
    public Company createCompany(CompanyRequest request) {
        // 사업자등록번호 중복 확인
        if (request.getBusinessRegistrationNumber() != null && 
            companyRepository.existsByBusinessRegistrationNumber(request.getBusinessRegistrationNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .contactPerson(request.getContactPerson())
                .phoneNumber(request.getPhoneNumber())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("업체가 생성되었습니다. ID: {}, 업체명: {}", savedCompany.getId(), savedCompany.getCompanyName());

        return savedCompany;
    }

    /**
     * 업체 정보를 업데이트합니다. (캠페인 수정 시 내부 사용)
     * @param company 업체 엔티티
     * @param request 업체 수정 요청
     * @return 수정된 업체 엔티티
     */
    @Transactional
    public Company updateCompany(Company company, CompanyRequest request) {
        // 사업자등록번호 중복 확인 (기존 업체 제외)
        if (request.getBusinessRegistrationNumber() != null && 
            !request.getBusinessRegistrationNumber().equals(company.getBusinessRegistrationNumber()) &&
            companyRepository.existsByBusinessRegistrationNumber(request.getBusinessRegistrationNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        company.updateCompanyInfo(
                request.getCompanyName(),
                request.getContactPerson(),
                request.getPhoneNumber(),
                request.getBusinessRegistrationNumber()
        );

        log.info("업체 정보가 수정되었습니다. ID: {}, 업체명: {}", company.getId(), company.getCompanyName());

        return company;
    }
}
