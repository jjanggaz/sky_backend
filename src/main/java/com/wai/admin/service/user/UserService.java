package com.wai.admin.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 사용자 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getAllUsers(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 사용자 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String usersUrl = authServerBaseUrl + "/api/v1/auth/users/search";
        logger.debug("사용자 목록 조회 URL: {}", usersUrl);
        
        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();
        
        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("사용자 목록 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(usersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("사용자 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("사용자 목록 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("사용자 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.userListSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userListFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 사용자 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 사용자 등록 - 외부 API 호출
     */
    public Map<String, Object> createUser(Map<String, Object> userData) {
        logger.debug("외부 인증 서버 사용자 등록 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (userData == null) {
            logger.warn("사용자 등록 실패: 사용자 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String usersUrl = authServerBaseUrl + "/api/v1/auth/users/";
        logger.debug("사용자 등록 URL: {}", usersUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(userData);
        logger.debug("사용자 등록 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(usersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("사용자 등록 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.userCreateSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userCreateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 사용자 등록 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 사용자 수정 - 외부 API 호출
     */
    public Map<String, Object> updateUser(String userId, Map<String, Object> userData) {
        logger.debug("외부 인증 서버 사용자 수정 시도: userId={}, server={}", userId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null || userId.isEmpty()) {
            logger.warn("사용자 수정 실패: 사용자 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserId");
            return result;
        }
        
        if (userData == null) {
            logger.warn("사용자 수정 실패: 사용자 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String userUrl = authServerBaseUrl + "/api/v1/auth/users/" + userId;
        logger.debug("사용자 수정 URL: {}", userUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(userData);
        logger.debug("사용자 수정 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(userUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("사용자 수정 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.userUpdateSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userUpdateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 사용자 수정 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 사용자 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteUser(String userId) {
        logger.debug("외부 인증 서버 사용자 삭제 시도: userId={}, server={}", userId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null || userId.isEmpty()) {
            logger.warn("사용자 삭제 실패: 사용자 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserId");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String userUrl = authServerBaseUrl + "/api/v1/auth/users/" + userId;
        logger.debug("사용자 삭제 URL: {}", userUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(userUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("사용자 삭제 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.userDeleteSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userDeleteFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 사용자 삭제 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 사용자 아이디 중복체크 - 외부 API 호출
     */
    public Map<String, Object> checkUserNameExists(String userName) {
        logger.debug("외부 인증 서버 사용자 아이디 중복체크 시도: userName={}, server={}", userName, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (userName == null || userName.isEmpty()) {
            logger.warn("사용자 아이디 중복체크 실패: 사용자명이 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserName");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String checkUrl = authServerBaseUrl + "/api/v1/auth/users/name_exist/" + userName;
        logger.debug("사용자 아이디 중복체크 URL: {}", checkUrl);
        
        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(checkUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("사용자 아이디 중복체크 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("사용자 아이디 중복체크 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("사용자 아이디 중복체크 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.userNameCheckSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userNameCheckFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 사용자 아이디 중복체크 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 공통코드 조회 (역할 목록) - 외부 API 호출
     */
    public Map<String, Object> getCommonRoles() {
        logger.debug("외부 인증 서버 역할 목록 조회 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String rolesUrl = authServerBaseUrl + "/api/v1/auth/roles/search";
        logger.debug("역할 목록 조회 URL: {}", rolesUrl);
        
        // 요청 본문 구성 - page_size를 100으로 설정
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("page_size", 100);
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("역할 목록 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(rolesUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("역할 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("역할 목록 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("역할 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleListSuccess");
            result.put("response", parsedResponse);
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.roleListFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 역할 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }
} 