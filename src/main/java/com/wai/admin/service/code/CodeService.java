package com.wai.admin.service.code;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.service.user.UserService;
import com.wai.admin.util.HttpUtil;
import com.wai.admin.util.JsonUtil;

@Service
public class CodeService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    public Map<String, Object> getAllCodes(Map<String, Object> searchParams) {

        logger.debug("외부 인증 서버 사용자 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String usersUrl = authServerBaseUrl + "/api/v1/common/common_codes/search";
        logger.debug("코드 목록 조회 URL: {}", usersUrl);
        
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
        logger.debug("코드 목록 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(usersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("코드 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("코드 목록 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("코드 목록 조회 성공");
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
            
            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userListFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 코드 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    public Map<String, Object> createCode(Map<String, Object> codeData) {
        logger.debug("외부 인증 서버 사용자 등록 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (codeData == null) {
            logger.warn("코드 등록 실패: 코드 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String codeUrl = authServerBaseUrl + "/api/v1/common/common_codes/";
        logger.debug("코드 등록 URL: {}", codeUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(codeData);
        logger.debug("코드 등록 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(codeUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("코드 등록 성공");
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
            
            logger.warn("외부 인증 서버 코드 등록 실패: {}", errorMessage);
        }
        
        return result;
    }

    public Map<String, Object> updateCode(String code_id, Map<String, Object> codeData) {

        logger.debug("외부 인증 서버 코드 수정 시도: userId={}, server={}", code_id, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (code_id == null || code_id.isEmpty()) {
            logger.warn("코드 수정 실패: 코드 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserId");
            return result;
        }
        
        if (codeData == null) {
            logger.warn("코드 수정 실패: 코드 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String codeUrl = authServerBaseUrl + "/api/v1/common/common_codes/" + code_id;
        logger.debug("코드 수정 URL: {}", codeUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(codeData);
        logger.debug("코드 수정 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(codeUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("코드 수정 성공");
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
            
            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.userUpdateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 코드 수정 실패: {}", errorMessage);
        }
        
        return result;

    }

    public Map<String, Object> deleteCode(String code_id) {

        logger.debug("외부 인증 서버 코드 삭제 시도: userId={}, server={}", code_id, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (code_id == null || code_id.isEmpty()) {
            logger.warn("코드 삭제 실패: 사용자 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidUserId");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String codeUrl = authServerBaseUrl + "/api/v1/common/common_codes/" + code_id;
        logger.debug("코드 삭제 URL: {}", codeUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(codeUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("코드 삭제 성공");
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
            
            logger.warn("외부 인증 서버 코드 삭제 실패: {}", errorMessage);
        }
        
        return result;

    }

    public Map<String, Object> multiCreateCodes(List<Map<String, Object>> codesList) {
        logger.debug("외부 인증 서버 다건 코드 등록 시도: server={}, codesCount={}", authServerBaseUrl, codesList != null ? codesList.size() : 0);
        Map<String, Object> result = new HashMap<>();
        
        if (codesList == null) {
            logger.warn("다건 코드 등록 실패: 요청 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidRequestData");
            return result;
        }
        
        if (codesList.isEmpty()) {
            logger.warn("다건 코드 등록 실패: codes 배열이 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.codesArrayEmpty");
            return result;
        }
        
        // 결과를 저장할 리스트
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedCodeKeys = new java.util.ArrayList<>();
        
        // 각 코드를 개별적으로 등록
        for (int i = 0; i < codesList.size(); i++) {
            Map<String, Object> codeData = codesList.get(i);
            logger.debug("코드 {} 등록 시도: {}", i + 1, codeData);
            
            // 기존 createCode 메서드 호출
            Map<String, Object> createResult = createCode(codeData);
            
            // 결과 저장
            Map<String, Object> resultItem = new HashMap<>();
            resultItem.put("index", i);
            resultItem.put("codeData", codeData);
            resultItem.put("result", createResult);
            
            if ((Boolean) createResult.get("success")) {
                successCount++;
                logger.debug("코드 {} 등록 성공", i + 1);
            } else {
                failCount++;
                // 실패한 code_key 누적
                String codeKey = (String) codeData.get("code_key");
                if (codeKey != null && !codeKey.isEmpty()) {
                    failedCodeKeys.add(codeKey);
                }
                logger.warn("코드 {} 등록 실패: code_key={}, message={}", i + 1, codeKey, createResult.get("message"));
            }
            
            results.add(resultItem);
        }
        
        // 전체 결과 구성
        result.put("success", failCount == 0); // 모든 등록이 성공해야 전체 성공
        result.put("totalCount", codesList.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("results", results);
        
        if (failCount == 0) {
            result.put("message", "messages.success.codesMultiCreateSuccess");
            logger.debug("다건 코드 등록 완료: 전체 {}개 성공", codesList.size());
        } else {
            // 실패한 code_key들을 메시지에 포함
            String failedKeysMessage = String.join(", ", failedCodeKeys);
            String errorMessage = "messages.error.codesMultiCreatePartialFail";
            result.put("message", errorMessage);
            result.put("failedCodeKeys", failedCodeKeys);
            logger.warn("다건 코드 등록 부분 실패: 성공 {}개, 실패 {}개, 실패한 코드: {}", successCount, failCount, failedKeysMessage);
        }
        
        return result;
    }
} 