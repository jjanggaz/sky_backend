package com.wai.admin.service.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.CommonCodeUtil;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class Asset3DService {

    private static final Logger logger = LoggerFactory.getLogger(Asset3DService.class);

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
            Map<String, Object> SecondDepthResult = commonCodeUtil.getCommonCodesByCategory("parent_key", parentValue,
                    true);

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
     * 공통 코드 트리 조회 - /api/v1/common/common_codes/custom/tree 호출
     *
     * @param searchParams 트리 조회에 사용할 조건
     * @return 외부 API 응답을 포함한 결과
     */
    public Map<String, Object> getCodeTree(Map<String, Object> searchParams) {
        logger.debug("공통 코드 트리 조회 시도: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> requestMap = (searchParams != null) ? new HashMap<>(searchParams) : new HashMap<>();

        try {
            String codeTreeUrl = authServerBaseUrl + "/api/v1/common/common_codes/custom/tree";
            if (!requestMap.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder("?");
                for (Map.Entry<String, Object> entry : requestMap.entrySet()) {
                    Object value = entry.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (queryBuilder.length() > 1) {
                        queryBuilder.append("&");
                    }
                    queryBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                    queryBuilder.append("=");
                    queryBuilder.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
                }
                if (queryBuilder.length() > 1) {
                    codeTreeUrl += queryBuilder;
                }
            }
            logger.debug("공통 코드 트리 조회 URL: {}", codeTreeUrl);

            HttpUtil.HttpResult httpResult = HttpUtil.get(codeTreeUrl, "application/json", "");

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                if (parsedResponse != null) {
                    logger.debug("공통 코드 트리 조회 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                    result.put("success", true);
                    result.put("data", parsedResponse);
                    result.put("message", "공통 코드 트리 조회가 완료되었습니다.");
                } else {
                    logger.error("공통 코드 트리 조회 응답 파싱 실패");
                    result.put("success", false);
                    result.put("message", "응답 파싱에 실패했습니다.");
                }
            } else {
                logger.error("공통 코드 트리 조회 실패: statusCode={}, body={}", httpResult.getStatus(),
                        httpResult.getBody());
                result.put("success", false);

                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "공통 코드 트리 조회에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } catch (Exception e) {
            logger.error("공통 코드 트리 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("statusCode", 500);
            result.put("message", "공통 코드 트리 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 프리셋 마스터 목록 조회 - 외부 API 호출
     *
     * @param searchParams 검색 조건
     * @return 프리셋 목록 결과
     */
    public Map<String, Object> getPresetList(Map<String, Object> searchParams) {
        logger.debug("프리셋 마스터 목록 조회 시도: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String presetUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/search/enhanced";
        logger.debug("프리셋 마스터 목록 조회 URL: {}", presetUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 분기처리
        Object keyword = searchParams.get("keyword");
        if (keyword != null && !keyword.toString().trim().isEmpty()) {
            requestMap.put("keyword", keyword);
        } else {
            requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        }

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 마스터 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(presetUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                logger.debug("프리셋 마스터 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("프리셋 마스터 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.presetListSuccess");
            result.put("response", parsedResponse);
        } else {
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.presetListFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("프리셋 마스터 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 3D 라이브러리 목록 조회 - 외부 API 호출
     *
     * @param searchParams 검색 조건
     * @return 라이브러리 목록 결과
     */
    public Map<String, Object> getLibraryList(Map<String, Object> searchParams) {
        logger.debug("3D 라이브러리 목록 조회 시도: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String libraryUrl = authServerBaseUrl + "/api/v1/equipment/3d_library/search/enhanced";
        logger.debug("3D 라이브러리 목록 조회 URL: {}", libraryUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 분기처리
        Object keyword = searchParams.get("keyword");
        if (keyword != null && !keyword.toString().trim().isEmpty()) {
            requestMap.put("keyword", keyword);
        } else {
            requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        }

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("3D 라이브러리 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(libraryUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                logger.debug("3D 라이브러리 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("3D 라이브러리 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.libraryListSuccess");
            result.put("response", parsedResponse);
        } else {
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.libraryListFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("3D 라이브러리 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 3D 라이브러리 수정 - 외부 API 호출
     *
     * @param libraryId   라이브러리 ID
     * @param libraryData 라이브러리 데이터
     * @return 수정 결과
     */
    public Map<String, Object> updateLibrary(String libraryId, Map<String, Object> libraryData) {
        Map<String, Object> result = new HashMap<>();

        if (libraryId == null || libraryId.isEmpty()) {
            logger.warn("3D 라이브러리 수정 실패: 라이브러리 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidLibraryId");
            return result;
        }

        if (libraryData == null) {
            logger.warn("3D 라이브러리 수정 실패: 라이브러리 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidLibraryData");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/3d_library/" + libraryId;
        logger.debug("3D 라이브러리 수정 URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // category
        Object category = libraryData.get("category");
        if (category != null) {
            requestMap.put("category", category);
        }

        // category_en
        Object categoryEn = libraryData.get("category_en");
        if (categoryEn != null) {
            requestMap.put("category_en", categoryEn);
        }

        // model_code
        Object modelCode = libraryData.get("model_code");
        if (modelCode != null) {
            requestMap.put("model_code", modelCode);
        }

        // model_name
        Object modelName = libraryData.get("model_name");
        if (modelName != null) {
            requestMap.put("model_name", modelName);
        }

        // model_name_en
        Object modelNameEn = libraryData.get("model_name_en");
        if (modelNameEn != null) {
            requestMap.put("model_name_en", modelNameEn);
        }

        // unit_system_code (기본값: METRIC)
        String unitSystemCode = (String) libraryData.getOrDefault("unit_system_code", "METRIC");
        requestMap.put("unit_system_code", unitSystemCode);

        // dtdx_model_id
        Object dtdxModelId = libraryData.get("dtdx_model_id");
        if (dtdxModelId != null) {
            requestMap.put("dtdx_model_id", dtdxModelId);
        }

        // thumbnail_id 설정: libraryData에서 직접 가져옴
        Object thumbnailId = libraryData.get("thumbnail_id");
        if (thumbnailId != null) {
            requestMap.put("thumbnail_id", thumbnailId);
            logger.debug("thumbnail_id 사용: {}", thumbnailId);
        }

        // remarks
        Object remarks = libraryData.get("remarks");
        if (remarks != null) {
            requestMap.put("remarks", remarks);
        }

        // metadata
        Object metadata = libraryData.get("metadata");
        if (metadata != null) {
            requestMap.put("metadata", metadata);
        }

        // is_active
        Object isActive = libraryData.get("is_active");
        if (isActive != null) {
            requestMap.put("is_active", isActive);
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("3D 라이브러리 수정 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답코드가 200 또는 204인 경우 성공으로 처리 (PATCH 요청의 표준 응답 코드)
            if (httpResult.getStatus() == 200 || httpResult.getStatus() == 204) {
                logger.debug("3D 라이브러리 수정 성공: library_id={}", libraryId);
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.libraryUpdateSuccess");
                result.put("response", parsedResponse);
            } else {
                // 기타 응답 코드는 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.libraryUpdateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("3D 라이브러리 수정 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
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
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.libraryUpdateFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 3D 라이브러리 수정 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 프리셋 마스터 생성 - 외부 API 호출
     *
     * @param presetData 프리셋 데이터
     * @param siteFile   썸네일 파일 (선택)
     * @return 생성 결과
     */
    public Map<String, Object> createPresetMaster(Map<String, Object> presetData,
            org.springframework.web.multipart.MultipartFile siteFile) {
        Map<String, Object> result = new HashMap<>();

        if (presetData == null) {
            logger.warn("프리셋 등록 실패: 프리셋 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetData");
            return result;
        }

        // siteFile이 있는 경우 썸네일 업로드를 위해 보관 (프리셋 생성 후 업로드)
        org.springframework.web.multipart.MultipartFile thumbnailFile = null;
        if (siteFile != null && !siteFile.isEmpty()) {
            thumbnailFile = siteFile;
            logger.debug("썸네일 파일이 제공됨, 프리셋 생성 후 업로드 예정: fileName={}, size={}",
                    siteFile.getOriginalFilename(), siteFile.getSize());
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/";

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // root_equipment_type
        Object rootEquipmentType = presetData.get("root_equipment_type");
        if (rootEquipmentType != null) {
            requestMap.put("root_equipment_type", rootEquipmentType);
        }

        // equipment_type
        Object equipmentType = presetData.get("equipment_type");
        if (equipmentType != null) {
            requestMap.put("equipment_type", equipmentType);
        }

        // preset_category
        Object presetCategory = presetData.get("preset_category");
        if (presetCategory != null) {
            requestMap.put("preset_category", presetCategory);
        }

        // total_unit_count
        Object totalUnitCount = presetData.get("total_unit_count");
        if (totalUnitCount != null) {
            requestMap.put("total_unit_count", totalUnitCount);
        }

        // preset_name_ko
        Object presetNameKo = presetData.get("preset_name_ko");
        if (presetNameKo != null) {
            requestMap.put("preset_name_ko", presetNameKo);
        }

        // preset_name_en
        Object presetNameEn = presetData.get("preset_name_en");
        if (presetNameEn != null) {
            requestMap.put("preset_name_en", presetNameEn);
        }

        // unit_system_code (기본값: METRIC)
        String unitSystemCode = (String) presetData.getOrDefault("unit_system_code", "METRIC");
        requestMap.put("unit_system_code", unitSystemCode);

        // diameter_value
        Object diameterValue = presetData.get("diameter_value");
        if (diameterValue != null) {
            requestMap.put("diameter_value", diameterValue);
        }

        // diameter_unit
        Object diameterUnit = presetData.get("diameter_unit");
        if (diameterUnit != null) {
            requestMap.put("diameter_unit", diameterUnit);
        }

        // set_dtdx_file_id
        Object setDtdxFileId = presetData.get("set_dtdx_file_id");
        if (setDtdxFileId != null) {
            requestMap.put("set_dtdx_file_id", setDtdxFileId);
        }

        // thumbnail_id 설정: presetData에서 직접 가져옴 (썸네일은 프리셋 생성 후 업로드)
        Object thumbnailId = presetData.get("thumbnail_id");
        if (thumbnailId != null) {
            requestMap.put("thumbnail_id", thumbnailId);
            logger.debug("thumbnail_id 사용: {}", thumbnailId);
        }

        // note
        Object note = presetData.get("note");
        if (note != null) {
            requestMap.put("note", note);
        }

        // metadata
        Object metadata = presetData.get("metadata");
        if (metadata != null) {
            requestMap.put("metadata", metadata);
        }

        // is_active
        Object isActive = presetData.get("is_active");
        if (isActive != null) {
            requestMap.put("is_active", isActive);
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 마스터 생성 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답코드가 201인 경우에도 응답 내용 검증
            if (httpResult.getStatus() == 201) {
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.presetCreateSuccess");
                result.put("response", parsedResponse);

                // preset_id 추출하여 result에 추가
                String presetId = JsonUtil.extractValue(responseBody, "preset_id");
                if (presetId == null || presetId.isEmpty()) {
                    presetId = JsonUtil.extractValue(responseBody, "id");
                }
                if (presetId != null && !presetId.isEmpty()) {
                    result.put("preset_id", presetId);
                    logger.debug("프리셋 마스터 생성 성공, preset_id: {}", presetId);

                    // 프리셋 생성 성공 후 썸네일 파일이 있으면 업로드
                    if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                        logger.debug("썸네일 파일 업로드 시작: presetId={}", presetId);
                        Map<String, Object> thumbnailResult = uploadPresetThumbnail(thumbnailFile, presetId);
                        if ((Boolean) thumbnailResult.get("success")) {
                            String uploadedThumbnailId = (String) thumbnailResult.get("symbol_id");
                            logger.debug("썸네일 업로드 성공, thumbnail_id: {}", uploadedThumbnailId);
                            result.put("thumbnail_id", uploadedThumbnailId);
                        } else {
                            logger.warn("썸네일 업로드 실패: {}", thumbnailResult.get("message"));
                            // 썸네일 업로드 실패해도 프리셋은 생성되었으므로 경고만 추가
                            result.put("thumbnail_upload_failed", true);
                            result.put("thumbnail_error_message", thumbnailResult.get("message"));
                        }
                    }
                }
            } else {
                // 응답코드가 201이 아닌 경우 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.presetCreateFail";
                }

                // 중복 키 오류 감지
                if (errorMessage != null && errorMessage.contains("duplicate key value violates unique constraint")) {
                    errorMessage = "중복된 프리셋 명 입니다";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("errorMessage", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프리셋 마스터 등록 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                return result;
            }
        } else {
            // 에러 응답 처리
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();

            result.put("success", false);
            result.put("status", statusCode);

            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "프리셋 마스터 등록에 실패했습니다.";
            }

            // 중복 키 오류 감지
            if (errorMessage != null && errorMessage.contains("duplicate key value violates unique constraint")) {
                errorMessage = "중복된 프리셋 명 입니다";
            }

            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            // logger.error("프리셋 마스터 등록 API 실패 상세 정보:");
            // logger.error("- API URL: {}", apiUrl);
            // logger.error("- HTTP Status: {}", statusCode);
            // logger.error("- Request Body: {}", requestBody);
            // logger.error("- Response Body: {}", responseBody);
            // logger.error("- Error Message: {}", errorMessage);
        }

        return result;
    }

    /**
     * 3D 라이브러리 생성 - 외부 API 호출
     *
     * @param libraryData   라이브러리 데이터
     * @param modelFile     Model 파일 (선택)
     * @param thumbnailFile 썸네일 파일 (선택)
     * @return 생성 결과
     */
    public Map<String, Object> createLibrary(Map<String, Object> libraryData,
            org.springframework.web.multipart.MultipartFile modelFile,
            org.springframework.web.multipart.MultipartFile thumbnailFile) {
        Map<String, Object> result = new HashMap<>();

        if (libraryData == null) {
            logger.warn("3D 라이브러리 등록 실패: 라이브러리 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidLibraryData");
            return result;
        }

        // modelFile과 thumbnailFile이 있는 경우 업로드를 위해 보관 (라이브러리 생성 후 업로드)
        org.springframework.web.multipart.MultipartFile modelFileToUpload = null;
        org.springframework.web.multipart.MultipartFile thumbFile = null;
        if (modelFile != null && !modelFile.isEmpty()) {
            modelFileToUpload = modelFile;
            logger.debug("Model 파일이 제공됨, 라이브러리 생성 후 업로드 예정: fileName={}, size={}",
                    modelFile.getOriginalFilename(), modelFile.getSize());
        }
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbFile = thumbnailFile;
            logger.debug("썸네일 파일이 제공됨, 라이브러리 생성 후 업로드 예정: fileName={}, size={}",
                    thumbnailFile.getOriginalFilename(), thumbnailFile.getSize());
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/3d_library/";

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // category
        Object category = libraryData.get("category");
        if (category != null) {
            requestMap.put("category", category);
        }

        // category_en
        Object categoryEn = libraryData.get("category_en");
        if (categoryEn != null) {
            requestMap.put("category_en", categoryEn);
        }

        // model_code
        Object modelCode = libraryData.get("model_code");
        if (modelCode != null) {
            requestMap.put("model_code", modelCode);
        }

        // model_name
        Object modelName = libraryData.get("model_name");
        if (modelName != null) {
            requestMap.put("model_name", modelName);
        }

        // model_name_en
        Object modelNameEn = libraryData.get("model_name_en");
        if (modelNameEn != null) {
            requestMap.put("model_name_en", modelNameEn);
        }

        // unit_system_code (기본값: METRIC)
        String unitSystemCode = (String) libraryData.getOrDefault("unit_system_code", "METRIC");
        requestMap.put("unit_system_code", unitSystemCode);

        // dtdx_model_id
        Object dtdxModelId = libraryData.get("dtdx_model_id");
        if (dtdxModelId != null) {
            requestMap.put("dtdx_model_id", dtdxModelId);
        }

        // thumbnail_id
        Object thumbnailId = libraryData.get("thumbnail_id");
        if (thumbnailId != null) {
            requestMap.put("thumbnail_id", thumbnailId);
        }

        // remarks
        Object remarks = libraryData.get("remarks");
        if (remarks != null) {
            requestMap.put("remarks", remarks);
        }

        // metadata
        Object metadata = libraryData.get("metadata");
        if (metadata != null) {
            requestMap.put("metadata", metadata);
        }

        // is_active
        Object isActive = libraryData.get("is_active");
        if (isActive != null) {
            requestMap.put("is_active", isActive);
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("3D 라이브러리 생성 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답코드가 201인 경우에도 응답 내용 검증
            if (httpResult.getStatus() == 201) {
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.libraryCreateSuccess");
                result.put("response", parsedResponse);

                // library_id 추출하여 result에 추가
                String libraryId = JsonUtil.extractValue(responseBody, "library_id");
                if (libraryId == null || libraryId.isEmpty()) {
                    libraryId = JsonUtil.extractValue(responseBody, "id");
                }
                if (libraryId != null && !libraryId.isEmpty()) {
                    result.put("library_id", libraryId);
                    logger.debug("3D 라이브러리 생성 성공, library_id: {}", libraryId);

                    // 라이브러리 생성 성공 후 Model 파일이 있으면 업로드
                    if (modelFileToUpload != null && !modelFileToUpload.isEmpty()) {
                        logger.debug("Model 파일 업로드 시작: libraryId={}", libraryId);
                        Map<String, Object> modelResult = uploadLibraryModel(modelFileToUpload, libraryId);
                        if ((Boolean) modelResult.get("success")) {
                            String uploadedModelFileId = (String) modelResult.get("model_file_id");
                            logger.debug("Model 파일 업로드 성공, model_file_id: {}", uploadedModelFileId);
                            result.put("model_file_id", uploadedModelFileId);
                        } else {
                            logger.warn("Model 파일 업로드 실패: {}", modelResult.get("message"));
                            // Model 파일 업로드 실패해도 라이브러리는 생성되었으므로 경고만 추가
                            result.put("model_upload_failed", true);
                            result.put("model_error_message", modelResult.get("message"));
                        }
                    }

                    // 라이브러리 생성 성공 후 썸네일 파일이 있으면 업로드
                    if (thumbFile != null && !thumbFile.isEmpty()) {
                        logger.debug("썸네일 파일 업로드 시작: libraryId={}", libraryId);
                        Map<String, Object> thumbnailResult = uploadLibraryThumbnail(thumbFile, libraryId);
                        if ((Boolean) thumbnailResult.get("success")) {
                            String uploadedThumbnailId = (String) thumbnailResult.get("symbol_id");
                            logger.debug("썸네일 업로드 성공, thumbnail_id: {}", uploadedThumbnailId);
                            result.put("thumbnail_id", uploadedThumbnailId);
                        } else {
                            logger.warn("썸네일 업로드 실패: {}", thumbnailResult.get("message"));
                            // 썸네일 업로드 실패해도 라이브러리는 생성되었으므로 경고만 추가
                            result.put("thumbnail_upload_failed", true);
                            result.put("thumbnail_error_message", thumbnailResult.get("message"));
                        }
                    }
                }
            } else {
                // 응답코드가 201이 아닌 경우 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.libraryCreateFail";
                }

                // 중복 키 오류 감지
                if (errorMessage != null && errorMessage.contains("duplicate key value violates unique constraint")) {
                    errorMessage = "중복된 3D 모델명이 존재합니다";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("errorMessage", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("3D 라이브러리 등록 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                return result;
            }
        } else {
            // 에러 응답 처리
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();

            result.put("success", false);
            result.put("status", statusCode);

            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "3D 라이브러리 등록에 실패했습니다.";
            }

            // 중복 키 오류 감지
            if (errorMessage != null && errorMessage.contains("duplicate key value violates unique constraint")) {
                errorMessage = "중복된 3D 모델명이 존재합니다";
            }

            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            logger.error("3D 라이브러리 등록 API 실패 상세 정보:");
            logger.error("- API URL: {}", apiUrl);
            logger.error("- HTTP Status: {}", statusCode);
            logger.error("- Request Body: {}", requestBody);
            logger.error("- Response Body: {}", responseBody);
            logger.error("- Error Message: {}", errorMessage);
        }

        return result;
    }

    /**
     * 프리셋 상세 생성 - 외부 API 호출
     *
     * @param presetId   프리셋 ID
     * @param detailData 프리셋 상세 데이터
     * @return 생성 결과
     */
    public Map<String, Object> createPresetDetail(String presetId, Map<String, Object> detailData) {
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 상세 생성 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        if (detailData == null || detailData.isEmpty()) {
            logger.warn("프리셋 상세 생성 실패: 상세 데이터가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetDetailData");
            return result;
        }

        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/details/";
        logger.debug("프리셋 상세 생성 URL: {}", apiUrl);

        // 요청 본문 구성 - 전달된 상세 데이터 사용 (null 값 제거)
        Map<String, Object> requestMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : detailData.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                requestMap.put(entry.getKey(), value);
            }
        }

        if (requestMap.isEmpty()) {
            logger.warn("프리셋 상세 생성 실패: 요청에 포함할 값이 없음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetDetailData");
            return result;
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 상세 생성 요청 본문: {}", requestBody);

        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (httpResult.getStatus() == 201 || httpResult.getStatus() == 200) {
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "프리셋 상세 생성에 성공했습니다.");
                result.put("response", parsedResponse);

                String detailId = JsonUtil.extractValue(responseBody, "detail_id");
                if (detailId == null || detailId.isEmpty()) {
                    detailId = JsonUtil.extractValue(responseBody, "id");
                }
                if (detailId != null && !detailId.isEmpty()) {
                    result.put("detail_id", detailId);
                    logger.debug("프리셋 상세 생성 성공, detail_id={}", detailId);
                }
            } else {
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.presetDetailCreateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프리셋 상세 생성 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
            }
        } else {
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "프리셋 상세 생성에 실패했습니다.";
            }

            result.put("success", false);
            result.put("status", statusCode);
            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("프리셋 상세 생성 API 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 프리셋 상세 목록 조회 - 외부 API 호출
     *
     * @param presetId 프리셋 ID
     * @return 프리셋 상세 목록 결과
     */
    public Map<String, Object> getPresetDetail(String presetId) {
        logger.debug("프리셋 상세 목록 조회 시도: presetId={}", presetId);
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 상세 목록 조회 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/details/";
        logger.debug("프리셋 상세 목록 조회 URL: {}", apiUrl);

        try {
            // HttpUtil을 사용하여 GET 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", "");

            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();

                // 응답이 배열 형태이므로 parseJsonToList 사용
                java.util.List<Object> parsedResponse = JsonUtil.parseJsonToList(responseBody);

                if (parsedResponse != null) {
                    logger.debug("프리셋 상세 목록 조회 성공 응답: {} items", parsedResponse.size());
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "messages.success.presetDetailListSuccess");
                    result.put("data", parsedResponse);
                } else {
                    logger.warn("프리셋 상세 목록 조회 응답 파싱 실패: {}", responseBody);
                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "프리셋 상세 목록 조회 응답 파싱에 실패했습니다.");
                    result.put("response", responseBody);
                }
            } else {
                int statusCode = httpResult.getStatus();
                String responseBody = httpResult.getBody();

                result.put("success", false);
                result.put("status", statusCode);

                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프리셋 상세 목록 조회에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프리셋 상세 목록 조회 API 실패: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("프리셋 상세 목록 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프리셋 상세 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 프리셋 상세 삭제 - 외부 API 호출
     *
     * @param presetId 프리셋 ID
     * @param detailId 상세 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deletePresetDetail(String presetId, String detailId) {
        logger.debug("프리셋 상세 삭제 시도: presetId={}, detailId={}", presetId, detailId);
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 상세 삭제 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        if (detailId == null || detailId.trim().isEmpty()) {
            logger.warn("프리셋 상세 삭제 실패: 상세 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDetailId");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/details/" + detailId;
        logger.debug("프리셋 상세 삭제 URL: {}", apiUrl);

        try {
            // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", null);

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("프리셋 상세 삭제 성공: presetId={}, detailId={}", presetId, detailId);
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.presetDetailDeleteSuccess");
                result.put("response", parsedResponse);
            } else {
                // 에러 응답 처리
                int statusCode = httpResult.getStatus();
                String responseBody = httpResult.getBody();

                result.put("success", false);
                result.put("status", statusCode);

                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    // detail.message 우선 확인
                    Object detailObj = parsedResponse.get("detail");
                    if (detailObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                        errorMessage = (String) detailMap.get("message");
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    // detail.error 확인
                    Object detailObj = parsedResponse.get("detail");
                    if (detailObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                        errorMessage = (String) detailMap.get("error");
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프리셋 상세 삭제에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                // 상세한 오류 정보 로깅
                logger.error("프리셋 상세 삭제 API 실패 상세 정보:");
                logger.error("- Preset ID: {}", presetId);
                logger.error("- Detail ID: {}", detailId);
                logger.error("- API URL: {}", apiUrl);
                logger.error("- HTTP Status: {}", statusCode);
                logger.error("- Response Body: {}", responseBody);
                logger.error("- Error Message: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("프리셋 상세 삭제 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프리셋 상세 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 프리셋 상세 수정 - 외부 API 호출
     *
     * @param presetId   프리셋 ID
     * @param detailId   상세 ID
     * @param detailData 프리셋 상세 데이터
     * @return 수정 결과
     */
    public Map<String, Object> updatePresetDetail(String presetId, String detailId, Map<String, Object> detailData) {
        logger.debug("프리셋 상세 수정 시도: presetId={}, detailId={}, detailData={}", presetId, detailId, detailData);
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 상세 수정 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        if (detailId == null || detailId.trim().isEmpty()) {
            logger.warn("프리셋 상세 수정 실패: 상세 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDetailId");
            return result;
        }

        if (detailData == null || detailData.isEmpty()) {
            logger.warn("프리셋 상세 수정 실패: 상세 데이터가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetDetailData");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/details/" + detailId;
        logger.debug("프리셋 상세 수정 URL: {}", apiUrl);

        // 요청 본문 구성 - 전달된 상세 데이터 사용 (null 값 제거)
        Map<String, Object> requestMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : detailData.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                requestMap.put(entry.getKey(), value);
            }
        }

        if (requestMap.isEmpty()) {
            logger.warn("프리셋 상세 수정 실패: 요청에 포함할 값이 없음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetDetailData");
            return result;
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 상세 수정 요청 본문: {}", requestBody);

        try {
            // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.patch(apiUrl, "application/json", requestBody);

            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // 응답코드가 200 또는 204인 경우 성공으로 처리 (PATCH 요청의 표준 응답 코드)
                if (httpResult.getStatus() == 200 || httpResult.getStatus() == 204) {
                    logger.debug("프리셋 상세 수정 성공: presetId={}, detailId={}", presetId, detailId);
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "프리셋 상세 수정에 성공했습니다.");
                    result.put("response", parsedResponse);
                } else {
                    // 기타 응답 코드는 에러로 처리
                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "프리셋 상세 수정에 실패했습니다.";
                    }

                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("프리셋 상세 수정 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                }
            } else {
                // 에러 응답 처리
                int statusCode = httpResult.getStatus();
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                result.put("success", false);
                result.put("status", statusCode);

                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    // detail.message 우선 확인
                    if (parsedResponse != null) {
                        Object detailObj = parsedResponse.get("detail");
                        if (detailObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                            errorMessage = (String) detailMap.get("message");
                        }
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프리셋 상세 수정에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.error("프리셋 상세 수정 API 실패 상세 정보:");
                logger.error("- Preset ID: {}", presetId);
                logger.error("- Detail ID: {}", detailId);
                logger.error("- API URL: {}", apiUrl);
                logger.error("- HTTP Status: {}", statusCode);
                logger.error("- Response Body: {}", responseBody);
                logger.error("- Error Message: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("프리셋 상세 수정 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프리셋 상세 수정 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 프리셋 상세 순서 변경 (swap_sequence) - 외부 API 호출
     *
     * @param presetId 프리셋 ID
     * @param swapData 교체할 상세 ID 정보 (detail_id_1, detail_id_2)
     * @return 순서 변경 결과
     */
    public Map<String, Object> swapSequence(String presetId, Map<String, Object> swapData) {
        logger.debug("프리셋 상세 순서 변경 시도: presetId={}, swapData={}", presetId, swapData);
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 상세 순서 변경 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        if (swapData == null || swapData.isEmpty()) {
            logger.warn("프리셋 상세 순서 변경 실패: 교체 데이터가 null 또는 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidSwapData");
            return result;
        }

        String detailId1 = (String) swapData.get("detail_id_1");
        String detailId2 = (String) swapData.get("detail_id_2");

        if (detailId1 == null || detailId1.trim().isEmpty()) {
            logger.warn("프리셋 상세 순서 변경 실패: detail_id_1이 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDetailId1");
            return result;
        }

        if (detailId2 == null || detailId2.trim().isEmpty()) {
            logger.warn("프리셋 상세 순서 변경 실패: detail_id_2가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDetailId2");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/details/swap_sequence";
        logger.debug("프리셋 상세 순서 변경 URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("detail_id_1", detailId1);
        requestMap.put("detail_id_2", detailId2);

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 상세 순서 변경 요청 본문: {}", requestBody);

        try {
            // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

            // 응답 처리
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("프리셋 상세 순서 변경 성공: presetId={}, detailId1={}, detailId2={}",
                        presetId, detailId1, detailId2);
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "프리셋 상세 순서 변경에 성공했습니다.");
                result.put("response", parsedResponse);
            } else {
                // 에러 응답 처리
                int statusCode = httpResult.getStatus();
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                result.put("success", false);
                result.put("status", statusCode);

                // HttpUtil에서 추출된 에러 메시지 사용
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    // detail.message 우선 확인
                    if (parsedResponse != null) {
                        Object detailObj = parsedResponse.get("detail");
                        if (detailObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                            errorMessage = (String) detailMap.get("message");
                        }
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프리셋 상세 순서 변경에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.error("프리셋 상세 순서 변경 API 실패 상세 정보:");
                logger.error("- Preset ID: {}", presetId);
                logger.error("- Detail ID 1: {}", detailId1);
                logger.error("- Detail ID 2: {}", detailId2);
                logger.error("- API URL: {}", apiUrl);
                logger.error("- HTTP Status: {}", statusCode);
                logger.error("- Response Body: {}", responseBody);
                logger.error("- Error Message: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("프리셋 상세 순서 변경 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프리셋 상세 순서 변경 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 프리셋 마스터 수정 - 외부 API 호출
     *
     * @param presetId   프리셋 ID
     * @param presetData 프리셋 데이터
     * @return 수정 결과
     */
    public Map<String, Object> updatePresetMaster(String presetId, Map<String, Object> presetData) {
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.isEmpty()) {
            logger.warn("프리셋 수정 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        if (presetData == null) {
            logger.warn("프리셋 수정 실패: 프리셋 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetData");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId;
        logger.debug("프리셋 수정 URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // root_equipment_type
        Object rootEquipmentType = presetData.get("root_equipment_type");
        if (rootEquipmentType != null) {
            requestMap.put("root_equipment_type", rootEquipmentType);
        }

        // equipment_type
        Object equipmentType = presetData.get("equipment_type");
        if (equipmentType != null) {
            requestMap.put("equipment_type", equipmentType);
        }

        // preset_category
        Object presetCategory = presetData.get("preset_category");
        if (presetCategory != null) {
            requestMap.put("preset_category", presetCategory);
        }

        // total_unit_count
        Object totalUnitCount = presetData.get("total_unit_count");
        if (totalUnitCount != null) {
            requestMap.put("total_unit_count", totalUnitCount);
        }

        // preset_name_ko
        Object presetNameKo = presetData.get("preset_name_ko");
        if (presetNameKo != null) {
            requestMap.put("preset_name_ko", presetNameKo);
        }

        // preset_name_en
        Object presetNameEn = presetData.get("preset_name_en");
        if (presetNameEn != null) {
            requestMap.put("preset_name_en", presetNameEn);
        }

        // unit_system_code (기본값: METRIC)
        String unitSystemCode = (String) presetData.getOrDefault("unit_system_code", "METRIC");
        requestMap.put("unit_system_code", unitSystemCode);

        // diameter_value
        Object diameterValue = presetData.get("diameter_value");
        if (diameterValue != null) {
            requestMap.put("diameter_value", diameterValue);
        }

        // diameter_unit
        Object diameterUnit = presetData.get("diameter_unit");
        if (diameterUnit != null) {
            requestMap.put("diameter_unit", diameterUnit);
        }

        // set_dtdx_file_id
        Object setDtdxFileId = presetData.get("set_dtdx_file_id");
        if (setDtdxFileId != null) {
            requestMap.put("set_dtdx_file_id", setDtdxFileId);
        }

        // thumbnail_id 설정: presetData에서 직접 가져옴
        Object thumbnailId = presetData.get("thumbnail_id");
        if (thumbnailId != null) {
            requestMap.put("thumbnail_id", thumbnailId);
            logger.debug("thumbnail_id 사용: {}", thumbnailId);
        }

        // note
        Object note = presetData.get("note");
        if (note != null) {
            requestMap.put("note", note);
        }

        // metadata
        Object metadata = presetData.get("metadata");
        if (metadata != null) {
            requestMap.put("metadata", metadata);
        }

        // is_active
        Object isActive = presetData.get("is_active");
        if (isActive != null) {
            requestMap.put("is_active", isActive);
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("프리셋 마스터 수정 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 PUT 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.put(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답코드가 200 또는 204인 경우 성공으로 처리 (PUT 요청의 표준 응답 코드)
            if (httpResult.getStatus() == 200 || httpResult.getStatus() == 204) {
                logger.debug("프리셋 수정 성공: preset_id={}", presetId);
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.presetUpdateSuccess");
                result.put("response", parsedResponse);
            } else {
                // 기타 응답 코드는 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.presetUpdateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프리셋 수정 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
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
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.presetUpdateFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 프리셋 수정 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 프리셋 마스터 삭제 - 외부 API 호출
     *
     * @param deleteData 삭제 데이터 (preset_id, thumbnail_id 포함)
     * @return 삭제 결과
     */
    public Map<String, Object> deletePresetMaster(Map<String, Object> deleteData) {
        Map<String, Object> result = new HashMap<>();

        if (deleteData == null) {
            logger.warn("프리셋 삭제 실패: 삭제 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.deleteDataRequired");
            return result;
        }

        // preset_id 추출
        String presetId = (String) deleteData.getOrDefault("preset_id", "");

        if (presetId == null || presetId.isEmpty()) {
            logger.warn("프리셋 삭제 실패: 프리셋 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidPresetId");
            return result;
        }

        logger.debug("프리셋 삭제 시작: preset_id={}", presetId);

        // 프리셋 삭제
        logger.debug("프리셋 삭제 시작");
        String presetUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId;
        logger.debug("프리셋 삭제 URL: {}", presetUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(presetUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            logger.debug("프리셋 삭제 성공: preset_id={}", presetId);
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.presetDeleteSuccess");
            result.put("response", parsedResponse);

            logger.debug("프리셋 삭제 프로세스 완료: preset_id={}", presetId);
        } else {
            // 에러 응답 처리
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();

            result.put("success", false);
            result.put("status", statusCode);

            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                // detail.message 우선 확인
                Object detailObj = parsedResponse.get("detail");
                if (detailObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                    errorMessage = (String) detailMap.get("message");
                }
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                // detail.error 확인
                Object detailObj = parsedResponse.get("detail");
                if (detailObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                    errorMessage = (String) detailMap.get("error");
                }
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "프리셋 삭제에 실패했습니다. 관련 데이터가 존재하여 삭제할 수 없습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            logger.error("프리셋 삭제 API 실패 상세 정보:");
            logger.error("- Preset ID: {}", presetId);
            logger.error("- API URL: {}", presetUrl);
            logger.error("- HTTP Status: {}", statusCode);
            logger.error("- Response Body: {}", responseBody);
            logger.error("- Error Message: {}", errorMessage);
            logger.warn("관련 데이터 삭제는 완료되었지만 프리셋 삭제에 실패했습니다.");

            // 일반적인 HTTP 상태 코드별 추가 안내
            if (statusCode == 404) {
                logger.debug("404 오류: 프리셋이 이미 삭제되었거나 존재하지 않을 수 있습니다.");
            } else if (statusCode == 403) {
                logger.debug("403 오류: 프리셋 삭제 권한이 없을 수 있습니다.");
            } else if (statusCode == 409) {
                logger.debug("409 오류: 프리셋이 다른 리소스에서 사용 중일 수 있습니다.");
            } else if (statusCode == 500) {
                logger.debug("500 오류: 프리셋과 관련된 하위 데이터(예: preset_units)가 존재하여 삭제할 수 없습니다. 관련 데이터를 먼저 삭제해야 합니다.");
            }
        }

        return result;
    }

    /**
     * 프리셋 썸네일 삭제
     *
     * @param presetId 프리셋 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deletePresetThumbnail(String presetId) {
        Map<String, Object> result = new HashMap<>();

        if (presetId == null || presetId.trim().isEmpty()) {
            logger.warn("프리셋 썸네일 삭제 실패: 프리셋 ID가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "messages.error.presetIdRequired");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/equipment/preset_masters/" + presetId + "/thumb";
            logger.debug("프리셋 썸네일 삭제 URL: {}", apiUrl);

            // HttpUtil을 사용하여 DELETE 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", null);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("프리셋 썸네일 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.presetThumbnailDeleteSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());

                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프리셋 썸네일 삭제에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프리셋 썸네일 삭제 API 실패: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("프리셋 썸네일 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프리셋 썸네일 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 3D 라이브러리 썸네일 삭제
     *
     * @param libraryId 라이브러리 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deleteLibraryThumbnail(String libraryId) {
        Map<String, Object> result = new HashMap<>();

        if (libraryId == null || libraryId.trim().isEmpty()) {
            logger.warn("3D 라이브러리 썸네일 삭제 실패: 라이브러리 ID가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "messages.error.libraryIdRequired");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/equipment/3d_library/" + libraryId + "/thumb";
            logger.debug("3D 라이브러리 썸네일 삭제 URL: {}", apiUrl);

            // HttpUtil을 사용하여 DELETE 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", null);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("3D 라이브러리 썸네일 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.libraryThumbnailDeleteSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());

                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "3D 라이브러리 썸네일 삭제에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("3D 라이브러리 썸네일 삭제 API 실패: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("3D 라이브러리 썸네일 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "3D 라이브러리 썸네일 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 3D 라이브러리 삭제
     *
     * @param deleteData 삭제 데이터 (library_id, thumbnail_id 포함)
     * @return 삭제 결과
     */
    public Map<String, Object> deleteLibrary(Map<String, Object> deleteData) {
        Map<String, Object> result = new HashMap<>();

        if (deleteData == null) {
            logger.warn("3D 라이브러리 삭제 실패: 삭제 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.deleteDataRequired");
            return result;
        }

        // library_id 추출
        String libraryId = (String) deleteData.getOrDefault("library_id", "");

        if (libraryId == null || libraryId.isEmpty()) {
            logger.warn("3D 라이브러리 삭제 실패: 라이브러리 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidLibraryId");
            return result;
        }

        logger.debug("3D 라이브러리 삭제 시작: library_id={}", libraryId);

        // 3D 라이브러리 삭제
        logger.debug("3D 라이브러리 삭제 시작");
        String libraryUrl = authServerBaseUrl + "/api/v1/equipment/3d_library/" + libraryId;
        logger.debug("3D 라이브러리 삭제 URL: {}", libraryUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(libraryUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            logger.debug("3D 라이브러리 삭제 성공: library_id={}", libraryId);
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.libraryDeleteSuccess");
            result.put("response", parsedResponse);

            logger.debug("3D 라이브러리 삭제 프로세스 완료: library_id={}", libraryId);
        } else {
            // 에러 응답 처리
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();

            result.put("success", false);
            result.put("status", statusCode);

            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                // detail.message 우선 확인
                Object detailObj = parsedResponse.get("detail");
                if (detailObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                    errorMessage = (String) detailMap.get("message");
                }
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                // detail.error 확인
                Object detailObj = parsedResponse.get("detail");
                if (detailObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailMap = (Map<String, Object>) detailObj;
                    errorMessage = (String) detailMap.get("error");
                }
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "3D 라이브러리 삭제에 실패했습니다. 관련 데이터가 존재하여 삭제할 수 없습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            logger.error("3D 라이브러리 삭제 API 실패 상세 정보:");
            logger.error("- Library ID: {}", libraryId);
            logger.error("- API URL: {}", libraryUrl);
            logger.error("- HTTP Status: {}", statusCode);
            logger.error("- Response Body: {}", responseBody);
            logger.error("- Error Message: {}", errorMessage);
            logger.warn("관련 데이터 삭제는 완료되었지만 3D 라이브러리 삭제에 실패했습니다.");

            // 일반적인 HTTP 상태 코드별 추가 안내
            if (statusCode == 404) {
                logger.debug("404 오류: 3D 라이브러리가 이미 삭제되었거나 존재하지 않을 수 있습니다.");
            } else if (statusCode == 403) {
                logger.debug("403 오류: 3D 라이브러리 삭제 권한이 없을 수 있습니다.");
            } else if (statusCode == 409) {
                logger.debug("409 오류: 3D 라이브러리가 다른 리소스에서 사용 중일 수 있습니다.");
            } else if (statusCode == 500) {
                logger.debug("500 오류: 3D 라이브러리와 관련된 하위 데이터가 존재하여 삭제할 수 없습니다. 관련 데이터를 먼저 삭제해야 합니다.");
            }
        }

        return result;
    }

    /**
     * 프리셋 썸네일 업로드
     *
     * @param file     썸네일 파일
     * @param presetId 프리셋 ID (UUID)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadPresetThumbnail(org.springframework.web.multipart.MultipartFile file,
            String presetId) {
        logger.debug("프리셋 썸네일 등록 API 호출: presetId={}, fileName={}", presetId,
                file != null ? file.getOriginalFilename() : null);
        Map<String, Object> result = new HashMap<>();

        try {
            // 파일 유효성 검사
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "messages.error.noFileToRegister");
                return result;
            }

            // preset_id 유효성 검사
            if (presetId == null || presetId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "preset_id가 필요합니다.");
                return result;
            }

            String apiUrl = authServerBaseUrl + "/api/v1/minio/preset_upload/thumbnail";
            logger.debug("썸네일 등록 URL: {}", apiUrl);

            // 파일이 있는 경우 multipart로 전송
            logger.debug("파일 업로드: fileName={}, size={}, presetId={}", file.getOriginalFilename(), file.getSize(),
                    presetId);

            // 외부 API가 요구하는 데이터 구성
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("file", file);
            uploadData.put("preset_id", presetId);

            logger.debug("multipart 업로드 데이터: preset_id={}", uploadData.get("preset_id"));

            HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(apiUrl, uploadData);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("썸네일 등록 응답 내용: {}", parsedResponse);

                // symbol_id 추출
                String symbolId = JsonUtil.extractValue(responseBody, "symbol_id");
                if (symbolId == null || symbolId.isEmpty()) {
                    // 다른 가능한 필드명들도 확인
                    symbolId = JsonUtil.extractValue(responseBody, "id");
                }

                logger.debug("썸네일 등록 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.thumbnailRegisterSuccess");
                result.put("response", parsedResponse);
                if (!isEmptySymbolId(symbolId)) {
                    result.put("symbol_id", symbolId);
                }
            } else {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("썸네일 등록 실패 응답: HTTP {}, Body: {}", httpResult.getStatus(), parsedResponse);

                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage != null ? errorMessage : "썸네일 등록에 실패했습니다.");
                result.put("response", parsedResponse);

                logger.warn("썸네일 등록 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("썸네일 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "썸네일 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 3D 라이브러리 썸네일 업로드
     *
     * @param file      썸네일 파일
     * @param libraryId 라이브러리 ID (UUID)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadLibraryThumbnail(org.springframework.web.multipart.MultipartFile file,
            String libraryId) {
        logger.debug("3D 라이브러리 썸네일 등록 API 호출: libraryId={}, fileName={}", libraryId,
                file != null ? file.getOriginalFilename() : null);
        Map<String, Object> result = new HashMap<>();

        try {
            // 파일 유효성 검사
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "messages.error.noFileToRegister");
                return result;
            }

            // library_id 유효성 검사
            if (libraryId == null || libraryId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "library_id가 필요합니다.");
                return result;
            }

            String apiUrl = authServerBaseUrl + "/api/v1/minio/3d_library/upload/thumbnail";
            logger.debug("3D 라이브러리 썸네일 등록 URL: {}", apiUrl);

            // 파일이 있는 경우 multipart로 전송
            logger.debug("파일 업로드: fileName={}, size={}, libraryId={}", file.getOriginalFilename(), file.getSize(),
                    libraryId);

            // 외부 API가 요구하는 데이터 구성
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("file", file);
            uploadData.put("library_id", libraryId);

            logger.debug("multipart 업로드 데이터: library_id={}", uploadData.get("library_id"));

            HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(apiUrl, uploadData);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("3D 라이브러리 썸네일 등록 응답 내용: {}", parsedResponse);

                // symbol_id 추출
                String symbolId = JsonUtil.extractValue(responseBody, "symbol_id");
                if (symbolId == null || symbolId.isEmpty()) {
                    // 다른 가능한 필드명들도 확인
                    symbolId = JsonUtil.extractValue(responseBody, "id");
                }

                logger.debug("3D 라이브러리 썸네일 등록 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.thumbnailRegisterSuccess");
                result.put("response", parsedResponse);
                if (!isEmptySymbolId(symbolId)) {
                    result.put("symbol_id", symbolId);
                }
            } else {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("3D 라이브러리 썸네일 등록 실패 응답: HTTP {}, Body: {}", httpResult.getStatus(), parsedResponse);

                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage != null ? errorMessage : "3D 라이브러리 썸네일 등록에 실패했습니다.");
                result.put("response", parsedResponse);

                logger.warn("3D 라이브러리 썸네일 등록 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("3D 라이브러리 썸네일 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "3D 라이브러리 썸네일 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 3D 라이브러리 Model 파일 업로드
     *
     * @param file      Model(DTDX) 파일
     * @param libraryId 라이브러리 ID (UUID)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadLibraryModel(org.springframework.web.multipart.MultipartFile file,
            String libraryId) {
        logger.debug("3D 라이브러리 Model 파일 등록 API 호출: libraryId={}, fileName={}", libraryId,
                file != null ? file.getOriginalFilename() : null);
        Map<String, Object> result = new HashMap<>();

        try {
            // 파일 유효성 검사
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "messages.error.noFileToRegister");
                return result;
            }

            // library_id 유효성 검사
            if (libraryId == null || libraryId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "library_id가 필요합니다.");
                return result;
            }

            String apiUrl = authServerBaseUrl + "/api/v1/minio/3d_library/upload";
            logger.debug("3D 라이브러리 Model 파일 등록 URL: {}", apiUrl);

            // 파일이 있는 경우 multipart로 전송
            logger.debug("파일 업로드: fileName={}, size={}, libraryId={}", file.getOriginalFilename(), file.getSize(),
                    libraryId);

            // 외부 API가 요구하는 데이터 구성
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("file", file);
            uploadData.put("library_id", libraryId);

            logger.debug("multipart 업로드 데이터: library_id={}", uploadData.get("library_id"));

            HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(apiUrl, uploadData);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("3D 라이브러리 Model 파일 등록 응답 내용: {}", parsedResponse);

                // model_file_id 추출
                String modelFileId = JsonUtil.extractValue(responseBody, "model_file_id");
                if (modelFileId == null || modelFileId.isEmpty()) {
                    // 다른 가능한 필드명들도 확인
                    modelFileId = JsonUtil.extractValue(responseBody, "id");
                }

                logger.debug("3D 라이브러리 Model 파일 등록 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.modelFileRegisterSuccess");
                result.put("response", parsedResponse);
                if (modelFileId != null && !modelFileId.trim().isEmpty()) {
                    result.put("model_file_id", modelFileId);
                }
            } else {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("3D 라이브러리 Model 파일 등록 실패 응답: HTTP {}, Body: {}", httpResult.getStatus(), parsedResponse);

                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage != null ? errorMessage : "3D 라이브러리 Model 파일 등록에 실패했습니다.");
                result.put("response", parsedResponse);

                logger.warn("3D 라이브러리 Model 파일 등록 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("3D 라이브러리 Model 파일 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "3D 라이브러리 Model 파일 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 심볼 ID가 비어있는지 확인
     *
     * @param symbolId 심볼 ID
     * @return 비어있으면 true
     */
    private boolean isEmptySymbolId(String symbolId) {
        if (symbolId == null || symbolId.trim().isEmpty()) {
            return true;
        }
        // 빈 UUID 체크: 00000000-0000-0000-0000-000000000000
        String trimmedSymbolId = symbolId.trim();
        return trimmedSymbolId.equals("00000000-0000-0000-0000-000000000000")
                || trimmedSymbolId.equalsIgnoreCase("00000000-0000-0000-0000-000000000000");
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

        // 기본값 설정
        requestMap.put("top_root_level", searchParams.getOrDefault("top_root_level", "PIPE_S"));
        requestMap.put("keyword", searchParams.getOrDefault("keyword", ""));
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("equipment_type", searchParams.getOrDefault("equipment_type", ""));
        requestMap.put("parent_type", searchParams.getOrDefault("parent_type", ""));
        requestMap.put("root_equipment_type", searchParams.getOrDefault("root_equipment_type", ""));

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));

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

}