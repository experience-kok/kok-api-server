package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 이미지 최적화 서비스
 * 이미지 리사이징 및 압축 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOptimizationService {

    @Value("${image.optimization.profile.size:400}")
    private int profileImageSize;

    @Value("${image.optimization.thumbnail.size:600}")
    private int thumbnailSize;

    @Value("${image.optimization.quality:0.7}")
    private float imageQuality;

    private final S3Service s3Service;

    /**
     * 프로필 이미지 최적화 후 S3에 업로드
     *
     * @param file 원본 이미지 파일
     * @return 최적화된 이미지의 S3 URL
     */
    public String optimizeAndUploadProfileImage(MultipartFile file) throws IOException {
        byte[] optimizedImage = optimizeImage(file.getBytes(), profileImageSize, imageQuality);
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        // 최적화된 이미지에 대한 presigned URL 생성
        String presignedUrl = s3Service.generatePresignedUrl(fileExtension);
        
        // 업로드 처리 로직 (여기서는 S3Service에 uploadOptimizedImage 메서드가 있다고 가정)
        // 실제 구현시에는 이 부분을 수정해야 함
        String imageUrl = s3Service.uploadOptimizedImage(presignedUrl, optimizedImage, "image/" + fileExtension);
        
        return imageUrl;
    }

    /**
     * 캠페인 썸네일 이미지 최적화 후 S3에 업로드
     *
     * @param file 원본 이미지 파일
     * @return 최적화된 이미지의 S3 URL
     */
    public String optimizeAndUploadThumbnailImage(MultipartFile file) throws IOException {
        byte[] optimizedImage = optimizeImage(file.getBytes(), thumbnailSize, imageQuality);
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        // 최적화된 이미지에 대한 presigned URL 생성
        String presignedUrl = s3Service.generatePresignedUrl(fileExtension);
        
        // 업로드 처리 로직
        String imageUrl = s3Service.uploadOptimizedImage(presignedUrl, optimizedImage, "image/" + fileExtension);
        
        return imageUrl;
    }

    /**
     * 이미지 최적화 - 크기 조정 및 압축
     *
     * @param imageData 원본 이미지 데이터
     * @param maxDimension 최대 가로/세로 크기
     * @param quality 압축 품질 (0.0 ~ 1.0)
     * @return 최적화된 이미지 데이터
     */
    public byte[] optimizeImage(byte[] imageData, int maxDimension, float quality) throws IOException {
        // 이미지 로드
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("이미지를 로드할 수 없습니다");
        }

        // 원본 크기
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 크기 조정 필요 여부 확인
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            // 크기가 이미 작으면 압축만 수행
            return compressImage(originalImage, quality);
        }

        // 비율을 유지하면서 크기 조정
        BufferedImage resizedImage = resizeImage(originalImage, maxDimension);
        
        // 압축
        return compressImage(resizedImage, quality);
    }

    /**
     * 이미지 크기 조정
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxDimension) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 새 크기 계산 (비율 유지)
        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            newWidth = maxDimension;
            newHeight = (int) (originalHeight * ((double) maxDimension / originalWidth));
        } else {
            newHeight = maxDimension;
            newWidth = (int) (originalWidth * ((double) maxDimension / originalHeight));
        }
        
        // 새 이미지 생성
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        
        // 고품질 렌더링 설정
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 이미지 그리기
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resizedImage;
    }

    /**
     * 이미지 압축
     */
    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 이미지 포맷 및 압축 설정
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG 이미지 라이터를 찾을 수 없습니다");
        }
        
        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);
        
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 품질 설정 (0.0 ~ 1.0)
        
        // 이미지 쓰기
        writer.write(null, new IIOImage(image, null, null), param);
        
        // 리소스 정리
        ios.close();
        writer.dispose();
        
        return outputStream.toByteArray();
    }

    /**
     * 파일명에서 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "jpg";
        }
        
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        
        return "jpg";
    }
}
