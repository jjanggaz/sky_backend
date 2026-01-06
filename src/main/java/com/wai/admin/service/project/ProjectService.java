package com.wai.admin.service.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.CommonCodeUtil;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    @Autowired
    private CommonCodeUtil commonCodeUtil;

    /**
     * 프로젝트 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getAllProjects(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("외부 인증 서버 프로젝트 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String projectsUrl = authServerBaseUrl + "/api/v1/project/projects/search";
            logger.debug("프로젝트 목록 조회 URL: {}", projectsUrl);
            
            // 요청 본문 구성 - 화면에서 받은 파라미터 사용 (기본값 포함)
            Map<String, Object> requestMap = new HashMap<>();
            
            // 웹뷰에서는 본인것만 조회해야하기 때문에
            String systemCode = request.getHeader("system_code");
            String authSuper = request.getHeader("authSuper");
            if(!"WAI_WEB_ADMIN".equals(systemCode) && !"true".equals(authSuper)){
                String userId = request.getHeader("user_id");
                requestMap.put("owner_id", userId);
            }

            requestMap.put("project_status", searchParams.getOrDefault("project_status", ""));
            requestMap.put("search_field", searchParams.getOrDefault("search_field", "project_name"));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
            requestMap.put("page", searchParams.getOrDefault("page", 1));
            requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
            requestMap.put("order_by", searchParams.getOrDefault("order_by", "created_at"));
            requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "desc"));
            
            
            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("프로젝트 목록 조회 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(projectsUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                if (parsedResponse != null) {
                    logger.debug("프로젝트 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));

                    // 헤더의 wai_lang이 en이면 project_status_info.code_value를 code_value_en으로 대치
                    try {
                        String waiLang = request.getHeader("wai_lang");
                        if (waiLang != null && "en".equalsIgnoreCase(waiLang)) {
                            Object itemsObj = parsedResponse.get("items");
                            if (itemsObj instanceof java.util.List) {
                                @SuppressWarnings("unchecked")
                                java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) itemsObj;
                                for (java.util.Map<String, Object> item : items) {
                                    Object statusInfoObj = item.get("project_status_info");
                                    if (statusInfoObj instanceof java.util.Map) {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<String, Object> statusInfo = (java.util.Map<String, Object>) statusInfoObj;
                                        Object codeValueEn = statusInfo.get("code_value_en");
                                        if (codeValueEn != null) {
                                            statusInfo.put("code_value", codeValueEn);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception langEx) {
                        logger.warn("wai_lang 처리 중 오류: {}", langEx.getMessage());
                    }

                    // 두 번째 호출: 승인 대기 프로젝트 건수 조회
                    Integer approvalCount = getApprovalPendingCount(searchParams, request);
                    parsedResponse.put("approvalCount", approvalCount != null ? approvalCount : 0);
                } else {
                    logger.warn("프로젝트 목록 조회 응답 파싱 실패: {}", responseBody);
                }
                
                logger.debug("프로젝트 목록 조회 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.projectSearchSuccess");
                result.put("response", parsedResponse);
                
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.projectSearchFailed";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("프로젝트 목록 조회 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("프로젝트 목록 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 목록 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 추천프로젝트 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getRecommendationProjects(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("외부 인증 서버 추천프로젝트 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();

        try {
            String recommendationUrl = authServerBaseUrl + "/api/v1/project/project_recommended/search";
            logger.debug("추천프로젝트 목록 조회 URL: {}", recommendationUrl);

            Map<String, Object> requestMap = new HashMap<>();
            if (searchParams != null) {
                requestMap.putAll(searchParams);
            }

            if(searchParams != null && searchParams.get("search_value") != null && !searchParams.get("search_value").toString().isEmpty()){
                requestMap.put("keyword", searchParams.get("search_value").toString());
            }
            
            requestMap.putIfAbsent("page", 1);
            requestMap.putIfAbsent("page_size", 10);
            requestMap.putIfAbsent("order_by", "created_at");
            requestMap.putIfAbsent("order_direction", "desc");

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("추천프로젝트 목록 조회 요청 본문: {}", requestBody);

            HttpUtil.HttpResult httpResult = HttpUtil.post(recommendationUrl, "application/json", requestBody);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("추천프로젝트 목록 조회 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.recommendationSearchSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());

                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.recommendationSearchFailed";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("추천프로젝트 목록 조회 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("추천프로젝트 목록 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "추천프로젝트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 승인 대기 프로젝트 건수 조회 - 외부 API 호출
     * @param searchParams 검색 파라미터
     * @param request HTTP 요청 객체
     * @return 승인 대기 프로젝트 건수 (실패 시 null)
     */
    private Integer getApprovalPendingCount(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("승인 대기 프로젝트 건수 조회 시도: server={}", authServerBaseUrl);
        
        try {
            String projectsUrl = authServerBaseUrl + "/api/v1/project/projects/search";
            logger.debug("승인 대기 프로젝트 건수 조회 URL: {}", projectsUrl);
            
            Map<String, Object> approvalRequestMap = new HashMap<>();
            
            // 웹뷰에서는 본인것만 조회해야하기 때문에
            String systemCode = request.getHeader("system_code");
            if(!"WAI_WEB_ADMIN".equals(systemCode)){
                String userId = request.getHeader("user_id");
                approvalRequestMap.put("owner_id", userId);
            }
            
            // project_status에 SC_WAITAPPR 하드코딩
            approvalRequestMap.put("project_status", "SC_WAITAPPR");
            approvalRequestMap.put("page", 1);
            approvalRequestMap.put("page_size", 1); // count만 필요하므로 최소값
            
            String approvalRequestBody = JsonUtil.objectMapToJson(approvalRequestMap);
            logger.debug("승인 대기 프로젝트 건수 조회 요청 본문: {}", approvalRequestBody);
            
            HttpUtil.HttpResult approvalHttpResult = HttpUtil.post(projectsUrl, "application/json", approvalRequestBody);
            
            if (approvalHttpResult.isSuccess()) {
                String approvalResponseBody = approvalHttpResult.getBody();
                Map<String, Object> approvalParsedResponse = JsonUtil.parseJson(approvalResponseBody);
                
                if (approvalParsedResponse != null) {
                    // items의 size 추출
                    Object itemsObj = approvalParsedResponse.get("items");
                    Integer approvalCount = 0;
                    
                    if (itemsObj instanceof java.util.List) {
                        approvalCount = ((java.util.List<?>) itemsObj).size();
                    } else if (itemsObj instanceof Object[]) {
                        approvalCount = ((Object[]) itemsObj).length;
                    } else if (itemsObj != null) {
                        logger.warn("승인 대기 건수: items가 List 또는 Array가 아님: {}", itemsObj.getClass().getName());
                    }
                    
                    logger.debug("승인 대기 프로젝트 건수: {}", approvalCount);
                    return approvalCount;
                } else {
                    logger.warn("승인 대기 프로젝트 건수 조회 응답 파싱 실패: {}", approvalResponseBody);
                    return 0;
                }
            } else {
                logger.warn("승인 대기 프로젝트 건수 조회 실패: {}", approvalHttpResult.getExtractedErrorMessage());
                return 0;
            }
        } catch (Exception e) {
            logger.warn("승인 대기 프로젝트 건수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 프로젝트 생성 - 외부 API 호출
     */
    public Map<String, Object> createProject(Map<String, Object> projectData) {
        logger.debug("외부 인증 서버 프로젝트 생성 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (projectData == null) {
            logger.warn("프로젝트 생성 실패: 프로젝트 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.projectDataNull");
            return result;
        }
        
        try {
            // 고객사, 부지정보, 프로젝트 정보 생성 (createClient 메서드에서 모든 API 호출 처리)
            Map<String, Object> creationResult = createClient(projectData);
            
            if (!(Boolean) creationResult.get("success")) {
                logger.warn("프로젝트 생성 실패: {}", creationResult.get("message"));
                result.put("success", false);
                result.put("message", creationResult.get("message"));
                return result;
            }
            
            // 생성 성공 시 상세 결과 반환
            Map<String, Object> clientResult = (Map<String, Object>) creationResult.get("clientResult");
            Map<String, Object> siteInfoResult = (Map<String, Object>) creationResult.get("siteInfoResult");
            Map<String, Object> projectResult = (Map<String, Object>) creationResult.get("projectResult");
            String projectId = (String) creationResult.get("projectId");
            
            // 통합 결과 데이터 구성
            Map<String, Object> data = new HashMap<>();
            data.put("clientResult", clientResult);
            data.put("siteInfoResult", siteInfoResult);
            data.put("projectResult", projectResult);
            data.put("projectId", projectId);
            
            result.put("success", true);
            result.put("status", 201);
            result.put("message", "messages.success.projectCreateSuccess");
            result.put("response", data);
            
        } catch (Exception e) {
            logger.error("프로젝트 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프로젝트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 프로젝트 수정 - 외부 API 호출
     */
    public Map<String, Object> updateProject(String projectId, Map<String, Object> projectData, HttpServletRequest request) {
        logger.debug("외부 인증 서버 프로젝트 수정 시도: projectId={}, server={}", projectId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (projectId == null || projectId.isEmpty()) {
            logger.warn("프로젝트 수정 실패: 프로젝트 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.projectIdInvalid");
            return result;
        }
        
        if (projectData == null) {
            logger.warn("프로젝트 수정 실패: 프로젝트 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.projectDataNull");
            return result;
        }
        
        try {
            // 고객사, 부지정보, 프로젝트 정보 수정 (updateClient 메서드에서 모든 API 호출 처리)
            Map<String, Object> updateResult = updateClient(projectId, projectData, request);
            
            if (!(Boolean) updateResult.get("success")) {
                logger.warn("프로젝트 수정 실패: {}", updateResult.get("message"));
                result.put("success", false);
                result.put("message", updateResult.get("message"));
                return result;
            }
            
            // 수정 성공 시 상세 결과 반환
            Map<String, Object> clientResult = (Map<String, Object>) updateResult.get("clientResult");
            Map<String, Object> siteInfoResult = (Map<String, Object>) updateResult.get("siteInfoResult");
            Map<String, Object> projectResult = (Map<String, Object>) updateResult.get("projectResult");
            
            // 통합 결과 데이터 구성
            Map<String, Object> data = new HashMap<>();
            data.put("clientResult", clientResult);
            data.put("siteInfoResult", siteInfoResult);
            data.put("projectResult", projectResult);
            
            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.projectUpdateSuccess");
            result.put("response", data);
            
        } catch (Exception e) {
            logger.error("프로젝트 수정 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "프로젝트 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 프로젝트 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteProject(String projectId, Map<String, Object> deleteData) {
        logger.debug("외부 인증 서버 프로젝트 삭제 시도: projectId={}, server={}", projectId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (projectId == null || projectId.isEmpty()) {
            logger.warn("프로젝트 삭제 실패: 프로젝트 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.projectIdInvalid");
            return result;
        }
        
        if (deleteData == null) {
            logger.warn("프로젝트 삭제 실패: 삭제 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.deleteDataNull");
        return result;
        }

        try {
            // 1. 프로젝트 삭제 API 호출
            String deleteUrl = authServerBaseUrl + "/api/v1/project/projects/" + projectId;
            logger.debug("프로젝트 삭제 URL: {}", deleteUrl);
            
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
            
            if (!httpResult.isSuccess()) {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프로젝트 삭제에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("프로젝트 삭제 API 실패: {}", errorMessage);
                return result;
            }

            // 2. 화면에서 받은 client_id와 site_id 추출
            String clientId = (String) deleteData.get("clientId");
            String siteId = (String) deleteData.get("siteId");
            
            logger.debug("삭제할 관련 정보: clientId={}, siteId={}", clientId, siteId);
            
            // 3. 부지정보 삭제 API 호출 (site_id가 있는 경우)
            if (siteId != null && !siteId.isEmpty()) {
                Map<String, Object> siteDeleteResult = deleteSiteInfoApi(siteId);
                if (!(Boolean) siteDeleteResult.get("success")) {
                    logger.warn("부지정보 삭제 실패: {}", siteDeleteResult.get("message"));
                    // 부지정보 삭제 실패는 경고만 하고 계속 진행
                }
            }
            
            // 4. 고객사 삭제 API 호출 (client_id가 있는 경우)
            if (clientId != null && !clientId.isEmpty()) {
                Map<String, Object> clientDeleteResult = deleteClientApi(clientId);
                if (!(Boolean) clientDeleteResult.get("success")) {
                    logger.warn("고객사 삭제 실패: {}", clientDeleteResult.get("message"));
                    // 고객사 삭제 실패는 경고만 하고 계속 진행
                }
            }
            
            // 전체 삭제 성공 결과 반환
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.projectDeleteSuccess");
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("deletedProjectId", projectId);
            responseData.put("deletedClientId", clientId);
            responseData.put("deletedSiteId", siteId);
            result.put("response", responseData);
            
        } catch (Exception e) {
            logger.error("프로젝트 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 프로젝트 관련 공통코드들을 조회합니다.
     */
    public Map<String, Object> getProjectCommonCodes() {
        logger.debug("프로젝트 관련 공통코드 조회 시도");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 비즈니스 타입 코드 (BUSINESS_CD)
            Map<String, Object> bizTypeResult = commonCodeUtil.getCommonCodesByCategory("parent_key", "BUSINESS_CD", true);
            logger.debug("비즈니스 타입 코드: {}", bizTypeResult);
            
            // 비즈니스 타입 코드 조회 실패 시
            if (!(Boolean) bizTypeResult.get("success")) {
                result.put("success", false);
                result.put("status", bizTypeResult.get("status"));
                result.put("message", bizTypeResult.get("message"));
                result.put("response", bizTypeResult);
                return result;
            }
            
            // 단위 시스템 조회
            Map<String, Object> unitResult = commonCodeUtil.getUnitSystems();
            
            // 단위 시스템 조회 실패 시
            if (!(Boolean) unitResult.get("success")) {
                result.put("success", false);
                result.put("status", unitResult.get("status"));
                result.put("message", unitResult.get("message"));
                result.put("response", unitResult);
                return result;
            }
            
            // 언어 설정 코드 (LANG_CD)
            Map<String, Object> langResult = commonCodeUtil.getCommonCodesByCategory("parent_key", "LANG_CD", true);
            
            // 언어 설정 코드 조회 실패 시
            if (!(Boolean) langResult.get("success")) {
                result.put("success", false);
                result.put("status", langResult.get("status"));
                result.put("message", langResult.get("message"));
                result.put("response", langResult);
                return result;
            }
            
            // 진행상태 코드 (STATUS_CD)
            Map<String, Object> statusResult = commonCodeUtil.getCommonCodesByCategory("parent_key", "STATUS_CD", true);
            
            // 진행상태 코드 조회 실패 시
            if (!(Boolean) statusResult.get("success")) {
                result.put("success", false);
                result.put("status", statusResult.get("status"));
                result.put("message", statusResult.get("message"));
                result.put("response", statusResult);
                return result;
            }
            
            // 국가 코드 (COUNTRY_CD)
            Map<String, Object> countryResult = commonCodeUtil.getCommonCodesByCategory("parent_key", "COUNTRY_CD", true);
            
            // 국가 코드 조회 실패 시
            if (!(Boolean) countryResult.get("success")) {
                result.put("success", false);
                result.put("status", countryResult.get("status"));
                result.put("message", countryResult.get("message"));
                result.put("response", countryResult);
                return result;
            }

            // 화폐 코드 (MONETARY_CD)
            Map<String, Object> monetaryResult = commonCodeUtil.getCommonCodesByCategory("parent_key", "MONETARY_CD", true);
            
            // 화폐 코드 조회 실패 시
            if (!(Boolean) monetaryResult.get("success")) {
                result.put("success", false);
                result.put("status", monetaryResult.get("status"));
                result.put("message", monetaryResult.get("message"));
                result.put("response", monetaryResult);
                return result;
            }

            // 각각의 결과를 key에 담아서 return
            Map<String, Object> data = new HashMap<>();
            data.put("bizTypeCodes", bizTypeResult.get("data"));
            data.put("unitCodes", unitResult.get("response"));
            data.put("langCodes", langResult.get("data"));
            data.put("status", statusResult.get("data"));
            data.put("countryCodes", countryResult.get("data"));
            data.put("monetaryCodes", monetaryResult.get("data"));

            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.projectCommonCodeSuccess");
            result.put("response", data);
            
        } catch (Exception e) {
            logger.error("프로젝트 관련 공통코드 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "공통코드 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 프로젝트 정보 생성 - 외부 API 호출
     */
    private Map<String, Object> createClient(Map<String, Object> projectData) {
        logger.debug("프로젝트 정보 생성 시도");
        Map<String, Object> result = new HashMap<>();
        
        String message = "";
            
        try {
            // 1. 고객사 생성 API 호출
            Map<String, Object> clientData = new HashMap<>();
            clientData.put("client_name", projectData.getOrDefault("customerName", ""));
            clientData.put("homepage", projectData.getOrDefault("siteAddress", ""));
            clientData.put("contact_person", projectData.getOrDefault("customerManager", ""));
            clientData.put("phone_number", projectData.getOrDefault("contact", ""));
            clientData.put("email", projectData.getOrDefault("email", ""));
            
            Map<String, Object> clientResult = createClientApi(clientData);
            if (!(Boolean) clientResult.get("success")) {
                return clientResult; // 고객사 생성 실패 시 즉시 반환
            }
            
            // 2. 부지정보 생성 API 호출
            Map<String, Object> siteInfoData = new HashMap<>();
            siteInfoData.put("site_name", projectData.getOrDefault("projectName", ""));
            siteInfoData.put("site_address", projectData.getOrDefault("siteAddress", ""));
            siteInfoData.put("site_capacity", 0); // 0 고정 필수값
            
            Map<String, Object> siteInfoResult = createSiteInfoApi(siteInfoData);
            if (!(Boolean) siteInfoResult.get("success")) {
                return siteInfoResult; // 부지정보 생성 실패 시 즉시 반환
            }else{
                message = "부지정보"; // 업데이트 있음으로 메시지 추가 안함
            }
            
            // 3. 프로젝트 생성 API 호출
            Map<String, Object> projectApiData = new HashMap<>();
            projectApiData.put("project_code", projectData.getOrDefault("project_code", generateRandomProjectCode())); // 부강테크 전용 공정코드 현재 임시 처리 함
            projectApiData.put("orderer", projectData.getOrDefault("orderer", ""));
            projectApiData.put("project_name", projectData.getOrDefault("projectName", ""));
            projectApiData.put("project_status", projectData.getOrDefault("projectStatus", ""));
            projectApiData.put("start_date", projectData.getOrDefault("startDate", ""));
            projectApiData.put("end_date", projectData.getOrDefault("endDate", ""));
            projectApiData.put("country_code", projectData.getOrDefault("country", ""));
            projectApiData.put("language_code", projectData.getOrDefault("language", ""));
            projectApiData.put("business_type", projectData.getOrDefault("businessType", ""));
            projectApiData.put("unit_system", projectData.getOrDefault("unitSelection", ""));
            projectApiData.put("used_currency", projectData.getOrDefault("used_currency", ""));
            
            // clientResult에서 client_id 추출하여 projectApiData에 추가
            Map<String, Object> clientResponse = (Map<String, Object>) clientResult.get("response");
            String clientId = clientResponse.get("client_id").toString();
            projectApiData.put("client_id", clientId);
            
            // siteInfoResult에서 site_id 추출하여 projectApiData에 추가
            Map<String, Object> siteResponse = (Map<String, Object>) siteInfoResult.get("response");
            String siteId = siteResponse.get("site_id").toString();
            projectApiData.put("site_id", siteId);
            
            Map<String, Object> projectResult = createProjectApi(projectApiData);
            if (!(Boolean) projectResult.get("success")) {
                projectResult.put("message", message+ " 생성 성공 / " + projectResult.get("message"));
                return projectResult; // 프로젝트 생성 실패 시 즉시 반환
            }else{
                message = message + ", 프로젝트";
            }

            // projectResult에서 project_id 추출하여 updateSiteData에 추가
            Map<String, Object> projectResponse = (Map<String, Object>) projectResult.get("response");
            String projectId = projectResponse.get("project_id").toString();
            
            // 4. 부지정보 업데이트 API 호출, project_id update 처리
            if (siteId != null) {
                Map<String, Object> updateSiteData = new HashMap<>();
                updateSiteData.put("project_id", projectId);
                
                Map<String, Object> updateSiteResult = updateSiteInfoApi(siteId, updateSiteData);
                if (!(Boolean) updateSiteResult.get("success")) {
                    logger.warn("부지정보 업데이트 실패: {}", updateSiteResult.get("message"));
                    updateSiteResult.put("message", message+ " 생성 성공 / " + updateSiteResult.get("message"));
                    return updateSiteResult; // 부지정보 업데이트 실패 시 즉시 반환
                } else{
                    //message = message + ", 부지정보"; // 중복 메시지 추가 안함
                }
            }
            
            // 5. MinIO 업로드 API 호출 (siteFile이 있을 때만)
            Object siteFile = projectData.get("siteFile");
            if (siteFile != null) {
                Map<String, Object> minioData = new HashMap<>();
                minioData.put("site_id", siteId);
                minioData.put("project_id", projectId);
                minioData.put("site_name", projectData.getOrDefault("projectName", ""));
                minioData.put("file", siteFile); 
                
                Map<String, Object> minioResult = uploadToMinioApi(minioData);
                if (!(Boolean) minioResult.get("success")) {
                    logger.warn("MinIO 업로드 실패: {}", minioResult.get("message"));
                    minioResult.put("message", message+ " 생성 성공 / " + minioResult.get("message"));
                    return minioResult; // MinIO 업로드 실패 시 즉시 반환
                }
            } else {
                logger.debug("siteFile이 null이므로 MinIO 업로드를 건너뜁니다.");
            }
            
            // 전체 성공 결과 반환
            result.put("success", true);
            result.put("message", "messages.success.projectCreateSuccess");
            result.put("clientResult", clientResult);
            result.put("siteInfoResult", siteInfoResult);
            result.put("projectResult", projectResult);
            result.put("projectId", projectId);
        } catch (Exception e) {
            logger.error("프로젝트 정보 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 정보 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 1. 고객사 생성 API 호출
     */
    private Map<String, Object> createClientApi(Map<String, Object> clientData) {
        logger.debug("고객사 생성 API 호출");
        Map<String, Object> result = new HashMap<>();
        
        try {
            String clientUrl = authServerBaseUrl + "/api/v1/project/clients/";
            logger.debug("고객사 생성 URL: {}", clientUrl);
            
            String requestBody = JsonUtil.objectMapToJson(clientData);
            logger.debug("고객사 생성 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(clientUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("고객사 생성 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.clientCreateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "고객사 생성에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("고객사 생성 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("고객사 생성 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "고객사 생성 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 2. 부지정보 생성 API 호출
     */
    private Map<String, Object> createSiteInfoApi(Map<String, Object> siteInfoData) {
        logger.debug("부지정보 생성 API 호출");
        Map<String, Object> result = new HashMap<>();
        
        try {
            String siteInfoUrl = authServerBaseUrl + "/api/v1/project/site_info/";
            logger.debug("부지정보 생성 URL: {}", siteInfoUrl);
            
            String requestBody = JsonUtil.objectMapToJson(siteInfoData);
            logger.debug("부지정보 생성 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(siteInfoUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("부지정보 생성 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.siteInfoCreateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "부지정보 생성에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("부지정보 생성 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("부지정보 생성 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지정보 생성 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 3. 프로젝트 생성 API 호출
     */
    private Map<String, Object> createProjectApi(Map<String, Object> projectApiData) {
        logger.debug("프로젝트 생성 API 호출");
        Map<String, Object> result = new HashMap<>();
        
        try {
            String projectUrl = authServerBaseUrl + "/api/v1/project/projects/";
            logger.debug("프로젝트 생성 URL: {}", projectUrl);
            
            String requestBody = JsonUtil.objectMapToJson(projectApiData);
            logger.debug("프로젝트 생성 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(projectUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("프로젝트 생성 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.projectCreateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프로젝트 생성에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("프로젝트 생성 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("프로젝트 생성 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 생성 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 4. 부지 정보 업데이트 API 호출
     */
    private Map<String, Object> updateSiteInfoApi(String siteId, Map<String, Object> updateData) {
        logger.debug("부지 정보 업데이트 API 호출: site_id={}", siteId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String updateSiteInfoUrl = authServerBaseUrl + "/api/v1/project/site_info/" + siteId;
            logger.debug("부지 정보 업데이트 URL: {}", updateSiteInfoUrl);
            
            String requestBody = JsonUtil.objectMapToJson(updateData);
            logger.debug("부지 정보 업데이트 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.patch(updateSiteInfoUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("부지 정보 업데이트 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.siteInfoUpdateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "부지 정보 업데이트에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("부지 정보 업데이트 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("부지 정보 업데이트 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지 정보 업데이트 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 5. MinIO 업로드 API 호출
     */
    private Map<String, Object> uploadToMinioApi(Map<String, Object> uploadData) {
        logger.debug("MinIO 업로드 API 호출");
        Map<String, Object> result = new HashMap<>();
        
        try {
            String minioUrl = authServerBaseUrl + "/api/v1/minio/site_info/upload";
            logger.debug("MinIO 업로드 URL: {}", minioUrl);
            
            // MultipartFile이 있는지 확인
            Object siteFile = uploadData.get("file");
            if (siteFile instanceof org.springframework.web.multipart.MultipartFile) {
                org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) siteFile;
                if (file != null && !file.isEmpty()) {
                    // 파일이 있는 경우 multipart로 전송
                    logger.debug("파일 업로드: fileName={}, size={}", file.getOriginalFilename(), file.getSize());
                    
                    HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(minioUrl, uploadData);
                    
                    if (httpResult.isSuccess()) {
                        String responseBody = httpResult.getBody();
                        Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                        
                        logger.debug("MinIO 업로드 API 성공");
                        result.put("success", true);
                        result.put("status", httpResult.getStatus());
                        result.put("message", "messages.success.fileUploadSuccess");
                        result.put("response", parsedResponse);
                    } else {
                        result.put("success", false);
                        result.put("status", httpResult.getStatus());
                        
                        String responseBody = httpResult.getBody();
                        Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                        
                        String errorMessage = JsonUtil.extractValue(responseBody, "message");
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = JsonUtil.extractValue(responseBody, "detail");
                        }
                        
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "파일 업로드에 실패했습니다.";
                        }
                        
                        result.put("message", errorMessage);
                        result.put("response", parsedResponse);
                        
                        logger.warn("MinIO 업로드 API 실패: {}", errorMessage);
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "messages.error.noFileToUpload");
                }
            } else {
                // 파일이 없는 경우 일반 JSON으로 전송
                String requestBody = JsonUtil.objectMapToJson(uploadData);
                logger.debug("MinIO 업로드 요청 본문 (파일 없음): {}", requestBody);
                
                HttpUtil.HttpResult httpResult = HttpUtil.post(minioUrl, "application/json", requestBody);
                
                if (httpResult.isSuccess()) {
                    String responseBody = httpResult.getBody();
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    
                    logger.debug("MinIO 업로드 API 성공 (파일 없음)");
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "messages.success.dataUploadSuccess");
                    result.put("response", parsedResponse);
                } else {
                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    
                    String responseBody = httpResult.getBody();
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    
                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "데이터 업로드에 실패했습니다.";
                    }
                    
                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);
                    
                    logger.warn("MinIO 업로드 API 실패: {}", errorMessage);
                }
            }
            
        } catch (Exception e) {
            logger.error("MinIO 업로드 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "MinIO 업로드 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 6. 고객사 삭제 API 호출
     */
    private Map<String, Object> deleteClientApi(String clientId) {
        logger.debug("고객사 삭제 API 호출: client_id={}", clientId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String deleteClientUrl = authServerBaseUrl + "/api/v1/project/clients/" + clientId;
            logger.debug("고객사 삭제 URL: {}", deleteClientUrl);
            
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteClientUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("고객사 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.clientDeleteSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "고객사 삭제에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("고객사 삭제 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("고객사 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "고객사 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 7. 부지정보 삭제 API 호출
     */
    private Map<String, Object> deleteSiteInfoApi(String siteId) {
        logger.debug("부지정보 삭제 API 호출: site_id={}", siteId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String deleteSiteInfoUrl = authServerBaseUrl + "/api/v1/project/site_info/" + siteId;
            logger.debug("부지정보 삭제 URL: {}", deleteSiteInfoUrl);
            
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteSiteInfoUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("부지정보 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.siteInfoDeleteSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "부지정보 삭제에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("부지정보 삭제 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("부지정보 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지정보 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 랜덤 프로젝트 코드 생성
     */
    private String generateRandomProjectCode() {
        // 연월일 시간 정보 포함 (YYYYMMDDHHmmss 형식, 14자리)
        LocalDateTime now = LocalDateTime.now();
        String dateTimeStr = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // 언더스코어 추가 후 나머지 5자리 랜덤 문자 생성
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(dateTimeStr); // 연월일시간으로 시작
        sb.append("_"); // 언더스코어 추가
        for (int i = 0; i < 5; i++) { // 5자리 랜덤 코드 추가 (총 20자리)
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 부지정보 파일 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteSiteInfoFile(String siteId, Map<String, Object> requestData, HttpServletRequest request) {
        logger.debug("외부 인증 서버 부지정보 파일 삭제 시도: siteId={}, server={}", siteId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (siteId == null || siteId.isEmpty()) {
            logger.warn("부지정보 파일 삭제 실패: site_id가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.siteInfoIdInvalid");
            return result;
        }
        
        try {
            String newSiteId = null;
            Map<String, Object> createSiteResult = null;
            
            // 1. 부지정보 재생성 API 호출 (requestData에서 site 정보 추출)
            if (requestData != null) {

                Map<String, Object> siteInfoData = new HashMap<>();
                siteInfoData.put("site_name", requestData.getOrDefault("site_name", ""));
                siteInfoData.put("site_address", requestData.getOrDefault("site_address", ""));
                siteInfoData.put("site_capacity", 0); // 0 고정 필수값
                
                createSiteResult = createSiteInfoApi(siteInfoData);
                if (!(Boolean) createSiteResult.get("success")) {
                    logger.warn("부지정보 재생성 실패: {}", createSiteResult.get("message"));
                    result.put("success", false);
                    result.put("message", "부지정보 재생성 실패: " + createSiteResult.get("message"));
                    return result;
                }
                
                // 새로 생성된 site_id 추출
                Map<String, Object> siteResponse = (Map<String, Object>) createSiteResult.get("response");
                newSiteId = siteResponse.get("site_id").toString();
                logger.debug("부지정보 재생성 성공: newSiteId={}", newSiteId);
                
                // 2. 프로젝트 업데이트 API 호출 (requestData에서 project 정보 추출)
                if (requestData != null && requestData.containsKey("project_id")) {
                    String projectId = requestData.get("project_id").toString();
                    Map<String, Object> projectData = new HashMap<>();
                    
                    // 새로 생성된 site_id를 projectData에 추가
                    projectData.put("site_id", newSiteId);
                    
                    Map<String, Object> updateProjectResult = updateProjectApi(projectId, projectData, request);
                    if (!(Boolean) updateProjectResult.get("success")) {
                        logger.warn("프로젝트 업데이트 실패: {}", updateProjectResult.get("message"));
                        result.put("success", false);
                        result.put("message", "부지정보 재생성 성공 / 프로젝트 업데이트 실패: " + updateProjectResult.get("message"));
                        return result;
                    }
                    
                    logger.debug("프로젝트 업데이트 성공");
                    
                    // 3. 새로 생성된 부지정보에 프로젝트 ID 업데이트
                    Map<String, Object> updateSiteData = new HashMap<>();
                    updateSiteData.put("project_id", projectId);
                    
                    Map<String, Object> updateSiteResult = updateSiteInfoApi(newSiteId, updateSiteData);
                    if (!(Boolean) updateSiteResult.get("success")) {
                        logger.warn("부지정보 업데이트 실패: {}", updateSiteResult.get("message"));
                        result.put("success", false);
                        result.put("message", "부지정보 재생성 성공 / 프로젝트 업데이트 성공 / 부지정보 업데이트 실패: " + updateSiteResult.get("message"));
                        return result;
                    }
                    
                    logger.debug("부지정보 업데이트 성공");
                    
                    // 4. 기존 부지정보 삭제 API 호출
                    Map<String, Object> deleteSiteResult = deleteSiteInfoApi(siteId);
                    if (!(Boolean) deleteSiteResult.get("success")) {
                        logger.warn("기존 부지정보 삭제 실패: {}", deleteSiteResult.get("message"));
                        result.put("success", false);
                        result.put("message", "부지정보 재생성 성공 / 프로젝트 업데이트 성공 / 부지정보 업데이트 성공 / 기존 부지정보 삭제 실패: " + deleteSiteResult.get("message"));
                        return result;
                    }
                    
                    logger.debug("기존 부지정보 삭제 성공");
                } else {
                    logger.warn("프로젝트 업데이트를 위한 projectId 또는 projectData가 없습니다.");
                }
            } else {
                logger.warn("부지정보 재생성을 위한 requestData가 없습니다.");
            }
            
            // 전체 성공 결과 반환
            result.put("success", true);
            result.put("status", 200);
            result.put("message", "messages.success.siteInfoFileDeleteSuccess");
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("deletedSiteId", siteId);
            if (newSiteId != null) {
                responseData.put("newSiteId", newSiteId);
            }
            result.put("response", responseData);
            
        } catch (Exception e) {
            logger.error("부지정보 파일 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지정보 파일 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 부지정보 파일 조회 - 외부 API 호출
     */
    public Map<String, Object> downloadSiteInfoFile(String siteId) {
        logger.debug("외부 인증 서버 부지정보 파일 조회 시도: siteId={}, server={}", siteId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (siteId == null || siteId.isEmpty()) {
            logger.warn("부지정보 파일 조회 실패: site_id가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.siteInfoIdInvalid");
            return result;
        }
        
        try {
            String downloadUrl = authServerBaseUrl + "/api/v1/minio/site_info/download/" + siteId;
            logger.debug("부지정보 파일 조회 URL: {}", downloadUrl);
            
            HttpUtil.HttpResult httpResult = HttpUtil.get(downloadUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("부지정보 파일 조회 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.siteInfoFileSearchSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "부지정보 파일 조회에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("부지정보 파일 조회 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("부지정보 파일 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지정보 파일 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 프로젝트 정보 수정 - 외부 API 호출
     */
    private Map<String, Object> updateClient(String projectId, Map<String, Object> projectData, HttpServletRequest request) {
        logger.debug("프로젝트 정보 수정 API 호출: projectId={}", projectId);
        Map<String, Object> result = new HashMap<>();

        String message = "";
        
        try {
            // client_id와 site_id 확인
            String clientId = null;
            String siteId = null;
            
            if (projectData.containsKey("client_id") && projectData.get("client_id") != null) {
                clientId = projectData.get("client_id").toString();
            }
            
            if (projectData.containsKey("site_id") && projectData.get("site_id") != null) {
                siteId = projectData.get("site_id").toString();
            }
            
            // 1. 고객사 수정 API 호출 (client_id가 있는 경우에만)
            Map<String, Object> clientResult = null;
            if (clientId != null) {
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("client_name", projectData.getOrDefault("client_name", ""));
                clientData.put("homepage", projectData.getOrDefault("site_address", ""));
                clientData.put("contact_person", projectData.getOrDefault("contact_person", ""));
                clientData.put("phone_number", projectData.getOrDefault("phone_number", ""));
                clientData.put("email", projectData.getOrDefault("email", ""));
                
                clientResult = updateClientApi(clientId, clientData);
                if (!(Boolean) clientResult.get("success")) {
                    return clientResult; // 고객사 수정 실패 시 즉시 반환
                }
                message = "고객사";
            }
            
            // 2. 부지정보 수정 API 호출 (site_id가 있는 경우에만)
            Map<String, Object> siteInfoResult = null;
            if (siteId != null) {
                Map<String, Object> siteInfoData = new HashMap<>();
                siteInfoData.put("site_name", projectData.getOrDefault("project_name", ""));
                siteInfoData.put("site_address", projectData.getOrDefault("site_address", ""));
                
                siteInfoResult = updateSiteInfoApiForUpdate(siteId, siteInfoData);
                if (!(Boolean) siteInfoResult.get("success")) {
                    return siteInfoResult; // 부지정보 수정 실패 시 즉시 반환
                }
                if (message.isEmpty()) {
                    message = "부지정보";
                } else {
                    message = message + ", 부지정보";
                }
            }
            
            // 3. 프로젝트 수정 API 호출
            Map<String, Object> projectApiData = new HashMap<>();
            projectApiData.put("orderer", projectData.getOrDefault("orderer", ""));
            projectApiData.put("project_name", projectData.getOrDefault("project_name", ""));
            projectApiData.put("project_status", projectData.getOrDefault("project_status", ""));
            projectApiData.put("start_date", projectData.getOrDefault("start_date", ""));
            projectApiData.put("end_date", projectData.getOrDefault("end_date", ""));
            projectApiData.put("country_code", projectData.getOrDefault("country_code", ""));
            projectApiData.put("language_code", projectData.getOrDefault("language_code", ""));
            projectApiData.put("business_type", projectData.getOrDefault("business_type", ""));
            projectApiData.put("unit_system", projectData.getOrDefault("unit_system", ""));
            projectApiData.put("used_currency", projectData.getOrDefault("used_currency", ""));
            projectApiData.put("org_status", projectData.getOrDefault("org_status", ""));
            projectApiData.put("new_status", projectData.getOrDefault("new_status", ""));
            
            // client_id와 site_id 설정
            if (clientId != null) {
                projectApiData.put("client_id", clientId);
            }
            if (siteId != null) {
                projectApiData.put("site_id", siteId);
            }
            
            Map<String, Object> projectResult = updateProjectApi(projectId, projectApiData, request);
            if (!(Boolean) projectResult.get("success")) {
                if (!message.isEmpty()) {
                    projectResult.put("message", message + " 수정 성공 / " + projectResult.get("message"));
                }
                return projectResult; // 프로젝트 수정 실패 시 즉시 반환
            } else {
                if (message.isEmpty()) {
                    message = "프로젝트";
                } else {
                    message = message + ", 프로젝트";
                }
            }
            
            // 4. MinIO 업로드 API 호출 (siteFile이 있을 때만)
            Object siteFile = projectData.get("siteFile");
            if (siteFile != null) {
                Map<String, Object> minioData = new HashMap<>();
                if (siteId != null) {
                    minioData.put("site_id", siteId);
                }
                minioData.put("project_id", projectId);
                minioData.put("site_name", projectData.getOrDefault("project_name", ""));
                minioData.put("file", siteFile); 
                
                Map<String, Object> minioResult = uploadToMinioApi(minioData);
                if (!(Boolean) minioResult.get("success")) {
                    logger.warn("MinIO 업로드 실패: {}", minioResult.get("message"));
                    minioResult.put("message", message + " 수정 성공 / " + minioResult.get("message"));
                    return minioResult; // MinIO 업로드 실패 시 즉시 반환
                }
            } else {
                logger.debug("siteFile이 null이므로 MinIO 업로드를 건너뜁니다.");
            }
            
            // 전체 성공 결과 반환
            result.put("success", true);
            result.put("message", "messages.success.projectInfoUpdateSuccess");
            if (clientResult != null) {
                result.put("clientResult", clientResult);
            }
            if (siteInfoResult != null) {
                result.put("siteInfoResult", siteInfoResult);
            }
            result.put("projectResult", projectResult);
            
        } catch (Exception e) {
            logger.error("프로젝트 정보 수정 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 정보 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 1. 고객사 수정 API 호출
     */
    private Map<String, Object> updateClientApi(String projectId, Map<String, Object> clientData) {
        logger.debug("고객사 수정 API 호출: projectId={}", projectId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String clientUrl = authServerBaseUrl + "/api/v1/project/clients/" + projectId;
            logger.debug("고객사 수정 URL: {}", clientUrl);
            
            String requestBody = JsonUtil.objectMapToJson(clientData);
            logger.debug("고객사 수정 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.patch(clientUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("고객사 수정 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.clientUpdateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "고객사 수정에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("고객사 수정 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("고객사 수정 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "고객사 수정 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 2. 부지정보 수정 API 호출 (수정용)
     */
    public Map<String, Object> updateSiteInfoApiForUpdate(String projectId, Map<String, Object> siteInfoData) {
        logger.debug("부지정보 수정 API 호출 (수정용): projectId={}", projectId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String siteInfoUrl = authServerBaseUrl + "/api/v1/project/site_info/" + projectId;
            logger.debug("부지정보 수정 URL: {}", siteInfoUrl);
            
            String requestBody = JsonUtil.objectMapToJson(siteInfoData);
            logger.debug("부지정보 수정 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.patch(siteInfoUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("부지정보 수정 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.siteInfoUpdateSuccess");
                result.put("response", parsedResponse);
            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "부지정보 업데이트에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("부지정보 업데이트 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("부지정보 업데이트 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "부지정보 업데이트 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 5. 프로젝트 단계 상태 API 호출
     */
    private Map<String, Object> updateProjectPhaseStatusApi(String projectId, Map<String, Object> projectData, HttpServletRequest request) {
        logger.debug("프로젝트 단계 상태 API 호출: projectId={}", projectId);
        Map<String, Object> result = null;
        
        try {
            String phaseStatusUrl = authServerBaseUrl + "/api/v1/project/project_phase_status/";
            logger.debug("프로젝트 단계 상태 API 호출 URL: {}", phaseStatusUrl);
            
            Map<String, Object> phaseStatusData = new HashMap<>();
            // 기존 상태와 새로운 상태 추출
            String orgStatus = projectData.containsKey("org_status") ? projectData.get("org_status").toString() : null;
            String newStatus = projectData.containsKey("new_status") ? projectData.get("new_status").toString() : null;

            phaseStatusData.put("project_id", projectId);
            phaseStatusData.put("status", newStatus);
            phaseStatusData.put("comments", orgStatus + " -> " + newStatus);
            phaseStatusData.put("created_by", request.getHeader("user_id"));
            
            String requestBody = JsonUtil.objectMapToJson(phaseStatusData);
            logger.debug("프로젝트 단계 상태 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(phaseStatusUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                logger.debug("프로젝트 단계 상태 API 호출 성공");
                result = parsedResponse;
            } else {
                logger.warn("프로젝트 단계 상태 API 호출 실패: status={}, body={}", 
                    httpResult.getStatus(), httpResult.getBody());
                // 실패해도 전체 프로세스는 성공으로 처리 (경고만 기록)
            }
        } catch (Exception e) {
            logger.error("프로젝트 단계 상태 API 호출 중 오류 발생", e);
            // 예외 발생해도 전체 프로세스는 성공으로 처리 (경고만 기록)
        }
        
        return result;
    }

    /**
     * 3. 프로젝트 수정 API 호출
     */
    public Map<String, Object> updateProjectApi(String projectId, Map<String, Object> projectApiData, HttpServletRequest request) {
        logger.debug("프로젝트 수정 API 호출: projectId={}", projectId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String projectUrl = authServerBaseUrl + "/api/v1/project/projects/" + projectId;
            logger.debug("프로젝트 수정 URL: {}", projectUrl);
            
            String requestBody = JsonUtil.objectMapToJson(projectApiData);
            logger.debug("프로젝트 수정 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.patch(projectUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("프로젝트 수정 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.projectUpdateSuccess");
                result.put("response", parsedResponse);
                
                // 프로젝트 단계 상태 API 호출 (project_status가 변경된 경우)
                String newStatus = projectApiData.containsKey("new_status") ? projectApiData.get("new_status").toString() : null;
                String orgStatus = projectApiData.containsKey("org_status") ? projectApiData.get("org_status").toString() : null;
                
                if (orgStatus != null && !orgStatus.equals(newStatus)) {
                    Map<String, Object> phaseStatusResult = updateProjectPhaseStatusApi(projectId, projectApiData, request);
                    if (phaseStatusResult != null) {
                        result.put("phaseStatusResult", phaseStatusResult);
                    }
                }

            } else {
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "프로젝트 수정에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("프로젝트 수정 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("프로젝트 수정 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프로젝트 수정 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 추천프로젝트 생성 - 외부 API 호출
     */
    public Map<String, Object> createRecommendationProject(Map<String, Object> recommendationData, HttpServletRequest request) {
        logger.debug("외부 인증 서버 추천프로젝트 생성 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (recommendationData == null) {
            logger.warn("추천프로젝트 생성 실패: 추천프로젝트 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.recommendationDataNull");
            return result;
        }
        
        try {
            String recommendationUrl = authServerBaseUrl + "/api/v1/project/project_recommended/";
            logger.debug("추천프로젝트 생성 URL: {}", recommendationUrl);

            // 추천프로젝트 데이터 구성
            Map<String, Object> projectApiData = new HashMap<>();
            projectApiData.put("project_id", recommendationData.getOrDefault("project_id", ""));
            //projectApiData.put("recommender_id", recommendationData.getOrDefault("recommender_id", ""));
            
            // 헤더에서 authUserId 값을 가져와서 approver_id에 설정
            String authUserId = request.getHeader("user_id");
            if (authUserId != null && !authUserId.isEmpty()) {
                projectApiData.put("approver_id", authUserId);
            } else {
                projectApiData.put("approver_id", recommendationData.getOrDefault("approver_id", ""));
            }
            
            //필요한가???
            projectApiData.put("status", "SC_APPROVED"); //공통 코드 (승인=SC_APPROVED) 하드코딩
            projectApiData.put("recommendation_reason", recommendationData.getOrDefault("recommendation_reason", ""));
            projectApiData.put("approval_reason", recommendationData.getOrDefault("approval_reason", ""));

            String requestBody = JsonUtil.objectMapToJson(projectApiData);
            logger.debug("추천프로젝트 생성 요청 본문: {}", requestBody);
            
            HttpUtil.HttpResult httpResult = HttpUtil.post(recommendationUrl, "application/json", requestBody);
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("추천프로젝트 생성 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.recommendationCreateSuccess");
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
                    errorMessage = "추천프로젝트 생성에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("추천프로젝트 생성 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("추천프로젝트 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "추천프로젝트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 추천프로젝트 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteRecommendationProject(String recommendationId) {
        logger.debug("외부 인증 서버 추천프로젝트 삭제 시도: recommendationId={}, server={}", recommendationId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (recommendationId == null || recommendationId.isEmpty()) {
            logger.warn("추천프로젝트 삭제 실패: 추천프로젝트 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.recommendationIdInvalid");
            return result;
        }
        
        try {
            String deleteUrl = authServerBaseUrl + "/api/v1/project/project_recommended/" + recommendationId;
            logger.debug("추천프로젝트 삭제 URL: {}", deleteUrl);
            
            HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
            
            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                
                logger.debug("추천프로젝트 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.recommendationDeleteSuccess");
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
                    errorMessage = "추천프로젝트 삭제에 실패했습니다.";
                }
                
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
                
                logger.warn("추천프로젝트 삭제 API 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("추천프로젝트 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "추천프로젝트 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
} 