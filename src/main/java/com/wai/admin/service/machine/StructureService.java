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
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class StructureService {

    private static final Logger logger = LoggerFactory.getLogger(StructureService.class);

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
     * 구조물 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getStructureList(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 구조물 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String structureUrl = authServerBaseUrl + "/api/v1/structure/structures/search";
        logger.debug("구조물 목록 조회 URL: {}", structureUrl);
        
        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();
        
        // 기본값 설정
        requestMap.put("root_structure_type", searchParams.getOrDefault("root_structure_type", ""));
        requestMap.put("structure_type", searchParams.getOrDefault("structure_type", ""));
        requestMap.put("unit_system_code", searchParams.getOrDefault("unit", ""));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("구조물 목록 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(structureUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("구조물 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("구조물 목록 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("구조물 목록 조회 성공");
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
            
            logger.warn("외부 인증 서버 구조물 목록 조회 실패: {}", errorMessage);
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
     * 구조체 파일 업로드 - 외부 API 호출 (Multipart)
     */
    public Map<String, Object> uploadStructureFiles(String structureType, 
            org.springframework.web.multipart.MultipartFile allFile) {
        logger.debug("외부 인증 서버 구조체 파일 업로드 시도: server={}, structureType={}", authServerBaseUrl, structureType);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String uploadUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/type/" + structureType;
        logger.debug("구조체 파일 업로드 URL: {}", uploadUrl);
        
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
                logger.debug("구조체 파일 업로드 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "구조체 파일 업로드가 완료되었습니다.");
            } else {
                logger.error("구조체 파일 업로드 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("구조체 파일 업로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "구조체 파일 업로드에 실패했습니다.";
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
     * 구조체 비고 업데이트 - 외부 API 호출 (PATCH)
     */
    public Map<String, Object> updateStructureRemark(String structureId, String remarks) {
        logger.debug("외부 인증 서버 구조체 업데이트 시도: server={}, structureId={}", authServerBaseUrl, structureId);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String updateUrl = authServerBaseUrl + "/api/v1/structure/structures/" + structureId;
        logger.debug("구조체 업데이트 URL: {}", updateUrl);
        
        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("description", remarks);
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("구조체 업데이트 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 PATCH 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.patch(updateUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("구조체 업데이트 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "구조체 업데이트가 완료되었습니다.");
            } else {
                logger.error("구조체 업데이트 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("구조체 업데이트 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "구조체 업데이트에 실패했습니다.";
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
     * 구조체 삭제 - 외부 API 호출 (DELETE) 및 추가 처리
     */
    public Map<String, Object> deleteStructure(String structureId, Map<String, Object> deleteParams) {
        logger.debug("구조체 삭제 처리 시작: structureId={}", structureId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 메인 구조체 삭제 API 호출
            Map<String, Object> deleteResult = deleteStructureFromAPI(structureId);
            
            if ((Boolean) deleteResult.get("success")) {
                // 2. 구조체 삭제 성공 시 파일들 순차적 삭제
                boolean allFilesDeleted = true;
                StringBuilder errorMessages = new StringBuilder();
                
                // DTD 모델 파일 삭제
                String dtdxModelFileId = (String) deleteParams.get("dtdx_model_file_id");
                if (dtdxModelFileId != null && !dtdxModelFileId.trim().isEmpty()) {
                    Map<String, Object> dtdxResult = deleteModelFile(structureId, dtdxModelFileId);
                    if (!(Boolean) dtdxResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("DTD 모델 파일 삭제 실패; ");
                    }
                }
                
                // RVT 파일 삭제
                String rvtModelFileId = (String) deleteParams.get("rvt_model_file_id");
                if (rvtModelFileId != null && !rvtModelFileId.trim().isEmpty()) {
                    Map<String, Object> rvtResult = deleteRvtFile(structureId, rvtModelFileId);
                    if (!(Boolean) rvtResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("RVT 파일 삭제 실패; ");
                    }
                }
                
                // 썸네일 파일 삭제
                String thumbnailSymbolId = (String) deleteParams.get("thumbnail_symbol_id");
                if (thumbnailSymbolId != null && !thumbnailSymbolId.trim().isEmpty()) {
                    Map<String, Object> thumbResult = deleteThumbnailFile(structureId, thumbnailSymbolId);
                    if (!(Boolean) thumbResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("썸네일 파일 삭제 실패; ");
                    }
                }
                
                // 공식 파일 삭제
                String formulaId = (String) deleteParams.get("formula_id");
                if (formulaId != null && !formulaId.trim().isEmpty()) {
                    Map<String, Object> formulaResult = deleteFormulaFile(structureId, formulaId);
                    if (!(Boolean) formulaResult.get("success")) {
                        allFilesDeleted = false;
                        errorMessages.append("공식 파일 삭제 실패; ");
                    }
                }
                
                result.put("success", true);
                if (allFilesDeleted) {
                    result.put("message", "구조체와 모든 관련 파일이 성공적으로 삭제되었습니다.");
                } else {
                    result.put("message", "구조체는 삭제되었지만 일부 파일 삭제에 실패했습니다: " + errorMessages.toString());
                }
                result.put("data", deleteResult.get("data"));
                
                logger.debug("구조체 삭제 완료: structureId={}, allFilesDeleted={}", structureId, allFilesDeleted);
            } else {
                // 구조체 삭제 실패
                result = deleteResult;
                logger.error("구조체 삭제 실패: structureId={}, message={}", structureId, deleteResult.get("message"));
            }
            
        } catch (Exception e) {
            logger.error("구조체 삭제 처리 중 예외 발생: structureId={}, error={}", structureId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "구조체 삭제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 구조체 삭제 API 호출 - 실제 DELETE 요청
     */
    private Map<String, Object> deleteStructureFromAPI(String structureId) {
        logger.debug("외부 인증 서버 구조체 삭제 시도: server={}, structureId={}", authServerBaseUrl, structureId);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String deleteUrl = authServerBaseUrl + "/api/v1/structure/structures/" + structureId;
        logger.debug("구조체 삭제 URL: {}", deleteUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // DELETE 요청 성공 - 응답 본문이 없거나 간단할 수 있음
            String responseBody = httpResult.getBody();
            logger.debug("구조체 삭제 성공: statusCode={}, responseBody={}", httpResult.getStatus(), responseBody);
            
            result.put("success", true);
            result.put("message", "구조체 삭제가 완료되었습니다.");
            
            // 응답 본문이 있는 경우에만 파싱
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } else {
            // 실패한 응답 처리
            logger.error("구조체 삭제 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "구조체 삭제에 실패했습니다.";
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
     * DTD 모델 파일 삭제
     */
    private Map<String, Object> deleteModelFile(String structureId, String modelFileId) {
        logger.debug("DTD 모델 파일 삭제 시도: structureId={}, modelFileId={}", structureId, modelFileId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/" + structureId + "/model/" + modelFileId;
        return executeFileDelete(deleteUrl, "DTD 모델 파일");
    }

    /**
     * RVT 파일 삭제
     */
    private Map<String, Object> deleteRvtFile(String structureId, String rvtFileId) {
        logger.debug("RVT 파일 삭제 시도: structureId={}, rvtFileId={}", structureId, rvtFileId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/" + structureId + "/rvt/" + rvtFileId;
        return executeFileDelete(deleteUrl, "RVT 파일");
    }

    /**
     * 썸네일 파일 삭제
     */
    private Map<String, Object> deleteThumbnailFile(String structureId, String thumbnailId) {
        logger.debug("썸네일 파일 삭제 시도: structureId={}, thumbnailId={}", structureId, thumbnailId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/" + structureId + "/thumb/" + thumbnailId;
        return executeFileDelete(deleteUrl, "썸네일 파일");
    }

    /**
     * 공식 파일 삭제
     */
    private Map<String, Object> deleteFormulaFile(String structureId, String formulaId) {
        logger.debug("공식 파일 삭제 시도: structureId={}, formulaId={}", structureId, formulaId);
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/" + structureId + "/formula/" + formulaId;
        return executeFileDelete(deleteUrl, "공식 파일");
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
     * 구조체 공식 검색 - 외부 API 호출 (POST 방식)
     */
    public Map<String, Object> searchStructureFormula(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 구조체 공식 검색 시도: server={}, searchParams={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String structureType = (String) searchParams.get("structure_type");
        String formulaSearchUrl = authServerBaseUrl + "/api/v1/common/with_formula/structure/search_history/"+structureType;
        logger.debug("구조체 공식 검색 URL: {}", formulaSearchUrl);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(formulaSearchUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("구조체 공식 검색 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "구조체 공식 검색이 완료되었습니다.");
            } else {
                logger.error("구조체 공식 검색 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("구조체 공식 검색 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "구조체 공식 검색에 실패했습니다.";
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
     * 구조체 공식 삭제 - 외부 API 호출 (DELETE 방식)
     */
    public Map<String, Object> deleteStructureFormula(String structureId, String formulaId) {
        logger.debug("외부 인증 서버 구조체 공식 삭제 시도: server={}, structureId={}, formulaId={}", authServerBaseUrl, structureId, formulaId);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String deleteUrl = authServerBaseUrl + "/api/v1/minio/structure/uploadzip/" + structureId + "/formula/" + formulaId;
        logger.debug("구조체 공식 삭제 URL: {}", deleteUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.delete(deleteUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // DELETE 요청 성공 - 응답 본문이 없거나 간단할 수 있음
            String responseBody = httpResult.getBody();
            logger.debug("구조체 공식 삭제 성공: statusCode={}, responseBody={}", httpResult.getStatus(), responseBody);
            
            result.put("success", true);
            result.put("message", "구조체 공식 삭제가 완료되었습니다.");
            
            // 응답 본문이 있는 경우에만 파싱
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    result.put("data", parsedResponse);
                }
            }
        } else {
            // 실패한 응답 처리
            logger.error("구조체 공식 삭제 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);
            
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "구조체 공식 삭제에 실패했습니다.";
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
     * 구조체 업데이트 - 외부 API 호출 (PATCH) 및 추가 처리
     */
    public Map<String, Object> updateStructure(String structureId, Map<String, Object> updateParams, 
            org.springframework.web.multipart.MultipartFile formulaFile,
            org.springframework.web.multipart.MultipartFile dtdModelFile,
            org.springframework.web.multipart.MultipartFile thumbnailFile,
            org.springframework.web.multipart.MultipartFile revitModelFile) {
        logger.debug("구조체 업데이트 처리 시작: structureId={}, updateParams={}", structureId, updateParams);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 메인 구조체 업데이트 API 호출 (remarks 업데이트)
            String remarks = (String) updateParams.getOrDefault("description", "");
            Map<String, Object> updateResult = updateStructureRemark(structureId, remarks);
            
            if ((Boolean) updateResult.get("success")) {
                // 2. 구조체 업데이트 성공 시 파일 업데이트 처리
                boolean allUpdatesSuccessful = true;
                StringBuilder errorMessages = new StringBuilder();
                
                // 파일들이 있는 경우 각각 업데이트 처리
                if (formulaFile != null && !formulaFile.isEmpty()) {
                    Map<String, Object> formulaResult = updateFormulaFile(structureId, formulaFile);
                    if (!(Boolean) formulaResult.get("success")) {
                        allUpdatesSuccessful = false;
                        errorMessages.append("공식 파일 업데이트 실패; ");
                    }
                }
                
                if (dtdModelFile != null && !dtdModelFile.isEmpty()) {
                    Map<String, Object> dtdResult = updateDtdModelFile(structureId, dtdModelFile);
                    if (!(Boolean) dtdResult.get("success")) {
                        allUpdatesSuccessful = false;
                        errorMessages.append("DTD 모델 파일 업데이트 실패; ");
                    }
                }
                
                if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                    Map<String, Object> thumbnailResult = updateThumbnailFile(structureId, thumbnailFile);
                    if (!(Boolean) thumbnailResult.get("success")) {
                        allUpdatesSuccessful = false;
                        errorMessages.append("썸네일 파일 업데이트 실패; ");
                    }
                }
                
                if (revitModelFile != null && !revitModelFile.isEmpty()) {
                    Map<String, Object> revitResult = updateRevitModelFile(structureId, revitModelFile);
                    if (!(Boolean) revitResult.get("success")) {
                        allUpdatesSuccessful = false;
                        errorMessages.append("REVIT 모델 파일 업데이트 실패; ");
                    }
                }
                
                result.put("success", true);
                if (allUpdatesSuccessful) {
                    result.put("message", "구조체와 모든 파일이 성공적으로 업데이트되었습니다.");
                } else {
                    result.put("message", "구조체는 업데이트되었지만 일부 파일 업데이트에 실패했습니다: " + errorMessages.toString());
                }
                result.put("data", updateResult.get("data"));
                
                logger.debug("구조체 업데이트 완료: structureId={}, allUpdatesSuccessful={}", structureId, allUpdatesSuccessful);
            } else {
                // 구조체 업데이트 실패
                result = updateResult;
                logger.error("구조체 업데이트 실패: structureId={}, message={}", structureId, updateResult.get("message"));
            }
            
        } catch (Exception e) {
            logger.error("구조체 업데이트 처리 중 예외 발생: structureId={}, error={}", structureId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "구조체 업데이트 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }


    /**
     * 공식 파일 업데이트
     */
    private Map<String, Object> updateFormulaFile(String structureId, org.springframework.web.multipart.MultipartFile formulaFile) {
        logger.debug("공식 파일 업데이트 시도: structureId={}", structureId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/structure/patchzip/" + structureId + "/formula";
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("file", formulaFile);
        
        return executeFilePatch(updateUrl, formData, "공식 파일");
    }

    /**
     * DTD 모델 파일 업데이트
     */
    private Map<String, Object> updateDtdModelFile(String structureId, org.springframework.web.multipart.MultipartFile dtdModelFile) {
        logger.debug("DTD 모델 파일 업데이트 시도: structureId={}", structureId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/structure/patchzip/" + structureId + "/model";
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("file", dtdModelFile);
        
        return executeFilePatch(updateUrl, formData, "DTD 모델 파일");
    }

    /**
     * 썸네일 파일 업데이트
     */
    private Map<String, Object> updateThumbnailFile(String structureId, org.springframework.web.multipart.MultipartFile thumbnailFile) {
        logger.debug("썸네일 파일 업데이트 시도: structureId={}", structureId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/structure/patchzip/" + structureId + "/thumb";
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("file", thumbnailFile);
        
        return executeFilePatch(updateUrl, formData, "썸네일 파일");
    }

    /**
     * REVIT 모델 파일 업데이트
     */
    private Map<String, Object> updateRevitModelFile(String structureId, org.springframework.web.multipart.MultipartFile revitModelFile) {
        logger.debug("REVIT 모델 파일 업데이트 시도: structureId={}", structureId);
        String updateUrl = authServerBaseUrl + "/api/v1/minio/structure/patchzip/" + structureId + "/rvt";
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("file", revitModelFile);
        
        return executeFilePatch(updateUrl, formData, "REVIT 모델 파일");
    }

    /**
     * 파일 PATCH 요청 공통 메서드
     */
    private Map<String, Object> executeFilePatch(String patchUrl, Map<String, Object> formData, String fileType) {
        Map<String, Object> result = new HashMap<>();
        logger.debug("{} 업데이트 URL: {}", fileType, patchUrl);
        
        try {
            HttpUtil.HttpResult httpResult = HttpUtil.patchMultipart(patchUrl, formData);
            
            if (httpResult.isSuccess()) {
                logger.debug("{} 업데이트 성공: statusCode={}", fileType, httpResult.getStatus());
                result.put("success", true);
                result.put("message", fileType + " 업데이트가 완료되었습니다.");
                
                String responseBody = httpResult.getBody();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        result.put("data", parsedResponse);
                    }
                }
            } else {
                logger.error("{} 업데이트 실패: statusCode={}, body={}", fileType, httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                
                String errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = fileType + " 업데이트에 실패했습니다.";
                }
                result.put("message", errorMessage);
                result.put("statusCode", httpResult.getStatus());
            }
        } catch (Exception e) {
            logger.error("{} 업데이트 중 예외 발생: {}", fileType, e.getMessage(), e);
            result.put("success", false);
            result.put("message", fileType + " 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
} 