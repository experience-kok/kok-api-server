package com.example.auth.service;

import com.example.auth.constant.PlatformType;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.repository.UserSnsPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnsCrawlService {

    private final UserSnsPlatformRepository platformRepository;

    @Value("${selenium.chromedriver.path:/usr/local/bin/chromedriver}")
    private String chromeDriverPath;

    /**
     * SNS 데이터 크롤링 (비동기 처리)
     * @param platformId 크롤링할 플랫폼 ID
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> crawlSnsDataAsync(Long platformId) {
        try {
            crawlSnsData(platformId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 크롤링 작업 실패: platformId={}, error={}", platformId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * SNS 데이터 크롤링 (동기 처리)
     * @param platformId 크롤링할 플랫폼 ID
     */
    public void crawlSnsData(Long platformId) {
        log.info("SNS 데이터 크롤링 시작: platformId={}", platformId);

        UserSnsPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new RuntimeException("플랫폼을 찾을 수 없습니다: " + platformId));

        try {
            // 플랫폼 타입에 따라 다른 크롤링 로직 적용
            switch(platform.getPlatformType()) {
                case "instagram":
                    crawlInstagram(platform);
                    break;
                case "youtube":
                    crawlYoutube(platform);
                    break;
                case "blog":
                    crawlBlog(platform);
                    break;
                default:
                    log.warn("지원하지 않는 플랫폼 타입: {}", platform.getPlatformType());
            }

            // 마지막 크롤링 시간 업데이트
            platform.updateLastCrawledAt(LocalDateTime.now());
            platformRepository.save(platform);

            log.info("SNS 데이터 크롤링 완료: platformId={}, followerCount={}",
                    platformId, platform.getFollowerCount());
        } catch (Exception e) {
            log.error("SNS 데이터 크롤링 실패: platformId={}, error={}", platformId, e.getMessage(), e);
            throw new RuntimeException("SNS 데이터 크롤링 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 인스타그램 크롤링
     */
    private void crawlInstagram(UserSnsPlatform platform) {
        log.info("인스타그램 크롤링 시작: url={}", platform.getAccountUrl());

        // ChromeDriver 경로 설정
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",         // 새로운 헤드리스 모드
                "--no-sandbox",           // 샌드박스 비활성화 (Linux에서 필요)
                "--disable-dev-shm-usage", // 공유 메모리 사용 제한
                "--disable-gpu",          // GPU 가속 비활성화
                "--window-size=1920,1080", // 창 크기 설정
                "--remote-allow-origins=*" // CORS 문제 방지
        );
        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.get(platform.getAccountUrl());

            // 명시적 대기 설정: 최대 10초간 대기하며 팔로워 수 요소가 나타날 때까지 기다림
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 팔로워 수 요소 찾기: title 속성이 '만', '천'으로 끝나거나 비어있지 않은 title을 가진 span 요소
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("span[title$='만'], span[title$='천'], span[title]:not([title=''])")
            ));

            String rawTitle = element.getAttribute("title"); // 예: "15.4만"
            log.info("인스타그램 팔로워 문자열: {}", rawTitle);

            // 숫자로 변환
            int followerCount = parseKoreanFollowerString(rawTitle);
            log.info("인스타그램 팔로워 숫자 변환 결과: {}", followerCount);

            // 팔로워 수 업데이트
            platform.updateFollowerCount(followerCount);

            log.info("인스타그램 크롤링 완료: url={}, followerCount={}",
                    platform.getAccountUrl(), followerCount);
        } catch (Exception e) {
            log.error("인스타그램 크롤링 실패: url={}, error={}",
                    platform.getAccountUrl(), e.getMessage(), e);
            throw new RuntimeException("인스타그램 크롤링 중 오류가 발생했습니다.", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.debug("WebDriver 종료 완료");
            }
        }
    }

    /**
     * 유튜브 크롤링
     */
    private void crawlYoutube(UserSnsPlatform platform) {
        log.info("유튜브 크롤링 시작: url={}", platform.getAccountUrl());

        // ChromeDriver 경로 설정
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",         // 새로운 헤드리스 모드
                "--no-sandbox",           // 샌드박스 비활성화 (Linux에서 필요)
                "--disable-dev-shm-usage", // 공유 메모리 사용 제한
                "--disable-gpu",          // GPU 가속 비활성화
                "--window-size=1920,1080", // 창 크기 설정
                "--remote-allow-origins=*" // CORS 문제 방지
        );
        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.get(platform.getAccountUrl());

            // 명시적 대기 설정
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 1. 새로운 UI: 제공된 클래스 이름으로 요소 찾기
            WebElement element = null;
            try {
                element = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("td.style-scope.ytd-about-channel-renderer")
                ));

                // "구독자 35.6만명" 형태의 문자열에서 숫자 추출
                String subscriberText = element.getText();
                log.info("유튜브 구독자 문자열: {}", subscriberText);

                // 정규식으로 숫자와 단위 추출
                Pattern pattern = Pattern.compile("구독자\\s+([0-9.,]+)\\s*([만천]*)명");
                Matcher matcher = pattern.matcher(subscriberText);

                if (matcher.find()) {
                    String numPart = matcher.group(1); // 숫자 부분 (예: "35.6")
                    String unitPart = matcher.group(2); // 단위 부분 (예: "만")

                    // 숫자 부분과 단위를 조합해서 변환
                    String fullNumber = numPart + unitPart;
                    int subscriberCount = parseKoreanFollowerString(fullNumber);

                    // 팔로워 수 업데이트
                    platform.updateFollowerCount(subscriberCount);
                    log.info("유튜브 구독자 수 추출 완료: {}", subscriberCount);
                } else {
                    throw new RuntimeException("구독자 수 텍스트 형식이 예상과 다릅니다: " + subscriberText);
                }
            } catch (Exception e) {
                // 첫 번째 방법 실패 시 다른 방법 시도
                log.warn("첫 번째 방법으로 유튜브 구독자 수 추출 실패, 다른 방법 시도: {}", e.getMessage());

                // 2. 표준 셀렉터 시도: #subscriber-count
                try {
                    element = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("#subscriber-count")
                    ));

                    String subscriberText = element.getText(); // 예: "35.6만 구독자"
                    log.info("유튜브 구독자 문자열(대체 방법): {}", subscriberText);

                    // 정규식으로 숫자와 단위 추출
                    Pattern pattern = Pattern.compile("([0-9.,]+)\\s*([만천]*)\\s*구독자");
                    Matcher matcher = pattern.matcher(subscriberText);

                    if (matcher.find()) {
                        String numPart = matcher.group(1);
                        String unitPart = matcher.group(2);

                        String fullNumber = numPart + unitPart;
                        int subscriberCount = parseKoreanFollowerString(fullNumber);

                        platform.updateFollowerCount(subscriberCount);
                        log.info("유튜브 구독자 수 추출 완료(대체 방법): {}", subscriberCount);
                    } else {
                        throw new RuntimeException("구독자 수 텍스트 형식이 예상과 다릅니다: " + subscriberText);
                    }
                } catch (Exception ex) {
                    log.error("두 번째 방법으로도 유튜브 구독자 수 추출 실패: {}", ex.getMessage());
                    throw new RuntimeException("유튜브 구독자 수를 찾을 수 없습니다.", ex);
                }
            }

            log.info("유튜브 크롤링 완료: url={}, subscriberCount={}",
                    platform.getAccountUrl(), platform.getFollowerCount());
        } catch (Exception e) {
            log.error("유튜브 크롤링 실패: url={}, error={}",
                    platform.getAccountUrl(), e.getMessage(), e);
            throw new RuntimeException("유튜브 크롤링 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.debug("WebDriver 종료 완료");
            }
        }
    }

    /**
     * 네이버 블로그 크롤링
     */
    private void crawlBlog(UserSnsPlatform platform) {
        log.info("네이버 블로그 크롤링 시작: url={}", platform.getAccountUrl());

        // ChromeDriver 경로 설정
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",         // 새로운 헤드리스 모드
                "--no-sandbox",           // 샌드박스 비활성화 (Linux에서 필요)
                "--disable-dev-shm-usage", // 공유 메모리 사용 제한
                "--disable-gpu",          // GPU 가속 비활성화
                "--window-size=1920,1080", // 창 크기 설정
                "--remote-allow-origins=*" // CORS 문제 방지
        );
        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.get(platform.getAccountUrl());

            // 명시적 대기 설정
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 이웃 수 추출
            try {
                // 제공된 구조: <em> 9,751 </em>
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".blog_profile_area .num em, .blog-profile-area .num em")
                ));

                String followerText = element.getText().trim(); // "9,751"
                log.info("네이버 블로그 이웃 수 문자열: {}", followerText);

                // 쉼표 제거 후 정수 변환
                int followerCount = Integer.parseInt(followerText.replace(",", ""));

                // 이웃 수 업데이트
                platform.updateFollowerCount(followerCount);
                log.info("네이버 블로그 이웃 수 추출 완료: {}", followerCount);
            } catch (Exception e) {
                // 첫 번째 방법 실패 시 대체 방법 시도
                log.warn("첫 번째 방법으로 네이버 블로그 이웃 수 추출 실패, 다른 방법 시도: {}", e.getMessage());

                try {
                    // 블로그 구조가 다양할 수 있으므로 여러 선택자 시도
                    List<WebElement> elements = driver.findElements(By.cssSelector(
                            ".blog_profile_area .num, " +
                                    ".blog-profile-area .num, " +
                                    ".profile_info .follower_count"
                    ));

                    if (!elements.isEmpty()) {
                        String followerText = elements.get(0).getText().trim();
                        log.info("네이버 블로그 이웃 수 문자열(대체 방법): {}", followerText);

                        // 숫자만 추출하는 정규식
                        Pattern pattern = Pattern.compile("([0-9,]+)");
                        Matcher matcher = pattern.matcher(followerText);

                        if (matcher.find()) {
                            String numStr = matcher.group(1);
                            int followerCount = Integer.parseInt(numStr.replace(",", ""));

                            platform.updateFollowerCount(followerCount);
                            log.info("네이버 블로그 이웃 수 추출 완료(대체 방법): {}", followerCount);
                        } else {
                            throw new RuntimeException("이웃 수 텍스트 형식이 예상과 다릅니다: " + followerText);
                        }
                    } else {
                        throw new RuntimeException("네이버 블로그 이웃 수 요소를 찾을 수 없습니다.");
                    }
                } catch (Exception ex) {
                    log.error("두 번째 방법으로도 네이버 블로그 이웃 수 추출 실패: {}", ex.getMessage());
                    throw new RuntimeException("네이버 블로그 이웃 수를 찾을 수 없습니다.", ex);
                }
            }

            log.info("네이버 블로그 크롤링 완료: url={}, followerCount={}",
                    platform.getAccountUrl(), platform.getFollowerCount());
        } catch (Exception e) {
            log.error("네이버 블로그 크롤링 실패: url={}, error={}",
                    platform.getAccountUrl(), e.getMessage(), e);
            throw new RuntimeException("네이버 블로그 크롤링 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.debug("WebDriver 종료 완료");
            }
        }
    }

    /**
     * 한글 팔로워 문자열을 숫자로 변환 ("15.4만", "1.2천", "1234" 등)
     */
    private int parseKoreanFollowerString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }

        str = str.replace(",", "").trim();

        if (str.endsWith("만")) {
            return (int)(Double.parseDouble(str.replace("만", "")) * 10000);
        } else if (str.endsWith("천")) {
            return (int)(Double.parseDouble(str.replace("천", "")) * 1000);
        } else {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                log.warn("숫자 변환 실패: {}, 0 반환", str);
                return 0;
            }
        }
    }
}