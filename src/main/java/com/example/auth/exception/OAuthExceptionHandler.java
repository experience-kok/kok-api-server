package com.example.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OAuthExceptionHandler {

    // 카카오 콜백 URL 처리 메서드
    private ModelAndView handleKakaoCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("카카오 OAuth 콜백 URL 요청 처리중: {}", request.getRequestURI());

        // 직접 HttpServletResponse 사용
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());

        PrintWriter out = response.getWriter();
        out.write("<html><body><script>");
        out.write("window.onload = function() {");
        out.write("  if (window.opener) {");
        out.write("    window.opener.postMessage({");
        out.write("      type: 'KAKAO_AUTH_CODE',");
        out.write("      code: new URLSearchParams(window.location.search).get('code')");
        out.write("    }, '*');");
        out.write("    window.close();");
        out.write("  } else {");
        out.write("    location.href = '/';");
        out.write("  }");
        out.write("};");
        out.write("</script>");
        out.write("<p>카카오 로그인 인증이 완료되었습니다. 잠시만 기다려주세요...</p>");
        out.write("</body></html>");
        out.flush();

        return new ModelAndView();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.warn("리소스를 찾지 못했습니다: {}", ex.getMessage());

        if (ex.getMessage().contains("/login/oauth2/code/kakao")) {
            return handleKakaoCallback(request, response);
        }

        // JSON 응답을 위한 ModelAndView 생성
        ModelAndView mav = new ModelAndView(new MappingJackson2JsonView());

        // 공통 에러 응답 포맷에 맞게 수정
        HttpStatus status = HttpStatus.NOT_FOUND;
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "요청한 리소스를 찾을 수 없습니다.");
        body.put("errorCode", "RESOURCE_NOT_FOUND");
        body.put("status", status.value());

        mav.addAllObjects(body);
        mav.setStatus(status);

        return mav;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.warn("지원되지 않는 HTTP 메서드: {}, URI: {}", ex.getMessage(), request.getRequestURI());

        // 카카오 콜백 URL에 대한 요청인 경우
        if (request.getRequestURI().contains("/login/oauth2/code/kakao")) {
            return handleKakaoCallback(request, response);
        }

        // JSON 응답을 위한 ModelAndView 생성
        ModelAndView mav = new ModelAndView(new MappingJackson2JsonView());

        // 공통 에러 응답 포맷에 맞게 수정
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "요청 메서드가 지원되지 않습니다.");
        body.put("errorCode", "METHOD_NOT_ALLOWED");
        body.put("status", status.value());

        mav.addAllObjects(body);
        mav.setStatus(status);

        return mav;
    }
}