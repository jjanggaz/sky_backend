package com.wai.admin.service.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wai.admin.util.CommonCodeUtil;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MachineService {

    private static final Logger logger = LoggerFactory.getLogger(MachineService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    @Autowired
    private CommonCodeUtil commonCodeUtil;

    /**
     * 공통 코드 조회 - 여러 API 호출하여 통합 결과 반환
     */
    public Map<String, Object> getCommonCodes(Map<String, Object> searchParams) {
        logger.debug("공통 코드 조회 시도: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        String parentValue = (String) searchParams.getOrDefault("parent_key", "");

        try {
            // 단위 시스템 검색 호출
            Map<String, Object> unitSystemsResult = commonCodeUtil.getUnitSystems();

            // 단위 시스템 조회 실패 시
            if (!(Boolean) unitSystemsResult.get("success")) {
                result.put("success", false);
                result.put("status", unitSystemsResult.get("status"));
                result.put("message", unitSystemsResult.get("message"));
                result.put("response", unitSystemsResult);
                return result;
            }

            // 분류 코드 조회
            Map<String, Object> SecondDepthResult = commonCodeUtil.getCommonCodesByCategory("parent_key", parentValue, true);

            // 분류 코드 조회 실패 시
            if (!(Boolean) SecondDepthResult.get("success")) {
                result.put("success", false);
                result.put("status", SecondDepthResult.get("status"));
                result.put("message", SecondDepthResult.get("message"));
                result.put("response", SecondDepthResult);
                return result;
            }

            // 결과 구성
            Map<String, Object> response = new HashMap<>();
            response.put("unitSystems", unitSystemsResult.get("response"));
            response.put("secondDepth", SecondDepthResult.get("data"));

            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.commonCodesSuccess");
            result.put("response", response);

            // 필요시 다른 API들도 추가로 호출하여 결과에 포함
            // 예: result.put("other_data", otherApiCall());

            logger.debug("공통 코드 조회 성공");
        } catch (Exception e) {
            logger.error("공통 코드 조회 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.commonCodesFail");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 깊이별 코드 조회 - CommonCodeUtil 호출
     */
    public Map<String, Object> getDepth(Map<String, Object> searchParams) {
        logger.debug("깊이별 코드 조회 시도: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        String parentValue = (String) searchParams.get("parent_value");

        try {
            // 깊이별 코드 조회
            Map<String, Object> depthResult = commonCodeUtil.getCommonCodesByCategory("parent_key", parentValue, true);

            // 깊이별 코드 조회 실패 시
            if (!(Boolean) depthResult.get("success")) {
                result.put("success", false);
                result.put("status", depthResult.get("status"));
                result.put("message", depthResult.get("message"));
                result.put("response", depthResult);
                return result;
            }

            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.depthSuccess");
            result.put("response", depthResult.get("data"));

            logger.debug("깊이별 코드 조회 성공");
        } catch (Exception e) {
            logger.error("깊이별 코드 조회 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "messages.error.depthFail");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 기계 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getMachineList(Map<String, Object> searchParams, String waiLang) {
        logger.debug("외부 인증 서버 기계 목록 조회 시도: server={}, params={}, waiLang={}", authServerBaseUrl, searchParams,
                waiLang);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String machineUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/search"
                + "?enable_dynamic_search=true"
                + "&include_hierarchy=true";
        logger.debug("기계 목록 조회 URL: {}", machineUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 하드코딩 (기계=EQUIP)으로 하기로함
        requestMap.put("top_root_level", "EQUIP");
        
        // 기본값 설정
        requestMap.put("keyword", searchParams.getOrDefault("keyword", ""));
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("equipment_type", searchParams.getOrDefault("equipment_type", ""));
        requestMap.put("root_equipment_type", searchParams.getOrDefault("root_equipment_type", ""));

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));

        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("is_active", searchParams.getOrDefault("is_active", true));
        requestMap.put("include_hierarchy", searchParams.getOrDefault("include_hierarchy", true));
        requestMap.put("include_vendor", searchParams.getOrDefault("include_vendor", true));
        requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));

        // JSON 형식이므로 get을 사용 Default 없음
        requestMap.put("search_criteria", searchParams.get("search_criteria"));
        requestMap.put("output_values", searchParams.get("output_values"));
        requestMap.put("specifications", searchParams.get("specifications"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("기계 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(machineUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("기계 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));

                // wai_lang이 "en"인 경우 _en 필드로 덮어쓰기
                if ("en".equalsIgnoreCase(waiLang) && parsedResponse.containsKey("items")) {
                    Object itemsObj = parsedResponse.get("items");
                    if (itemsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                        for (Map<String, Object> item : items) {
                            // equipment_type_name_en -> equipment_type_name
                            if (item.containsKey("equipment_type_name_en")
                                    && item.get("equipment_type_name_en") != null) {
                                item.put("equipment_type_name", item.get("equipment_type_name_en"));
                            }

                            // vendor_info.vendor_name_en -> vendor_info.vendor_name
                            if (item.containsKey("vendor_info") && item.get("vendor_info") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> vendorInfo = (Map<String, Object>) item.get("vendor_info");
                                if (vendorInfo.containsKey("vendor_name_en") && vendorInfo.get("vendor_name_en") != null) {
                                    vendorInfo.put("vendor_name", vendorInfo.get("vendor_name_en"));
                                }
                            }
                        }
                        logger.debug("언어 설정(en)에 따라 필드 덮어쓰기 완료");
                    }
                }
            } else {
                logger.warn("기계 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            logger.debug("기계 목록 조회 성공");
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

            logger.warn("외부 인증 서버 기계 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 공통 코드 조회 - 외부 API 호출 (GET 방식)
     */
    public Map<String, Object> getCommonDepthCode(String searchKey) {
        logger.debug("외부 인증 서버 공통 코드 조회 시도: server={}, searchKey={}", authServerBaseUrl, searchKey);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String commonCodeUrl = authServerBaseUrl + "/api/v1/common/common_codes/custom/code_key/" + searchKey;
        logger.debug("공통 코드 조회 URL: {}", commonCodeUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(commonCodeUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("공통 코드 조회 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "공통 코드 조회가 완료되었습니다.");
            } else {
                logger.error("공통 코드 조회 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("공통 코드 조회 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "공통 코드 조회에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }

        return result;
    }

    /**
     * 깊이별 상세 코드 조회 - CommonCodeUtil 호출
     */
    public Map<String, Object> getDepthDetail(Map<String, Object> searchParams) {
        logger.debug("깊이별 상세 코드 조회 시도: params={}", searchParams);

        // CommonCodeUtil에 위임 (자동으로 언어 처리됨)
        return commonCodeUtil.getDepthDetail(searchParams);
    }

    /**
     * 상세검색 타입 조회 - 외부 API 호출 (GET 방식)
     */
    public Map<String, Object> getDepthDetailSearchType(String search_key) {
        logger.debug("외부 인증 서버 상세검색 타입 조회 시도: server={}, search_key={}", authServerBaseUrl, search_key);
        Map<String, Object> result = new HashMap<>();

        if (search_key == null || search_key.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "search_key 파라미터가 필요합니다.");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String searchTypeUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/search/available-fields/"
                + search_key;
        logger.debug("상세검색 타입 조회 URL: {}", searchTypeUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(searchTypeUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("상세검색 타입 조회 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "상세검색 타입 조회가 완료되었습니다.");
            } else {
                logger.error("상세검색 타입 조회 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("상세검색 타입 조회 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "상세검색 타입 조회에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }

        return result;
    }

    /**
     * 카탈로그 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getCatalogList(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 카탈로그 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String catalogUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/search"
                + "?enable_dynamic_search=true"
                + "&include_hierarchy=true";
        logger.debug("카탈로그 목록 조회 URL: {}", catalogUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 하드코딩 (기계=EQUIP)으로 하기로함
        requestMap.put("top_root_level", "EQUIP");

        // 기본값 설정
        // requestMap.put("keyword", searchParams.getOrDefault("keyword", ""));
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        // requestMap.put("equipment_type", searchParams.getOrDefault("equipment_type",
        // ""));
        // requestMap.put("root_equipment_type",
        // searchParams.getOrDefault("root_equipment_type", ""));

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));

        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("is_active", searchParams.getOrDefault("is_active", true));
        requestMap.put("include_hierarchy", searchParams.getOrDefault("include_hierarchy", true));
        requestMap.put("include_vendor", searchParams.getOrDefault("include_vendor", true));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("카탈로그 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(catalogUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("카탈로그 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));

                // search_criteria와 output_values 필터링 처리
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>(item);

                        // search_criteria 처리 - item에 바로 추가
                        if (filteredItem.containsKey("search_criteria")
                                && filteredItem.get("search_criteria") instanceof Map) {
                            Map<String, Object> searchCriteria = (Map<String, Object>) filteredItem
                                    .get("search_criteria");

                            for (Map.Entry<String, Object> entry : searchCriteria.entrySet()) {
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> fieldData = (Map<String, Object>) entry.getValue();
                                    String key = entry.getKey(); // 외부 키 사용 (예: "max_power_kW")
                                    Object value = fieldData.get("value");
                                    filteredItem.put(key, value);
                                }
                            }
                        }

                        // output_values 처리 - item에 바로 추가
                        if (filteredItem.containsKey("output_values")
                                && filteredItem.get("output_values") instanceof Map) {
                            Map<String, Object> outputValues = (Map<String, Object>) filteredItem.get("output_values");

                            for (Map.Entry<String, Object> entry : outputValues.entrySet()) {
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> fieldData = (Map<String, Object>) entry.getValue();
                                    String key = entry.getKey(); // 외부 키 사용 (예: "unit_price_kr")
                                    Object value = fieldData.get("value");
                                    filteredItem.put(key, value);
                                }
                            }
                        }

                        filteredResponse.add(filteredItem);
                    }

                    // 필터링된 items로 교체
                    parsedResponse.put("items", filteredResponse);
                }
            } else {
                logger.warn("카탈로그 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            logger.debug("카탈로그 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.catalogListSuccess");
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
                errorMessage = "messages.error.catalogListFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 카탈로그 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 카탈로그 컬럼 목록 조회 - 외부 API 호출 (GET 방식)
     */
    public Map<String, Object> getCatalogColumnList(String search_key) {
        logger.debug("외부 인증 서버 카탈로그 컬럼 목록 조회 시도: server={}, search_key={}", authServerBaseUrl, search_key);
        Map<String, Object> result = new HashMap<>();

        if (search_key == null || search_key.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "search_key 파라미터가 필요합니다.");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String catalogColumnUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/search/available-fields/"
                + search_key;
        logger.debug("카탈로그 컬럼 목록 조회 URL: {}", catalogColumnUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(catalogColumnUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("카탈로그 컬럼 목록 조회 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));

                // fields_metadata 추출 및 필터링
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                if (parsedResponse.containsKey("fields_metadata")) {
                    Map<String, Object> fieldsMetadata = (Map<String, Object>) parsedResponse.get("fields_metadata");

                    // search_criteria 항목 추출
                    if (fieldsMetadata.containsKey("search_criteria")) {
                        List<Map<String, Object>> searchCriteria = (List<Map<String, Object>>) fieldsMetadata
                                .get("search_criteria");
                        if (searchCriteria != null) {
                            for (Map<String, Object> field : searchCriteria) {
                                Map<String, Object> fieldItem = new HashMap<>();
                                fieldItem.put("field_name", field.get("field_name"));
                                fieldItem.put("name_kr", field.get("name_kr"));
                                fieldItem.put("unit_code", field.get("unit_code"));
                                fieldItem.put("type", "search_criteria");
                                filteredResponse.add(fieldItem);
                            }
                        }
                    }

                    // output_values 항목 추출
                    if (fieldsMetadata.containsKey("output_values")) {
                        List<Map<String, Object>> outputValues = (List<Map<String, Object>>) fieldsMetadata
                                .get("output_values");
                        if (outputValues != null) {
                            for (Map<String, Object> field : outputValues) {
                                Map<String, Object> fieldItem = new HashMap<>();
                                fieldItem.put("field_name", field.get("field_name"));
                                fieldItem.put("name_kr", field.get("name_kr"));
                                fieldItem.put("unit_code", field.get("unit_code"));
                                fieldItem.put("type", "output_values");
                                filteredResponse.add(fieldItem);
                            }
                        }
                    }
                }

                result.put("success", true);
                result.put("available_fields", filteredResponse);
                // result.put("original_data", parsedResponse);
                result.put("message", "카탈로그 컬럼 목록 조회가 완료되었습니다.");
            } else {
                logger.error("카탈로그 컬럼 목록 조회 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("카탈로그 컬럼 목록 조회 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "카탈로그 컬럼 목록 조회에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }

        return result;
    }

    /**
     * 기계 엑셀 템플릿 다운로드 - 외부 API 호출 (GET)
     */
    public Map<String, Object> downloadTemplateExcel(String equipmentType) {
        logger.debug("외부 인증 서버 기계 엑셀 템플릿 다운로드 시도: server={}, equipmentType={}", authServerBaseUrl, equipmentType);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String downloadUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/migration/download/" + equipmentType;
        logger.debug("기계 엑셀 템플릿 다운로드 URL: {}", downloadUrl);
        
        // HttpUtil을 사용하여 GET 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.get(downloadUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("기계 엑셀 템플릿 다운로드 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "기계 엑셀 템플릿 다운로드가 완료되었습니다.");
            } else {
                logger.error("기계 엑셀 템플릿 다운로드 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("기계 엑셀 템플릿 다운로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "기계 엑셀 템플릿 다운로드에 실패했습니다.";
            }
            
            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }
        
        return result;
    }

    /**
     * 기계 엑셀 파일 업로드 - 외부 API 호출 (Multipart)
     */
    public Map<String, Object> uploadModelExcel(String equipmentType, MultipartFile excelFile) {
        logger.debug("외부 인증 서버 기계 엑셀 파일 업로드 시도: server={}, equipmentType={}", authServerBaseUrl, equipmentType);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String uploadUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/migration/upload";
        logger.debug("기계 엑셀 파일 업로드 URL: {}", uploadUrl);
        
        // HttpUtil을 사용하여 Multipart POST 요청 수행
        Map<String, Object> formData = new HashMap<>();
        if (excelFile != null) {
            formData.put("file", excelFile);
        }
        formData.put("equipment_type", equipmentType);
        formData.put("upsert_mode", true);
        formData.put("validate_units", true);
        formData.put("apply_jsonb_schema", true);
        
        HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(uploadUrl, formData);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("기계 엑셀 파일 업로드 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "기계 엑셀 파일 업로드가 완료되었습니다.");
            } else {
                logger.error("기계 엑셀 파일 업로드 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("기계 엑셀 파일 업로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "기계 엑셀 파일 업로드에 실패했습니다.";
            }
            
            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }
        
        return result;
    }

    /**
     * 기계 파일 업로드 - 외부 API 호출 (Multipart)
     */
    public Map<String, Object> uploadModelZip(String machineName, MultipartFile allFile) {
        logger.debug("외부 인증 서버 기계 파일 업로드 시도: server={}, machineName={}", authServerBaseUrl, machineName);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String uploadUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/uploadzip/type/" + machineName;
        logger.debug("기계 파일 업로드 URL: {}", uploadUrl);
        
        // HttpUtil을 사용하여 Multipart POST 요청 수행
        Map<String, Object> formData = new HashMap<>();
        if (allFile != null) {
            formData.put("file", allFile);
        }
        HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(uploadUrl, formData);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("기계 파일 업로드 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "기계 파일 업로드가 완료되었습니다.");
            } else {
                logger.error("기계 파일 업로드 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("기계 파일 업로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "기계 파일 업로드에 실패했습니다.";
            }
            
            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }
        
        return result;
    }

    /**
     * 기계 삭제 - 외부 API 호출 (DELETE) 및 추가 처리
     */
    public Map<String, Object> deleteMachine(String equipmentId, Map<String, Object> deleteParams) {
        logger.debug("기계 삭제 처리 시작: equipmentId={}", equipmentId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 메인 기계 삭제 API 호출
            Map<String, Object> deleteResult = deleteEquipmentFromAPI(equipmentId);
            
            if ((Boolean) deleteResult.get("success")) {
                // 2. 기계 삭제 성공 시 파일들 순차적 삭제
                boolean allFilesDeleted = true;
                StringBuilder errorMessages = new StringBuilder();
                
                // Model 파일 삭제
                String modelFileId = (String) deleteParams.get("model_file_id");
                if (modelFileId != null && !modelFileId.trim().isEmpty()) {
                    Map<String, Object> modelResult = deleteModelFile(equipmentId, modelFileId);
                    if (!(Boolean) modelResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("Model 파일 삭제 실패; ");
                    }
                }
                
                // RVT 파일 삭제
                // String rvtFileId = (String) deleteParams.get("rvt_file_id");
                // if (rvtFileId != null && !rvtFileId.trim().isEmpty()) {
                //     Map<String, Object> rvtResult = deleteRvtFile(equipmentId, rvtFileId);
                //     if (!(Boolean) rvtResult.get("success")) {
                //         allFilesDeleted = false;
                //         errorMessages.append("RVT 파일 삭제 실패; ");
                //     }
                // }

                // RFA 파일 삭제
                String rfaFileId = (String) deleteParams.get("rfa_file_id");
                String equipmentType = (String) deleteParams.get("equipment_type");
                if (rfaFileId != null && !rfaFileId.trim().isEmpty() && equipmentType != null && !equipmentType.trim().isEmpty()) {
                    Map<String, Object> rfaResult = deleteRfaFile(equipmentType, rfaFileId);
                    if (!(Boolean) rfaResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("RFA 파일 삭제 실패; ");
                    }
                }
                
                // Symbol 파일 삭제
                String symbolId = (String) deleteParams.get("symbol_id");
                if (symbolId != null && !symbolId.trim().isEmpty()) {
                    Map<String, Object> symbolResult = deleteSymbolFile(equipmentId, symbolId);
                    if (!(Boolean) symbolResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("Symbol 파일 삭제 실패; ");
                    }
                }
                
                // Thumbnail 파일 삭제
                String thumbnailId = (String) deleteParams.get("thumbnail_id");
                if (thumbnailId != null && !thumbnailId.trim().isEmpty()) {
                    Map<String, Object> thumbResult = deleteThumbnailFile(equipmentId, thumbnailId);
                    if (!(Boolean) thumbResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("Thumbnail 파일 삭제 실패; ");
                    }
                }
                
                // Formula 파일 삭제 - TODO: API URL이 정해지면 주석 해제
                String formulaId = (String) deleteParams.get("formula_id");
                if (formulaId != null && !formulaId.trim().isEmpty()) {
                    Map<String, Object> formulaResult = deleteFormulaFile(equipmentId, formulaId);
                    if (!(Boolean) formulaResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("Formula 파일 삭제 실패; ");
                    }
                }
                
                result.put("success", true);
                if (allFilesDeleted) {
                    result.put("message", "기계와 모든 관련 파일이 성공적으로 삭제되었습니다.");
                } else {
                    result.put("message", "기계는 삭제되었지만 일부 파일 삭제에 실패했습니다: " + errorMessages.toString());
                }
                result.put("data", deleteResult.get("data"));
                
                logger.debug("기계 삭제 완료: equipmentId={}, allFilesDeleted={}", equipmentId, allFilesDeleted);
            } else {
                // 기계 삭제 실패
                result = deleteResult;
                logger.error("기계 삭제 실패: equipmentId={}, message={}", equipmentId, deleteResult.get("message"));
            }
            
        } catch (Exception e) {
            logger.error("기계 삭제 처리 중 예외 발생: equipmentId={}, error={}", equipmentId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "기계 삭제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 기계 삭제 API 호출 - 실제 DELETE 요청
     */
    private Map<String, Object> deleteEquipmentFromAPI(String equipmentId) {
        logger.debug("외부 인증 서버 기계 삭제 시도: server={}, equipmentId={}", authServerBaseUrl, equipmentId);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String deleteUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/" + equipmentId;
        logger.debug("기계 삭제 URL: {}", deleteUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // DELETE 요청 성공 - 응답 본문이 없거나 간단할 수 있음
            String responseBody = httpResult.getBody();
            logger.debug("기계 삭제 성공: statusCode={}, responseBody={}", httpResult.getStatus(), responseBody);
            
            result.put("success", true);
            result.put("message", "기계 삭제가 완료되었습니다.");
            
            // 응답 본문이 있는 경우에만 파싱
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } else {
            // 실패한 응답 처리
            logger.error("기계 삭제 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "기계 삭제에 실패했습니다.";
            }
            
            result.put("message", errorMessage);
            result.put("statusCode", httpResult.getStatus());
            if (parsedResponse != null) {
                result.put("data", parsedResponse);
            }
        }
        
        return result;
    }

    /**
     * Model 파일 삭제
     */
    private Map<String, Object> deleteModelFile(String equipmentId, String modelFileId) {
        logger.debug("Model 파일 삭제 시도: equipmentId={}, modelFileId={}", equipmentId, modelFileId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/" + equipmentId + "/model/" + modelFileId;
        return executeFileDelete(deleteUrl, "Model 파일");
    }

    /**
     * RVT 파일 삭제
     */
    private Map<String, Object> deleteRvtFile(String equipmentId, String rvtFileId) {
        logger.debug("RVT 파일 삭제 시도: equipmentId={}, rvtFileId={}", equipmentId, rvtFileId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/" + equipmentId + "/rvt/" + rvtFileId;
        return executeFileDelete(deleteUrl, "RVT 파일");
    }

    /**
     * RFA 파일 삭제
     */
    private Map<String, Object> deleteRfaFile(String equipmentType, String rfaFileId) {
        logger.debug("RFA 파일 삭제 시도: equipmentType={}, rfaFileId={}", equipmentType, rfaFileId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/" + equipmentType + "/rfa/";
        return executeFileDelete(deleteUrl, "RFA 파일");
    }

    /**
     * Symbol 파일 삭제
     */
    private Map<String, Object> deleteSymbolFile(String equipmentId, String symbolId) {
        logger.debug("Symbol 파일 삭제 시도: equipmentId={}, symbolId={}", equipmentId, symbolId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/" + equipmentId + "/symbol/" + symbolId;
        return executeFileDelete(deleteUrl, "Symbol 파일");
    }

    /**
     * Thumbnail 파일 삭제
     */
    private Map<String, Object> deleteThumbnailFile(String equipmentId, String thumbnailId) {
        logger.debug("Thumbnail 파일 삭제 시도: equipmentId={}, thumbnailId={}", equipmentId, thumbnailId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/" + equipmentId + "/thumbnail/" + thumbnailId;
        return executeFileDelete(deleteUrl, "Thumbnail 파일");
    }

    /**
     * Formula 파일 삭제 - TODO: API URL이 정해지면 사용
     */
    private Map<String, Object> deleteFormulaFile(String equipmentId, String formulaId) {
        logger.debug("Formula 파일 삭제 시도: equipmentId={}, formulaId={}", equipmentId, formulaId);
        // TODO: Formula 삭제 API URL이 정해지면 아래 URL 수정
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/formula/" + formulaId;
        return executeFileDelete(deleteUrl, "Formula 파일");
    }

    /**
     * 파일 삭제 공통 메서드
     */
    private Map<String, Object> executeFileDelete(String deleteUrl, String fileType) {
        Map<String, Object> result = new HashMap<>();
        logger.debug("{} 삭제 URL: {}", fileType, deleteUrl);
        
        try {
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                logger.debug("{} 삭제 성공: statusCode={}", fileType, httpResult.getStatus());
                result.put("success", true);
                result.put("message", fileType + " 삭제가 완료되었습니다.");
                
                String responseBody = httpResult.getBody();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("data", parsedResponse);
                    }
                }
            } else {
                logger.error("{} 삭제 실패: statusCode={}, body={}", fileType, httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = fileType + " 삭제에 실패했습니다.";
                }
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
            }
        } catch (Exception e) {
            logger.error("{} 삭제 중 예외 발생: {}", fileType, e.getMessage(), e);
            result.put("success", false);
            result.put("message", fileType + " 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 기계 상세 업체 정보 조회
     */
    public Map<String, Object> getDetailCommon(String equipmentType) {
        logger.debug("기계 상세 업체 정보 조회 시작: equipmentType={}", equipmentType);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 업체 정보 조회 (Vendors)
            logger.debug("업체 정보 조회 시작");
            String vendorsUrl = authServerBaseUrl + "/api/v1/equipment/vendors/search?limit=0";
            logger.debug("업체 정보 조회 URL: {}", vendorsUrl);
            
            HttpUtil.HttpResult vendorsResult = HttpUtil.get(vendorsUrl, "application/json", "");
            
            if (vendorsResult.isSuccess()) {
                String responseBody = vendorsResult.getBody();
                
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    logger.debug("업체 정보 조회 성공");
                    
                    // 응답이 배열 형태이므로 List로 파싱
                    java.util.List<Object> parsedVendors = JsonUtil.parseJsonToList(responseBody);
                    
                    if (parsedVendors != null) {
                        result.put("success", true);
                        result.put("data", parsedVendors);
                        result.put("message", "업체 정보 조회가 완료되었습니다.");
                    } else {
                        // 파싱 실패 시 원본 문자열 반환
                        logger.warn("JSON 배열 파싱 실패, 원본 응답 반환");
                        result.put("success", true);
                        result.put("data", responseBody);
                        result.put("message", "업체 정보 조회가 완료되었습니다.");
                    }
                } else {
                    logger.error("업체 정보 응답이 비어있습니다");
                    result.put("success", false);
                    result.put("message", "응답이 비어있습니다.");
                }
            } else {
                logger.error("업체 정보 조회 실패: statusCode={}, body={}", 
                    vendorsResult.getStatus(), vendorsResult.getBody());
                result.put("success", false);
                
                String errorMessage = vendorsResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "업체 정보 조회에 실패했습니다.";
                }
                result.put("message", errorMessage);
                result.put("statusCode", vendorsResult.getStatus());
            }
            
            logger.debug("기계 상세 업체 정보 조회 완료: equipmentType={}", equipmentType);
            
        } catch (Exception e) {
            logger.error("기계 상세 업체 정보 조회 중 예외 발생: equipmentType={}, error={}", 
                equipmentType, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "업체 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 기계 상세 파일 조회 - 외부 API 호출 (GET)
     */
    public Map<String, Object> getDetailFiles(String equipmentId, Map<String, Object> fileParams) {
        logger.debug("기계 상세 파일 다운로드 처리 시작: equipmentId={}", equipmentId);
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> filesData = new HashMap<>();
        
        try {
            boolean allFilesDownloaded = true;
            StringBuilder errorMessages = new StringBuilder();
            
            // Formula 정보 조회
            String equipmentType = (String) fileParams.get("equipment_type");
            if (equipmentType != null && !equipmentType.trim().isEmpty()) {
                Map<String, Object> formulaResult = getFormulaByEquipmentType(equipmentType);
                filesData.put("formula_info", formulaResult);
                if (!(Boolean) formulaResult.get("success")) {
                    allFilesDownloaded = false;
                    errorMessages.append("Formula 정보 조회 실패; ");
                }
            }
            
            // // Model 파일 정보조회  
            // String modelFileId = (String) fileParams.get("model_file_id");
            // if (modelFileId != null && !modelFileId.trim().isEmpty()) {
            //     Map<String, Object> modelResult = downloadModelFile(equipmentId, modelFileId);
            //     filesData.put("model_file", modelResult);
            //     if (!(Boolean) modelResult.get("success")) {
            //         allFilesDownloaded = false;
            //         errorMessages.append("Model 파일 다운로드 실패; ");
            //     }
            // }
            
            // // RVT 파일 정보조회
            // String rvtFileId = (String) fileParams.get("rvt_file_id");
            // if (rvtFileId != null && !rvtFileId.trim().isEmpty()) {
            //     Map<String, Object> rvtResult = downloadRvtFile(equipmentId, rvtFileId);
            //     filesData.put("rvt_file", rvtResult);
            //     if (!(Boolean) rvtResult.get("success")) {
            //         allFilesDownloaded = false;
            //         errorMessages.append("RVT 파일 다운로드 실패; ");
            //     }
            // }
            
            // // Symbol 파일 정보조회
            // String symbolId = (String) fileParams.get("symbol_id");
            // if (symbolId != null && !symbolId.trim().isEmpty()) {
            //     Map<String, Object> symbolResult = downloadSymbolFile(equipmentId, symbolId);
            //     filesData.put("symbol_file", symbolResult);
            //     if (!(Boolean) symbolResult.get("success")) {
            //         allFilesDownloaded = false;
            //         errorMessages.append("Symbol 파일 다운로드 실패; ");
            //     }
            // }
            
            // // Thumbnail 파일 정보조회
            // String thumbnailId = (String) fileParams.get("thumbnail_id");
            // if (thumbnailId != null && !thumbnailId.trim().isEmpty()) {
            //     Map<String, Object> thumbnailResult = downloadThumbnailFile(equipmentId, thumbnailId);
            //     filesData.put("thumbnail_file", thumbnailResult);
            //     if (!(Boolean) thumbnailResult.get("success")) {
            //         allFilesDownloaded = false;
            //         errorMessages.append("Thumbnail 파일 다운로드 실패; ");
            //     }
            // }
            
            result.put("success", true);
            result.put("data", filesData);
            if (allFilesDownloaded) {
                result.put("message", "모든 파일 다운로드가 완료되었습니다.");
            } else {
                result.put("message", "일부 파일 다운로드에 실패했습니다: " + errorMessages.toString());
            }
            
            logger.debug("기계 상세 파일 다운로드 완료: equipmentId={}, allFilesDownloaded={}", equipmentId, allFilesDownloaded);
            
        } catch (Exception e) {
            logger.error("기계 상세 파일 다운로드 중 예외 발생: equipmentId={}, error={}", equipmentId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "파일 다운로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Equipment Type으로 Formula 정보 조회
     */
    public Map<String, Object> getFormulaByEquipmentType(String equipmentType) {
        logger.debug("Formula 정보 조회 시도: equipmentType={}", equipmentType);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 외부 API URL 구성
            String formulaUrl = authServerBaseUrl + "/api/v1/common/with_formula/equipment/search_history/" + equipmentType;
            logger.debug("Formula 정보 조회 URL: {}", formulaUrl);
            
            // HttpUtil을 사용하여 GET 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.get(formulaUrl, "application/json", "");
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("Formula 정보 조회 성공: statusCode={}", httpResult.getStatus());
                
                result.put("success", true);
                result.put("message", "Formula 정보 조회가 완료되었습니다.");
                result.put("data", parsedResponse);
            } else {
                // 실패한 응답 처리
                logger.error("Formula 정보 조회 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Formula 정보 조회에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("Formula 정보 조회 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Formula 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Model 파일 다운로드
     */
    private Map<String, Object> downloadModelFile(String equipmentId, String modelFileId) {
        logger.debug("Model 파일 다운로드 시도: equipmentId={}, modelFileId={}", equipmentId, modelFileId);
        String downloadUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/download/" + equipmentId + "/model/" + modelFileId;
        return executeFileDownload(downloadUrl, "Model 파일");
    }

    /**
     * RVT 파일 다운로드
     */
    private Map<String, Object> downloadRvtFile(String equipmentId, String rvtFileId) {
        logger.debug("RVT 파일 다운로드 시도: equipmentId={}, rvtFileId={}", equipmentId, rvtFileId);
        String downloadUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/download/" + equipmentId + "/rvt/" + rvtFileId;
        return executeFileDownload(downloadUrl, "RVT 파일");
    }

    /**
     * Symbol 파일 다운로드
     */
    private Map<String, Object> downloadSymbolFile(String equipmentId, String symbolId) {
        logger.debug("Symbol 파일 다운로드 시도: equipmentId={}, symbolId={}", equipmentId, symbolId);
        String downloadUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/download/" + equipmentId + "/symbol/" + symbolId;
        return executeFileDownload(downloadUrl, "Symbol 파일");
    }

    /**
     * Thumbnail 파일 다운로드
     */
    private Map<String, Object> downloadThumbnailFile(String equipmentId, String thumbnailId) {
        logger.debug("Thumbnail 파일 다운로드 시도: equipmentId={}, thumbnailId={}", equipmentId, thumbnailId);
        String downloadUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/download/" + equipmentId + "/thumbnail/" + thumbnailId;
        return executeFileDownload(downloadUrl, "Thumbnail 파일");
    }

    /**
     * 파일 다운로드 공통 메서드
     */
    private Map<String, Object> executeFileDownload(String downloadUrl, String fileType) {
        Map<String, Object> result = new HashMap<>();
        logger.debug("{} 다운로드 URL: {}", fileType, downloadUrl);
        
        try {
            HttpUtil.HttpResult httpResult = HttpUtil.get(downloadUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                logger.debug("{} 다운로드 성공: statusCode={}", fileType, httpResult.getStatus());
                result.put("success", true);
                result.put("message", fileType + " 다운로드가 완료되었습니다.");
                
                String responseBody = httpResult.getBody();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("data", parsedResponse);
                    } else {
                        result.put("data", responseBody);
                    }
                }
            } else {
                logger.error("{} 다운로드 실패: statusCode={}, body={}", fileType, httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = fileType + " 다운로드에 실패했습니다.";
                }
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
            }
        } catch (Exception e) {
            logger.error("{} 다운로드 중 예외 발생: {}", fileType, e.getMessage(), e);
            result.put("success", false);
            result.put("message", fileType + " 다운로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 기계 정보 업데이트 - 외부 API 호출 (PATCH) 및 파일 업로드
     * @param equipmentId 장비 ID
     * @param updateParamsJson 업데이트 파라미터 (JSON 문자열)
     * @param formulaFile Formula 파일 (Model로 업로드)
     * @param dtdModelFile DTD Model 파일 (Symbol로 업로드)
     * @param thumbnailFile Thumbnail 파일
     * @param revitModelFile Revit Model 파일 (RVT로 업로드)
     * @return 업데이트 결과
     */
    public Map<String, Object> updateMachine(String equipmentId, String updateParamsJson,
                                            MultipartFile dtdModelFile, MultipartFile thumbnailFile,
                                            MultipartFile revitModelFile, MultipartFile symbolFile) {
        logger.debug("기계 업데이트 처리 시작: equipmentId={}", equipmentId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 메인 장비 정보 업데이트 API 호출 (PATCH)
            Map<String, Object> updateResult = updateEquipmentFromAPI(equipmentId, updateParamsJson);
            
            if ((Boolean) updateResult.get("success")) {
                // 2. 장비 정보 업데이트 성공 시 파일들 순차적 업로드
                boolean allFilesUploaded = true;
                StringBuilder errorMessages = new StringBuilder();
                Map<String, Object> uploadResults = new HashMap<>();
                
                // DTD Model 파일 업로드 (dtdModelFile)
                if (dtdModelFile != null && !dtdModelFile.isEmpty()) {
                    Map<String, Object> modelResult = updateModelFile(equipmentId, dtdModelFile);
                    uploadResults.put("model_file", modelResult);
                    if (!(Boolean) modelResult.get("success")) {
                        allFilesUploaded = false;
                        errorMessages.append("Model 파일 업로드 실패; ");
                    }
                }

                // Thumbnail 파일 업로드 (thumbnailFile)
                if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                    Map<String, Object> thumbResult = updateThumbnailFile(equipmentId, thumbnailFile);
                    uploadResults.put("thumbnail_file", thumbResult);
                    if (!(Boolean) thumbResult.get("success")) {
                        allFilesUploaded = false;
                        errorMessages.append("Thumbnail 파일 업로드 실패; ");
                    }
                }
                
                // RVT 파일 업로드 (revitModelFile)
                // if (revitModelFile != null && !revitModelFile.isEmpty()) {
                //     Map<String, Object> rvtResult = updateRvtFile(equipmentId, revitModelFile);
                //     uploadResults.put("rvt_file", rvtResult);
                //     if (!(Boolean) rvtResult.get("success")) {
                //         allFilesUploaded = false;
                //         errorMessages.append("RVT 파일 업로드 실패; ");
                //     }
                // }

                // RFA 파일 업로드 (revitModelFile)
                Map<String, Object> updateParams = JsonUtil.parseJson(updateParamsJson);
                String equipmentType = updateParams != null ? (String) updateParams.get("equipment_type") : null;
                if (revitModelFile != null && !revitModelFile.isEmpty() && equipmentType != null && !equipmentType.trim().isEmpty()) {
                    Map<String, Object> rfaResult = updateRfaFile(equipmentType, revitModelFile);
                    uploadResults.put("rfa_file", rfaResult);
                    if (!(Boolean) rfaResult.get("success")) {
                        allFilesUploaded = false;
                        errorMessages.append("RFA 파일 업로드 실패; ");
                    }
                }
                
                // Symbol 파일 업로드 (symbolFile)
                if (symbolFile != null && !symbolFile.isEmpty()) {
                    Map<String, Object> symbolResult = updateSymbolFile(equipmentId, symbolFile);
                    uploadResults.put("symbol_file", symbolResult);
                    if (!(Boolean) symbolResult.get("success")) {
                        allFilesUploaded = false;
                        errorMessages.append("Symbol 파일 업로드 실패; ");
                    }
                }
                
                result.put("success", true);
                result.put("equipment_update", updateResult.get("data"));
                result.put("file_uploads", uploadResults);
                
                if (allFilesUploaded) {
                    result.put("message", "기계 정보와 모든 파일이 성공적으로 업데이트되었습니다.");
                } else {
                    result.put("message", "기계 정보는 업데이트되었지만 일부 파일 업로드에 실패했습니다: " + errorMessages.toString());
                }
                
                logger.debug("기계 업데이트 완료: equipmentId={}, allFilesUploaded={}", equipmentId, allFilesUploaded);
            } else {
                // 장비 정보 업데이트 실패
                result = updateResult;
                logger.error("기계 정보 업데이트 실패: equipmentId={}, message={}", equipmentId, updateResult.get("message"));
            }
            
        } catch (Exception e) {
            logger.error("기계 업데이트 처리 중 예외 발생: equipmentId={}, error={}", equipmentId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "기계 업데이트 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 장비 정보 업데이트 API 호출 - 실제 PATCH 요청
     */
    private Map<String, Object> updateEquipmentFromAPI(String equipmentId, String updateParamsJson) {
        logger.debug("외부 인증 서버 기계 정보 업데이트 시도: server={}, equipmentId={}", authServerBaseUrl, equipmentId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // JSON 파싱하여 파라미터 추출
            Map<String, Object> updateParams = JsonUtil.parseJson(updateParamsJson);
            
            if (updateParams == null) {
                result.put("success", false);
                result.put("message", "업데이트 파라미터 파싱에 실패했습니다.");
                return result;
            }
            
            // equipment_code 생성: equipment_type + "_" + vendor_id + "_" + model_number
            String equipmentType = (String) updateParams.get("equipment_type");
            String vendorId = (String) updateParams.get("vendor_id");
            String modelNumber = (String) updateParams.get("model_number");
            
            if (equipmentType != null && vendorId != null && modelNumber != null) {
                String equipmentCode = equipmentType + "_" + vendorId + "_" + modelNumber;
                //updateParams.put("equipment_code", equipmentCode); // equipment_code는 수정 안하기로하였음
                logger.debug("equipment_code 생성: {}", equipmentCode);
            }
            
            // 수정된 파라미터를 JSON으로 변환
            String requestBody = JsonUtil.objectMapToJson(updateParams);
            logger.debug("기계 정보 업데이트 요청 본문: {}", requestBody);
            
            // 외부 인증 서버 URL 구성
            String updateUrl = authServerBaseUrl + "/api/v1/equipment/equipment_catalog/" + equipmentId;
            logger.debug("기계 정보 업데이트 URL: {}", updateUrl);
            
            // HttpUtil을 사용하여 PATCH 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.patch(updateUrl, "application/json", requestBody);
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("기계 정보 업데이트 성공: statusCode={}", httpResult.getStatus());
                
                result.put("success", true);
                result.put("message", "기계 정보 업데이트가 완료되었습니다.");
                result.put("data", parsedResponse);
            } else {
                // 실패한 응답 처리
                logger.error("기계 정보 업데이트 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "기계 정보 업데이트에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("기계 정보 업데이트 처리 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "기계 정보 업데이트 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Model 파일 업데이트 (PATCH multipart)
     */
    private Map<String, Object> updateModelFile(String equipmentId, MultipartFile modelFile) {
        logger.debug("Model 파일 업데이트 시도: equipmentId={}", equipmentId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/patchzip/" + equipmentId + "/model";
        return executeFileUpload(updateUrl, modelFile, "file", "Model 파일");
    }

    /**
     * RVT 파일 업데이트 (PATCH multipart)
     */
    private Map<String, Object> updateRvtFile(String equipmentId, MultipartFile rvtFile) {
        logger.debug("RVT 파일 업데이트 시도: equipmentId={}", equipmentId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/patchzip/" + equipmentId + "/rvt";
        return executeFileUpload(updateUrl, rvtFile, "file", "RVT 파일");
    }

    /**
     * RFA 파일 업데이트 (PATCH multipart)
     */
    private Map<String, Object> updateRfaFile(String equipmentType, MultipartFile rfaFile) {
        logger.debug("RFA 파일 업데이트 시도: equipmentType={}", equipmentType);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/patchzip/" + equipmentType + "/rfa/";
        return executeFileUpload(updateUrl, rfaFile, "file", "RFA 파일");
    }

    /**
     * Symbol 파일 업데이트 (PATCH multipart)
     */
    private Map<String, Object> updateSymbolFile(String equipmentId, MultipartFile symbolFile) {
        logger.debug("Symbol 파일 업데이트 시도: equipmentId={}", equipmentId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/patchzip/" + equipmentId + "/symbol";
        return executeFileUpload(updateUrl, symbolFile, "file", "Symbol 파일");
    }

    /**
     * Thumbnail 파일 업데이트 (PATCH multipart)
     */
    private Map<String, Object> updateThumbnailFile(String equipmentId, MultipartFile thumbnailFile) {
        logger.debug("Thumbnail 파일 업데이트 시도: equipmentId={}", equipmentId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/equipment_catalog/patchzip/" + equipmentId + "/thumbnail";
        return executeFileUpload(updateUrl, thumbnailFile, "file", "Thumbnail 파일");
    }

    /**
     * 파일 업로드 공통 메서드 (PATCH multipart)
     */
    private Map<String, Object> executeFileUpload(String uploadUrl, MultipartFile file, String paramName, String fileType) {
        Map<String, Object> result = new HashMap<>();
        logger.debug("{} 업로드 URL: {}", fileType, uploadUrl);
        
        try {
            Map<String, Object> formData = new HashMap<>();
            formData.put(paramName, file);
            
            HttpUtil.HttpResult httpResult = HttpUtil.patchMultipart(uploadUrl, formData);
            
            if (httpResult.isSuccess()) {
                logger.debug("{} 업로드 성공: statusCode={}", fileType, httpResult.getStatus());
                result.put("success", true);
                result.put("message", fileType + " 업로드가 완료되었습니다.");
                
                String responseBody = httpResult.getBody();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("data", parsedResponse);
                    }
                }
            } else {
                logger.error("{} 업로드 실패: statusCode={}, body={}", fileType, httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = fileType + " 업로드에 실패했습니다.";
                }
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
            }
        } catch (Exception e) {
            logger.error("{} 업로드 중 예외 발생: {}", fileType, e.getMessage(), e);
            result.put("success", false);
            result.put("message", fileType + " 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Formula 파일 생성 (업로드)
     * @param pythonFile Python 파일
     * @param formulaId Formula ID (선택)
     * @return 업로드 결과
     */
    public Map<String, Object> createMachineFormula(MultipartFile pythonFile, String formulaId, String equipmentType) {
        logger.debug("Formula 파일 생성 시도: formulaId={}, equipmentType={}", formulaId, equipmentType);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 파일 검증
            if (pythonFile == null || pythonFile.isEmpty()) {
                result.put("success", false);
                result.put("message", "Python 파일이 필요합니다.");
                return result;
            }
            
            // 외부 API URL 구성
            String uploadUrl = authServerBaseUrl + "/api/v1/minio/formula/equipment/upload";
            logger.debug("Formula 파일 업로드 URL: {}", uploadUrl);
            
            // Multipart 데이터 구성
            Map<String, Object> formData = new HashMap<>();
            formData.put("python_file", pythonFile);
            if (formulaId != null && !formulaId.trim().isEmpty()) {
                formData.put("formula_id", formulaId);
            }
            if (equipmentType != null && !equipmentType.trim().isEmpty()) {
                formData.put("equipment_type", equipmentType);
            }
            formData.put("version_increment", "PATCH");
            
            // HttpUtil을 사용하여 POST Multipart 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(uploadUrl, formData);
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("Formula 파일 업로드 성공: statusCode={}", httpResult.getStatus());
                
                result.put("success", true);
                result.put("message", "Formula 파일 업로드가 완료되었습니다.");
                result.put("data", parsedResponse);
            } else {
                // 실패한 응답 처리
                logger.error("Formula 파일 업로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Formula 파일 업로드에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("Formula 파일 업로드 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Formula 파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Formula 삭제
     * @param formulaId Formula ID
     * @return 삭제 결과
     */
    public Map<String, Object> deleteMachineFormula(String formulaId) {
        logger.debug("Formula 삭제 시도: formulaId={}", formulaId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (formulaId == null || formulaId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Formula ID가 필요합니다.");
                return result;
            }
            
            // 외부 API URL 구성
            String deleteUrl = authServerBaseUrl + "/api/v1/minio/formula/" + formulaId;
            logger.debug("Formula 삭제 URL: {}", deleteUrl);
            
            // HttpUtil을 사용하여 DELETE 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                logger.debug("Formula 삭제 성공: statusCode={}, responseBody={}", httpResult.getStatus(), responseBody);
                
                result.put("success", true);
                result.put("message", "Formula 삭제가 완료되었습니다.");
                
                // 응답 본문이 있는 경우에만 파싱
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("data", parsedResponse);
                    }
                }
            } else {
                // 실패한 응답 처리
                logger.error("Formula 삭제 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Formula 삭제에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("Formula 삭제 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Formula 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 장비 가격 이력 등록
     * @param priceParams 가격 정보 파라미터
     * @return 등록 결과
     */
    public Map<String, Object> createEquipmentPriceHistory(Map<String, Object> priceParams) {
        logger.debug("장비 가격 이력 등록 시도: params={}", priceParams);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 외부 API URL 구성
            String priceHistoryUrl = authServerBaseUrl + "/api/v1/equipment/equipment_price_history/";
            logger.debug("장비 가격 이력 등록 URL: {}", priceHistoryUrl);
            
            // 요청 본문 구성
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("equipment_id", priceParams.get("equipment_id"));
            requestMap.put("equipment_code", priceParams.get("equipment_code"));
            requestMap.put("price_type", priceParams.get("price_type"));
            requestMap.put("price_unit_code", priceParams.get("price_unit_code"));
            requestMap.put("price_unit_symbol", priceParams.get("price_unit_symbol"));
            requestMap.put("price_value", priceParams.get("price_value"));
            requestMap.put("price_date", LocalDate.now());
            requestMap.put("price_reference", priceParams.get("price_reference"));
            
            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("장비 가격 이력 등록 요청 본문: {}", requestBody);
            
            // HttpUtil을 사용하여 POST 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.post(priceHistoryUrl, "application/json", requestBody);
            
            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                logger.debug("장비 가격 이력 등록 성공: statusCode={}, responseBody={}", httpResult.getStatus(), responseBody);
                
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.equipmentPriceHistoryCreated");
                
                // 응답 본문이 있는 경우에만 파싱
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("response", parsedResponse);
                    }
                }
            } else {
                // 실패한 응답 처리
                logger.error("장비 가격 이력 등록 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                result.put("statusCode", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.equipmentPriceHistoryCreateFailed";
                }
                
                result.put("message", errorMessage);
                if (parsedResponse != null) {
                    result.put("response", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("장비 가격 이력 등록 중 예외 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "messages.error.equipmentPriceHistoryCreateFailed: " + e.getMessage());
        }
        
        return result;
    }
}