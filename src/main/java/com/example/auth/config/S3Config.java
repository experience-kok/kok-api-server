package com.example.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class S3Config {

    // region과 bucket은 계속 필요합니다.
    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.accelerate.enabled:false}")
    private boolean accelerateEnabled;

    @Bean
    @Primary
    public AmazonS3Client amazonS3Client() {

        //  BasicAWSCredentials 및 AWSStaticCredentialsProvider 사용 코드 제거

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(region);

        // S3 Transfer Acceleration 활성화 (옵션)
        if (accelerateEnabled) {
            builder.enableAccelerateMode();
        }

        // AmazonS3ClientBuilder.standard()를 사용하면 자동으로 자격 증명을 찾기 때문에,
        // 별도의 자격 증명 설정 없이 build()를 호출하면 됩니다.
        return (AmazonS3Client) builder.build();
    }

}
