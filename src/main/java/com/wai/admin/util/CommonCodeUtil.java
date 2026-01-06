package com.wai.admin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통코드 조회를 위한 유틸리티 클래스
 */
@Component
public class CommonCodeUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonCodeUtil.class);

    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    @Autowired
    private HttpServletRequest request;

    /**
     * 공통코드 목록을 조회합니다.
     */
    public Map<String, Object> getCommonCodes(Map<String, Object> searchParams) {
        logger.debug("공통코드 목록 조회: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String codesUrl = authServerBaseUrl + "/api/v1/common/common_codes/search";
            
            // 요청 본문 구성
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
            requestMap.put("page", searchParams.getOrDefault("page", 1));
            requestMap.put("page_size", searchParams.getOrDefault("page_size", 100));
            requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
            requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
            
            // 헤더에서 authSuper 값 확인
            String authSuper = request.getHeader("authSuper");
            if (authSuper != null && "false".equalsIgnoreCase(authSuper.trim())) {
                requestMap.put("is_admin_only", false);
            }

            //is_active null인 경우 모든 데이터 조회
            requestMap.put("is_active", searchParams.getOrDefault("is_active", null));
            
            String requestBody = JsonUtil.objectMapToJson(requestMap);
            
            // HTTP 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.post(codesUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                
                // parsedResponse에서 item을 추출하여 data에 세팅
                if (parsedResponse != null && parsedResponse.containsKey("items")) {
                    Object items = parsedResponse.get("items");
                    
                    // 세션에서 언어 확인하여 영어인 경우 code_value_en 값을 code_value에 덮어쓰기
                    if (items instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) items;
                        processLanguageOverride(itemsList);
                    }
                    
                    result.put("data", items);
                } else {
                    result.put("data", parsedResponse);
                }
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.error.commonCodeRetrieveFail");
            }
            
        } catch (Exception e) {
            logger.error("공통코드 조회 중 오류", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.commonCodeRetrieveError");
        }
        
        return result;
    }

    /**
     * 언어 설정에 따라 code_value_en 값을 code_value에 덮어쓰기
     */
    private void processLanguageOverride(List<Map<String, Object>> itemsList) {
        try {
            // 헤더에서 wai_lang 값 가져오기
            String language = request.getHeader("wai_lang");
            language = (language != null && !language.trim().isEmpty()) ? language.trim() : null;
            
            if ("en".equals(language)) {
                logger.debug("영어 언어 설정 감지, code_value_en 값을 code_value에 덮어쓰기");
                
                for (Map<String, Object> item : itemsList) {
                    if (item.containsKey("code_value_en") && item.get("code_value_en") != null) {
                        String englishValue = item.get("code_value_en").toString();
                        if (!englishValue.trim().isEmpty()) {
                            item.put("code_value", englishValue);
                            logger.debug("code_value_en 값을 code_value에 덮어씀: {}", englishValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("언어 처리 중 오류 발생", e);
        }
    }

    /**
     * 언어 설정에 따라 code_value_en 값을 code_value에 덮어쓰기 (children 배열 구조용)
     * 새로운 응답 구조: { code_group, parent_key, code_level, system_codes, children: [...] }
     */
    private void processLanguageOverride2(Map<String, Object> responseMap) {
        try {
            // 헤더에서 wai_lang 값 가져오기
            String language = request.getHeader("wai_lang");
            language = (language != null && !language.trim().isEmpty()) ? language.trim() : null;
            
            if ("en".equals(language)) {
                logger.debug("영어 언어 설정 감지, children 배열의 code_value_en 값을 code_value에 덮어쓰기");
                
                // children 배열 확인
                if (responseMap.containsKey("children")) {
                    Object children = responseMap.get("children");
                    if (children instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> childrenList = (List<Map<String, Object>>) children;
                        
                        for (Map<String, Object> child : childrenList) {
                            if (child.containsKey("code_value_en") && child.get("code_value_en") != null) {
                                String englishValue = child.get("code_value_en").toString();
                                if (!englishValue.trim().isEmpty()) {
                                    child.put("code_value", englishValue);
                                    logger.debug("children 항목의 code_value_en 값을 code_value에 덮어씀: {}", englishValue);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("언어 처리 중 오류 발생", e);
        }
    }

    /**
     * 특정 카테고리의 공통코드를 조회합니다.
     */
    public Map<String, Object> getCommonCodesByCategory(String searchField, String searchValue, Boolean isActive) {
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("search_field", searchField);
        searchParams.put("search_value", searchValue);
        searchParams.put("page", 1);
        searchParams.put("page_size", 100);
        searchParams.put("order_by", "code_order");
        searchParams.put("order_direction", "asc");
        searchParams.put("is_active", isActive);
        
        return getCommonCodes(searchParams);
    }

    /**
     * 깊이별 상세 코드 조회 - 외부 API 호출 (GET 방식)
     */
    public Map<String, Object> getDepthDetail(Map<String, Object> searchParams) {
        logger.debug("깊이별 상세 코드 조회 시도: searchParams={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        // searchParams에서 code_group과 code_level 추출
        String codeGroup = (String) searchParams.get("code_group");
        String codeLevel = String.valueOf(searchParams.get("code_level"));
        String parentKey = String.valueOf(searchParams.get("parent_key"));

        if (codeGroup == null || codeGroup.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "code_group 파라미터가 필요합니다.");
            return result;
        }

        if (codeLevel == null || codeLevel.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "code_level 파라미터가 필요합니다.");
            return result;
        }

        if (parentKey == null || parentKey.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "parent_key 파라미터가 필요합니다.");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String depthDetailUrl = authServerBaseUrl + "/api/v1/common/common_codes/custom/" + codeGroup + "/" + parentKey + "/children/" + codeLevel;
            //String depthDetailUrl = authServerBaseUrl + "/api/v1/common/common_codes/custom/" + codeGroup + "/level/" + codeLevel;
            logger.debug("깊이별 상세 코드 조회 URL: {}", depthDetailUrl);

            // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.get(depthDetailUrl, "application/json", "");

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                if (parsedResponse != null) {
                    logger.debug("깊이별 상세 코드 조회 성공");
                    
                    // 새로운 구조(children 배열)에 언어 처리 적용
                    if (parsedResponse.containsKey("children")) {
                        processLanguageOverride2(parsedResponse);
                    }
                    // 기존 구조(codes 배열)에 언어 처리 적용
                    else if (parsedResponse.containsKey("codes")) {
                        Object codes = parsedResponse.get("codes");
                        if (codes instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> codesList = (List<Map<String, Object>>) codes;
                            processLanguageOverride(codesList);
                        }
                    }
                    
                    result.put("success", true);
                    result.put("data", parsedResponse);
                    result.put("message", "깊이별 상세 코드 조회가 완료되었습니다.");
                } else {
                    logger.error("깊이별 상세 코드 조회 응답 파싱 실패");
                    result.put("success", false);
                    result.put("message", "응답 파싱에 실패했습니다.");
                }
            } else {
                // 실패한 응답 처리
                logger.error("깊이별 상세 코드 조회 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);

                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "깊이별 상세 코드 조회에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("깊이별 상세 코드 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("statusCode", 500);
            result.put("message", "깊이별 상세 코드 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 단위 시스템 조회 - 외부 API 호출
     */
    public Map<String, Object> getUnitSystems() {
        logger.debug("단위 시스템 조회 시도");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 단위 시스템 조회 URL 구성
            String unitUrl = authServerBaseUrl + "/api/v1/common/unit_systems/search";
            logger.debug("단위 시스템 조회 URL: {}", unitUrl);
            
            // 요청 본문 구성
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("search_field", "");
            requestMap.put("search_value", "");
            requestMap.put("page", 1);
            requestMap.put("page_size", 100);
            requestMap.put("order_by", "system_code");
            requestMap.put("order_direction", "asc");
            
            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("단위 시스템 조회 요청 본문: {}", requestBody);
            
            // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.post(unitUrl, "application/json", requestBody);
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("단위 시스템 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.unitSystemSearchSuccess");
                result.put("response", parsedResponse.get("items"));
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
                    errorMessage = "단위 시스템 조회에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("단위 시스템 조회 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("단위 시스템 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "단위 시스템 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
}
