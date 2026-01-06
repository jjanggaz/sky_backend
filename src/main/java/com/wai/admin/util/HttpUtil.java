package com.wai.admin.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 호출 유틸리티 클래스 - Apache HttpClient 기반
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    // 재시도 관련 기본 설정값
    private static final int MAX_RETRY_COUNT = 2;          // 최초 1회 + 추가 2회 재시도 = 최대 3번 시도
    private static final long RETRY_DELAY_MILLIS = 1000L;  // 재시도 사이 대기 시간 (1초)
    
    /**
     * 헤더에서 system_code를 가져오는 헬퍼 메서드
     */
    private static String getSystemCodeFromHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String systemCode = request.getHeader("system_code");
                if (systemCode != null && !systemCode.trim().isEmpty()) {
                    logger.debug("헤더에서 system_code 추출: {}", systemCode);
                    return systemCode.trim();
                }
            }
        } catch (Exception e) {
            logger.warn("헤더에서 system_code 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 쿠키에서 액세스 토큰을 자동으로 추출하는 메서드 (system_code 기반)
     * @param systemCode 
     */
    private static String getAccessTokenFromCookie(String systemCode) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // system_code에 따른 쿠키 이름 결정
                String cookieName;
                if ("WAI_WEB_ADMIN".equals(systemCode)) {
                    cookieName = "admin_access";
                } else {
                    cookieName = "webView_access";
                }
                
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookieName.equals(cookie.getName())) {
                            logger.debug("쿠키에서 액세스 토큰 자동 추출 (system_code: {}): {}", systemCode, cookie.getValue());
                            return cookie.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("쿠키에서 액세스 토큰 자동 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 쿠키에서 리프레시 토큰을 자동으로 추출하는 메서드 (system_code 기반)
     * @param systemCode
     */
    private static String getRefreshTokenFromCookie(String systemCode) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // system_code에 따른 쿠키 이름 결정
                String cookieName;
                if ("WAI_WEB_ADMIN".equals(systemCode)) {
                    cookieName = "admin_refresh";
                } else {
                    cookieName = "webView_refresh";
                }
                
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookieName.equals(cookie.getName())) {
                            logger.debug("쿠키에서 리프레시 토큰 자동 추출 (system_code: {}): {}", systemCode, cookie.getValue());
                            return cookie.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("쿠키에서 리프레시 토큰 자동 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 쿠키에서 세션 토큰을 자동으로 추출하는 메서드 (system_code 기반)
     * @param systemCode
     */
    private static String getSessionTokenFromCookie(String systemCode) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // system_code에 따른 쿠키 이름 결정
                String cookieName;
                if ("WAI_WEB_ADMIN".equals(systemCode)) {
                    cookieName = "admin_session";
                } else {
                    cookieName = "webView_session";
                }
                
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookieName.equals(cookie.getName())) {
                            logger.debug("쿠키에서 세션 토큰 자동 추출 (system_code: {}): {}", systemCode, cookie.getValue());
                            return cookie.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("쿠키에서 세션 토큰 자동 추출 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * HTTP POST 요청 수행 (자동 토큰 추출)
     * @param url 요청 URL
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    public static HttpResult post(String url, String contentType, String requestBody) {
        return executeRequest(url, "POST", contentType, requestBody);
    }
    
    /**
     * HTTP POST 요청 수행 (multipart/form-data 지원)
     * @param url 요청 URL
     * @param formData multipart 데이터
     * @return HTTP 응답 결과
     */
    public static HttpResult postMultipart(String url, Map<String, Object> formData) {
        return executeMultipartRequest(url, "POST", formData);
    }
    
    /**
     * HTTP PATCH 요청 수행 (multipart/form-data 지원)
     * @param url 요청 URL
     * @param formData multipart 데이터
     * @return HTTP 응답 결과
     */
    public static HttpResult patchMultipart(String url, Map<String, Object> formData) {
        return executePatchMultipartRequest(url, formData);
    }
    
    /**
     * HTTP GET 요청 수행 (자동 토큰 추출)
     * @param url 요청 URL
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    public static HttpResult get(String url, String contentType, String requestBody) {
        return executeRequest(url, "GET", contentType, requestBody);
    }
    
    /**
     * HTTP PUT 요청 수행 (자동 토큰 추출)
     * @param url 요청 URL
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    public static HttpResult put(String url, String contentType, String requestBody) {
        return executeRequest(url, "PUT", contentType, requestBody);
    }
    
    /**
     * HTTP PATCH 요청 수행 (자동 토큰 추출) - WebClient로 완전 지원
     * @param url 요청 URL
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    public static HttpResult patch(String url, String contentType, String requestBody) {
        return executeRequest(url, "PATCH", contentType, requestBody);
    }
    
    /**
     * HTTP DELETE 요청 수행 (자동 토큰 추출)
     * @param url 요청 URL
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    public static HttpResult delete(String url, String contentType, String requestBody) {
        return executeRequest(url, "DELETE", contentType, requestBody);
    }
    
    /**
     * HTTP 요청 실행 - Apache HttpClient 기반
     * @param url 요청 URL
     * @param method HTTP 메서드
     * @param contentType Content-Type 헤더
     * @param requestBody 요청 본문
     * @return HTTP 응답 결과
     */
    private static HttpResult executeRequest(String url, String method, String contentType, String requestBody) {
        int attempt = 0;
        while (true) {
            attempt++;
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                logger.debug("Apache HttpClient HTTP 요청 시작 (시도 {}): {} {}", attempt, method, url);

                // HTTP 메서드에 따른 요청 객체 생성
                ClassicHttpRequest request;
                switch (method.toUpperCase()) {
                    case "GET":
                        request = new HttpGet(url);
                        break;
                    case "POST":
                        request = new HttpPost(url);
                        break;
                    case "PUT":
                        request = new HttpPut(url);
                        break;
                    case "PATCH":
                        request = new HttpPatch(url);
                        break;
                    case "DELETE":
                        request = new HttpDelete(url);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                }

                // 헤더 설정
                request.setHeader("Accept", "application/json");
                if (contentType != null && !contentType.isEmpty()) {
                    request.setHeader("Content-Type", contentType);
                }

                // 헤더에서 system_code 확인하여 쿠키 이름 결정
                String systemCode = getSystemCodeFromHeader();

                // Cookie 헤더 자동 설정 (system_code 기반 쿠키에서 토큰 추출)
                String accessToken = "";
                String refreshToken = "";
                String sessionToken = "";
                if (systemCode != null) {
                    accessToken = getAccessTokenFromCookie(systemCode);
                    refreshToken = getRefreshTokenFromCookie(systemCode);
                    sessionToken = getSessionTokenFromCookie(systemCode);
                }

                StringBuilder cookieBuilder = new StringBuilder();
                if (accessToken != null && !accessToken.isEmpty()) {
                    cookieBuilder.append("wai_access=").append(accessToken);
                }
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                    cookieBuilder.append("wai_refresh=").append(refreshToken);
                }
                if (sessionToken != null && !sessionToken.isEmpty()) {
                    if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                    cookieBuilder.append("wai_session=").append(sessionToken);
                }

                if (cookieBuilder.length() > 0) {
                    request.setHeader("Cookie", cookieBuilder.toString());
                    logger.debug("Cookie 헤더 자동 설정: {}", cookieBuilder.toString());
                }

                // 요청 본문 설정
                if (requestBody != null && !requestBody.isEmpty() && !"GET".equals(method) && !"DELETE".equals(method)) {
                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    if (request instanceof HttpPost) {
                        ((HttpPost) request).setEntity(entity);
                    } else if (request instanceof HttpPut) {
                        ((HttpPut) request).setEntity(entity);
                    } else if (request instanceof HttpPatch) {
                        ((HttpPatch) request).setEntity(entity);
                    }
                    logger.debug("요청 본문: {}", requestBody);
                }

                // 요청 실행
                try (ClassicHttpResponse response = httpClient.execute(request)) {
                    int status = response.getCode();
                    String responseBody = "";

                    // 응답 본문이 있는 경우에만 읽기
                    if (response.getEntity() != null) {
                        responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    }

                    // 외부 API의 Set-Cookie를 파싱하고 system_code에 맞게 변환하여 브라우저로 전달
                    org.apache.hc.core5.http.Header[] setCookieHeaders = response.getHeaders("Set-Cookie");
                    if (setCookieHeaders != null && setCookieHeaders.length > 0 && systemCode != null) {
                        processCookiesFromExternalApi(setCookieHeaders, systemCode);
                    }

                    logger.info("HTTP 응답 코드: {}", status);
                    logger.info("HTTP 응답: {}", responseBody);

                    return new HttpResult(status, responseBody, null);
                }

            } catch (Exception e) {
                boolean retryable = isRetryableNetworkException(e);
                if (retryable && attempt <= MAX_RETRY_COUNT + 1) {
                    if (attempt <= MAX_RETRY_COUNT + 0) { // 남은 재시도가 있을 때만 로그 및 대기
                        logger.warn("HTTP 요청 실패(시도 {}), 재시도 예정: {} - {}", attempt, e.getClass().getSimpleName(), e.getMessage());
                    }
                    if (attempt <= MAX_RETRY_COUNT) {
                        try {
                            Thread.sleep(RETRY_DELAY_MILLIS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.error("재시도 대기 중 인터럽트 발생", ie);
                            return new HttpResult(-1, null, e.getMessage());
                        }
                        continue;
                    }
                }

                logger.error("Apache HttpClient 요청 중 오류 발생 (최종 실패, 시도 {}): {}", attempt, e.getMessage(), e);
                return new HttpResult(-1, null, e.getMessage());
            }
        }
    }

    /**
     * 재시도 대상 네트워크 예외인지 여부 판단
     */
    private static boolean isRetryableNetworkException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConnectException ||
                cause instanceof SocketTimeoutException ||
                cause instanceof ConnectTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
    
    /**
     * 외부 API의 Set-Cookie 헤더를 파싱하고 system_code에 맞게 변환하여 브라우저로 전달
     * @param setCookieHeaders Set-Cookie 헤더 배열
     * @param systemCode 시스템 코드
     */
    private static void processCookiesFromExternalApi(org.apache.hc.core5.http.Header[] setCookieHeaders, String systemCode) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            
            jakarta.servlet.http.HttpServletResponse servletResponse = attributes.getResponse();
            if (servletResponse == null) {
                return;
            }
            
            // system_code에 따른 쿠키 이름 prefix 결정
            String cookiePrefix = "WAI_WEB_ADMIN".equals(systemCode) ? "admin" : "webView";
            
            for (org.apache.hc.core5.http.Header header : setCookieHeaders) {
                String setCookieValue = header.getValue();
                logger.debug("외부 API의 Set-Cookie 처리: {}", setCookieValue);
                
                // Set-Cookie 값 파싱 (wai_refresh=xxx; Max-Age=604800; Path=/; SameSite=lax)
                String[] parts = setCookieValue.split(";");
                if (parts.length > 0) {
                    String[] cookiePair = parts[0].split("=", 2);
                    if (cookiePair.length == 2) {
                        String cookieName = cookiePair[0].trim();
                        String cookieValue = cookiePair[1].trim();
                        
                        // wai_* 쿠키를 system_code에 맞게 변환
                        String targetCookieName = null;
                        if ("wai_refresh".equals(cookieName)) {
                            targetCookieName = cookiePrefix + "_refresh";
                        } else if ("wai_session".equals(cookieName)) {
                            targetCookieName = cookiePrefix + "_session";
                        } else if ("wai_access".equals(cookieName)) {
                            targetCookieName = cookiePrefix + "_access";
                        }
                        
                        if (targetCookieName != null) {
                            // 나머지 속성들 (Max-Age, Path, SameSite 등)은 그대로 유지
                            StringBuilder newSetCookie = new StringBuilder(targetCookieName).append("=").append(cookieValue);
                            for (int i = 1; i < parts.length; i++) {
                                newSetCookie.append("; ").append(parts[i].trim());
                            }
                            
                            // HttpOnly 속성 추가 (없으면)
                            String finalSetCookie = newSetCookie.toString();
                            if (!finalSetCookie.toLowerCase().contains("httponly")) {
                                finalSetCookie += "; HttpOnly";
                            }
                            
                            servletResponse.addHeader("Set-Cookie", finalSetCookie);
                            logger.debug("외부 API 쿠키를 변환하여 브라우저로 전달: {} -> {}", cookieName, targetCookieName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("외부 API Set-Cookie 처리 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * HTTP multipart 요청 실행 - Apache HttpClient 기반
     * @param url 요청 URL
     * @param method HTTP 메서드
     * @param formData multipart 데이터
     * @return HTTP 응답 결과
     */
    private static HttpResult executeMultipartRequest(String url, String method, Map<String, Object> formData) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            logger.debug("Apache HttpClient Multipart HTTP 요청 시작: {} {}", method, url);
            
            // HTTP 메서드에 따른 요청 객체 생성
            ClassicHttpRequest request;
            switch (method.toUpperCase()) {
                case "POST":
                    request = new HttpPost(url);
                    break;
                default:
                    throw new IllegalArgumentException("Multipart requests only support POST method");
            }
            
            // 헤더 설정
            request.setHeader("Accept", "application/json");

            // 헤더에서 system_code 확인하여 쿠키 이름 결정
            String systemCode = getSystemCodeFromHeader();
            
            // Cookie 헤더 자동 설정 (system_code 기반 쿠키에서 토큰 추출)
            String accessToken = "";
            String refreshToken = "";
            String sessionToken = "";
            if (systemCode != null) {
                accessToken = getAccessTokenFromCookie(systemCode);
                refreshToken = getRefreshTokenFromCookie(systemCode);
                sessionToken = getSessionTokenFromCookie(systemCode);
            }
            
            StringBuilder cookieBuilder = new StringBuilder();
            if (accessToken != null && !accessToken.isEmpty()) {
                cookieBuilder.append("wai_access=").append(accessToken);
            }
            if (refreshToken != null && !refreshToken.isEmpty()) {
                if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                cookieBuilder.append("wai_refresh=").append(refreshToken);
            }
            if (sessionToken != null && !sessionToken.isEmpty()) {
                if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                cookieBuilder.append("wai_session=").append(sessionToken);
            }
            
            if (cookieBuilder.length() > 0) {
                request.setHeader("Cookie", cookieBuilder.toString());
                logger.debug("Cookie 헤더 자동 설정: {}", cookieBuilder.toString());
            }
            
            // Multipart 데이터 설정
            if (formData != null && !formData.isEmpty()) {
                try {
                    // Spring의 RestTemplate을 사용하여 multipart 전송
                    org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
                    
                    // 모든 쿠키를 RestTemplate 헤더에 설정
                    if (cookieBuilder.length() > 0) {
                        headers.add("Cookie", cookieBuilder.toString());
                        logger.debug("RestTemplate에 쿠키 헤더 설정: {}", cookieBuilder.toString());
                    }
                    
                    // Multipart 데이터 구성
                    org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                    
                    for (Map.Entry<String, Object> entry : formData.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        if (value instanceof org.springframework.web.multipart.MultipartFile) {
                            org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) value;
                            if (file != null && !file.isEmpty()) {
                                try {
                                    org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                                        @Override
                                        public String getFilename() {
                                            return file.getOriginalFilename();
                                        }
                                    };
                                    body.add(key, fileResource);
                                    logger.debug("파일 추가: key={}, fileName={}, size={}", key, file.getOriginalFilename(), file.getSize());
                                } catch (Exception e) {
                                    logger.warn("파일 처리 중 오류: key={}, error={}", key, e.getMessage());
                                }
                            }
                        } else if (value != null) {
                            // 일반 텍스트 데이터
                            body.add(key, value.toString());
                            logger.debug("텍스트 데이터 추가: key={}, value={}", key, value);
                        }
                    }
                    
                    org.springframework.http.HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = 
                        new org.springframework.http.HttpEntity<>(body, headers);
                    
                    org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        String responseBody = response.getBody();
                        return new HttpResult(response.getStatusCodeValue(), responseBody, null);
                    } else {
                        String responseBody = response.getBody();
                        return new HttpResult(response.getStatusCodeValue(), responseBody, "HTTP " + response.getStatusCodeValue());
                    }
                    
                } catch (Exception e) {
                    logger.error("RestTemplate을 사용한 multipart 전송 중 오류 발생: {}", e.getMessage(), e);
                    return new HttpResult(-1, null, "Multipart 전송 중 오류가 발생했습니다: " + e.getMessage());
                }
            }
            
            // 데이터가 없는 경우 빈 응답
            return new HttpResult(400, null, "Multipart 데이터가 없습니다.");
            
        } catch (Exception e) {
            logger.error("Apache HttpClient Multipart 요청 중 오류 발생: {}", e.getMessage(), e);
            return new HttpResult(-1, null, e.getMessage());
        }
     }
     
     /**
      * HTTP PATCH multipart 요청 실행 - RestTemplate 기반
      * @param url 요청 URL
      * @param formData multipart 데이터
      * @return HTTP 응답 결과
      */
     private static HttpResult executePatchMultipartRequest(String url, Map<String, Object> formData) {
         try {
             logger.debug("RestTemplate PATCH Multipart HTTP 요청 시작: {}", url);
             
             // Spring의 RestTemplate을 사용하여 PATCH multipart 전송
             org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
             
             // Apache HttpClient를 사용하여 PATCH 메서드 지원 활성화
             org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory = 
                 new org.springframework.http.client.HttpComponentsClientHttpRequestFactory();
             restTemplate.setRequestFactory(factory);
             org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
             headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
             
             // 헤더에서 system_code 확인하여 쿠키 이름 결정
             String systemCode = getSystemCodeFromHeader();
             
             // Cookie 헤더 자동 설정 (system_code 기반 쿠키에서 토큰 추출)
             String accessToken = "";
             String refreshToken = "";
             String sessionToken = "";
             if (systemCode != null) {
                 accessToken = getAccessTokenFromCookie(systemCode);
                 refreshToken = getRefreshTokenFromCookie(systemCode);
                 sessionToken = getSessionTokenFromCookie(systemCode);
             }
             
             StringBuilder cookieBuilder = new StringBuilder();
             if (accessToken != null && !accessToken.isEmpty()) {
                 cookieBuilder.append("wai_access=").append(accessToken);
             }
             if (refreshToken != null && !refreshToken.isEmpty()) {
                 if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                 cookieBuilder.append("wai_refresh=").append(refreshToken);
             }
             if (sessionToken != null && !sessionToken.isEmpty()) {
                 if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                 cookieBuilder.append("wai_session=").append(sessionToken);
             }
             
             // 모든 쿠키를 RestTemplate 헤더에 설정
             if (cookieBuilder.length() > 0) {
                 headers.add("Cookie", cookieBuilder.toString());
                 logger.debug("RestTemplate에 쿠키 헤더 설정: {}", cookieBuilder.toString());
             }
             
             // Multipart 데이터 구성
             org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
             
             if (formData != null && !formData.isEmpty()) {
                 for (Map.Entry<String, Object> entry : formData.entrySet()) {
                     String key = entry.getKey();
                     Object value = entry.getValue();
                     
                     if (value instanceof org.springframework.web.multipart.MultipartFile) {
                         org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) value;
                         if (file != null && !file.isEmpty()) {
                             try {
                                 org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                                     @Override
                                     public String getFilename() {
                                         return file.getOriginalFilename();
                                     }
                                 };
                                 body.add(key, fileResource);
                                 logger.debug("파일 추가: key={}, fileName={}, size={}", key, file.getOriginalFilename(), file.getSize());
                             } catch (Exception e) {
                                 logger.warn("파일 처리 중 오류: key={}, error={}", key, e.getMessage());
                             }
                         }
                     } else if (value != null) {
                         // 일반 텍스트 데이터
                         body.add(key, value.toString());
                         logger.debug("텍스트 데이터 추가: key={}, value={}", key, value);
                     }
                 }
             }
             
             org.springframework.http.HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = 
                 new org.springframework.http.HttpEntity<>(body, headers);
             
             // RestTemplate을 사용하여 PATCH 요청 수행
             org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                 url, org.springframework.http.HttpMethod.PATCH, requestEntity, String.class);
             
             if (response.getStatusCode().is2xxSuccessful()) {
                 String responseBody = response.getBody();
                 logger.debug("PATCH Multipart 응답 성공: statusCode={}", response.getStatusCodeValue());
                 return new HttpResult(response.getStatusCodeValue(), responseBody, null);
             } else {
                 String responseBody = response.getBody();
                 logger.error("PATCH Multipart 응답 실패: statusCode={}, body={}", response.getStatusCodeValue(), responseBody);
                 return new HttpResult(response.getStatusCodeValue(), responseBody, "HTTP " + response.getStatusCodeValue());
             }
             
         } catch (Exception e) {
             logger.error("PATCH Multipart 요청 중 오류 발생: {}", e.getMessage(), e);
             return new HttpResult(-1, null, "PATCH Multipart 전송 중 오류가 발생했습니다: " + e.getMessage());
         }
     }
     
     /**
      * HTTP 응답에서 에러 메시지를 추출하는 메서드
     * @param responseBody 응답 본문
     * @return 추출된 에러 메시지
     */
    private static String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        
        // 1. message 필드 먼저 확인
        String errorMessage = JsonUtil.extractValue(responseBody, "message");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        }
        
        // 2. detail 필드 확인
        errorMessage = JsonUtil.extractValue(responseBody, "detail");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        }
        
        // 3. detail이 배열 형태인 경우 msg 값 추출
        try {
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            if (parsedResponse != null && parsedResponse.containsKey("detail")) {
                Object detail = parsedResponse.get("detail");
                if (detail instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> detailList = (java.util.List<Object>) detail;
                    if (!detailList.isEmpty() && detailList.get(0) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> firstDetail = (Map<String, Object>) detailList.get(0);
                        if (firstDetail.containsKey("msg")) {
                            return (String) firstDetail.get("msg");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("에러 메시지 추출 중 오류 발생: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * HTTP 응답 결과를 담는 클래스
     */
    public static class HttpResult {
        private final int status;
        private final String body;
        private final String errorMessage;
        private final String extractedErrorMessage;
        
        public HttpResult(int status, String body, String errorMessage) {
            this.status = status;
            this.body = body;
            this.errorMessage = errorMessage;
            this.extractedErrorMessage = extractErrorMessage(body);
        }
        
        public int getStatus() {
            return status;
        }
        
        public String getBody() {
            return body;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * 응답에서 추출된 에러 메시지를 반환
         * @return 추출된 에러 메시지 (없으면 null)
         */
        public String getExtractedErrorMessage() {
            return extractedErrorMessage;
        }
        
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
        
        public boolean hasError() {
            return errorMessage != null;
        }
    }
} 