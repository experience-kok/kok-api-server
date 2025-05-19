package com.example.auth.config;

import com.example.auth.service.S3Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UserDTO 관련 설정을 제공하는 클래스
 */
@Configuration
public class UserDtoConfig {

    /**
     * S3Service 인스턴스를 스태틱 변수에 저장하여 UserDTO에서 사용할 수 있게 함
     * 
     * @param s3Service S3Service 빈
     * @return S3Service 인스턴스
     */
    @Bean
    public S3Service userDtoS3Service(S3Service s3Service) {
        UserDtoHelper.setS3Service(s3Service);
        return s3Service;
    }
    
    /**
     * S3Service 인스턴스를 정적으로 제공하는 헬퍼 클래스
     */
    public static class UserDtoHelper {
        private static S3Service s3Service;
        
        public static void setS3Service(S3Service service) {
            s3Service = service;
        }
        
        public static S3Service getS3Service() {
            return s3Service;
        }
    }
}
