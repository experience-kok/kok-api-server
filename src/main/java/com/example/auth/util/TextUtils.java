package com.example.auth.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 텍스트 처리 유틸리티
 */
public class TextUtils {

    private static final Set<String> STOP_WORDS = Set.of(
            "의", "이", "가", "을", "를", "에", "에서", "와", "과", "하고", "그리고",
            "모집", "체험단", "리뷰어", "블로거", "인플루언서", "참여자", "선정",
            "이벤트", "행사", "프로모션", "광고", "마케팅"
    );

    /**
     * 텍스트에서 의미있는 키워드 추출
     */
    public static Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Set.of();
        }

        return Arrays.stream(text.split("[\\s\\-_,().]+"))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .filter(word -> word.length() >= 2) // 2글자 이상
                .filter(word -> word.length() <= 10) // 10글자 이하
                .filter(word -> !STOP_WORDS.contains(word)) // 불용어 제거
                .filter(word -> !isNumeric(word)) // 숫자만 있는 단어 제거
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * 문자열이 숫자로만 구성되어 있는지 확인
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("\\d+");
    }

    /**
     * 검색어 정규화 (공백 제거, 소문자 변환)
     */
    public static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase();
    }

    /**
     * 텍스트가 유효한 검색어인지 확인
     */
    public static boolean isValidSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        String normalized = normalizeQuery(query);
        return normalized.length() >= 1 && normalized.length() <= 50;
    }

    /**
     * 유사도 계산 (간단한 포함 관계 기반)
     */
    public static double calculateSimilarity(String text, String query) {
        if (text == null || query == null) {
            return 0.0;
        }
        
        String normalizedText = normalizeQuery(text);
        String normalizedQuery = normalizeQuery(query);
        
        if (normalizedText.equals(normalizedQuery)) {
            return 1.0; // 완전 일치
        } else if (normalizedText.startsWith(normalizedQuery)) {
            return 0.8; // 접두사 일치
        } else if (normalizedText.contains(normalizedQuery)) {
            return 0.6; // 부분 일치
        } else {
            return 0.0; // 일치하지 않음
        }
    }

    /**
     * 검색어 하이라이팅을 위한 정보 생성
     */
    public static HighlightInfo createHighlightInfo(String text, String query) {
        String normalizedText = normalizeQuery(text);
        String normalizedQuery = normalizeQuery(query);
        
        int startIndex = normalizedText.indexOf(normalizedQuery);
        
        if (startIndex == -1) {
            return new HighlightInfo(text, -1, -1);
        }
        
        return new HighlightInfo(text, startIndex, startIndex + normalizedQuery.length());
    }

    /**
     * 하이라이트 정보 클래스
     */
    public static class HighlightInfo {
        public final String originalText;
        public final int startIndex;
        public final int endIndex;
        
        public HighlightInfo(String originalText, int startIndex, int endIndex) {
            this.originalText = originalText;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public boolean hasHighlight() {
            return startIndex >= 0 && endIndex > startIndex;
        }
        
        public String getPrefix() {
            return hasHighlight() ? originalText.substring(0, startIndex) : "";
        }
        
        public String getHighlighted() {
            return hasHighlight() ? originalText.substring(startIndex, endIndex) : "";
        }
        
        public String getSuffix() {
            return hasHighlight() ? originalText.substring(endIndex) : originalText;
        }
    }
}
