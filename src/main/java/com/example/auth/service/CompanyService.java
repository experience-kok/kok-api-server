package com.example.auth.service;

import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import com.example.auth.dto.business.BusinessInfoRequest;
import com.example.auth.dto.company.CompanyRequest;
import com.example.auth.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

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
     * @param user 업체를 소유할 사용자
     * @param request 업체 등록 요청
     * @return 생성된 업체 엔티티
     */
    @Transactional
    public Company createCompany(com.example.auth.domain.User user, CompanyRequest request) {
        // 사업자등록번호 중복 확인
        if (request.getBusinessRegistrationNumber() != null && 
            companyRepository.existsByBusinessRegistrationNumber(request.getBusinessRegistrationNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        Company company = Company.builder()
                .user(user)  // 사용자 설정 추가
                .companyName(request.getCompanyName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .contactPerson(request.getContactPerson())
                .phoneNumber(request.getPhoneNumber())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("업체가 생성되었습니다. ID: {}, 업체명: {}, 소유자: {}", 
                savedCompany.getId(), savedCompany.getCompanyName(), user.getNickname());

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

    /**
     * 사용자 ID로 업체 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return 업체 정보 (Optional)
     */
    public Optional<Company> findByUserId(Long userId) {
        return companyRepository.findByUserId(userId);
    }

    /**
     * 사업자 정보를 생성하거나 업데이트합니다. (CLIENT 심사용)
     * @param user 사용자
     * @param request 사업자 정보 요청
     * @return 생성/수정된 업체 엔티티
     */
    @Transactional
    public Company createOrUpdateBusinessInfo(User user, BusinessInfoRequest request) {
        Optional<Company> existingCompany = companyRepository.findByUserId(user.getId());
        
        // 사업자등록번호 중복 확인 (기존 업체 제외)
        if (request.getBusinessRegistrationNumber() != null) {
            Optional<Company> duplicateCompany = companyRepository.findByBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
            if (duplicateCompany.isPresent() && 
                (existingCompany.isEmpty() || !duplicateCompany.get().getId().equals(existingCompany.get().getId()))) {
                throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
            }
        }
        
        if (existingCompany.isPresent()) {
            // 기존 업체 정보 업데이트
            Company company = existingCompany.get();
            company.updateCompanyInfo(
                request.getCompanyName(),
                company.getContactPerson(), // 기존값 유지
                company.getPhoneNumber(),   // 기존값 유지  
                request.getBusinessRegistrationNumber()
            );
            log.info("사업자 정보가 업데이트되었습니다. userId={}, companyName={}", user.getId(), request.getCompanyName());
            return company;
        } else {
            // 새 업체 생성
            Company company = Company.builder()
                    .user(user)
                    .companyName(request.getCompanyName())
                    .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                    .build();
            Company savedCompany = companyRepository.save(company);
            log.info("새 사업자 정보가 생성되었습니다. userId={}, companyName={}", user.getId(), request.getCompanyName());
            return savedCompany;
        }
    }

    /**
     * 사업자 정보를 신규 등록만 허용합니다. (CLIENT 심사용 - 수정 불가)
     * @param user 사용자
     * @param request 사업자 정보 요청
     * @return 생성된 업체 엔티티
     * @throws IllegalArgumentException 이미 등록된 사업자 정보가 있거나 중복된 사업자등록번호인 경우, 약관 미동의인 경우
     */
    @Transactional
    public Company createBusinessInfoOnly(User user, BusinessInfoRequest request) {
        // 약관 동의 여부 확인
        if (request.getTermsAgreed() == null || !request.getTermsAgreed()) {
            throw new IllegalArgumentException("약관에 동의해야 사업자 정보를 등록할 수 있습니다.");
        }
        
        // 이미 등록된 사업자 정보가 있는지 확인
        Optional<Company> existingCompany = companyRepository.findByUserId(user.getId());
        if (existingCompany.isPresent()) {
            throw new IllegalArgumentException("이미 사업자 정보가 등록되어 있습니다.");
        }
        
        // 사업자등록번호 중복 확인
        if (request.getBusinessRegistrationNumber() != null) {
            Optional<Company> duplicateCompany = companyRepository.findByBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
            if (duplicateCompany.isPresent()) {
                throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
            }
        }
        
        // 새 업체 생성
        Company company = Company.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .termsAgreed(request.getTermsAgreed())
                .termsAgreedAt(ZonedDateTime.now())
                .build();
        Company savedCompany = companyRepository.save(company);
        log.info("사업자 정보가 신규 등록되었습니다. userId={}, companyName={}, termsAgreed={}", 
                user.getId(), request.getCompanyName(), request.getTermsAgreed());
        return savedCompany;
    }

    /**
     * 사용자의 업체 정보를 삭제합니다.
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        Optional<Company> company = companyRepository.findByUserId(userId);
        if (company.isPresent()) {
            companyRepository.delete(company.get());
            log.info("사업자 정보가 삭제되었습니다. userId={}", userId);
        }
    }

    /**
     * 업체 정보를 저장합니다.
     * @param company 업체 엔티티
     * @return 저장된 업체 엔티티
     */
    @Transactional
    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }
}
