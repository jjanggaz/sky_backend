package com.wai.admin.service.inflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class InflowService {

    private static final Logger logger = LoggerFactory.getLogger(InflowService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 공통코드 조회 - 외부 API 호출
     */
    public Map<String, Object> getCommonCodes(String codeGroup, String parentKey) {
        logger.debug("외부 인증 서버 공통코드 조회 시도: server={}, codeGroup={}, parentKey={}", 
                    authServerBaseUrl, codeGroup, parentKey);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성 - GET 방식으로 쿼리 파라미터 추가
        StringBuilder urlBuilder = new StringBuilder(authServerBaseUrl + "/api/v1/common/common_codes/filtered");
        
        // 쿼리 파라미터 구성
        Map<String, String> queryParams = new HashMap<>();
        // if (codeGroup != null && !codeGroup.isEmpty()) {
        //     queryParams.put("code_group", codeGroup);
        // }
        if (parentKey != null && !parentKey.isEmpty()) {
            queryParams.put("parent_key", parentKey);
        }
        
        // 쿼리 파라미터를 URL에 추가
        if (!queryParams.isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                try {
                    urlBuilder.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                }
                first = false;
            }
        }
        
        String commonCodesUrl = urlBuilder.toString();
        logger.debug("공통코드 조회 URL: {}", commonCodesUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(commonCodesUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("공통코드 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("공통코드 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                    
                    // is_active = true인 항목만 필터링 (추가 필터링)
                    if (parsedResponse.get("items") != null) {
                        Object items = parsedResponse.get("items");
                        if (items instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String, Object>> itemList = (java.util.List<Map<String, Object>>) items;
                            
                            // is_active가 true인 항목만 필터링
                            java.util.List<Map<String, Object>> filteredItems = new java.util.ArrayList<>();
                            for (Map<String, Object> item : itemList) {
                                Object isActiveItem = item.get("is_active");
                                if (isActiveItem != null && (Boolean.TRUE.equals(isActiveItem) || "true".equals(String.valueOf(isActiveItem)))) {
                                    filteredItems.add(item);
                                }
                            }
                            
                            // 필터링된 결과로 응답 업데이트
                            parsedResponse.put("items", filteredItems);
                            parsedResponse.put("total", filteredItems.size());
                            
                            logger.debug("is_active=true 필터링 완료: 전체 {} -> 활성 {}", itemList.size(), filteredItems.size());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("공통코드 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("items", new java.util.ArrayList<>());
                parsedResponse.put("total", 0);
            }
            
            logger.debug("공통코드 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.commonCodesSuccess");
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
                errorMessage = "messages.error.commonCodesFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 공통코드 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류별 파라미터 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getWaterFlowTypeParameters(Map<String, Object> searchParams) {

        logger.debug("외부 인증 서버 유입종류 파라미터 조회 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String parametersUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/custom/search";
        logger.debug("유입종류 파라미터 조회 URL: {}", parametersUrl);

        // 요청 본문 구성
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("flow_direction", searchParams.getOrDefault("flowDirection", ""));
        requestData.put("flow_type_code", searchParams.getOrDefault("flowTypeCode", ""));
        requestData.put("page", 1);
        requestData.put("page_size", 100);

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("유입종류 파라미터 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(parametersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("유입종류 파라미터 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("파라미터 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                    
                    // is_active = true인 항목만 필터링
                    if (parsedResponse.get("items") != null) {
                        Object items = parsedResponse.get("items");
                        if (items instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String, Object>> itemList = (java.util.List<Map<String, Object>>) items;
                            
                            // is_active가 true인 항목만 필터링하고 unit_system_code별로 분리
                            java.util.List<Map<String, Object>> metricItems = new java.util.ArrayList<>();
                            java.util.List<Map<String, Object>> uscsItems = new java.util.ArrayList<>();
                            
                            for (Map<String, Object> item : itemList) {
                                Object isActive = item.get("is_active");
                                if (isActive != null && (Boolean.TRUE.equals(isActive) || "true".equals(String.valueOf(isActive)))) {
                                    String unitSystemCode = item.get("unit_system_code") != null ? 
                                        String.valueOf(item.get("unit_system_code")) : "";
                                    
                                    if ("METRIC".equals(unitSystemCode)) {
                                        metricItems.add(item);
                                    } else if ("USCS".equals(unitSystemCode)) {
                                        uscsItems.add(item);
                                    }
                                }
                            }
                            
                            // unit_system_code별로 분리된 결과를 filteredItems에 담기
                            Map<String, Object> filteredItems = new HashMap<>();
                            filteredItems.put("metric", metricItems);
                            filteredItems.put("uscs", uscsItems);
                            
                            //계산식 목록 불러오기
                            try {
                                // METRIC과 USCS 각각에 대해 계산식 목록 조회
                                Map<String, Object> metricFormulas = new HashMap<>();
                                Map<String, Object> uscsFormulas = new HashMap<>();
                                
                                // METRIC 계산식 조회
                                Map<String, Object> metricFormulaParams = new HashMap<>();
                                metricFormulaParams.put("formula_scope", "FLOWTYPE");
                                metricFormulaParams.put("unit_system_code", "METRIC");
                                metricFormulaParams.put("flow_type_id", searchParams.getOrDefault("flowTypeId", ""));
                                metricFormulaParams.put("limit", 100);
                                metricFormulaParams.put("offset", 0);
                                
                                Map<String, Object> metricFormulaResult = getFormulaList(metricFormulaParams);
                                if ((Boolean) metricFormulaResult.get("success")) {
                                    metricFormulas = (Map<String, Object>) metricFormulaResult.get("response");
                                }
                                
                                // USCS 계산식 조회
                                Map<String, Object> uscsFormulaParams = new HashMap<>();
                                uscsFormulaParams.put("formula_scope", "FLOWTYPE");
                                uscsFormulaParams.put("unit_system_code", "USCS");
                                uscsFormulaParams.put("flow_type_id", searchParams.getOrDefault("flowTypeId", ""));
                                uscsFormulaParams.put("limit", 100);
                                uscsFormulaParams.put("offset", 0);
                                
                                Map<String, Object> uscsFormulaResult = getFormulaList(uscsFormulaParams);
                                if ((Boolean) uscsFormulaResult.get("success")) {
                                    uscsFormulas = (Map<String, Object>) uscsFormulaResult.get("response");
                                }
                                
                                // 계산식 결과를 filteredItems에 추가
                                filteredItems.put("metric_formulas", metricFormulas);
                                filteredItems.put("uscs_formulas", uscsFormulas);
                                
                                logger.debug("계산식 목록 조회 완료: METRIC={}, USCS={}", 
                                    metricFormulas.get("items") != null ? ((java.util.List<?>) metricFormulas.get("items")).size() : 0,
                                    uscsFormulas.get("items") != null ? ((java.util.List<?>) uscsFormulas.get("items")).size() : 0);
                                    
                            } catch (Exception e) {
                                logger.warn("계산식 목록 조회 중 오류 발생: {}", e.getMessage());
                                // 오류가 발생해도 빈 객체라도 넣어서 응답 구조 유지
                                filteredItems.put("metric_formulas", new HashMap<>());
                                filteredItems.put("uscs_formulas", new HashMap<>());
                            }

                            // 필터링된 결과로 응답 업데이트
                            parsedResponse.put("items", filteredItems);
                            parsedResponse.put("total", metricItems.size() + uscsItems.size());
                            
                            logger.debug("is_active=true 필터링 완료: 전체 {} -> METRIC: {}, USCS: {}", 
                                itemList.size(), metricItems.size(), uscsItems.size());
                        }
                    }

                    //파일 목록 불러오기

                }
            } catch (Exception e) {
                logger.warn("파라미터 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("items", new java.util.ArrayList<>());
                parsedResponse.put("total", 0);
            }
            logger.debug("유입종류 데이터 확인 : {}", parsedResponse);
            logger.debug("유입종류 파라미터 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterFlowTypeParametersSuccess");
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
                errorMessage = "messages.error.waterFlowTypeParametersFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 유입종류 파라미터 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류별 파라미터 목록 조회
     * (수정할때는 is_active 체크 하지 않고 체크 해야하므로 getWaterFlowTypeParameters하고 차이 있음) - 외부 API 호출
     */
    public Map<String, Object> getWaterFlowTypeParameters2(Map<String, Object> searchParams) {

        String flowDirection = (String) searchParams.get("flowDirection");
        String flowTypeCode = (String) searchParams.get("flowTypeCode");

        logger.debug("외부 인증 서버 유입종류 파라미터 조회 시도: server={}, flowTypeCode={}", authServerBaseUrl, flowTypeCode);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String parametersUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/custom/search";
        logger.debug("유입종류 파라미터 조회 URL: {}", parametersUrl);

        // 요청 본문 구성
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("flow_direction", flowDirection);
        requestData.put("flow_type_code", flowTypeCode);
        requestData.put("page", 1);
        requestData.put("page_size", 100);

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("유입종류 파라미터 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(parametersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("유입종류 파라미터 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("파라미터 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("파라미터 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("items", new java.util.ArrayList<>());
                parsedResponse.put("total", 0);
            }
            logger.debug("유입종류 데이터 확인 : {}", parsedResponse);
            logger.debug("유입종류 파라미터 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterFlowTypeParametersSuccess");
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
                errorMessage = "messages.error.waterFlowTypeParametersFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 유입종류 파라미터 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getAllWaterFlowTypes(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("외부 인증 서버 유입종류 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성 - GET 방식으로 쿼리 파라미터 추가
        StringBuilder urlBuilder = new StringBuilder(authServerBaseUrl + "/api/v1/common/water_flow_types/");
        
        // 쿼리 파라미터 구성
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("skip", searchParams.getOrDefault("skip", 0));
        queryParams.put("limit", searchParams.getOrDefault("limit", 100));
        
        // 쿼리 파라미터를 URL에 추가
        urlBuilder.append("?skip=").append(queryParams.get("skip"));
        urlBuilder.append("&limit=").append(queryParams.get("limit"));
        
        String waterFlowTypesUrl = urlBuilder.toString();
        logger.debug("유입종류 목록 조회 URL: {}", waterFlowTypesUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(waterFlowTypesUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = new HashMap<>();
            
            // 배열 응답을 객체로 감싸기
            if (responseBody != null && responseBody.trim().startsWith("[")) {
                String wrappedResponse = "{\"items\":" + responseBody + ",\"total\":0}";
                parsedResponse = JsonUtil.parseJson(wrappedResponse);
            } else {
                parsedResponse = JsonUtil.parseJson(responseBody);
            }
            
            logger.debug("유입종류 목록 조회 원본 응답: {}", parsedResponse);
                        
            // flow_direction과 flowTypeCode 필터링
            if (parsedResponse != null && parsedResponse.get("items") != null) {
                Object items = parsedResponse.get("items");
                if (items instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> itemList = (java.util.List<Map<String, Object>>) items;
                    
                    // flow_direction이 INFLUENT이고 flowTypeCode가 일치하는 항목만 필터링
                    java.util.List<Map<String, Object>> filteredItems = new java.util.ArrayList<>();
                    String targetFlowTypeCode = searchParams.get("flowTypeCode") != null ? 
                        searchParams.get("flowTypeCode").toString() : null;
                    
                    logger.debug("유입종류 필터링: 전체 {} -> INFLUENT & flowTypeCode={}", 
                        itemList.size(), targetFlowTypeCode);
                    
                    for (Map<String, Object> item : itemList) {
                        Object flowDirection = item.get("flow_direction").toString();
                        boolean isInfluent = flowDirection != null && flowDirection.equals(targetFlowTypeCode);

                        logger.debug("항목 flow_direction: {}, isInfluent: {}", flowDirection, isInfluent);

                        if (isInfluent) {
                            filteredItems.add(item);
                        }
                    }
                    
                    // 다국어 처리: wai_lang 헤더에 따라 flow_type_name 처리
                    String language = request.getHeader("wai_lang");
                    logger.debug("wai_lang 헤더 값: {}", language);
                    
                    for (Map<String, Object> item : filteredItems) {
                        if ("en".equals(language)) {
                            // 영어인 경우 flow_type_name_en으로 덮어쓰기
                            Object flowTypeNameEn = item.get("flow_type_name_en");
                            if (flowTypeNameEn != null) {
                                item.put("flow_type_name", flowTypeNameEn);
                                logger.debug("flow_type_name을 영어로 변경: {}", flowTypeNameEn);
                            }
                        } else {
                            // 한국어(kr) 또는 기본값인 경우 flow_type_name 유지
                            logger.debug("flow_type_name 한국어 유지: {}", item.get("flow_type_name"));
                        }
                    }
                    
                    // 필터링된 결과로 응답 업데이트
                    parsedResponse.put("items", filteredItems);
                    parsedResponse.put("total", filteredItems.size());
                    
                    // 페이지 정보 재계산
                    int pageSize = 20;
                    try {
                        pageSize = Integer.parseInt(searchParams.getOrDefault("page_size", 20).toString());
                    } catch (Exception e) {
                        pageSize = 20;
                    }
                    int totalPages = (int) Math.ceil((double) filteredItems.size() / pageSize);
                    parsedResponse.put("total_pages", totalPages);
                    
                    logger.debug("유입종류 필터링 완료: 전체 {} -> INFLUENT & flowTypeCode={} & active {}", 
                        itemList.size(), targetFlowTypeCode, filteredItems.size());
                }
            }

            
            logger.debug("유입종류 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterFlowTypeListSuccess");
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
                errorMessage = "messages.error.waterFlowTypeListFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 유입종류 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류 등록 - 외부 API 호출
     */
    public Map<String, Object> createWaterFlowType(Map<String, Object> waterFlowTypeData) {
        logger.debug("외부 인증 서버 유입종류 등록 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (waterFlowTypeData == null) {
            logger.warn("유입종류 등록 실패: 유입종류 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidWaterFlowTypeData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String waterUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/";
        logger.debug("유입종류 등록 URL: {}", waterUrl);

        // 요청 본문 구성 - metric_parameters와 uscs_parameters 포함
        Map<String, Object> requestData = new HashMap<>();

        String flowTypeCode = (String) waterFlowTypeData.get("flow_type_code");
        
        // 기본 유입종류 정보
        requestData.put("flow_type_code", flowTypeCode);
        requestData.put("flow_type_name", waterFlowTypeData.getOrDefault("flow_type_name", ""));
        requestData.put("flow_direction", waterFlowTypeData.getOrDefault("flow_direction", ""));

        // 선택적 필드들
        requestData.put("flow_type_name_en", waterFlowTypeData.getOrDefault("flow_type_name_en", ""));
        requestData.put("description", waterFlowTypeData.getOrDefault("description", ""));
        requestData.put("svg_symbol_id", waterFlowTypeData.getOrDefault("svg_symbol_id", ""));
        requestData.put("is_active", waterFlowTypeData.getOrDefault("is_active", true));
        requestData.put("is_required", waterFlowTypeData.getOrDefault("is_required", true));

        // 트랜잭션 처리를 위한 변수들
        Map<String, Object> waterFlowTypeResponse = null;
        boolean waterFlowTypeSuccess = false;
        String flowTypeId = null;
        List<String> formulaIds = new ArrayList<>(); // 업로드된 formula_id들을 저장할 배열

        // 0. 심볼 등록 (심볼ID 반환)
        String symbolId = createSymbolForWaterFlowType(waterFlowTypeData);
        
        // 심볼 ID 추가 (심볼 등록이 성공한 경우)
        if (symbolId != null) {
            // 심볼 색상 업데이트 (심볼이 생성되고 색상 정보가 있는 경우)
            updateSymbolForWaterFlowType(symbolId, waterFlowTypeData);
            requestData.put("svg_symbol_id", symbolId);
            logger.debug("심볼 ID를 유입종류 등록 데이터에 추가: {}", symbolId);
        } else {
            // 심볼 색상 생성 (심볼이미지가 첨부되지 않고 색상 정보만만 있는 경우)
            symbolId = createSymbolColorOnly(waterFlowTypeData);
            requestData.put("svg_symbol_id", symbolId);
            logger.debug("심볼 ID를 유입종류 등록 데이터에 추가: {}", symbolId);

        }

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("유입종류 등록 요청 본문: {}", requestBody);

        try {
            // 1. 유입종류 등록
            HttpUtil.HttpResult waterResult = HttpUtil.post(waterUrl, "application/json", requestBody);
            
            if (waterResult.isSuccess()) {
                String responseBody = waterResult.getBody();
                waterFlowTypeResponse = JsonUtil.parseJson(responseBody);
                waterFlowTypeSuccess = true;
                
                // 응답에서 flow_type_id 추출
                if (waterFlowTypeResponse != null && waterFlowTypeResponse.get("flow_type_id") != null) {
                    flowTypeId = waterFlowTypeResponse.get("flow_type_id").toString();
                    logger.debug("유입종류 등록 성공, flow_type_id: {}", flowTypeId);

                    
                } else {
                    logger.warn("유입종류 등록 응답에서 flow_type_id를 찾을 수 없음");
                    throw new RuntimeException("flow_type_id를 추출할 수 없습니다.");
                }
            } else {
                String errorBody = waterResult.getBody();
                String errorMessage = waterResult.getExtractedErrorMessage();
                logger.error("유입종류 등록 실패: status={}, error={}", waterResult.getStatus(), errorMessage);
                
                result.put("success", false);
                result.put("status", waterResult.getStatus());
                result.put("message", errorMessage != null ? errorMessage : "messages.error.waterFlowTypeCreateFail");
                result.put("response", JsonUtil.parseJson(errorBody));
                return result;
            }
            
            if (flowTypeId != null) {
                // Metric 파라미터 등록
                if (waterFlowTypeData.get("metric_parameters") != null && 
                    !((java.util.List<?>) waterFlowTypeData.get("metric_parameters")).isEmpty()) {
                    
                    java.util.List<?> metricParams = (java.util.List<?>) waterFlowTypeData.get("metric_parameters");
                    logger.debug("Metric 파라미터 등록 시작 - 총 {}개", metricParams.size());
                    
                    for (Object paramObj : metricParams) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> param = (Map<String, Object>) paramObj;
                        
                        Map<String, Object> metricResult = createMetricParameter(param, flowTypeId);
                        if (!(Boolean) metricResult.get("success")) {

                            // 유입종류가 성공적으로 등록되었다면 롤백 시도
                            if (waterFlowTypeSuccess && flowTypeId != null) {
                                rollbackWaterFlowType(flowTypeId);
                                // MinIO에서 심볼 파일 삭제
                                if (symbolId != null) {
                                    deleteMinIOSymbol(symbolId);
                                }
                            }

                            return metricResult;
                        }
                    }

                    logger.debug("모든 Metric 파라미터 등록 완료");
                } else {
                    logger.debug("Metric 파라미터가 없어 등록을 건너뜁니다.");
                }

                //계산식 등록
                if (waterFlowTypeData.get("metricFile") != null) {
                    MultipartFile metricFormulaFile = (MultipartFile) waterFlowTypeData.get("metricFile");
                    if (!metricFormulaFile.isEmpty()) {
                        Map<String, Object> metricFormulaResult = uploadFormula(
                            metricFormulaFile, 
                            "FLOWTYPE", 
                            flowTypeId, 
                            "METRIC",
                            flowTypeCode
                        );
                        if (!(Boolean) metricFormulaResult.get("success")) {
                            logger.warn("Metric 계산식 등록 실패: {}", metricFormulaResult.get("message"));
                        } else {
                            logger.debug("Metric 계산식 등록 성공");
                            // formula_id를 배열에 추가
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) metricFormulaResult.get("data");
                            if (data != null && data.get("formula_id") != null) {
                                formulaIds.add(data.get("formula_id").toString());
                            }
                        }
                    }
                } else {
                    logger.debug("Metric 파일이 없어 계산식 파일 등록을 건너뜁니다.");
                }
                
                // Uscs 파라미터 등록
                if (waterFlowTypeData.get("uscs_parameters") != null && 
                    !((java.util.List<?>) waterFlowTypeData.get("uscs_parameters")).isEmpty()) {
                    
                    java.util.List<?> uscsParams = (java.util.List<?>) waterFlowTypeData.get("uscs_parameters");
                    logger.debug("Uscs 파라미터 등록 시작 - 총 {}개", uscsParams.size());
                    
                    for (Object paramObj : uscsParams) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> param = (Map<String, Object>) paramObj;
                        
                        Map<String, Object> uscsResult = createUscsParameter(param, flowTypeId);
                        if (!(Boolean) uscsResult.get("success")) {

                            // 유입종류가 성공적으로 등록되었다면 롤백 시도
                            if (waterFlowTypeSuccess && flowTypeId != null) {
                                rollbackWaterFlowType(flowTypeId);
                                // MinIO에서 심볼 파일 삭제
                                if (symbolId != null) {
                                    deleteMinIOSymbol(symbolId);
                                }
                                // MinIO에서 계산식 파일들 삭제 (Metric 계산식을 처리했을 수 있기 때문에 여기서만 삭제)
                                for (String formulaId : formulaIds) {
                                    Map<String, Object> deleteResult = deleteMinIOFormula(formulaId);
                                    if (!(Boolean) deleteResult.get("success")) {
                                        logger.warn("MinIO 계산식 삭제 실패: {}, error: {}", formulaId, deleteResult.get("message"));
                                    }
                                }
                            }

                            return uscsResult;
                        }
                    }
                    
                    logger.debug("모든 Uscs 파라미터 등록 완료");
                } else {
                    logger.debug("Uscs 파라미터가 없어 등록을 건너뜁니다.");
                }

                //계산식 등록
                if (waterFlowTypeData.get("uscsFile") != null) {
                    MultipartFile uscsFormulaFile = (MultipartFile) waterFlowTypeData.get("uscsFile");
                    if (!uscsFormulaFile.isEmpty()) {
                        Map<String, Object> uscsFormulaResult = uploadFormula(
                            uscsFormulaFile, 
                            "FLOWTYPE", 
                            flowTypeId, 
                            "USCS",
                            flowTypeCode
                        );
                        if (!(Boolean) uscsFormulaResult.get("success")) {
                            logger.warn("Uscs 계산식 등록 실패: {}", uscsFormulaResult.get("message"));
                        } else {
                            logger.debug("Uscs 계산식 등록 성공");
                            // formula_id를 배열에 추가
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) uscsFormulaResult.get("data");
                            if (data != null && data.get("formula_id") != null) {
                                formulaIds.add(data.get("formula_id").toString());
                            }
                        }
                    }
                } else {
                    logger.debug("Uscs 파일이 없어 계산식 파일 등록을 건너뜁니다.");
                }
            }

            // 모든 작업이 성공한 경우
            logger.debug("유입종류와 파라미터 등록 모두 성공");
            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.waterFlowTypeCreateSuccess");
            result.put("response", waterFlowTypeResponse);

        } catch (Exception e) {
            logger.error("유입종류 등록 중 예외 발생: {}", e.getMessage(), e);
            
            // 유입종류가 성공적으로 등록되었다면 롤백 시도
            if (waterFlowTypeSuccess && flowTypeId != null) {
                rollbackWaterFlowType(flowTypeId);
                // MinIO에서 심볼 파일 삭제
                if (symbolId != null) {
                    deleteMinIOSymbol(symbolId);
                }
                // MinIO에서 계산식 파일들 삭제
                for (String formulaId : formulaIds) {
                    Map<String, Object> deleteResult = deleteMinIOFormula(formulaId);
                    if (!(Boolean) deleteResult.get("success")) {
                        logger.warn("MinIO 계산식 삭제 실패: {}, error: {}", formulaId, deleteResult.get("message"));
                    }
                }
            }
            
            result.put("success", false);
            result.put("message", "messages.error.waterFlowTypeCreateFail: " + e.getMessage());
            result.put("status", 500);
        }
        
        return result;
    }

    /**
     * Metric 파라미터 단건 등록
     * @param param 파라미터 데이터
     * @param flowTypeId 유입종류 ID
     * @return 등록 결과
     */
    private Map<String, Object> createMetricParameter(Map<String, Object> param, String flowTypeId) {
        Map<String, Object> result = new HashMap<>();
        
        // parameter_id 조회
        String parameterName = (String) param.get("parameter_code");
        String parameterId = getParameterIdByName(parameterName);
        
        if (parameterId == null) {
            logger.error("Metric 파라미터 ID 조회 실패: {}", parameterName);
            result.put("success", false);
            result.put("message", "messages.error.metricParameterIdSearchFail");
            return result;
        }
        
        String parameterUrl = authServerBaseUrl + "/api/v1/common/water_flow_type_parameters/";
        
        // 단건 파라미터 등록 요청 구성
        Map<String, Object> singleParamRequest = new HashMap<>();
        singleParamRequest.put("flow_type_id", flowTypeId);
        singleParamRequest.put("parameter_id", parameterId);
        singleParamRequest.put("is_active", param.getOrDefault("is_active", false));
        singleParamRequest.put("is_required", param.getOrDefault("is_required", false));
        singleParamRequest.put("default_value", param.get("default_value"));
        singleParamRequest.put("min_value", param.get("min_value"));
        singleParamRequest.put("max_value", param.get("max_value"));
        singleParamRequest.put("parameter_unit", param.get("parameter_unit"));
        singleParamRequest.put("remarks", param.get("remarks"));
        singleParamRequest.put("unit_system_code", "METRIC");
        singleParamRequest.put("unit_id", param.get("unit_id"));
        
        String singleParamRequestBody = JsonUtil.objectMapToJson(singleParamRequest);
        logger.debug("Metric 파라미터 단건 등록 요청: {}", singleParamRequestBody);

        HttpUtil.HttpResult singleParamResult = HttpUtil.post(parameterUrl, "application/json", singleParamRequestBody);
        if (!singleParamResult.isSuccess()) {
            logger.error("Metric 파라미터 등록 실패: {} - {}", parameterName, singleParamResult.getExtractedErrorMessage());
            result.put("success", false);
            result.put("status", singleParamResult.getStatus());
            result.put("message", "messages.error.metricParameterCreateFail");
            return result;
        }
        
        logger.debug("Metric 파라미터 등록 성공: {}", parameterName);
        result.put("success", true);
        result.put("message", "Metric 파라미터 등록 성공");
        return result;
    }

    /**
     * Uscs 파라미터 단건 등록
     * @param param 파라미터 데이터
     * @param flowTypeId 유입종류 ID
     * @return 등록 결과
     */
    private Map<String, Object> createUscsParameter(Map<String, Object> param, String flowTypeId) {
        Map<String, Object> result = new HashMap<>();
        
        // parameter_id 조회
        String parameterName = (String) param.get("parameter_code");
        String parameterId = getParameterIdByName(parameterName);
        
        if (parameterId == null) {
            logger.error("Uscs 파라미터 ID 조회 실패: {}", parameterName);
            result.put("success", false);
            result.put("message", "messages.error.uscsParameterIdSearchFail");
            return result;
        }
        
        String parameterUrl = authServerBaseUrl + "/api/v1/common/water_flow_type_parameters/";
        
        // 단건 파라미터 등록 요청 구성
        Map<String, Object> singleParamRequest = new HashMap<>();
        singleParamRequest.put("flow_type_id", flowTypeId);
        singleParamRequest.put("parameter_id", parameterId);
        singleParamRequest.put("is_active", param.getOrDefault("is_active", false));
        singleParamRequest.put("is_required", param.getOrDefault("is_required", false));
        singleParamRequest.put("default_value", param.get("default_value"));
        singleParamRequest.put("min_value", param.get("min_value"));
        singleParamRequest.put("max_value", param.get("max_value"));
        singleParamRequest.put("parameter_unit", param.get("parameter_unit"));
        singleParamRequest.put("remarks", param.get("remarks"));
        singleParamRequest.put("unit_system_code", "USCS");
        singleParamRequest.put("unit_id", param.get("unit_id"));
        
        String singleParamRequestBody = JsonUtil.objectMapToJson(singleParamRequest);
        logger.debug("Uscs 파라미터 단건 등록 요청: {}", singleParamRequestBody);
        
        HttpUtil.HttpResult singleParamResult = HttpUtil.post(parameterUrl, "application/json", singleParamRequestBody);
        if (!singleParamResult.isSuccess()) {
            logger.error("Uscs 파라미터 등록 실패: {} - {}", parameterName, singleParamResult.getExtractedErrorMessage());
            result.put("success", false);
            result.put("status", singleParamResult.getStatus());
            result.put("message", "messages.error.uscsParameterCreateFail");
            return result;
        }
        
        logger.debug("Uscs 파라미터 등록 성공: {}", parameterName);
        result.put("success", true);
        result.put("message", "Uscs 파라미터 등록 성공");
        return result;
    }

    /**
     * 유입종류-파라미터 매핑(PK: mapping_id) 업데이트 (PATCH)
     * /api/v1/common/water_flow_type_parameters/{mapping_id}
     * 요청 필드 맵 예시: { "is_active": false, "is_required": true }
     */
    public Map<String, Object> updateWaterFlowTypeParameterMapping(String mappingId, Map<String, Object> fields) {
        logger.debug("유입종류 파라미터 매핑 업데이트 시도: mapping_id={}", mappingId);

        Map<String, Object> result = new HashMap<>();

        if (mappingId == null || mappingId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "messages.error.mappingIdRequired");
            result.put("status", 400);
            return result;
        }

        String url = authServerBaseUrl + "/api/v1/common/water_flow_type_parameters/" + mappingId;

        Map<String, Object> requestData = new HashMap<>();
        if (fields != null) {
            Object activeObj = fields.get("is_active");
            if (activeObj instanceof Boolean) {
                requestData.put("is_active", (Boolean) activeObj);
            } else if (activeObj instanceof String) {
                requestData.put("is_active", Boolean.parseBoolean((String) activeObj));
            }

            Object requiredObj = fields.get("is_required");
            if (requiredObj instanceof Boolean) {
                requestData.put("is_required", (Boolean) requiredObj);
            } else if (requiredObj instanceof String) {
                requestData.put("is_required", Boolean.parseBoolean((String) requiredObj));
            }
        }

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("유입종류 파라미터 매핑 PATCH 요청 본문: {}", requestBody);

        HttpUtil.HttpResult httpResult = HttpUtil.patch(url, "application/json", requestBody);

        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = null;
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
            } catch (Exception e) {
                logger.warn("매핑 업데이트 응답 파싱 실패: {}", e.getMessage());
            }

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterFlowTypeParameterMappingUpdateSuccess");
            result.put("response", parsedResponse);
            logger.debug("유입종류 파라미터 매핑 업데이트 성공: mapping_id={}", mappingId);
        } else {
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.waterFlowTypeParameterMappingUpdateFail";
            }

            Map<String, Object> parsedResponse = null;
            try {
                parsedResponse = JsonUtil.parseJson(httpResult.getBody());
            } catch (Exception ignore) {
            }

            result.put("success", false);
            result.put("status", httpResult.getStatus());
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            logger.warn("유입종류 파라미터 매핑 업데이트 실패: mapping_id={}, error={}", mappingId, errorMessage);
        }

        return result;
    }

    /**
     * 수질 파라미터 업데이트 - 외부 API 호출
     * PATCH /api/v1/common/water_quality_parameters/{parameter_id}
     */
    public Map<String, Object> updateWaterQualityParameter(String parameterId, Map<String, Object> parameterData) {
        logger.debug("수질 파라미터 업데이트 시도: parameter_id={}", parameterId);

        Map<String, Object> result = new HashMap<>();

        if (parameterId == null || parameterId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "messages.error.parameterIdRequired");
            result.put("status", 400);
            return result;
        }

        String url = authServerBaseUrl + "/api/v1/common/water_quality_parameters/" + parameterId;

        Map<String, Object> requestData = new HashMap<>();

        requestData.put("parameter_code", parameterData.getOrDefault("parameter_code", ""));
        requestData.put("parameter_name", parameterData.getOrDefault("parameter_name", ""));
        requestData.put("parameter_name_en", parameterData.getOrDefault("parameter_name_en", ""));
        requestData.put("default_unit", parameterData.getOrDefault("default_unit", ""));
        requestData.put("description", parameterData.getOrDefault("description", ""));

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("수질 파라미터 PATCH 요청 본문: {}", requestBody);

        HttpUtil.HttpResult httpResult = HttpUtil.patch(url, "application/json", requestBody);

        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = null;
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
            } catch (Exception e) {
                logger.warn("수질 파라미터 업데이트 응답 파싱 실패: {}", e.getMessage());
            }

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterQualityParameterUpdateSuccess");
            result.put("response", parsedResponse);
            logger.debug("수질 파라미터 업데이트 성공: parameter_id={}", parameterId);
        } else {
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.waterQualityParameterUpdateFail";
            }

            Map<String, Object> parsedResponse = null;
            try {
                parsedResponse = JsonUtil.parseJson(httpResult.getBody());
            } catch (Exception ignore) {
            }

            result.put("success", false);
            result.put("status", httpResult.getStatus());
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            logger.warn("수질 파라미터 업데이트 실패: parameter_id={}, error={}", parameterId, errorMessage);
        }

        return result;
    }

    /**
     * 수질 파라미터 삭제 - 외부 API 호출
     * DELETE /api/v1/common/water_quality_parameters/{parameter_id}
     */
    public Map<String, Object> deleteWaterQualityParameter(String parameterId) {
        logger.debug("수질 파라미터 삭제 시도: parameter_id={}", parameterId);
        Map<String, Object> result = new HashMap<>();

        if (parameterId == null || parameterId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "messages.error.parameterIdRequired");
            result.put("status", 400);
            return result;
        }

        String deleteUrl = authServerBaseUrl + "/api/v1/common/water_quality_parameters/" + parameterId;
        logger.debug("수질 파라미터 삭제 URL: {}", deleteUrl);

        try {
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("수질 파라미터 삭제 성공: {}", parameterId);
                result.put("success", true);
                result.put("status", deleteResult.getStatus());
                result.put("message", "messages.success.waterQualityParameterDeleteSuccess");
                result.put("response", null);
            } else {
                String errorMessage = deleteResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.waterQualityParameterDeleteFail";
                }
                logger.error("수질 파라미터 삭제 실패: {}, status={}, error={}", 
                        parameterId, deleteResult.getStatus(), errorMessage);
                result.put("success", false);
                result.put("status", deleteResult.getStatus());
                result.put("message", errorMessage);
                
                String responseBody = deleteResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                result.put("response", parsedResponse);
            }
        } catch (Exception e) {
            logger.error("수질 파라미터 삭제 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.waterQualityParameterDeleteError");
            result.put("response", null);
        }

        return result;
    }

    /**
     * 유입종류에 대한 심볼 생성 - 외부 API 호출
     * @return 생성된 symbol_id, 실패 시 null
     */
    private String createSymbolForWaterFlowType(Map<String, Object> waterFlowTypeData) {
        try {
            logger.debug("유입종류 심볼 등록 시작");
            
            // 심볼 업로드 URL
            String symbolUrl = authServerBaseUrl + "/api/v1/minio/symbols/upload";
            
            // 필수 필드들
            String flowTypeCode = (String) waterFlowTypeData.get("flow_type_code");
            String flowTypeName = (String) waterFlowTypeData.get("flow_type_name");
            Object symbolFileObj = waterFlowTypeData.get("symbolFile");
            
            // 심볼 파일이 없으면 심볼 생성을 건너뜀
            if (symbolFileObj == null) {
                logger.debug("심볼 파일이 없어 심볼 생성을 건너뜀");
                return null;
            }
            
            // 멀티파트 폼 데이터 구성
            Map<String, Object> formData = new HashMap<>();
            formData.put("symbol_code", flowTypeCode != null ? flowTypeCode : "");
            formData.put("symbol_type", "INFLUENT");
            formData.put("symbol_name", flowTypeName != null ? flowTypeName : "");
            formData.put("symbol_description", flowTypeName + " 탭 심볼 정보");
            formData.put("version", 1);
            formData.put("file_category", "symbol");
            formData.put("file", symbolFileObj);
            
            logger.debug("심볼 등록 폼 데이터: {}", formData);
            
            // API 호출 - Multipart POST
            HttpUtil.HttpResult symbolResult = HttpUtil.postMultipart(symbolUrl, formData);
            
            if (symbolResult.isSuccess()) {
                logger.debug("유입종류 심볼 등록 성공");
                String responseBody = symbolResult.getBody();
                Map<String, Object> symbolResponse = JsonUtil.parseJson(responseBody);
                if (symbolResponse != null && symbolResponse.get("data") != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) symbolResponse.get("data");
                    if (data != null && data.get("symbol_id") != null) {
                        String symbolId = data.get("symbol_id").toString();
                        logger.debug("생성된 심볼 ID: {}", symbolId);
                        return symbolId;
                    }
                }
            } else {
                String errorMessage = symbolResult.getExtractedErrorMessage();
                logger.warn("유입종류 심볼 등록 실패 (계속 진행): error={}", errorMessage);
                // 심볼 등록 실패는 치명적이지 않으므로 예외를 던지지 않음
            }
            
        } catch (Exception e) {
            logger.warn("유입종류 심볼 등록 중 예외 발생 (계속 진행): error={}", e.getMessage());
            // 심볼 등록 실패는 치명적이지 않으므로 예외를 다시 던지지 않음
        }
        
        return null;
    }

    /**
     * 유입종류에 대한 심볼 생성(파일없고 색상만 있을경우)
     * @return 생성된 symbol_id, 실패 시 null
     */
    private String createSymbolColorOnly(Map<String, Object> waterFlowTypeData) {
        try {
            logger.debug("유입종류 심볼 등록 시작");
            
            // 심볼 등록 URL
            String symbolUrl = authServerBaseUrl + "/api/v1/common/symbols/";
            
            // 심볼 데이터 구성
            Map<String, Object> symbolData = new HashMap<>();
            
            // 필수 필드들
            String flowTypeCode = (String) waterFlowTypeData.get("flow_type_code");
            String flowTypeName = (String) waterFlowTypeData.get("flow_type_name");
            String symbolColor = (String) waterFlowTypeData.get("symbol_color");
            
            symbolData.put("symbol_code", flowTypeCode != null ? flowTypeCode : "");
            symbolData.put("symbol_type", "INFLUENT");
            symbolData.put("symbol_name", flowTypeName != null ? flowTypeName : "");
            symbolData.put("symbol_color", symbolColor);
            symbolData.put("symbol_description", flowTypeName + " 탭 심볼 색상");
            
            // 요청 본문 생성
            String requestBody = JsonUtil.objectMapToJson(symbolData);
            logger.debug("심볼 등록 요청 본문: {}", requestBody);
            
            // API 호출
            HttpUtil.HttpResult symbolResult = HttpUtil.post(symbolUrl, "application/json", requestBody);
            
            if (symbolResult.isSuccess()) {
                logger.debug("유입종류 심볼 등록 성공");
                String responseBody = symbolResult.getBody();
                Map<String, Object> symbolResponse = JsonUtil.parseJson(responseBody);
                
                if (symbolResponse != null && symbolResponse.get("symbol_id") != null) {
                    String symbolId = symbolResponse.get("symbol_id").toString();
                    logger.debug("생성된 심볼 ID: {}", symbolId);
                    return symbolId;
                }
            } else {
                String errorMessage = symbolResult.getExtractedErrorMessage();
                logger.warn("유입종류 심볼 등록 실패 (계속 진행):, error={}", errorMessage);
                // 심볼 등록 실패는 치명적이지 않으므로 예외를 던지지 않음
            }
            
        } catch (Exception e) {
            logger.warn("유입종류 심볼 등록 중 예외 발생 (계속 진행):, error={}", e.getMessage());
            // 심볼 등록 실패는 치명적이지 않으므로 예외를 다시 던지지 않음
        }
        
        return null;
    }

    /**
     * 심볼 파일 업데이트 - 외부 API 호출
     */
    private void updateSymbolFileForWaterFlowType(String symbolId, Map<String, Object> waterFlowTypeData) {
        try {
            Object symbolFileObj = waterFlowTypeData.get("symbolFile");
            
            // 심볼 파일이 없으면 파일 업데이트를 건너뜀
            if (symbolFileObj == null) {
                logger.debug("심볼 파일이 없어 파일 업데이트를 건너뜀");
                return;
            }
            
            logger.debug("심볼 파일 업데이트 시작: symbolId={}", symbolId);
            
            // 심볼 파일 업데이트 URL
            String symbolFileUrl = authServerBaseUrl + "/api/v1/minio/symbols/upload/" + symbolId;
            
            // 멀티파트 폼 데이터 구성
            Map<String, Object> formData = new HashMap<>();
            formData.put("file", symbolFileObj);
            formData.put("version", 1);
            formData.put("file_category", "symbol");
            
            logger.debug("심볼 파일 업데이트 폼 데이터: {}", formData);
            
            // API 호출 - Multipart POST
            HttpUtil.HttpResult symbolResult = HttpUtil.postMultipart(symbolFileUrl, formData);
            
            if (symbolResult.isSuccess()) {
                logger.debug("심볼 파일 업데이트 성공: symbolId={}", symbolId);
            } else {
                String errorMessage = symbolResult.getExtractedErrorMessage();
                logger.warn("심볼 파일 업데이트 실패 (계속 진행): symbolId={}, error={}", symbolId, errorMessage);
                // 심볼 파일 업데이트 실패는 치명적이지 않으므로 예외를 던지지 않음
            }
            
        } catch (Exception e) {
            logger.warn("심볼 파일 업데이트 중 예외 발생 (계속 진행): symbolId={}, error={}", symbolId, e.getMessage());
            // 심볼 파일 업데이트 실패는 치명적이지 않으므로 예외를 다시 던지지 않음
        }
    }

    /**
     * 심볼 업데이트 - 외부 API 호출
     */
    private void updateSymbolForWaterFlowType(String symbolId, Map<String, Object> waterFlowTypeData) {
        try {
            String symbolColor = (String) waterFlowTypeData.get("symbol_color");
            
            // svg_symbol_id와 symbol_color가 모두 있어야 업데이트 진행
            if (symbolId == null || symbolId.trim().isEmpty() || 
                symbolColor == null || symbolColor.trim().isEmpty()) {
                logger.debug("심볼 업데이트 건너뜀: svg_symbol_id={}, symbol_color={}", symbolId, symbolColor);
                return;
            }
            
            logger.debug("심볼 업데이트 시작: symbolId={}, symbolColor={}", symbolId, symbolColor);
            
            // 심볼 업데이트 URL
            String symbolUrl = authServerBaseUrl + "/api/v1/common/symbols/" + symbolId;
            
            // 업데이트 데이터 구성
            Map<String, Object> symbolData = new HashMap<>();
            symbolData.put("symbol_color", symbolColor);
            
            // 요청 본문 생성
            String requestBody = JsonUtil.objectMapToJson(symbolData);
            logger.debug("심볼 업데이트 요청 본문: {}", requestBody);
            
            // API 호출 - PATCH 방식
            HttpUtil.HttpResult symbolResult = HttpUtil.patch(symbolUrl, "application/json", requestBody);
            
            if (symbolResult.isSuccess()) {
                logger.debug("심볼 업데이트 성공: symbolId={}", symbolId);
            } else {
                String errorMessage = symbolResult.getExtractedErrorMessage();
                logger.warn("심볼 업데이트 실패 (계속 진행): symbolId={}, error={}", symbolId, errorMessage);
                // 심볼 업데이트 실패는 치명적이지 않으므로 예외를 던지지 않음
            }
            
        } catch (Exception e) {
            logger.warn("심볼 업데이트 중 예외 발생 (계속 진행): error={}", e.getMessage());
            // 심볼 업데이트 실패는 치명적이지 않으므로 예외를 다시 던지지 않음
        }
    }

    /**
     * 파라미터명으로 parameter_id 조회 - GET 방식
     */
    private String getParameterIdByName(String parameterName) {
        try {
            String parameterSearchUrl = authServerBaseUrl + "/api/v1/common/water_quality_parameters/by/code/" + parameterName;
            logger.debug("파라미터 ID 조회 URL: {}", parameterSearchUrl);
            
            HttpUtil.HttpResult parameterResult = HttpUtil.get(parameterSearchUrl, "application/json", null);
            if (parameterResult.isSuccess()) {
                String responseBody = parameterResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                if (parsedResponse != null) {
                    // 단일 객체 응답 처리
                    Object isActive = parsedResponse.get("is_active");
                    if (isActive != null && (Boolean.TRUE.equals(isActive) || "true".equals(String.valueOf(isActive)))) {
                        if (parsedResponse.get("parameter_id") != null) {
                            String parameterId = parsedResponse.get("parameter_id").toString();
                            logger.debug("파라미터 ID 조회 성공: {} -> {}", parameterName, parameterId);
                            return parameterId;
                        }
                    } else {
                        logger.warn("비활성화된 파라미터: {}", parameterName);
                        return null;
                    }
                } else {
                    logger.warn("파라미터 ID 조회 응답을 파싱할 수 없음: {}", responseBody);
                    return null;
                }
            } else {
                logger.error("파라미터 ID 조회 실패: {} - {}", parameterName, parameterResult.getExtractedErrorMessage());
                return null;
            }
        } catch (Exception e) {
            logger.error("파라미터 ID 조회 중 예외 발생: {} - {}", parameterName, e.getMessage(), e);
            return null;
        }
        return null;
    }

    /**
     * 유입종류 롤백 (삭제)
     */
    private void rollbackWaterFlowType(String flowTypeId) {
        try {
            String deleteUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/" + flowTypeId;
            logger.debug("유입종류 롤백 시도: {}", deleteUrl);
            
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("유입종류 롤백 성공: {}", flowTypeId);
            } else {
                logger.error("유입종류 롤백 실패: {}, error: {}", flowTypeId, deleteResult.getExtractedErrorMessage());
            }
        } catch (Exception e) {
            logger.error("유입종류 롤백 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * MinIO에서 심볼 파일 삭제
     */
    private void deleteMinIOSymbol(String symbolId) {
        try {
            String deleteUrl = authServerBaseUrl + "/api/v1/minio/symbols/" + symbolId;
            logger.debug("MinIO 심볼 삭제 시도: {}", deleteUrl);
            
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("MinIO 심볼 삭제 성공: {}", symbolId);
            } else {
                logger.error("MinIO 심볼 삭제 실패: {}, error: {}", symbolId, deleteResult.getExtractedErrorMessage());
            }
        } catch (Exception e) {
            logger.error("MinIO 심볼 삭제 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 유입종류별 파라미터 삭제 (by-flow-type)
     * - DELETE /api/v1/common/water_flow_type_parameters/by-flow-type
     */
    public Map<String, Object> deleteForWaterFlowParameter(String flowTypeId, String unitSystemCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            String baseUrl = authServerBaseUrl + "/api/v1/common/water_flow_type_parameters/by-flow-type";

            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            boolean hasQuery = false;
            if (flowTypeId != null && !flowTypeId.trim().isEmpty()) {
                urlBuilder.append("?flow_type_id=").append(java.net.URLEncoder.encode(flowTypeId, java.nio.charset.StandardCharsets.UTF_8));
                hasQuery = true;
            }
            if (unitSystemCode != null && !unitSystemCode.trim().isEmpty()) {
                urlBuilder.append(hasQuery ? "&" : "?")
                         .append("unit_system_code=")
                         .append(java.net.URLEncoder.encode(unitSystemCode, java.nio.charset.StandardCharsets.UTF_8));
            }

            String finalUrl = urlBuilder.toString();
            logger.debug("유입종류별 파라미터 삭제 요청: {}", finalUrl);

            HttpUtil.HttpResult httpResult = HttpUtil.delete(finalUrl, "application/json", null);
            if (httpResult.isSuccess()) {
                logger.debug("유입종류별 파라미터 삭제 성공");
                result.put("success", true);
                result.put("status", 200);
                result.put("message", "messages.success.waterFlowTypeParamDeleteSuccess");
                result.put("response", null);
            } else {
                String errorMessage = httpResult.getExtractedErrorMessage();
                logger.error("유입종류별 파라미터 삭제 실패: {}", errorMessage);
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.error.waterFlowTypeParamDeleteFail");
                result.put("response", null);
            }
        } catch (Exception e) {
            logger.error("유입종류별 파라미터 삭제 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.waterFlowTypeParamDeleteError");
            result.put("response", null);
        }

        return result;
    }

    /**
     * MinIO에서 계산식 파일 삭제
     */
    public Map<String, Object> deleteMinIOFormula(String formulaId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String deleteUrl = authServerBaseUrl + "/api/v1/minio/formula/" + formulaId;
            logger.debug("MinIO 계산식 삭제 시도: {}", deleteUrl);
            
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("MinIO 계산식 삭제 성공: {}", formulaId);
                
                result.put("success", true);
                result.put("status", 200);
                result.put("message", "messages.success.formulaDeleteSuccess");
                result.put("response", Map.of("formulaId", formulaId));
            } else {
                logger.error("MinIO 계산식 삭제 실패: {}, error: {}", formulaId, deleteResult.getExtractedErrorMessage());
                
                result.put("success", false);
                result.put("status", 500);
                result.put("message", "messages.error.formulaDeleteFail");
                result.put("response", null);
            }
        } catch (Exception e) {
            logger.error("MinIO 계산식 삭제 중 예외 발생: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.formulaDeleteError");
            result.put("response", null);
        }
        
        return result;
    }

    /**
     * MinIO에서 계산식 파일 삭제 (RequestBody 포함)
     * 필요 시 requestBody를 활용할 수 있도록 오버로드 제공
     */
    public Map<String, Object> deleteMinIOFormula(String formulaId, Map<String, Object> requestBody) {
        Map<String, Object> baseResult = deleteMinIOFormula(formulaId);
        try {
            // 삭제 성공 시에만 추가 로직 수행
            if (Boolean.TRUE.equals(baseResult.get("success")) && requestBody != null) {
                boolean isLastFormula = Boolean.TRUE.equals(requestBody.get("last_formula"));

                if (isLastFormula) {
                    String flowTypeId = requestBody.get("flow_type_id") != null ? String.valueOf(requestBody.get("flow_type_id")) : null;
                    if (flowTypeId != null && !flowTypeId.trim().isEmpty()) {
                        String unitSystemCode = requestBody.get("unit_system_code") != null ? String.valueOf(requestBody.get("unit_system_code")) : null;
                        String unitUpper = unitSystemCode != null ? unitSystemCode.trim().toUpperCase() : null;

                        Map<String, Object> response = new java.util.HashMap<>();
                        response.put("formulaId", formulaId);
                        response.put("flow_type_id", flowTypeId);

                        boolean anyCalled = false;
                        if ("METRIC".equals(unitUpper)) {
                            Map<String, Object> metricDeleteResult = deleteForWaterFlowParameter(flowTypeId, "METRIC");
                            response.put("metricParamDeleteSuccess", Boolean.TRUE.equals(metricDeleteResult.get("success")));
                            anyCalled = true;
                            if (!Boolean.TRUE.equals(metricDeleteResult.get("success"))) {
                                baseResult.put("message", "messages.success.formulaDeleteSuccessWithParamCleanupWarnings");
                            }
                        } else if ("USCS".equals(unitUpper)) {
                            Map<String, Object> uscsDeleteResult = deleteForWaterFlowParameter(flowTypeId, "USCS");
                            response.put("uscsParamDeleteSuccess", Boolean.TRUE.equals(uscsDeleteResult.get("success")));
                            anyCalled = true;
                            if (!Boolean.TRUE.equals(uscsDeleteResult.get("success"))) {
                                baseResult.put("message", "messages.success.formulaDeleteSuccessWithParamCleanupWarnings");
                            }
                        }

                        if (anyCalled) {
                            baseResult.put("response", response);
                        }
                    } else {
                        // flow_type_id 누락
                        baseResult.put("message", "messages.warn.flowTypeIdMissingForParamCleanup");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("추가 삭제 로직 처리 중 오류: {}", e.getMessage(), e);
        }
        return baseResult;
    }

    /**
     * MinIO에서 심볼 파일 삭제 및 재생성
     */
    public Map<String, Object> deleteSymbol(String symbolId, Map<String, Object> waterFlowTypeData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String deleteUrl = authServerBaseUrl + "/api/v1/minio/symbols/" + symbolId;
            logger.debug("MinIO 심볼 삭제 시도: {}", deleteUrl);
            
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("MinIO 심볼 삭제 성공: {}", symbolId);
                
                // 삭제 성공 후 createSymbolColorOnly 호출
                String newSymbolId = createSymbolColorOnly(waterFlowTypeData);
                
                if (newSymbolId != null) {
                    logger.debug("심볼 재생성 성공: {}", newSymbolId);
                    
                    // flow_type의 svg_symbol_id 업데이트
                    String flowTypeId = (String) waterFlowTypeData.get("flow_type_id");
                    if (flowTypeId != null) {
                        updateWaterFlowTypeSymbolId(flowTypeId, newSymbolId);
                    } else {
                        logger.warn("flow_type_id가 없어 심볼 ID 업데이트를 건너뜁니다.");
                    }
                    
                    result.put("success", true);
                    result.put("status", 200);
                    result.put("message", "messages.success.symbolDeleteAndRecreateSuccess");
                    result.put("response", Map.of(
                        "deletedSymbolId", symbolId,
                        "newSymbolId", newSymbolId
                    ));
                } else {
                    logger.warn("심볼 삭제는 성공했으나 재생성 실패: {}", symbolId);
                    
                    result.put("success", true);
                    result.put("status", 200);
                    result.put("message", "messages.success.symbolDeleteSuccessButRecreateWarning");
                    result.put("response", Map.of(
                        "deletedSymbolId", symbolId,
                        "newSymbolId", (Object) null
                    ));
                }
            } else {
                logger.error("MinIO 심볼 삭제 실패: {}, error: {}", symbolId, deleteResult.getExtractedErrorMessage());
                
                result.put("success", false);
                result.put("status", 500);
                result.put("message", "messages.error.symbolDeleteFail");
                result.put("response", null);
            }
        } catch (Exception e) {
            logger.error("MinIO 심볼 삭제 중 예외 발생: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.symbolDeleteError");
            result.put("response", null);
        }
        
        return result;
    }

    /**
     * 유입종류의 svg_symbol_id 업데이트 - 외부 API 호출
     */
    private void updateWaterFlowTypeSymbolId(String flowTypeId, String newSymbolId) {
        try {
            String patchUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/" + flowTypeId;
            logger.debug("유입종류 심볼 ID 업데이트 시도: {}", patchUrl);
            
            // PATCH 요청 데이터 구성
            Map<String, Object> patchData = new HashMap<>();
            patchData.put("svg_symbol_id", newSymbolId);
            
            String patchBody = JsonUtil.objectMapToJson(patchData);
            logger.debug("PATCH 요청 본문: {}", patchBody);
            
            HttpUtil.HttpResult patchResult = HttpUtil.patch(patchUrl, "application/json", patchBody);
            if (patchResult.isSuccess()) {
                logger.debug("유입종류 심볼 ID 업데이트 성공: flowTypeId={}, newSymbolId={}", flowTypeId, newSymbolId);
            } else {
                logger.warn("유입종류 심볼 ID 업데이트 실패: flowTypeId={}, error={}", flowTypeId, patchResult.getExtractedErrorMessage());
            }
        } catch (Exception e) {
            logger.warn("유입종류 심볼 ID 업데이트 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 수질 파라미터 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getWaterQualityParameters(Map<String, Object> parameterData) {
        logger.debug("외부 수질 파라미터 목록 조회 요청조회 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String parametersUrl = authServerBaseUrl + "/api/v1/common/water_quality_parameters/search";
        logger.debug("수질 파라미터 목록 조회 URL: {}", parametersUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용 (기본값 포함)
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("search_field", parameterData.getOrDefault("search_field", "flow_direction"));
        requestMap.put("search_value", parameterData.getOrDefault("flow_direction", ""));
        requestMap.put("page", parameterData.getOrDefault("page", 1));
        requestMap.put("page_size", parameterData.getOrDefault("page_size", 100));
        requestMap.put("order_by", parameterData.getOrDefault("order_by", "created_at"));
        requestMap.put("order_direction", parameterData.getOrDefault("order_direction", "asc"));
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프로젝트 목록 조회 요청 본문: {}", requestBody);

        HttpUtil.HttpResult httpResult = HttpUtil.post(parametersUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("수질 파라미터 목록 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                // 응답이 배열인 경우 직접 파싱
                if (responseBody != null && responseBody.trim().startsWith("[")) {
                    // 배열 응답을 객체로 감싸서 처리
                    String wrappedResponse = "{\"items\":" + responseBody + ",\"total\":0}";
                    parsedResponse = JsonUtil.parseJson(wrappedResponse);
                    
                    if (parsedResponse != null && parsedResponse.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) parsedResponse.get("items");
                        parsedResponse.put("total", items.size());
                        
                        // is_active = true인 항목만 필터링
                        java.util.List<Map<String, Object>> filteredItems = new java.util.ArrayList<>();
                        for (Object item : items) {
                            if (item instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> itemMap = (Map<String, Object>) item;
                                Object isActive = itemMap.get("is_active");
                                if (isActive != null && (Boolean.TRUE.equals(isActive) || "true".equals(String.valueOf(isActive)))) {
                                    filteredItems.add(itemMap);
                                }
                            }
                        }
                        
                        parsedResponse.put("items", filteredItems);
                        parsedResponse.put("total", filteredItems.size());
                        
                        logger.debug("수질 파라미터 is_active=true 필터링 완료: 전체 {} -> 활성 {}", items.size(), filteredItems.size());
                    }
                } else {
                    // 일반 객체 응답 처리
                    parsedResponse = JsonUtil.parseJson(responseBody);
                }
                
                if (parsedResponse != null) {
                    logger.debug("수질 파라미터 목록 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("수질 파라미터 목록 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("items", new java.util.ArrayList<>());
                parsedResponse.put("total", 0);
            }
            
            logger.debug("수질 파라미터 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterQualityParametersSuccess");
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
                errorMessage = "messages.error.waterQualityParametersFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 수질 파라미터 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류 수정 - 외부 API 호출
     */
    public Map<String, Object> updateWaterFlowType(String flowTypeId, Map<String, Object> waterFlowTypeData) {
        logger.debug("외부 인증 서버 유입종류 수정 시도: server={}, flowTypeId={}", authServerBaseUrl, flowTypeId);
        Map<String, Object> result = new HashMap<>();
        List<String> formulaIds = new ArrayList<>(); // 업로드된 formula_id들을 저장할 배열
        
        if (waterFlowTypeData == null || flowTypeId == null || flowTypeId.trim().isEmpty()) {
            logger.warn("유입종류 수정 실패: 필수 데이터가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidWaterFlowTypeData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String waterFlowTypeUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/" + flowTypeId;
        logger.debug("유입종류 수정 URL: {}", waterFlowTypeUrl);

        // 요청 본문 구성
        Map<String, Object> requestData = new HashMap<>();

        String flowTypeCode = (String) waterFlowTypeData.get("flow_type_code");
        
        // 수정 가능한 필드들만 포함
        requestData.put("flow_type_code", flowTypeCode);
        requestData.put("flow_type_name", waterFlowTypeData.getOrDefault("flow_type_name", ""));
        requestData.put("flow_direction", waterFlowTypeData.getOrDefault("flow_direction", ""));
        requestData.put("flow_type_name_en", waterFlowTypeData.getOrDefault("flow_type_name_en", ""));
        requestData.put("description", waterFlowTypeData.getOrDefault("description", ""));
        requestData.put("svg_symbol_id", waterFlowTypeData.getOrDefault("svg_symbol_id", ""));
        requestData.put("is_active", waterFlowTypeData.getOrDefault("is_active", true));
        requestData.put("is_required", waterFlowTypeData.getOrDefault("is_required", true));

        String requestBody = JsonUtil.objectMapToJson(requestData);
        logger.debug("유입종류 수정 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(waterFlowTypeUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("유입종류 수정 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("유입종류 수정 성공. 응답 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("유입종류 수정 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("flow_type_id", flowTypeId);
                parsedResponse.put("message", "messages.success.updateComplete");
            }
            
            logger.debug("유입종류 수정 성공");
            
            // 심볼 업데이트 (svg_symbol_id가 있고 symbol_color가 있는 경우)
            String symbolId = (String) waterFlowTypeData.get("svg_symbol_id");
            if(!"".equals(symbolId)){
                // 심볼 파일 업데이트 (새로운 파일이 있는 경우)
                updateSymbolFileForWaterFlowType(symbolId, waterFlowTypeData);
                updateSymbolForWaterFlowType(symbolId, waterFlowTypeData);
            }

            //Metric 파일 삭제처리
            Map<String, Object> metricDeleteResult = deleteForWaterFlowParameter(flowTypeId, "METRIC");
            if (!(Boolean) metricDeleteResult.get("success")) {
                logger.warn("Metric 파라미터 삭제 실패: {}", metricDeleteResult.get("message"));
            }
            //Uscs 파일 삭제처리
            Map<String, Object> uscsDeleteResult = deleteForWaterFlowParameter(flowTypeId, "USCS");
            if (!(Boolean) uscsDeleteResult.get("success")) {
                logger.warn("Uscs 파라미터 삭제 실패: {}", uscsDeleteResult.get("message"));
            }

            if (waterFlowTypeData.get("metric_parameters") != null && 
                !((java.util.List<?>) waterFlowTypeData.get("metric_parameters")).isEmpty()) {

                java.util.List<?> metricParams = (java.util.List<?>) waterFlowTypeData.get("metric_parameters");
                logger.debug("Metric 파라미터 업데이트 시작 - 총 {}개", metricParams.size());

                for (Object paramObj : metricParams) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> param = (Map<String, Object>) paramObj;

                    // 매칭 없음 => 생성
                    Map<String, Object> createResult = createMetricParameter(param, flowTypeId);
                    if (!(Boolean) createResult.get("success")) {
                        return createResult;
                    }
                }
                
                logger.debug("모든 Metric 파라미터 업데이트 완료");
            } else {
                logger.debug("Metric 파라미터가 없어 등록을 건너뜁니다.");
            }

            // Metric 계산식 업데이트
            if (waterFlowTypeData.get("metricFile") != null) {
                MultipartFile metricFormulaFile = (MultipartFile) waterFlowTypeData.get("metricFile");
                if (!metricFormulaFile.isEmpty()) {
                    Map<String, Object> metricFormulaResult = uploadFormula(
                        metricFormulaFile, 
                        "FLOWTYPE", 
                        flowTypeId, 
                        "METRIC",
                        flowTypeCode
                    );
                    if (!(Boolean) metricFormulaResult.get("success")) {
                        logger.warn("Metric 계산식 파일 업데이트 실패: {}", metricFormulaResult.get("message"));
                    } else {
                        logger.debug("Metric 계산식 파일 업데이트 성곱");
                        // formula_id를 배열에 추가
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) metricFormulaResult.get("data");
                        if (data != null && data.get("formula_id") != null) {
                            formulaIds.add(data.get("formula_id").toString());
                        }
                    }
                }
            } else {
                logger.debug("Metric 파일이 없어 계산식 파일 업데이트를 건너뜁니다.");
            }
            
            if (waterFlowTypeData.get("uscs_parameters") != null && 
                !((java.util.List<?>) waterFlowTypeData.get("uscs_parameters")).isEmpty()) {

                java.util.List<?> uscsParams = (java.util.List<?>) waterFlowTypeData.get("uscs_parameters");
                logger.debug("Uscs 파라미터 업데이트 시작 - 총 {}개", uscsParams.size());

                for (Object paramObj : uscsParams) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> param = (Map<String, Object>) paramObj;

                    // 매칭 없음 => 생성
                    Map<String, Object> createResult = createUscsParameter(param, flowTypeId);
                    if (!(Boolean) createResult.get("success")) {
                        // MinIO에서 계산식 파일들 삭제 (Uscs계산식을 처리했을 수 있기 때문에 여기서만 삭제)
                        for (String formulaId : formulaIds) {
                            Map<String, Object> deleteResult = deleteMinIOFormula(formulaId);
                            if (!(Boolean) deleteResult.get("success")) {
                                logger.warn("MinIO 계산식 삭제 실패: {}, error: {}", formulaId, deleteResult.get("message"));
                            }
                        }
                        return createResult;
                    }
                }
                
                logger.debug("모든 Uscs 파라미터 업데이트 완료");
            } else {
                logger.debug("Uscs 파라미터가 없어 등록을 건너뜁니다.");
            }

            // Uscs 계산식 업데이트
            if (waterFlowTypeData.get("uscsFile") != null) {
                MultipartFile uscsFormulaFile = (MultipartFile) waterFlowTypeData.get("uscsFile");
                if (!uscsFormulaFile.isEmpty()) {
                    Map<String, Object> uscsFormulaResult = uploadFormula(
                        uscsFormulaFile, 
                        "FLOWTYPE", 
                        flowTypeId, 
                        "USCS",
                        flowTypeCode
                    );
                    if (!(Boolean) uscsFormulaResult.get("success")) {
                        logger.warn("Uscs 계산식 업데이트 실패: {}", uscsFormulaResult.get("message"));
                    } else {
                        logger.debug("Uscs 계산식 업데이트 성공");
                        // formula_id를 배열에 추가 (Uscs에서는 필요 없을 수도...)
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) uscsFormulaResult.get("data");
                        if (data != null && data.get("formula_id") != null) {
                            formulaIds.add(data.get("formula_id").toString());
                        }
                    }
                }
            } else {
                logger.debug("Uscs 파일이 없어 계산식 파일 업데이트를 건너뜁니다.");
            }
            
            
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterFlowTypeUpdateSuccess");
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
                errorMessage = "messages.error.waterFlowTypeUpdateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 유입종류 수정 실패: {}", errorMessage);
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

    /**
     * 심볼 파일 정보 조회 - 외부 API 호출
     */
    public Map<String, Object> getSymbolFileInfo(String symbolId) {
        logger.debug("외부 인증 서버 심볼 파일 정보 조회 시도: server={}, symbolId={}", authServerBaseUrl, symbolId);
        Map<String, Object> result = new HashMap<>();
        
        if (symbolId == null || symbolId.trim().isEmpty()) {
            logger.warn("심볼 파일 정보 조회 실패: symbolId가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.symbolIdRequired");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String symbolFileInfoUrl = authServerBaseUrl + "/api/v1/minio/symbols/upload/info?symbol_id=" + symbolId;
        logger.debug("심볼 파일 정보 조회 URL: {}", symbolFileInfoUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(symbolFileInfoUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("심볼 파일 정보 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("심볼 파일 정보 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("심볼 파일 정보 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("message", "messages.error.fileInfoParseFail");
            }
            
            // 심볼 파일 정보 조회 성공 시 다운로드 함수 호출
            Map<String, Object> downloadResult = downloadSymbolFile(symbolId, "inline");
            if (parsedResponse != null) {
                parsedResponse.put("file_info", downloadResult);
            }
            
            logger.debug("심볼 파일 정보 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.symbolFileInfoSuccess");
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
                errorMessage = "messages.error.symbolFileInfoFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 심볼 파일 정보 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 심볼 파일 다운로드 - 외부 API 호출
     */
    public Map<String, Object> downloadSymbolFile(String symbolId, String downloadType) {
        logger.debug("외부 인증 서버 심볼 파일 다운로드 시도: server={}, symbolId={}", authServerBaseUrl, symbolId);
        Map<String, Object> result = new HashMap<>();
        
        if (symbolId == null || symbolId.trim().isEmpty()) {
            logger.warn("심볼 파일 다운로드 실패: symbolId가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.symbolIdRequired");
            return result;
        }
        
        // 외부 인증 서버 URL 구성 (download_type=inline 쿼리 파라미터 포함)
        String downloadUrl = authServerBaseUrl + "/api/v1/minio/symbols/download/" + symbolId + "?download_type=" + downloadType;
        logger.debug("심볼 파일 다운로드 URL: {}", downloadUrl);
        
        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(downloadUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("심볼 파일 다운로드 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                // SVG 파일인지 확인 (XML 태그로 시작하는지 체크)
                if (responseBody != null && responseBody.trim().startsWith("<")) {
                    logger.debug("SVG 파일 응답 감지");
                    parsedResponse = new HashMap<>();
                    parsedResponse.put("content_type", "svg");
                    parsedResponse.put("content", responseBody);
                } else {
                    // JSON 파싱 시도
                    parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        logger.debug("심볼 파일 다운로드 성공. 데이터 키: {}", parsedResponse.keySet());
                    }
                }
            } catch (Exception e) {
                logger.warn("심볼 파일 다운로드 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("message", "messages.error.downloadParseFail");
            }
            
            logger.debug("심볼 파일 다운로드 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.symbolDownloadSuccess");
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
                errorMessage = "messages.error.symbolDownloadFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 심볼 파일 다운로드 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 계산식 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getFormulaList(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 계산식 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String formulaListUrl = authServerBaseUrl + "/api/v1/minio/formula/list";
        logger.debug("계산식 목록 조회 URL: {}", formulaListUrl);
        
        // 쿼리 파라미터 구성
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("formula_scope", searchParams.getOrDefault("formula_scope", ""));
        queryParams.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));
        queryParams.put("flow_type_id", searchParams.getOrDefault("flow_type_id", ""));
        queryParams.put("limit", searchParams.getOrDefault("limit", 100));
        queryParams.put("offset", searchParams.getOrDefault("offset", 0));
        
        // 쿼리 파라미터를 URL에 추가
        StringBuilder urlBuilder = new StringBuilder(formulaListUrl);
        urlBuilder.append("?formula_scope=").append(queryParams.get("formula_scope"));
        urlBuilder.append("&unit_system_code=").append(queryParams.get("unit_system_code"));
        urlBuilder.append("&flow_type_id=").append(queryParams.get("flow_type_id"));
        urlBuilder.append("&limit=").append(queryParams.get("limit"));
        urlBuilder.append("&offset=").append(queryParams.get("offset"));
        
        String finalUrl = urlBuilder.toString();
        logger.debug("최종 계산식 목록 조회 URL: {}", finalUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(finalUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("계산식 목록 조회 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("계산식 목록 조회 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("계산식 목록 조회 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("items", new java.util.ArrayList<>());
                parsedResponse.put("total", 0);
            }
            
            logger.debug("계산식 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.formulaListSuccess");
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.formulaListFail";
            }
            
            result.put("message", errorMessage);
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
            
            logger.warn("외부 인증 서버 계산식 목록 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 계산식 업로드 API - 외부 API 호출
     */
    public Map<String, Object> uploadFormula(MultipartFile pythonFile, String formulaScope, String flowTypeId, String unitSystemCode, String flowTypeCode) {
        logger.debug("외부 인증 서버 계산식 업로드 시도: server={}, fileName={}, formulaScope={}, flowTypeId={}, unitSystemCode={}", 
            authServerBaseUrl, pythonFile != null ? pythonFile.getOriginalFilename() : "null", formulaScope, flowTypeId, unitSystemCode);
        Map<String, Object> result = new HashMap<>();
        
        if (pythonFile == null || pythonFile.isEmpty()) {
            logger.warn("계산식 업로드 실패: 파일이 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.fileRequired");
            return result;
        }
        
        if (formulaScope == null || formulaScope.trim().isEmpty()) {
            logger.warn("계산식 업로드 실패: formulaScope가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.formulaScopeRequired");
            return result;
        }
        
        if (flowTypeId == null || flowTypeId.trim().isEmpty()) {
            logger.warn("계산식 업로드 실패: flowTypeId가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.flowTypeIdRequired");
            return result;
        }
        
        if (unitSystemCode == null || unitSystemCode.trim().isEmpty()) {
            logger.warn("계산식 업로드 실패: unitSystemCode가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.unitSystemCodeRequired");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String formulaUploadUrl = authServerBaseUrl + "/api/v1/minio/formula/upload";
        logger.debug("계산식 업로드 URL: {}", formulaUploadUrl);

        // 멀티파트 폼 데이터 구성
        Map<String, Object> formData = new HashMap<>();
        formData.put("python_file", pythonFile);
        formData.put("formula_scope", formulaScope);
        formData.put("flow_type_id", flowTypeId);
        formData.put("unit_system_code", unitSystemCode);
        formData.put("flow_type_code", flowTypeCode);
        
        logger.debug("계산식 업로드 폼 데이터: fileName={}, size={}, formulaScope={}, flowTypeId={}, unitSystemCode={}", 
            pythonFile.getOriginalFilename(), pythonFile.getSize(), formulaScope, flowTypeId, unitSystemCode);

        // HttpUtil을 사용하여 Multipart POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(formulaUploadUrl, formData);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("계산식 업로드 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("계산식 업로드 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("계산식 업로드 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("message", "messages.error.formulaUploadParseFail");
            }
            
            logger.debug("계산식 업로드 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.formulaUploadSuccess");
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.formulaUploadFail";
            }
            
            result.put("message", errorMessage);
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
            
            logger.warn("외부 인증 서버 계산식 업로드 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 유입종류 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteWaterFlowType(String flowTypeId, Map<String, Object> parameterData) {
        logger.debug("유입종류 삭제 시도: flowTypeId={}, parameterData={}", flowTypeId, parameterData);

        String symbolId = null;
        if (parameterData != null) {
            Object svgSymbolId = parameterData.get("svg_symbol_id");
            if (svgSymbolId != null) {
                symbolId = String.valueOf(svgSymbolId).trim();
            }
        }

        if (symbolId != null && !symbolId.isEmpty()) {
            deleteMinIOSymbol(symbolId);
        }

        if (flowTypeId != null && !flowTypeId.trim().isEmpty()) {
            if (parameterData != null) {
                Object metricFormulaIdObj = parameterData.get("metric_formula_ids");
                if (metricFormulaIdObj instanceof Iterable) {
                    @SuppressWarnings("unchecked")
                    Iterable<Object> metricFormulaIds = (Iterable<Object>) metricFormulaIdObj;
                    for (Object idObj : metricFormulaIds) {
                        if (idObj == null) {
                            continue;
                        }
                        String metricFormulaId = String.valueOf(idObj).trim();
                        if (metricFormulaId.isEmpty()) {
                            continue;
                        }
                        Map<String, Object> metricRequest = new HashMap<>();
                        metricRequest.put("last_formula", true);
                        metricRequest.put("flow_type_id", flowTypeId);
                        metricRequest.put("unit_system_code", "METRIC");
                        deleteMinIOFormula(metricFormulaId, metricRequest);
                    }
                }

                Object uscsFormulaIdObj = parameterData.get("uscs_formula_ids");
                if (uscsFormulaIdObj instanceof Iterable) {
                    @SuppressWarnings("unchecked")
                    Iterable<Object> uscsFormulaIds = (Iterable<Object>) uscsFormulaIdObj;
                    for (Object idObj : uscsFormulaIds) {
                        if (idObj == null) {
                            continue;
                        }
                        String uscsFormulaId = String.valueOf(idObj).trim();
                        if (uscsFormulaId.isEmpty()) {
                            continue;
                        }
                        Map<String, Object> uscsRequest = new HashMap<>();
                        uscsRequest.put("last_formula", true);
                        uscsRequest.put("flow_type_id", flowTypeId);
                        uscsRequest.put("unit_system_code", "USCS");
                        deleteMinIOFormula(uscsFormulaId, uscsRequest);
                    }
                }
            }

            deleteWaterFlowTypeFromAuth(flowTypeId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", 200);
        result.put("message", "messages.success.waterFlowTypeDeleteSuccess");
        
        return result;
    }

    /**
     * 외부 인증 서버에서 유입종류 삭제
     */
    private void deleteWaterFlowTypeFromAuth(String flowTypeId) {
        String deleteUrl = authServerBaseUrl + "/api/v1/common/water_flow_types/" + flowTypeId;
        logger.debug("외부 인증 서버 유입종류 삭제 시도: {}", deleteUrl);
        try {
            HttpUtil.HttpResult deleteResult = HttpUtil.delete(deleteUrl, "application/json", null);
            if (deleteResult.isSuccess()) {
                logger.debug("외부 인증 서버 유입종류 삭제 성공: {}", flowTypeId);
            } else {
                logger.error("외부 인증 서버 유입종류 삭제 실패: {}, status={}, error={}",
                        flowTypeId, deleteResult.getStatus(), deleteResult.getExtractedErrorMessage());
            }
        } catch (Exception e) {
            logger.error("외부 인증 서버 유입종류 삭제 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 계산식 추출 API - 외부 API 호출
     */
    public Map<String, Object> extractFormula(MultipartFile file) {
        logger.debug("외부 인증 서버 계산식 추출 시도: server={}, fileName={}", authServerBaseUrl, file != null ? file.getOriginalFilename() : "null");
        Map<String, Object> result = new HashMap<>();
        
        if (file == null || file.isEmpty()) {
            logger.warn("계산식 추출 실패: 파일이 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.fileRequired");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String formulaUrl = authServerBaseUrl + "/api/v1/minio/formula/influents";
        logger.debug("계산식 추출 URL: {}", formulaUrl);

        // 멀티파트 폼 데이터 구성
        Map<String, Object> formData = new HashMap<>();
        formData.put("python_file", file);
        logger.debug("계산식 추출 폼 데이터: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        // HttpUtil을 사용하여 Multipart POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(formulaUrl, formData);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("계산식 추출 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("계산식 추출 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("계산식 추출 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("message", "messages.error.formulaParseFail");
            }
            
            logger.debug("계산식 추출 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.formulaExtractSuccess");
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.formulaExtractFail";
            }
            
            result.put("message", errorMessage);
            if (parsedResponse != null) {
                result.put("response", parsedResponse);
            } else {
                result.put("response", new HashMap<>());
            }
            
            logger.warn("외부 인증 서버 계산식 추출 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 수질 파라미터 등록 - 외부 API 호출
     */
    public Map<String, Object> insertWaterQualityParameter(Map<String, Object> parameterData) {
        logger.debug("외부 인증 서버 수질 파라미터 등록 시도: server={}, parameterData={}", authServerBaseUrl, parameterData);
        Map<String, Object> result = new HashMap<>();
        
        if (parameterData == null || parameterData.isEmpty()) {
            logger.warn("수질 파라미터 등록 실패: parameterData가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.parameterDataRequired");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String parameterInsertUrl = authServerBaseUrl + "/api/v1/common/water_quality_parameters/";
        logger.debug("수질 파라미터 등록 URL: {}", parameterInsertUrl);

        // 요청 본문 생성
        String requestBody = JsonUtil.objectMapToJson(parameterData);
        logger.debug("수질 파라미터 등록 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(parameterInsertUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            logger.debug("수질 파라미터 등록 원본 응답: {}", responseBody);
            
            Map<String, Object> parsedResponse = null;
            
            // JSON 파싱 시도
            try {
                parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    logger.debug("수질 파라미터 등록 성공. 데이터 키: {}", parsedResponse.keySet());
                }
            } catch (Exception e) {
                logger.warn("수질 파라미터 등록 응답 파싱 실패: {}", e.getMessage());
                parsedResponse = new HashMap<>();
                parsedResponse.put("message", "messages.error.parameterInfoParseFail");
            }
            
            logger.debug("수질 파라미터 등록 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.waterQualityParameterInsertSuccess");
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
                errorMessage = "messages.error.waterQualityParameterInsertFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 수질 파라미터 등록 실패: {}", errorMessage);
        }
        
        return result;
    }
} 