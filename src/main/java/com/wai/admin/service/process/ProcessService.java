package com.wai.admin.service.process;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

@Service
public class ProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);

    // 외부 api 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    public Map<String, Object> getProcessById(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("processId", id);
        data.put("processName", "프로세스명");
        data.put("processType", "프로세스 유형");
        data.put("status", "실행중");
        return data;
    }

    // Create Process Masters
    public Map<String, Object> createProcess(Map<String, Object> processData,
            org.springframework.web.multipart.MultipartFile siteFile) {
        Map<String, Object> result = new HashMap<>();

        if (processData == null) {
            logger.warn("프로세스 등록 실패: 프로세스 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidProcessData");
            return result;
        }

        // siteFile이 있는 경우 심볼 생성 수행
        String symbolId = null;
        if (siteFile != null && !siteFile.isEmpty()) {
            logger.debug("심볼 파일이 제공됨, 심볼 생성 수행: fileName={}, size={}", siteFile.getOriginalFilename(),
                    siteFile.getSize());

            // processData에서 심볼 관련 데이터 추출
            Map<String, Object> symbolData = new HashMap<>();

            // symbol_name과 symbol_code가 비어있으면 기본값 생성
            String symbolName = (String) processData.getOrDefault("process_name", "");
            String symbolCode = (String) processData.getOrDefault("process_code", "");

            symbolData.put("symbol_name", symbolName);
            symbolData.put("symbol_code", symbolCode);
            symbolData.put("symbol_type", processData.getOrDefault("symbol_type", "PROCESS"));
            symbolData.put("siteFile", siteFile);

            logger.debug("심볼 생성 데이터: symbol_name={}, symbol_code={}, symbol_type={}",
                    symbolName, symbolCode, processData.getOrDefault("symbol_type", "PROCESS"));

            // createSymbol 호출하여 symbol_id 획득
            Map<String, Object> symbolResult = createSymbol(symbolData);
            if ((Boolean) symbolResult.get("success")) {
                symbolId = (String) symbolResult.get("symbol_id");
                logger.debug("심볼 생성 성공, symbol_id: {}", symbolId);
            } else {
                logger.warn("심볼 생성 실패: {}", symbolResult.get("message"));
                // 심볼 생성 실패해도 프로세스는 생성 계속 진행
                // result에 심볼 생성 실패 정보를 저장하지만 프로세스 생성은 계속
                result.put("symbol_creation_failed", true);
                result.put("symbol_error_message", symbolResult.get("message"));
            }
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/";

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정 - 빈 값이 아닌 경우에만 추가
        String processCode = (String) processData.getOrDefault("process_code", "");
        if (processCode != null && !processCode.trim().isEmpty()) {
            requestMap.put("process_code", processCode);
        }

        String processName = (String) processData.getOrDefault("process_name", "");
        if (processName != null && !processName.trim().isEmpty()) {
            requestMap.put("process_name", processName);
        }

        String processTypeCode = (String) processData.getOrDefault("process_type_code", "");
        if (processTypeCode != null && !processTypeCode.trim().isEmpty()) {
            requestMap.put("process_type_code", processTypeCode);
        }

        String processCategory = (String) processData.getOrDefault("process_category", "");
        if (processCategory != null && !processCategory.trim().isEmpty()) {
            requestMap.put("process_category", processCategory);
        }

        String processDescription = (String) processData.getOrDefault("process_description", "");
        if (processDescription != null && !processDescription.trim().isEmpty()) {
            requestMap.put("process_description", processDescription);
        }

        String technologyModeCode = (String) processData.getOrDefault("technology_mode_code", "");
        if (technologyModeCode != null && !technologyModeCode.trim().isEmpty()) {
            requestMap.put("technology_mode_code", technologyModeCode);
        }

        Object typicalRemovalEfficiency = processData.getOrDefault("typical_removal_efficiency", null);
        if (typicalRemovalEfficiency != null) {
            requestMap.put("typical_removal_efficiency", typicalRemovalEfficiency);
        }

        Object typicalDetentionTime = processData.getOrDefault("typical_detention_time", null);
        if (typicalDetentionTime != null) {
            requestMap.put("typical_detention_time", typicalDetentionTime);
        }

        Object defaultFormulaSet = processData.getOrDefault("default_formula_set", null);
        if (defaultFormulaSet != null) {
            requestMap.put("default_formula_set", defaultFormulaSet);
        }

        Object isActive = processData.getOrDefault("is_active", null);
        if (isActive != null) {
            requestMap.put("is_active", isActive);
        }

        Object pocInputCount = processData.getOrDefault("poc_input_count", null);
        if (pocInputCount != null) {
            requestMap.put("poc_input_count", pocInputCount);
        }

        Object pocOutputCount = processData.getOrDefault("poc_output_count", null);
        if (pocOutputCount != null) {
            requestMap.put("poc_output_count", pocOutputCount);
        }

        String workflowSvgUri = (String) processData.getOrDefault("workflow_svg_uri", "");
        if (workflowSvgUri != null && !workflowSvgUri.trim().isEmpty()) {
            requestMap.put("workflow_svg_uri", workflowSvgUri);
        }

        Object flowDiagramData = processData.getOrDefault("flow_diagram_data", null);
        if (flowDiagramData != null) {
            requestMap.put("flow_diagram_data", flowDiagramData);
        }

        // symbol_id 설정: 새로 생성된 symbol_id가 있으면 사용, 없으면 기존 값 사용
        String existingSymbolId = (String) processData.getOrDefault("symbol_id", "");
        if (!isEmptySymbolId(symbolId)) {
            requestMap.put("symbol_id", symbolId);
        } else if (!isEmptySymbolId(existingSymbolId)) {
            requestMap.put("symbol_id", existingSymbolId);
        }

        // language_code와 unit_system_code 추가 (기본값 설정)
        String languageCode = (String) processData.getOrDefault("language_code", "ko");
        requestMap.put("language_code", languageCode);

        String unitSystemCode = (String) processData.getOrDefault("unit_system_code", "METRIC");
        requestMap.put("unit_system_code", unitSystemCode);

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답코드가 201인 경우에도 응답 내용 검증
            if (httpResult.getStatus() == 201) {
                // 응답 내용 검증 - 실제 생성된 데이터가 있는지 확인
                boolean isValidResponse = isValidCreateResponse(parsedResponse);

                if (isValidResponse) {
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "messages.success.processCreateSuccess");
                    result.put("response", parsedResponse);

                    // process_id 추출하여 result에 추가
                    String processId = JsonUtil.extractValue(responseBody, "process_id");
                    if (processId == null || processId.isEmpty()) {
                        processId = JsonUtil.extractValue(responseBody, "id");
                    }
                    if (processId != null && !processId.isEmpty()) {
                        result.put("process_id", processId);
                        logger.debug("프로세스 생성 성공, process_id: {}", processId);
                    }

                    // 심볼 생성 실패 정보가 있으면 추가
                    if (result.containsKey("symbol_creation_failed")
                            && (Boolean) result.get("symbol_creation_failed")) {
                        result.put("symbol_warning", "심볼 생성에 실패했습니다: " + result.get("symbol_error_message"));
                        logger.warn("프로세스는 생성되었지만 심볼 생성 실패: {}", result.get("symbol_error_message"));
                    }
                } else {
                    // 응답 코드는 성공이지만 내용에 문제가 있는 경우
                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "messages.error.processCreateFail";
                    }

                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("프로세스 등록 실패 (응답 내용 검증 실패): {}", errorMessage);
                    return result;
                }
            } else {
                // 응답코드가 201이 아닌 경우 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.processCreateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프로세스 등록 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
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
                errorMessage = "프로세스 등록에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            logger.error("프로세스 등록 API 실패 상세 정보:");
            logger.error("- API URL: {}", apiUrl);
            logger.error("- HTTP Status: {}", statusCode);
            logger.error("- Request Body: {}", requestBody);
            logger.error("- Response Body: {}", responseBody);
            logger.error("- Error Message: {}", errorMessage);

            // process_code 중복 오류인 경우 추가 정보 로깅 및 실제 데이터 존재 여부 확인
            if (errorMessage != null && errorMessage.contains("process_code")) {
                // 이미 선언된 processCode 변수 사용
                logger.error("Process Code 관련 오류 추가 정보:");
                logger.error("- 요청한 process_code: {}", processCode);
                logger.error("- 중복 체크 대상: process_code 필드");

                // 실제 데이터 존재 여부 확인
                if (processCode != null && !processCode.trim().isEmpty()) {
                    try {
                        boolean actuallyExists = checkProcessCodeExists(processCode);
                        if (actuallyExists) {
                            logger.error("- 실제 데이터 존재: YES - process_code '{}' 가 데이터베이스에 존재합니다.", processCode);
                            logger.debug("해결 방안: 1) 다른 process_code 사용, 2) 기존 데이터 수정/삭제 후 재시도");
                        } else {
                            logger.error("- 실제 데이터 존재: NO - process_code '{}' 가 데이터베이스에 존재하지 않습니다.", processCode);
                            logger.error("⚠️  중복 오류가 발생했지만 실제 데이터가 없는 상황입니다.");
                            logger.debug("해결 방안: 1) 데이터베이스 상태 확인, 2) 캐시 초기화, 3) API 서버 재시작");
                        }
                    } catch (Exception e) {
                        logger.error("- 데이터 존재 여부 확인 중 오류 발생: {}", e.getMessage());
                        logger.debug("해결 방안: 1) 다른 process_code 사용, 2) 시스템 관리자 문의");
                    }
                }
            }

            logger.warn("외부 인증 서버 프로세스 등록 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * process_code가 실제로 존재하는지 확인하는 메서드
     * 
     * @param processCode 확인할 process_code
     * @return 존재 여부
     */
    private boolean checkProcessCodeExists(String processCode) {
        try {
            // getAllProcesses를 사용하여 process_code로 검색
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("search_field", "process_code");
            searchParams.put("search_value", processCode);
            searchParams.put("page", 1);
            searchParams.put("page_size", 1);

            Map<String, Object> searchResult = getAllProcesses(searchParams);

            if ((Boolean) searchResult.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> processes = (List<Map<String, Object>>) searchResult.get("response");

                if (processes != null && !processes.isEmpty()) {
                    // 정확히 일치하는 process_code가 있는지 확인
                    for (Map<String, Object> process : processes) {
                        String existingCode = (String) process.get("process_code");
                        if (processCode.equals(existingCode)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            logger.warn("process_code 존재 여부 확인 중 예외 발생: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 프로세스 등록 응답이 유효한지 검증하는 메서드
     * 
     * @param response 외부 API 응답 데이터
     * @return 유효성 여부
     */
    private boolean isValidCreateResponse(Map<String, Object> response) {
        logger.debug("프로세스 등록 응답 검증 시작: {}", response);

        if (response == null) {
            logger.warn("프로세스 등록 응답이 null");
            return false;
        }

        // 응답에 에러 메시지가 포함되어 있는지 확인
        if (response.containsKey("error") || response.containsKey("errors")) {
            logger.warn("프로세스 등록 응답에 에러 포함: {}", response);
            return false;
        }

        // has_warnings 필드가 true인지 확인 (계산식 중복 등)
        Object hasWarningsObj = response.get("has_warnings");
        if (hasWarningsObj != null && Boolean.TRUE.equals(hasWarningsObj)) {
            logger.warn("프로세스 등록 응답에 경고 포함 - 중복 파일 또는 기타 문제: {}", response);
            return false;
        }

        // 응답에 status 필드가 있고 'error' 또는 'skipped'인지 확인
        Object statusObj = response.get("status");
        if (statusObj != null) {
            String status = statusObj.toString();
            if ("error".equals(status)) {
                logger.warn("프로세스 등록 응답에 status: error 포함: {}", response);
                return false;
            } else if ("skipped".equals(status)) {
                logger.warn("프로세스 등록 응답에 status: skipped 포함 - 중복 파일: {}", response);
                return false;
            }
        }

        // 응답에 message 필드가 있고 에러 관련 메시지인지 확인
        Object messageObj = response.get("message");
        if (messageObj != null) {
            String message = messageObj.toString();
            logger.debug("프로세스 등록 응답 message 필드: {}", message);
            if (message.toLowerCase().contains("error") ||
                    message.toLowerCase().contains("fail") ||
                    message.toLowerCase().contains("실패") ||
                    message.toLowerCase().contains("이미 사용 중") ||
                    message.toLowerCase().contains("중복")) {
                logger.warn("프로세스 등록 응답에 에러 메시지 포함: {}", message);
                return false;
            }
        }

        // 응답에 detail 필드가 있고 에러 정보인지 확인
        Object detailObj = response.get("detail");
        if (detailObj != null) {
            if (detailObj instanceof String) {
                String detail = detailObj.toString();
                if (detail.toLowerCase().contains("error") ||
                        detail.toLowerCase().contains("fail") ||
                        detail.toLowerCase().contains("실패") ||
                        detail.toLowerCase().contains("이미 사용 중") ||
                        detail.toLowerCase().contains("중복")) {
                    logger.warn("프로세스 등록 응답에 에러 detail 포함: {}", detail);
                    return false;
                }
            }
        }

        // 중첩된 객체 내부의 에러 상태 확인 (response.data.status 등)
        logger.debug("중첩된 객체 에러 검증 시작");
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            logger.debug("검증 중인 필드: {} = {}", key, value);

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                logger.debug("중첩된 Map 발견: {} = {}", key, nestedMap);
                logger.debug("중첩된 Map의 키들: {}", nestedMap.keySet());

                // response 키인 경우 data 키도 확인
                if ("response".equals(key) && nestedMap.containsKey("data")) {
                    Object dataValue = nestedMap.get("data");
                    if (dataValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) dataValue;
                        logger.debug("response.data Map 발견: {}", dataMap);

                        boolean hasDataError = isNestedErrorResponse(dataMap);
                        logger.debug("response.data 에러 검증 결과: {}", hasDataError);

                        if (hasDataError) {
                            logger.warn("프로세스 등록 응답에 response.data 에러 포함: {}", dataMap);
                            return false;
                        }
                    }
                }

                boolean hasNestedError = isNestedErrorResponse(nestedMap);
                logger.debug("중첩된 Map {} 에러 검증 결과: {}", key, hasNestedError);

                if (hasNestedError) {
                    logger.warn("프로세스 등록 응답에 중첩된 에러 포함: {} = {}", key, nestedMap);
                    return false;
                }
            }
        }

        // 응답 데이터의 품질 검증 - 빈 값이 너무 많은 경우 의심
        int emptyFieldCount = 0;
        int totalFieldCount = 0;

        for (Map.Entry<String, Object> entry : response.entrySet()) {
            totalFieldCount++;
            Object value = entry.getValue();
            if (value == null ||
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof Number && ((Number) value).doubleValue() == 0.0)) {
                emptyFieldCount++;
            }
        }

        // 빈 필드가 전체의 70% 이상인 경우 의심
        if (totalFieldCount > 0 && (double) emptyFieldCount / totalFieldCount > 0.7) {
            logger.warn("프로세스 등록 응답에 빈 필드가 너무 많음: {}/{}", emptyFieldCount, totalFieldCount);
            return false;
        }

        logger.debug("프로세스 등록 응답 검증 완료: 유효함");
        // 기본적으로 유효하다고 판단 (추가 검증 로직 필요시 확장)
        return true;
    }

    // Update Process Masters
    public Map<String, Object> updateProcess(String processId, Map<String, Object> processData,
            org.springframework.web.multipart.MultipartFile siteFile) {
        // logger.debug("프로세스 수정 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();

        if (processId == null || processId.isEmpty()) {
            logger.warn("프로세스 수정 실패: 프로세스 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidprocessId");
            return result;
        }

        if (processData == null) {
            logger.warn("프로세스 수정 실패: 프로세스 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidProcessData");
            return result;
        }

        // siteFile이 있는 경우 심볼 생성 수행 (createProcess와 동일한 로직)
        String symbolId = null;
        if (siteFile != null && !siteFile.isEmpty()) {
            logger.debug("심볼 파일이 제공됨, 심볼 생성 수행: fileName={}, size={}", siteFile.getOriginalFilename(),
                    siteFile.getSize());

            // processData에서 심볼 관련 데이터 추출
            Map<String, Object> symbolData = new HashMap<>();

            // symbol_name과 symbol_code가 비어있으면 기본값 생성
            String symbolName = (String) processData.getOrDefault("process_name", "");
            String symbolCode = (String) processData.getOrDefault("process_code", "");

            symbolData.put("symbol_name", symbolName);
            symbolData.put("symbol_code", symbolCode);
            symbolData.put("symbol_type", processData.getOrDefault("symbol_type", "PROCESS"));
            symbolData.put("siteFile", siteFile);

            logger.debug("심볼 생성 데이터: symbol_name={}, symbol_code={}, symbol_type={}",
                    symbolName, symbolCode, processData.getOrDefault("symbol_type", "PROCESS"));

            // createSymbol 호출하여 symbol_id 획득
            Map<String, Object> symbolResult = createSymbol(symbolData);
            if ((Boolean) symbolResult.get("success")) {
                symbolId = (String) symbolResult.get("symbol_id");
                logger.debug("심볼 생성 성공, symbol_id: {}", symbolId);
            } else {
                logger.warn("심볼 생성 실패: {}", symbolResult.get("message"));
                // 심볼 생성 실패해도 프로세스는 수정 계속 진행
                // result에 심볼 생성 실패 정보를 저장하지만 프로세스 수정은 계속
                result.put("symbol_creation_failed", true);
                result.put("symbol_error_message", symbolResult.get("message"));
            }
        }

        // 외부 인증 서버 URL 구성 (createProcess와 동일한 엔드포인트 사용)
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/" + processId;
        logger.debug("프로세스 수정 URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("process_code", processData.getOrDefault("process_code", ""));
        requestMap.put("process_name", processData.getOrDefault("process_name", ""));
        requestMap.put("process_type_code",
                processData.getOrDefault("process_type_code", ""));
        requestMap.put("process_category",
                processData.getOrDefault("process_category", ""));
        requestMap.put("process_description",
                processData.getOrDefault("process_description", ""));
        requestMap.put("technology_mode_code",
                processData.getOrDefault("technology_mode_code", ""));
        requestMap.put("typical_removal_efficiency",
                processData.getOrDefault("typical_removal_efficiency", new HashMap<>()));
        requestMap.put("typical_detention_time",
                processData.getOrDefault("typical_detention_time", 0.0));
        requestMap.put("default_formula_set",
                processData.getOrDefault("default_formula_set", new HashMap<>()));
        requestMap.put("is_active", processData.getOrDefault("is_active", true));
        requestMap.put("poc_input_count", processData.getOrDefault("poc_input_count",
                0));
        requestMap.put("poc_output_count",
                processData.getOrDefault("poc_output_count", 0));
        requestMap.put("workflow_svg_uri",
                processData.getOrDefault("workflow_svg_uri", ""));
        requestMap.put("flow_diagram_data",
                processData.getOrDefault("flow_diagram_data", new HashMap<>()));

        // symbol_id 설정
        String existingSymbolIdValue = (String) processData.getOrDefault("symbol_id", "");
        boolean isSiteFileEmpty = (siteFile == null || siteFile.isEmpty());
        String emptyUuid = "00000000-0000-0000-0000-000000000000";

        // siteFile이 없고 symbol_id가 빈 UUID로 명시적으로 전달된 경우, 해당 값으로 업데이트
        if (isSiteFileEmpty && existingSymbolIdValue != null && existingSymbolIdValue.trim().equals(emptyUuid)) {
            requestMap.put("symbol_id", emptyUuid);
            logger.debug("siteFile 없음, symbol_id를 빈 UUID로 설정: {}", emptyUuid);
        } else if (!isEmptySymbolId(symbolId)) {
            // 새로 생성된 symbol_id가 있으면 사용
            requestMap.put("symbol_id", symbolId);
        } else if (!isEmptySymbolId(existingSymbolIdValue)) {
            // 기존 symbol_id가 유효하면 사용
            requestMap.put("symbol_id", existingSymbolIdValue);
        }

        // language_code와 unit_system_code 추가 (기본값 설정)
        requestMap.put("language_code", processData.getOrDefault("language_code", "ko"));
        requestMap.put("unit_system_code", processData.getOrDefault("unit_system_code", "METRIC"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 PUT 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.put(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅: 응답 내용 로깅
            // logger.debug("프로세스 수정 응답 내용: {}", parsedResponse);

            // 응답코드가 200 또는 204인 경우 성공으로 처리 (PUT 요청의 표준 응답 코드)
            if (httpResult.getStatus() == 200 || httpResult.getStatus() == 204) {
                // 응답 내용 검증 - 실제 수정된 데이터가 있는지 확인
                boolean isValidResponse = isValidUpdateResponse(parsedResponse);
                // logger.debug("프로세스 수정 응답 검증 결과: {}", isValidResponse);

                if (isValidResponse) {
                    // logger.debug("프로세스 수정 성공");
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "messages.success.processUpdateSuccess");
                    result.put("response", parsedResponse);

                    // 심볼 수정 실패 정보가 있으면 추가
                    if (result.containsKey("symbol_update_failed") && (Boolean) result.get("symbol_update_failed")) {
                        result.put("symbol_warning", "심볼 수정에 실패했습니다: " + result.get("symbol_error_message"));
                        logger.warn("프로세스는 수정되었지만 심볼 수정 실패: {}", result.get("symbol_error_message"));
                    }
                } else {
                    // 응답 코드는 성공이지만 내용에 문제가 있는 경우
                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "messages.error.processUpdateFail";
                    }

                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("프로세스 수정 실패 (응답 내용 검증 실패): {}", errorMessage);
                    return result;
                }
            } else {
                // 기타 응답 코드는 에러로 처리
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.processUpdateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("프로세스 수정 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                return result;
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
                errorMessage = "messages.error.processUpdateFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 프로세스 수정 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 프로세스 수정 응답이 유효한지 검증하는 메서드
     * 
     * @param response 외부 API 응답 데이터
     * @return 유효성 여부
     */
    private boolean isValidUpdateResponse(Map<String, Object> response) {
        logger.debug("프로세스 수정 응답 검증 시작: {}", response);

        if (response == null) {
            logger.warn("프로세스 수정 응답이 null");
            return false;
        }

        // 응답에 에러 메시지가 포함되어 있는지 확인
        if (response.containsKey("error") || response.containsKey("errors")) {
            logger.warn("프로세스 수정 응답에 에러 포함: {}", response);
            return false;
        }

        // 응답에 status 필드가 있고 'error'인지 확인
        Object statusObj = response.get("status");
        if (statusObj != null && "error".equals(statusObj.toString())) {
            logger.warn("프로세스 수정 응답에 status: error 포함: {}", response);
            return false;
        }

        // 응답에 message 필드가 있고 에러 관련 메시지인지 확인
        Object messageObj = response.get("message");
        if (messageObj != null) {
            String message = messageObj.toString();
            logger.debug("프로세스 수정 응답 message 필드: {}", message);
            if (message.toLowerCase().contains("error") ||
                    message.toLowerCase().contains("fail") ||
                    message.toLowerCase().contains("실패") ||
                    message.toLowerCase().contains("이미 사용 중") ||
                    message.toLowerCase().contains("중복")) {
                logger.warn("프로세스 수정 응답에 에러 메시지 포함: {}", message);
                return false;
            }
        }

        // 응답에 detail 필드가 있고 에러 정보인지 확인
        Object detailObj = response.get("detail");
        if (detailObj != null) {
            if (detailObj instanceof String) {
                String detail = detailObj.toString();
                if (detail.toLowerCase().contains("error") ||
                        detail.toLowerCase().contains("fail") ||
                        detail.toLowerCase().contains("실패") ||
                        detail.toLowerCase().contains("이미 사용 중") ||
                        detail.toLowerCase().contains("중복")) {
                    logger.warn("프로세스 수정 응답에 에러 detail 포함: {}", detail);
                    return false;
                }
            }
        }

        // 중첩된 객체 내부의 에러 상태 확인 (response.data.status 등)
        logger.debug("중첩된 객체 에러 검증 시작");
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            logger.debug("검증 중인 필드: {} = {}", key, value);

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                logger.debug("중첩된 Map 발견: {} = {}", key, nestedMap);
                if (isNestedErrorResponse(nestedMap)) {
                    logger.warn("프로세스 수정 응답에 중첩된 에러 포함: {} = {}", key, nestedMap);
                    return false;
                }
            }
        }

        // 응답 데이터의 품질 검증 - 빈 값이 너무 많은 경우 의심
        int emptyFieldCount = 0;
        int totalFieldCount = 0;

        for (Map.Entry<String, Object> entry : response.entrySet()) {
            totalFieldCount++;
            Object value = entry.getValue();
            if (value == null ||
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof Number && ((Number) value).doubleValue() == 0.0)) {
                emptyFieldCount++;
            }
        }

        // 빈 필드가 전체의 70% 이상인 경우 의심
        if (totalFieldCount > 0 && (double) emptyFieldCount / totalFieldCount > 0.7) {
            logger.warn("프로세스 수정 응답에 빈 필드가 너무 많음: {}/{}", emptyFieldCount, totalFieldCount);
            return false;
        }

        logger.debug("프로세스 수정 응답 검증 완료: 유효함");
        // 기본적으로 유효하다고 판단 (추가 검증 로직 필요시 확장)
        return true;
    }

    /**
     * 중첩된 객체 내부에 에러가 있는지 확인하는 메서드
     * 
     * @param nestedResponse 중첩된 응답 객체
     * @return 에러 여부
     */
    private boolean isNestedErrorResponse(Map<String, Object> nestedResponse) {
        logger.debug("중첩된 객체 에러 검증 시작: {}", nestedResponse);

        if (nestedResponse == null) {
            logger.debug("중첩된 객체가 null");
            return false;
        }

        // status 필드가 'error'인지 확인
        Object statusObj = nestedResponse.get("status");
        if (statusObj != null) {
            String status = statusObj.toString();
            logger.debug("중첩된 객체 status 필드: {}", status);
            if ("error".equals(status)) {
                logger.warn("중첩된 객체에 status: error 발견");
                return true;
            }

            // 숫자 상태 코드가 4xx 또는 5xx인 경우 에러로 처리
            try {
                int statusCode = Integer.parseInt(status);
                logger.debug("중첩된 객체 status 필드 숫자 변환 성공: {} -> {}", status, statusCode);
                if (statusCode >= 400) {
                    logger.warn("중첩된 객체에 에러 상태 코드 발견: {}", statusCode);
                    return true;
                }
            } catch (NumberFormatException e) {
                // 숫자가 아닌 경우 무시
                logger.debug("중첩된 객체 status 필드가 숫자가 아님: {} (예외: {})", status, e.getMessage());
            }
        }

        // message 필드에 에러 키워드가 포함되어 있는지 확인
        Object messageObj = nestedResponse.get("message");
        if (messageObj != null) {
            String message = messageObj.toString();
            logger.debug("중첩된 객체 message 필드: {}", message);
            if (message.toLowerCase().contains("error") ||
                    message.toLowerCase().contains("fail") ||
                    message.toLowerCase().contains("실패") ||
                    message.toLowerCase().contains("이미 사용 중") ||
                    message.toLowerCase().contains("중복")) {
                logger.warn("중첩된 객체에 에러 메시지 발견: {}", message);
                return true;
            }
        }

        // success 필드가 false인지 확인
        Object successObj = nestedResponse.get("success");
        if (successObj != null) {
            logger.debug("중첩된 객체 success 필드: {} (타입: {})", successObj, successObj.getClass().getSimpleName());
            if (successObj instanceof Boolean && !(Boolean) successObj) {
                logger.warn("중첩된 객체에 success: false 발견 (Boolean)");
                return true;
            } else if (successObj instanceof String && "false".equals(successObj.toString())) {
                logger.warn("중첩된 객체에 success: false 발견 (String)");
                return true;
            }
        }

        // error 또는 errors 필드가 있는지 확인
        if (nestedResponse.containsKey("error") || nestedResponse.containsKey("errors")) {
            logger.warn("중첩된 객체에 error/errors 필드 발견");
            return true;
        }

        logger.debug("중첩된 객체 에러 검증 완료: 에러 없음");
        return false;
    }

    /**
     * 프로세스 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteProcess(Map<String, Object> deleteData) {
        Map<String, Object> result = new HashMap<>();

        if (deleteData == null) {
            logger.warn("프로세스 삭제 실패: 삭제 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.deleteDataRequired");
            return result;
        }

        // process_id 추출
        String processId = (String) deleteData.getOrDefault("process_id", "");

        if (processId == null || processId.isEmpty()) {
            logger.warn("프로세스 삭제 실패: 프로세스 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidprocessId");
            return result;
        }

        logger.debug("프로세스 삭제 시작: process_id={}", processId);

        // 1단계: 심볼 삭제 (선택사항 - 실패해도 다음 단계 진행)
        String symbolId = (String) deleteData.getOrDefault("symbol_id", "");
        if (!isEmptySymbolId(symbolId)) {
            logger.debug("1단계: 심볼 삭제 시작: symbol_id={}", symbolId);
            try {
                Map<String, Object> symbolDeleteResult = deleteSymbol(symbolId);
                if ((Boolean) symbolDeleteResult.get("success")) {
                    logger.debug("심볼 삭제 성공");
                    result.put("symbol_delete_result", symbolDeleteResult);
                } else {
                    logger.warn("심볼 삭제 실패하였으나 프로세스 삭제를 계속 진행합니다: {}", symbolDeleteResult.get("message"));
                    result.put("symbol_delete_result", symbolDeleteResult);
                    result.put("symbol_delete_warning", "심볼 삭제에 실패했지만 프로세스 삭제는 계속 진행됩니다.");
                }
            } catch (Exception e) {
                logger.warn("심볼 삭제 중 예외 발생하였으나 프로세스 삭제를 계속 진행합니다: {}", e.getMessage());
                Map<String, Object> symbolDeleteResult = new HashMap<>();
                symbolDeleteResult.put("success", false);
                symbolDeleteResult.put("message", "심볼 삭제 중 예외 발생: " + e.getMessage());
                result.put("symbol_delete_result", symbolDeleteResult);
                result.put("symbol_delete_warning", "심볼 삭제에 실패했지만 프로세스 삭제는 계속 진행됩니다.");
            }
        } else {
            logger.debug("1단계: 심볼 삭제 건너뜀 (symbol_id 없음)");
        }

        // 2단계: 관련 계산식 삭제
        logger.debug("2단계: 관련 계산식 삭제 시작");
        Map<String, Object> formulaSearchParams = new HashMap<>();
        formulaSearchParams.put("search_field", "process_id");
        formulaSearchParams.put("search_value", processId);
        formulaSearchParams.put("page", 1);
        formulaSearchParams.put("page_size", 100);

        Map<String, Object> formulaDeleteResult = deleteProcessFormula(formulaSearchParams);
        if ((Boolean) formulaDeleteResult.get("success")) {
            int deletedFormulaCount = (Integer) formulaDeleteResult.getOrDefault("deleted_count", 0);
            int failedFormulaCount = (Integer) formulaDeleteResult.getOrDefault("failed_count", 0);
            logger.debug("계산식 삭제 완료: 성공={}, 실패={}", deletedFormulaCount, failedFormulaCount);
            result.put("formula_delete_result", formulaDeleteResult);
        } else {
            logger.warn("계산식 삭제 실패: {}", formulaDeleteResult.get("message"));
            result.put("formula_delete_result", formulaDeleteResult);
        }

        // 3단계: 관련 도면 삭제
        logger.debug("3단계: 관련 도면 삭제 시작");
        Map<String, Object> drawingSearchParams = new HashMap<>();
        drawingSearchParams.put("search_field", "process_id");
        drawingSearchParams.put("search_value", processId);
        drawingSearchParams.put("page", 1);
        drawingSearchParams.put("page_size", 100);

        Map<String, Object> drawingDeleteResult = deleteProcessDrawing(drawingSearchParams);
        if ((Boolean) drawingDeleteResult.get("success")) {
            int deletedDrawingCount = (Integer) drawingDeleteResult.getOrDefault("deleted_count", 0);
            int failedDrawingCount = (Integer) drawingDeleteResult.getOrDefault("failed_count", 0);
            logger.debug("도면 삭제 완료: 성공={}, 실패={}", deletedDrawingCount, failedDrawingCount);
            result.put("drawing_delete_result", drawingDeleteResult);
        } else {
            logger.warn("도면 삭제 실패: {}", drawingDeleteResult.get("message"));
            result.put("drawing_delete_result", drawingDeleteResult);
        }

        // 4단계: 프로세스 삭제
        logger.debug("4단계: 프로세스 삭제 시작");
        String processUrl = authServerBaseUrl + "/api/v1/process/process_masters/" + processId;
        logger.debug("프로세스 삭제 URL: {}", processUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(processUrl, "application/json", null);

        // 응답 처리
        int statusCode = httpResult.getStatus();
        String responseBody = httpResult.getBody();

        // 403, 404, 422, 500 응답 코드는 모두 실패 처리
        if (httpResult.isSuccess() && statusCode != 403 && statusCode != 404 && statusCode != 422
                && statusCode != 500) {
            // 성공적인 응답 처리
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            logger.debug("프로세스 삭제 성공: process_id={}", processId);
            result.put("success", true);
            result.put("status", statusCode);
            result.put("message", "messages.success.processDeleteSuccess");
            result.put("response", parsedResponse);

            logger.debug("프로세스 삭제 프로세스 완료: process_id={}", processId);
        } else {
            // 에러 응답 처리 (403, 404, 422, 500 및 기타 실패 응답)
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
                errorMessage = "프로세스 삭제에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            // 상세한 오류 정보 로깅
            logger.error("프로세스 삭제 API 실패 상세 정보:");
            logger.error("- Process ID: {}", processId);
            logger.error("- API URL: {}", processUrl);
            logger.error("- HTTP Status: {}", statusCode);
            logger.error("- Response Body: {}", responseBody);
            logger.error("- Error Message: {}", errorMessage);
            logger.warn("관련 데이터 삭제는 완료되었지만 프로세스 삭제에 실패했습니다.");

            // 일반적인 HTTP 상태 코드별 추가 안내
            if (statusCode == 404) {
                logger.debug("404 오류: 프로세스가 이미 삭제되었거나 존재하지 않을 수 있습니다.");
            } else if (statusCode == 403) {
                logger.debug("403 오류: 프로세스 삭제 권한이 없을 수 있습니다.");
            } else if (statusCode == 422) {
                logger.debug("422 오류: 요청 데이터가 유효하지 않거나 처리할 수 없습니다.");
            } else if (statusCode == 409) {
                logger.debug("409 오류: 프로세스가 다른 리소스에서 참조되고 있을 수 있습니다.");
            } else if (statusCode >= 500) {
                logger.debug("서버 오류 ({}): 외부 API 서버에 일시적 문제가 있을 수 있습니다.", statusCode);
            }
        }

        return result;
    }

    /**
     * 계산식 삭제
     * 
     * @param formulaId 삭제할 계산식 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deleteFormula(String formulaId) {
        // logger.debug("외부 인증 서버 계산식 삭제 시도: formulaId={}, server={}", formulaId,
        // authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();

        if (formulaId == null || formulaId.isEmpty()) {
            logger.warn("계산식 삭제 실패: 계산식 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidFormulaId");
            return result;
        }

        // 외부 인증 서버 URL 구성 - MinIO formula 삭제 API 사용
        String formulaUrl = authServerBaseUrl + "/api/v1/common/formula_library/" + formulaId;
        // logger.debug("계산식 삭제 URL: {}", formulaUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(formulaUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // logger.debug("계산식 삭제 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.formulaDeleteSuccess");
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
                errorMessage = "messages.error.formulaDeleteFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 계산식 삭제 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 자식 Excel 업로드 - 새로운 API 사용
     * 
     * @param excelData Excel 데이터 (pid_id 포함)
     * @param excelFile Excel 파일 (필수)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadChildExcel(Map<String, Object> excelData,
            org.springframework.web.multipart.MultipartFile excelFile) {
        Map<String, Object> result = new HashMap<>();

        if (excelData == null) {
            logger.warn("자식 Excel 업로드 실패: Excel 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidExcelData");
            return result;
        }

        // 디버깅용: excelData 입력변수 출력
        logger.debug("uploadChildExcel 입력 excelData: {}", excelData);

        if (excelFile == null || excelFile.isEmpty()) {
            logger.warn("자식 Excel 업로드 실패: excelFile이 필수값입니다");
            result.put("success", false);
            result.put("message", "Excel 업로드에는 파일이 필수입니다.");
            return result;
        }

        // pid_id 추출 (필수)
        String pidId = (String) excelData.get("parent_drawing_id");
        if (pidId == null || pidId.trim().isEmpty()) {
            logger.warn("자식 Excel 업로드 실패: parent_drawing_id 가 필수값입니다");
            result.put("success", false);
            result.put("message", "parent_drawing_id 가 필수입니다.");
            return result;
        }

        logger.debug("pidId 추출 완료: {}", pidId);

        // force_update 파라미터 추출 (기본값: false)
        Object forceUpdateObj = excelData.get("force_update");
        boolean forceUpdate = false; // 기본값
        if (forceUpdateObj != null) {
            if (forceUpdateObj instanceof Boolean) {
                forceUpdate = (Boolean) forceUpdateObj;
            } else if (forceUpdateObj instanceof String) {
                forceUpdate = Boolean.parseBoolean((String) forceUpdateObj);
            }
        }
        logger.debug("force_update 파라미터: {}", forceUpdate);

        if (forceUpdate) {
            deletePidComponents(pidId); // pidId 기준 Pid 매핑 Components 삭제
        }

        try {
            // /api/v1/process/drawing_masters/upload/PNID_TO_EXCEL/{drawing_id}?force_update={force_update}
            // API 호출
            String uploadApiUrl = authServerBaseUrl + "/api/v1/process/drawing_masters/upload/PNID_TO_EXCEL/" + pidId
                    + "?force_update=" + forceUpdate;
            logger.debug("Excel 업로드 API 호출: {}", uploadApiUrl);

            // multipart 요청 데이터 구성
            Map<String, Object> requestMap = new HashMap<>();

            // drawing_id를 pidId로 설정 (요구사항)
            requestMap.put("drawing_id", pidId);

            // Excel 파일을 requestMap에 추가
            requestMap.put("excel_file", excelFile);

            logger.debug("Excel 업로드 요청 데이터: {}", requestMap);

            // multipart 요청 실행
            HttpUtil.HttpResult uploadResult = HttpUtil.postMultipart(uploadApiUrl, requestMap);

            if (uploadResult.isSuccess()) {
                String responseBody = uploadResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("Excel 업로드 성공: {}", responseBody);

                result.put("success", true);
                result.put("status", uploadResult.getStatus());
                result.put("message", "Excel 업로드가 성공했습니다.");
                result.put("response", parsedResponse);

                // excel_drawing_id 추출하여 추가
                Object excelDrawingId = JsonUtil.extractValue(responseBody, "excel_drawing_id");
                if (excelDrawingId != null) {
                    result.put("excel_drawing_id", excelDrawingId);
                    logger.debug("excel_drawing_id 추출 완료: {}", excelDrawingId);
                }

            } else {
                // 업로드 실패
                String responseBody = uploadResult.getBody();
                logger.debug("Excel 업로드 실패 응답 본문: {}", responseBody);

                String errorMessage = null;

                // 1. HttpUtil에서 추출된 에러 메시지 사용
                errorMessage = uploadResult.getExtractedErrorMessage();

                // 2. 응답 본문에서 detail 추출 (우선순위 높음)
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                // 3. 응답 본문에서 message 추출
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }

                // 4. 기본 메시지
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Excel 업로드에 실패했습니다.";
                }

                logger.warn("Excel 업로드 실패: status={}, message={}", uploadResult.getStatus(), errorMessage);
                result.put("success", false);
                result.put("status", uploadResult.getStatus());
                result.put("message", errorMessage);
                result.put("errorMessage", errorMessage);
                result.put("response", JsonUtil.parseJson(responseBody));
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // HTTP 클라이언트 오류 (4xx) 처리
            String responseBody = e.getResponseBodyAsString();
            logger.error("Excel 업로드 API 호출 중 HTTP 오류 발생: status={}, body={}",
                    e.getStatusCode(), responseBody);

            // 응답 본문에서 에러 메시지 추출
            String errorMessage = null;
            if (responseBody != null && !responseBody.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Excel 업로드에 실패했습니다. (HTTP " + e.getStatusCode() + ")";
            }

            result.put("success", false);
            result.put("status", e.getStatusCode().value());
            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", JsonUtil.parseJson(responseBody));
        } catch (Exception e) {
            logger.error("Excel 업로드 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "Excel 업로드 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도면 삭제
     * 
     * @param drawingId 삭제할 도면 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deleteDrawing(String drawingId) {
        Map<String, Object> result = new HashMap<>();

        if (drawingId == null || drawingId.isEmpty()) {
            logger.warn("도면 삭제 실패: 도면 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDrawingId");
            return result;
        }

        // 1단계: drawingId로 도면 정보 조회하여 drawing_type 확인
        String drawingType = null;
        Map<String, Object> drawingInfo = getDrawingInfo(drawingId);
        if (drawingInfo != null && (Boolean) drawingInfo.get("success")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> drawingData = (Map<String, Object>) drawingInfo.get("response");
            drawingType = (String) drawingData.get("drawing_type");

            logger.debug("도면 정보 조회 성공 - drawing_id: {}, drawing_type: {}", drawingId, drawingType);

            // PNID 타입인 경우 Component 먼저 삭제
            if ("PNID".equals(drawingType)) {
                logger.debug("PNID 타입 도면 - Component 삭제 시작: {}", drawingId);

                // Component 검색 및 삭제
                Map<String, Object> searchResult = searchComponentsByPidId(drawingId);
                if ((Boolean) searchResult.get("success")) {
                    @SuppressWarnings("unchecked")
                    List<String> componentIds = (List<String>) searchResult.get("component_ids");

                    if (componentIds != null && !componentIds.isEmpty()) {
                        logger.debug("PNID Component 검색 성공: {}개 component_id 발견", componentIds.size());

                        // 각 component_id로 deleteComponent 수행
                        boolean allComponentsDeleted = true;
                        for (String componentId : componentIds) {
                            Map<String, Object> componentDeleteResult = deleteComponent(componentId);
                            if (!(Boolean) componentDeleteResult.get("success")) {
                                allComponentsDeleted = false;
                                logger.error("PNID Component 삭제 실패: componentId={}, 오류: {}",
                                        componentId, componentDeleteResult.get("message"));
                            } else {
                                logger.debug("PNID Component 삭제 성공: componentId={}", componentId);
                            }
                        }

                        if (!allComponentsDeleted) {
                            logger.warn("PNID Component 삭제 중 일부 실패: drawingId={}", drawingId);
                        }
                    } else {
                        logger.debug("PNID Component 검색 결과 없음: drawingId={}", drawingId);
                    }
                } else {
                    logger.warn("PNID Component 검색 실패: drawingId={}, 오류: {}", drawingId, searchResult.get("message"));
                }
            }
        } else {
            logger.warn("도면 정보 조회 실패: drawingId={}", drawingId);
        }

        // 2단계: 도면 삭제 수행 - drawing_type에 따른 API 엔드포인트 분기
        String drawingUrl;
        if ("PFDCARD".equals(drawingType)) {
            drawingUrl = authServerBaseUrl +
                    "/api/v1/minio/drawing_files/drawing/PFDCARD/" + drawingId;
            logger.debug("PFDCARD 도면 삭제 API 호출: {}", drawingUrl);
        } else if ("PNID".equals(drawingType)) {
            drawingUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/drawing/PNID/"
                    + drawingId;
            logger.debug("PNID 도면 삭제 API 호출: {}", drawingUrl);
        } else {
            // 기타 타입이거나 타입을 알 수 없는 경우 기본 API 사용
            drawingUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/" + drawingId;
            logger.debug("기본 도면 삭제 API 호출 (drawing_type: {}): {}", drawingType, drawingUrl);
        }

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(drawingUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.drawingDeleteSuccess");
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
                errorMessage = "messages.error.drawingDeleteFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 도면 삭제 실패: {}", errorMessage);
        }

        return result;
    }

    public Map<String, Object> getProcessStatus(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("processId", id);
        data.put("status", "실행중");
        data.put("startTime", "2024-01-15 10:00:00");
        data.put("progress", "75%");
        return data;
    }

    public Map<String, Object> startProcess(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("processId", id);
        data.put("action", "started");
        data.put("startedAt", System.currentTimeMillis());
        return data;
    }

    public Map<String, Object> stopProcess(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("processId", id);
        data.put("action", "stopped");
        data.put("stoppedAt", System.currentTimeMillis());
        return data;
    }

    /// api/v1/process/processes/search (ProcessMaster검색)
    public Map<String, Object> getAllProcesses(Map<String, Object> searchParams) {

        // logger.debug("ProcessMaster검색 시작: server={}, params={}", authServerBaseUrl,
        // searchParams);
        Map<String, Object> result = new HashMap<>();
        // URL 구성 - 원래 의도한 Process API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/process/processes/search/enhanced";
        // logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", "process_code"));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", "METRIC"));
        // requestMap.put("limit", searchParams.getOrDefault("page", 100));

        // 공정구분(level2_code)과 공정 중분류(level3_code) 필터링 추가
        if (searchParams.containsKey("level2_code") && searchParams.get("level2_code") != null) {
            requestMap.put("level2_code", searchParams.get("level2_code"));
        }
        if (searchParams.containsKey("level3_code") && searchParams.get("level3_code") != null) {
            requestMap.put("level3_code", searchParams.get("level3_code"));
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("Process 검색 요청 본문: {}", requestBody);
        logger.debug("클라이언트에서 전달된 모든 파라미터: {}", searchParams);
        logger.debug("필터링 파라미터 - level2_code: {}, level3_code: {}",
                searchParams.get("level2_code"), searchParams.get("level3_code"));

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                // logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // 특정 키만 남기기
            if (parsedResponse != null) {

                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    // 서버 측 필터링 적용
                    List<Map<String, Object>> filteredItems = new ArrayList<>();
                    String targetLevel2Code = (String) searchParams.get("level2_code");
                    String targetLevel3Code = (String) searchParams.get("level3_code");

                    logger.debug("서버 측 필터링 적용 - level2_code: {}, level3_code: {}", targetLevel2Code, targetLevel3Code);

                    for (Map<String, Object> item : items) {
                        // process_info에서 실제 프로세스 정보 추출
                        Map<String, Object> processInfo = null;
                        if (item.containsKey("process_info") && item.get("process_info") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> processInfoMap = (Map<String, Object>) item.get("process_info");
                            processInfo = processInfoMap;
                        }

                        // level2_code 필터링 (process_info에서 추출)
                        if (targetLevel2Code != null && !targetLevel2Code.trim().isEmpty()) {
                            String itemLevel2Code = null;
                            if (processInfo != null) {
                                itemLevel2Code = (String) processInfo.get("level2_code_key");
                            }
                            if (itemLevel2Code == null || !targetLevel2Code.equals(itemLevel2Code)) {
                                continue; // 필터링 조건에 맞지 않으면 건너뛰기
                            }
                        }

                        // level3_code 필터링 (process_info에서 추출)
                        if (targetLevel3Code != null && !targetLevel3Code.trim().isEmpty()) {
                            String itemLevel3Code = null;
                            if (processInfo != null) {
                                itemLevel3Code = (String) processInfo.get("level3_code_key");
                            }
                            if (itemLevel3Code == null || !targetLevel3Code.equals(itemLevel3Code)) {
                                continue; // 필터링 조건에 맞지 않으면 건너뛰기
                            }
                        }

                        filteredItems.add(item);
                    }

                    logger.debug("필터링 결과: 전체 {} -> 필터링 후 {}", items.size(), filteredItems.size());

                    for (Map<String, Object> item : filteredItems) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // process_info에서 실제 프로세스 정보 추출
                        Map<String, Object> processInfo = null;
                        if (item.containsKey("process_info") && item.get("process_info") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> processInfoMap = (Map<String, Object>) item.get("process_info");
                            processInfo = processInfoMap;
                        }

                        // process_info가 있는 경우 해당 정보 사용, 없으면 item에서 직접 추출
                        Map<String, Object> sourceData = (processInfo != null) ? processInfo : item;

                        // 원하는 키만 추가 (process_info에서 추출)
                        filteredItem.put("level2_code_key", sourceData.getOrDefault("level2_code_key", ""));
                        filteredItem.put("level2_code_value", sourceData.getOrDefault("level2_code_value", ""));
                        filteredItem.put("level2_code_value_en", sourceData.getOrDefault("level2_code_value_en", ""));
                        filteredItem.put("level3_code_key", sourceData.getOrDefault("level3_code_key", ""));
                        filteredItem.put("level3_code_value", sourceData.getOrDefault("level3_code_value", ""));
                        filteredItem.put("level3_code_value_en", sourceData.getOrDefault("level3_code_value_en", ""));
                        filteredItem.put("symbol_uri", sourceData.getOrDefault("symbol_uri", ""));
                        filteredItem.put("symbol_id", sourceData.getOrDefault("symbol_id", ""));

                        // ccs_file_info에서 file_id와 original_filename 추출
                        // item의 최상위 레벨에서 먼저 확인, 없으면 process_info에서 확인
                        String ccsFileId = "";
                        String ccsFileName = "";
                        try {
                            Object ccsFileInfoObj = null;

                            // 1. item의 최상위 레벨에서 ccs_file_info 확인
                            if (item.containsKey("ccs_file_info")) {
                                ccsFileInfoObj = item.get("ccs_file_info");
                            }
                            // 2. process_info 안에서도 확인
                            else if (processInfo != null && processInfo.containsKey("ccs_file_info")) {
                                ccsFileInfoObj = processInfo.get("ccs_file_info");
                            }

                            if (ccsFileInfoObj != null && ccsFileInfoObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> ccsFileInfoList = (List<Map<String, Object>>) ccsFileInfoObj;
                                if (ccsFileInfoList != null && !ccsFileInfoList.isEmpty()) {
                                    Map<String, Object> ccsFileInfo = ccsFileInfoList.get(0);
                                    if (ccsFileInfo != null) {
                                        Object fileIdObj = ccsFileInfo.get("file_id");
                                        Object originalFileNameObj = ccsFileInfo.get("original_filename");
                                        if (fileIdObj != null) {
                                            ccsFileId = fileIdObj.toString();
                                        }
                                        if (originalFileNameObj != null) {
                                            ccsFileName = originalFileNameObj.toString();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("ccs_file_info 추출 중 오류 발생: {}", e.getMessage(), e);
                        }
                        filteredItem.put("ccs_file_id", ccsFileId);
                        filteredItem.put("ccs_file_name", ccsFileName);

                        // 프로세스 기본 정보 추가
                        if (sourceData.containsKey("process_id")) {
                            filteredItem.put("process_id", sourceData.get("process_id"));
                        }
                        if (sourceData.containsKey("process_code")) {
                            filteredItem.put("process_code", sourceData.get("process_code"));
                        }
                        if (sourceData.containsKey("process_name")) {
                            filteredItem.put("process_name", sourceData.get("process_name"));
                        }
                        if (sourceData.containsKey("process_description")) {
                            filteredItem.put("process_description", sourceData.get("process_description"));
                        }

                        // unit_system_code 추가
                        filteredItem.put("unit_system_code", sourceData.getOrDefault("unit_system_code", "METRIC"));

                        // 추가 프로세스 정보 (process_info에만 있는 필드들)
                        if (processInfo != null) {
                            filteredItem.put("process_type_code", processInfo.getOrDefault("process_type_code", ""));
                            filteredItem.put("process_category", processInfo.getOrDefault("process_category", ""));
                            filteredItem.put("level1_code_key", processInfo.getOrDefault("level1_code_key", ""));
                            filteredItem.put("level1_code_value", processInfo.getOrDefault("level1_code_value", ""));
                            filteredItem.put("level1_code_value_en",
                                    processInfo.getOrDefault("level1_code_value_en", ""));
                            filteredItem.put("level4_code_key", processInfo.getOrDefault("level4_code_key", ""));
                            filteredItem.put("level4_code_value", processInfo.getOrDefault("level4_code_value", ""));
                            filteredItem.put("level4_code_value_en",
                                    processInfo.getOrDefault("level4_code_value_en", ""));
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);

                // response 객체에 items와 페이지네이션 정보를 모두 포함
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("items", filteredResponse);

                // 페이지네이션 정보 추출하여 response에 포함
                if (parsedResponse.containsKey("total")) {
                    responseData.put("total", parsedResponse.get("total"));
                }
                if (parsedResponse.containsKey("page")) {
                    responseData.put("page", parsedResponse.get("page"));
                }
                if (parsedResponse.containsKey("page_size")) {
                    responseData.put("page_size", parsedResponse.get("page_size"));
                }
                if (parsedResponse.containsKey("total_pages")) {
                    responseData.put("total_pages", parsedResponse.get("total_pages"));
                }

                result.put("response", responseData);

            } else {
                // parsedResponse가 null인 경우도 메뉴 권한이 없는 것으로 처리
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            int statusCode = httpResult.getStatus();
            String responseBody = httpResult.getBody();

            result.put("success", false);
            result.put("status", statusCode);

            // 500 Internal Server Error에 대한 특별 처리
            if (statusCode == 500) {
                logger.error("Process 검색 API 서버 오류 (500): 외부 API 서버에서 내부 오류 발생");
                logger.error("서버 응답 내용: {}", responseBody);

                // anyio.WouldBlock 등 Python 비동기 오류 감지
                if (responseBody != null && responseBody.contains("anyio.WouldBlock")) {
                    logger.error("외부 API 서버의 비동기 처리 오류 감지 - 서버 측 일시적 문제로 추정");
                    result.put("message", "외부 API 서버에서 일시적인 처리 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                } else {
                    result.put("message", "외부 API 서버에서 내부 오류가 발생했습니다.");
                }

                // 빈 응답 데이터 설정
                result.put("response", new ArrayList<>());
            } else {
                // 기타 HTTP 오류 처리
                logger.warn("Process 검색 API 오류 ({}): {}", statusCode, responseBody);

                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // HTTP 에러 응답에서 message 또는 detail 메시지 추출
                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Process 검색 중 오류가 발생했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);
            }
        }

        return result;
    }

    /// api/v1/common/common_codes/search (Process 용 공통코드 검색)
    public Map<String, Object> getProcessCode(Map<String, Object> searchParams) {

        // logger.debug("ProcessMaster검색 시작: server={}, params={}", authServerBaseUrl,
        // searchParams);
        Map<String, Object> result = new HashMap<>();
        // URL 구성 - 원래 의도한 Process API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/common/common_codes/search";
        // logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 100));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", "code_order"));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (로그인은 Authorization 헤더 없음)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                // logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // 특정 키만 남기기
            if (parsedResponse != null) {

                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 원하는 키만 추가
                        if (item.containsKey("code_group")) {
                            filteredItem.put("code_group", item.get("code_group"));
                        }
                        if (item.containsKey("code_key")) {
                            filteredItem.put("code_key", item.get("code_key"));
                        }
                        if (item.containsKey("code_value")) {
                            filteredItem.put("code_value", item.get("code_value"));
                        }
                        if (item.containsKey("code_value_en")) {
                            filteredItem.put("code_value_en", item.get("code_value_en"));
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus()); // HTTP 상태 코드 추가

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                // logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.loginFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    /// api/v1/process/pid_components/by_pid/{pid_id} (Process Components 검색)
    public Map<String, Object> getComponents(Map<String, Object> searchParams) {

        logger.debug("Process Components 검색 시작: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();

        // 디버깅: searchParams의 모든 키 확인
        if (searchParams != null) {
            logger.debug("searchParams 키 목록: {}", searchParams.keySet());
            for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
                logger.debug("파라미터: {} = {} (타입: {})",
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
            }
        } else {
            logger.warn("searchParams가 null입니다");
        }

        // pid_id 추출 (필수) - 두 가지 방식 지원
        Object pidIdObj = searchParams.get("pid_id");
        logger.debug("직접 추출된 pid_id 객체: {} (타입: {})", pidIdObj,
                pidIdObj != null ? pidIdObj.getClass().getSimpleName() : "null");

        String pidId = null;
        if (pidIdObj != null) {
            pidId = pidIdObj.toString();
            logger.debug("직접 방식 pid_id 문자열 변환 결과: '{}'", pidId);
        } else {
            // 기존 검색 방식 지원: search_field=pid_id이고 search_value에 실제 값이 있는 경우
            String searchField = (String) searchParams.get("search_field");
            String searchValue = (String) searchParams.get("search_value");
            logger.debug("검색 방식 - search_field: '{}', search_value: '{}'", searchField, searchValue);

            if ("pid_id".equals(searchField) && searchValue != null && !searchValue.trim().isEmpty()) {
                pidId = searchValue.trim();
                logger.debug("검색 방식에서 pid_id 추출 성공: '{}'", pidId);
            }
        }

        if (pidId == null || pidId.trim().isEmpty()) {
            logger.error("Process Components 검색 실패: pid_id가 필수입니다");
            logger.error("최종 pid_id 값: '{}', 길이: {}", pidId, pidId != null ? pidId.length() : "null");
            logger.error("지원되는 방식: 1) pid_id 직접 전달, 2) search_field=pid_id & search_value=실제값");
            result.put("success", false);
            result.put("message", "pid_id가 필수입니다.");
            return result;
        }

        logger.debug("유효한 pid_id 확인: '{}'", pidId);

        // URL 구성 - 새로운 API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/process/pid_components/by_pid/" + pidId;
        logger.debug("API URL: {}", apiUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (요청 본문 불필요)
        HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답 파싱 검증
            if (parsedResponse == null) {
                logger.warn("Process Components 응답 파싱 실패: {}", responseBody);
            }

            // 특정 키만 남기기
            if (parsedResponse != null) {

                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 지정된 컴포넌트 정보만 추가
                        if (item.containsKey("component_code")) {
                            filteredItem.put("component_code", item.get("component_code"));
                        }
                        if (item.containsKey("project_id")) {
                            filteredItem.put("project_id", item.get("project_id"));
                        }
                        if (item.containsKey("process_id")) {
                            filteredItem.put("process_id", item.get("process_id"));
                        }
                        if (item.containsKey("mapping_type")) {
                            filteredItem.put("mapping_type", item.get("mapping_type"));
                        }
                        if (item.containsKey("pid_id")) {
                            filteredItem.put("pid_id", item.get("pid_id"));
                        }
                        if (item.containsKey("pid_number")) {
                            filteredItem.put("pid_number", item.get("pid_number"));
                        }
                        if (item.containsKey("input_poc")) {
                            filteredItem.put("input_poc", item.get("input_poc"));
                        }
                        if (item.containsKey("output_poc")) {
                            filteredItem.put("output_poc", item.get("output_poc"));
                        }
                        if (item.containsKey("component_type")) {
                            filteredItem.put("component_type", item.get("component_type"));
                        }
                        if (item.containsKey("component_hierachy")) {
                            filteredItem.put("component_hierachy", item.get("component_hierachy"));
                        }
                        if (item.containsKey("equipment_id")) {
                            filteredItem.put("equipment_id", item.get("equipment_id"));
                        }
                        if (item.containsKey("structure_id")) {
                            filteredItem.put("structure_id", item.get("structure_id"));
                        }
                        if (item.containsKey("standard_quantity")) {
                            filteredItem.put("standard_quantity", item.get("standard_quantity"));
                        }
                        if (item.containsKey("spare_quantity")) {
                            filteredItem.put("spare_quantity", item.get("spare_quantity"));
                        }
                        if (item.containsKey("total_quantity")) {
                            filteredItem.put("total_quantity", item.get("total_quantity"));
                        }
                        if (item.containsKey("is_active")) {
                            filteredItem.put("is_active", item.get("is_active"));
                        }
                        if (item.containsKey("component_id")) {
                            filteredItem.put("component_id", item.get("component_id"));
                        }
                        if (item.containsKey("created_at")) {
                            filteredItem.put("created_at", item.get("created_at"));
                        }
                        if (item.containsKey("updated_at")) {
                            filteredItem.put("updated_at", item.get("updated_at"));
                        }
                        if (item.containsKey("created_by")) {
                            filteredItem.put("created_by", item.get("created_by"));
                        }
                        if (item.containsKey("updated_by")) {
                            filteredItem.put("updated_by", item.get("updated_by"));
                        }
                        if (item.containsKey("component_info")) {
                            filteredItem.put("component_info", item.get("component_info"));
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus()); // HTTP 상태 코드 추가

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.componentsSearchFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    /// api/v1/process/pid_components/by_pid/{pid_id} (Process Component ID 검색)
    public void deletePidComponents(String pidId) {

        logger.debug("Process Component ID 검색 시작: server={}, pidId={}", authServerBaseUrl, pidId);

        // pid_id 유효성 검증
        if (pidId == null || pidId.trim().isEmpty()) {
            logger.error("Process Component ID 검색 실패: pid_id가 null이거나 비어있습니다");
            return;
        }

        logger.debug("유효한 pid_id 확인: '{}'", pidId);

        // URL 구성 - 새로운 API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/process/pid_components/by_pid/" + pidId;
        logger.debug("API URL: {}", apiUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (요청 본문 불필요)
        HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답 파싱 검증
            if (parsedResponse == null) {
                logger.warn("Process Component ID 응답 파싱 실패: {}", responseBody);
                return;
            }

            // component_id만 추출
            if (parsedResponse != null) {

                List<String> componentIds = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        // component_id만 추출하여 리스트에 추가
                        if (item.containsKey("component_id")) {
                            Object componentId = item.get("component_id");
                            if (componentId != null) {
                                String componentIdStr = componentId.toString();
                                logger.debug("component_id 발견: {}", componentIdStr);

                                // component_id 발견 시 deleteComponent 함수 호출
                                Map<String, Object> deleteResult = deleteComponent(componentIdStr);
                                logger.debug("component_id {} 삭제 결과: {}", componentIdStr, deleteResult.get("success"));

                                componentIds.add(componentIdStr);
                            }
                        }
                    }
                }

                logger.debug("총 {}개의 component_id 추출 완료: {}", componentIds.size(), componentIds);

            } else {
                // parsedResponse가 null인 경우
                logger.warn("Process Component ID 응답이 null입니다");
                return;
            }

        } else {
            // 에러 응답 처리
            logger.error("Process Component ID 검색 실패: HTTP 상태 코드 {}", httpResult.getStatus());

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.componentIdSearchFail";
            }

            logger.error("에러 메시지: {}", errorMessage);
        }
    }

    /// api/v1/process/formula_library/search (Formula 검색)
    public Map<String, Object> getFormula(Map<String, Object> searchParams) {

        // logger.debug("Formula 검색 시작: server={}, params={}", authServerBaseUrl,
        // searchParams);
        Map<String, Object> result = new HashMap<>();
        // URL 구성 - Formula API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/common/formula_library/search";
        // logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", "") + "");
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        // requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", "created_at"));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (로그인은 Authorization 헤더 없음)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                // logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // 특정 키만 남기기
            if (parsedResponse != null) {

                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 원하는 키만 추가
                        if (item.containsKey("formula_id")) {
                            filteredItem.put("formula_id", item.get("formula_id"));
                        }
                        if (item.containsKey("formula_scope")) {
                            filteredItem.put("formula_scope", item.get("formula_scope"));
                        }
                        if (item.containsKey("formula_name")) {
                            filteredItem.put("formula_name", item.get("formula_name"));
                        }
                        if (item.containsKey("formula_code")) {
                            filteredItem.put("formula_code", item.get("formula_code"));
                        }
                        if (item.containsKey("input_parameters")) {
                            filteredItem.put("input_parameters", item.get("input_parameters"));
                        }
                        if (item.containsKey("output_type")) {
                            filteredItem.put("output_type", item.get("output_type"));
                        }
                        if (item.containsKey("python_version")) {
                            filteredItem.put("python_version", item.get("python_version"));
                        }
                        if (item.containsKey("formula_version")) {
                            filteredItem.put("formula_version", item.get("formula_version"));
                        }
                        if (item.containsKey("is_active")) {
                            filteredItem.put("is_active", item.get("is_active"));
                        }
                        if (item.containsKey("process_id")) {
                            filteredItem.put("process_id", item.get("process_id"));
                        }
                        if (item.containsKey("equipment_type")) {
                            filteredItem.put("equipment_type", item.get("equipment_type"));
                        }
                        if (item.containsKey("flow_type_id")) {
                            filteredItem.put("flow_type_id", item.get("flow_type_id"));
                        }
                        if (item.containsKey("unit_system_code")) {
                            filteredItem.put("unit_system_code", item.get("unit_system_code"));
                        }
                        if (item.containsKey("dependencies")) {
                            filteredItem.put("dependencies", item.get("dependencies"));
                        }
                        if (item.containsKey("file_hash")) {
                            filteredItem.put("file_hash", item.get("file_hash"));
                        }
                        if (item.containsKey("file_name")) {
                            filteredItem.put("file_name", item.get("file_name"));
                        }
                        if (item.containsKey("file_unit_version_cnt")) {
                            filteredItem.put("file_unit_version_cnt", item.get("file_unit_version_cnt"));
                        }
                        if (item.containsKey("created_at")) {
                            String formattedDate = formatDateToYYYYMMDD(item.get("created_at"));
                            filteredItem.put("created_at", formattedDate);
                        }
                        if (item.containsKey("created_by")) {
                            filteredItem.put("created_by", item.get("created_by"));
                        }
                        if (item.containsKey("updated_at")) {
                            String formattedDate = formatDateToYYYYMMDD(item.get("updated_at"));
                            filteredItem.put("updated_at", formattedDate);
                        }
                        if (item.containsKey("updated_by")) {
                            filteredItem.put("updated_by", item.get("updated_by"));
                        }
                        if (item.containsKey("process_dependencies")) {
                            Object processDependencies = item.get("process_dependencies");
                            List<Map<String, Object>> restructuredDependencies = parseProcessDependencies(
                                    processDependencies);
                            filteredItem.put("process_dependencies", restructuredDependencies);
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus()); // HTTP 상태 코드 추가

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                // logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.formulaSearchFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    /**
     * 도면파일 Child 조회
     * 
     * @param searchParams 검색 파라미터
     * @return 검색 결과
     */
    public Map<String, Object> getDrawingChild(Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();

        // URL 구성 - Drawing Masters API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/search";
        logger.debug("도면 Master 검색 API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        String searchField = (String) searchParams.getOrDefault("search_field", "");
        String searchValue = (String) searchParams.getOrDefault("search_value", "");

        requestMap.put("search_field", searchField);
        requestMap.put("search_value", searchValue);
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("도면 검색 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (getFormula와 동일)
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // API 호출 실패 시 오류 반환
        if (!httpResult.isSuccess()) {
            logger.error("API 서버 연결 실패: {}", httpResult.getBody());
            result.put("success", false);
            result.put("message", "API 서버 연결에 실패했습니다: " + httpResult.getBody());
            return result;
        }

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("도면 검색 응답 파싱 성공");
            } else {
                logger.warn("도면 검색 응답 파싱 실패: {}", responseBody);
            }

            // 새로운 JSON 구조에 맞게 응답 처리
            if (parsedResponse != null) {
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // items 배열에서 도면 정보 추출
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // child_drawings 배열 처리 - 각 child_drawing_id에 대해 추가 API 호출
                        if (item.containsKey("child_drawings") && item.get("child_drawings") instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> childDrawings = (List<Map<String, Object>>) item
                                    .get("child_drawings");
                            logger.debug("child_drawings 발견: {}개 항목", childDrawings.size());

                            List<Map<String, Object>> childDrawingDetails = new ArrayList<>();

                            for (Map<String, Object> childDrawing : childDrawings) {
                                String relationshipType = (String) childDrawing.get("relationship_type");

                                // relationship_type이 PFDCARD_TO_PNID인 경우만 처리
                                if (!"PFDCARD_TO_PNID".equals(relationshipType)) {
                                    logger.debug("relationship_type이 PFDCARD_TO_PNID가 아니므로 건너뜀: {}", relationshipType);
                                    continue;
                                }

                                String childDrawingId = (String) childDrawing.get("child_drawing_id");
                                if (childDrawingId != null && !childDrawingId.trim().isEmpty()) {
                                    try {
                                        // child_drawing_id로 추가 API 호출
                                        Map<String, Object> childSearchParams = new HashMap<>();
                                        childSearchParams.put("search_field", "drawing_id");
                                        childSearchParams.put("search_value", childDrawingId);

                                        Map<String, Object> childResult = getDrawingMaster(childSearchParams);
                                        if (childResult.containsKey("response")
                                                && childResult.get("response") instanceof List) {
                                            @SuppressWarnings("unchecked")
                                            List<Map<String, Object>> childResponse = (List<Map<String, Object>>) childResult
                                                    .get("response");
                                            if (!childResponse.isEmpty()) {
                                                // detail과 current_file 정보 포함
                                                Map<String, Object> childData = new HashMap<>();
                                                Map<String, Object> detailData = childResponse.get(0);

                                                // detail 정보 추가
                                                childData.put("detail", detailData);

                                                // current_file 정보 추가 (detail에서 추출)
                                                if (detailData.containsKey("current_file")
                                                        && detailData.get("current_file") instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> currentFile = (Map<String, Object>) detailData
                                                            .get("current_file");
                                                    childData.put("current_file", currentFile);
                                                }

                                                childDrawingDetails.add(childData);
                                            } else {
                                                // 상세 정보가 없는 경우 빈 객체 사용
                                                Map<String, Object> emptyData = new HashMap<>();
                                                emptyData.put("detail", new HashMap<String, Object>());
                                                emptyData.put("current_file", new HashMap<String, Object>());
                                                childDrawingDetails.add(emptyData);
                                            }
                                        } else {
                                            // API 호출 실패 시 빈 객체 사용
                                            logger.warn("child_drawing_id {} 상세 조회 실패, 빈 객체 사용", childDrawingId);
                                            Map<String, Object> emptyData = new HashMap<>();
                                            emptyData.put("detail", new HashMap<String, Object>());
                                            emptyData.put("current_file", new HashMap<String, Object>());
                                            childDrawingDetails.add(emptyData);
                                        }
                                    } catch (Exception e) {
                                        // 예외 발생 시 빈 객체 사용
                                        logger.error("child_drawing_id {} 상세 조회 중 오류 발생: {}", childDrawingId,
                                                e.getMessage());
                                        Map<String, Object> emptyData = new HashMap<>();
                                        emptyData.put("detail", new HashMap<String, Object>());
                                        emptyData.put("current_file", new HashMap<String, Object>());
                                        childDrawingDetails.add(emptyData);
                                    }
                                }
                            }

                            filteredItem.put("child_drawings", childDrawingDetails);
                        } else {
                            logger.debug("child_drawings 없음 또는 List가 아님: {}", item.get("child_drawings"));
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

                // 데이터가 없는 경우 메시지 추가
                if (filteredResponse.isEmpty()) {
                    result.put("message", "검색 조건에 맞는 도면이 없습니다.");
                    logger.debug("도면 검색 결과: 데이터 없음 - process_id={}, drawing_type={}",
                            searchParams.get("process_id"), searchParams.get("drawing_type"));
                } else {
                    result.put("message", "도면 검색이 완료되었습니다.");
                    logger.debug("도면 검색 결과: {}개 도면 발견", filteredResponse.size());
                }

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus()); // HTTP 상태 코드 추가

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("도면 검색 에러 응답 파싱 성공");
            } else {
                logger.warn("도면 검색 에러 응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.drawingSearchFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    /**
     * 도면 자식 정보 조회 - PNID_TO_SVG, PNID_TO_EXCEL 관계 처리
     * 
     * @param searchParams 검색 파라미터
     * @return 검색 결과
     */
    public Map<String, Object> getDrawingChildInfo(Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();

        // URL 구성 - Drawing Masters API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/search";
        logger.debug("도면 자식 정보 조회 API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        String searchField = (String) searchParams.getOrDefault("search_field", "");
        String searchValue = (String) searchParams.getOrDefault("search_value", "");

        requestMap.put("search_field", searchField);
        requestMap.put("search_value", searchValue);
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        // requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("도면 자식 정보 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // API 호출 실패 시 오류 반환
        if (!httpResult.isSuccess()) {
            logger.error("API 서버 연결 실패: {}", httpResult.getBody());
            result.put("success", false);
            result.put("message", "API 서버 연결에 실패했습니다: " + httpResult.getBody());
            return result;
        }

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // items 배열에서 도면 정보 추출
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 기본 도면 정보 복사
                        String[] basicFields = {
                                "drawing_id", "drawing_number", "drawing_title", "drawing_type",
                                "drawing_status", "revision", "project_id", "process_id"
                        };

                        for (String field : basicFields) {
                            if (item.containsKey(field)) {
                                filteredItem.put(field, item.get(field));
                            }
                        }

                        // child_drawings 배열 처리 - PNID_TO_SVG, PNID_TO_EXCEL 관계에서 ID 추출
                        if (item.containsKey("child_drawings") && item.get("child_drawings") instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> childDrawings = (List<Map<String, Object>>) item
                                    .get("child_drawings");
                            logger.debug("child_drawings 발견: {}개 항목", childDrawings.size());

                            String svgDrawingId = null;
                            String excelDrawingId = null;

                            for (Map<String, Object> childDrawing : childDrawings) {
                                String relationshipType = (String) childDrawing.get("relationship_type");
                                String childDrawingId = (String) childDrawing.get("child_drawing_id");

                                if ("PNID_TO_SVG".equals(relationshipType)) {
                                    svgDrawingId = childDrawingId;
                                    logger.debug("PNID_TO_SVG 관계에서 svg_drawing_id 추출: {}", svgDrawingId);
                                } else if ("PNID_TO_EXCEL".equals(relationshipType)) {
                                    excelDrawingId = childDrawingId;
                                    logger.debug("PNID_TO_EXCEL 관계에서 excel_drawing_id 추출: {}", excelDrawingId);
                                }
                            }

                            // svg_drawing_id 추가
                            if (svgDrawingId != null && !svgDrawingId.trim().isEmpty()) {
                                filteredItem.put("svg_drawing_id", svgDrawingId);
                            }

                            // excel_drawing_id 추가
                            if (excelDrawingId != null && !excelDrawingId.trim().isEmpty()) {
                                filteredItem.put("excel_drawing_id", excelDrawingId);
                            }
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

                // 디버깅용: filteredResponse 내용 출력
                logger.debug("getDrawingChildInfo filteredResponse: {}", filteredResponse);

                // 데이터가 없는 경우 메시지 추가
                if (filteredResponse.isEmpty()) {
                    result.put("message", "조회된 도면 자식 정보가 없습니다.");
                }

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.drawingChildInfoSearchFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    /**
     * 도면 Master 상세 정보 조회 (개별 drawing_id용)
     * 
     * @param searchParams 검색 파라미터
     * @return 검색 결과
     */
    public Map<String, Object> getDrawingMaster(Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();

        // URL 구성 - Drawing Masters API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/search";
        logger.debug("도면 Master 상세 조회 API URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();

        // 추가 검색 필드들 - searchParams에 값이 있는 경우에만 추가
        String[] additionalFields = {
                "search_field", "search_value",
                "drawing_id", "project_id", "process_id", "drawing_number",
                "drawing_title", "drawing_type", "drawing_status", "revision",
                "drawing_category", "drawing_scale"
        };

        for (String field : additionalFields) {
            Object value = searchParams.get(field);
            if (value != null && !value.toString().trim().isEmpty()) {
                requestMap.put(field, value);
                logger.debug("추가 필드 추가: {} = {}", field, value);
            }
        }
        requestMap.put("page", 1);
        // requestMap.put("page_size", 10);
        requestMap.put("order_by", "created_at");
        requestMap.put("order_direction", "asc");

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("도면 Master 상세 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // items 배열에서 도면 정보 추출
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 도면 마스터 정보 직접 추출
                        if (item.containsKey("project_id")) {
                            filteredItem.put("project_id", item.get("project_id"));
                        }
                        if (item.containsKey("process_id")) {
                            filteredItem.put("process_id", item.get("process_id"));
                        }
                        if (item.containsKey("drawing_number")) {
                            filteredItem.put("drawing_number", item.get("drawing_number"));
                        }
                        if (item.containsKey("drawing_title")) {
                            filteredItem.put("drawing_title", item.get("drawing_title"));
                        }
                        if (item.containsKey("drawing_type")) {
                            filteredItem.put("drawing_type", item.get("drawing_type"));
                        }
                        if (item.containsKey("drawing_status")) {
                            filteredItem.put("drawing_status", item.get("drawing_status"));
                        }
                        if (item.containsKey("revision")) {
                            filteredItem.put("revision", item.get("revision"));
                        }
                        if (item.containsKey("drawing_id")) {
                            filteredItem.put("drawing_id", item.get("drawing_id"));
                        }
                        if (item.containsKey("structure_id")) {
                            filteredItem.put("structure_id", item.get("structure_id"));
                        }
                        if (item.containsKey("description")) {
                            filteredItem.put("description", item.get("description"));
                        }
                        if (item.containsKey("current_file_id")) {
                            filteredItem.put("current_file_id", item.get("current_file_id"));
                        }
                        if (item.containsKey("symbol_id")) {
                            filteredItem.put("symbol_id", item.get("symbol_id"));
                        }
                        if (item.containsKey("symbol_uri")) {
                            filteredItem.put("symbol_uri", item.get("symbol_uri"));
                        }
                        if (item.containsKey("created_at")) {
                            String formattedDate = formatDateToYYYYMMDD(item.get("created_at"));
                            filteredItem.put("created_at", formattedDate);
                        }

                        // current_file 객체 처리
                        if (item.containsKey("current_file") && item.get("current_file") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> currentFile = (Map<String, Object>) item.get("current_file");
                            filteredItem.put("current_file", currentFile);
                        }

                        // drawing_type이 PNID인 경우 SVG와 Excel 데이터를 서로 다른 구조에서 추출
                        String drawingType = (String) item.get("drawing_type");
                        if ("PNID".equals(drawingType)) {

                            // 1. SVG 데이터는 child_drawings에서 추출
                            if (item.containsKey("child_drawings") && item.get("child_drawings") instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> childDrawings = (List<Map<String, Object>>) item
                                        .get("child_drawings");

                                for (Map<String, Object> childDrawing : childDrawings) {
                                    String relationshipType = (String) childDrawing.get("relationship_type");
                                    String childDrawingId = (String) childDrawing.get("child_drawing_id");

                                    if ("PNID_TO_SVG".equals(relationshipType) && childDrawingId != null
                                            && !childDrawingId.trim().isEmpty()) {
                                        filteredItem.put("svg_drawing_id", childDrawingId);

                                        // child_drawing_files에서 file_name 추출
                                        String svgFileName = "테스트.svg"; // 기본값
                                        if (childDrawing.containsKey("child_drawing_files")
                                                && childDrawing.get("child_drawing_files") instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> childDrawingFiles = (Map<String, Object>) childDrawing
                                                    .get("child_drawing_files");
                                            if (childDrawingFiles.containsKey("file_name")
                                                    && childDrawingFiles.get("file_name") != null) {
                                                svgFileName = (String) childDrawingFiles.get("file_name");
                                            }
                                        }

                                        filteredItem.put("svg_file_name", svgFileName);
                                        logger.debug("PNID 도면에 svg_drawing_id 추가: {}, svg_file_name: {}",
                                                childDrawingId, svgFileName);
                                    } else if ("PNID_TO_EXCEL".equals(relationshipType) && childDrawingId != null
                                            && !childDrawingId.trim().isEmpty()) {
                                        filteredItem.put("excel_drawing_id", childDrawingId);

                                        // child_drawing_info에서 drawing_title 추출하여 파일명으로 사용
                                        String excelFileName = "테스트.xlsx"; // 기본값
                                        if (childDrawing.containsKey("child_drawing_info")
                                                && childDrawing.get("child_drawing_info") instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> childDrawingInfo = (Map<String, Object>) childDrawing
                                                    .get("child_drawing_info");
                                            if (childDrawingInfo.containsKey("drawing_title")
                                                    && childDrawingInfo.get("drawing_title") != null) {
                                                String drawingTitle = (String) childDrawingInfo.get("drawing_title");
                                                // "PID-Excel 매핑: 엑셀업로드샘플.xlsx" 형태에서 파일명 추출
                                                if (drawingTitle.contains(": ") && drawingTitle.contains(".xlsx")) {
                                                    String[] parts = drawingTitle.split(": ", 2);
                                                    if (parts.length > 1) {
                                                        excelFileName = parts[1];
                                                    }
                                                } else {
                                                    excelFileName = drawingTitle;
                                                }
                                            }
                                        }

                                        filteredItem.put("excel_file_name", excelFileName);
                                        logger.debug("PNID 도면에 excel_drawing_id 추가: {}, excel_file_name: {}",
                                                childDrawingId, excelFileName);
                                    }
                                }
                            }

                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                result.put("success", true);
                result.put("response", filteredResponse);
            } else {
                result.put("success", false);
                result.put("response", new ArrayList<>());
            }
        } else {
            result.put("success", false);
            result.put("response", new ArrayList<>());
        }

        return result;
    }

    /**
     * 도면 수정
     * 
     * @param drawingId   수정할 도면 ID
     * @param drawingData 수정할 도면 데이터
     * @param siteFile    도면 파일 (선택사항)
     * @param symbolFile  심볼 파일 (선택사항)
     * @return 수정 결과
     */
    public Map<String, Object> updateDrawing(String drawingId, Map<String, Object> drawingData,
            org.springframework.web.multipart.MultipartFile siteFile,
            org.springframework.web.multipart.MultipartFile symbolFile) {
        Map<String, Object> result = new HashMap<>();

        if (drawingId == null || drawingId.trim().isEmpty()) {
            logger.warn("도면 수정 실패: drawing_id가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "도면 ID가 필요합니다.");
            return result;
        }

        if (drawingData == null) {
            logger.debug("도면 데이터가 null이므로 관련 처리를 skip합니다.");
            // drawingData가 null인 경우 기본값으로 처리
            drawingData = new HashMap<>();
        }

        // symbol_id가 입력되지 않고 symbolFile이 있는 경우 심볼 생성 수행 (updateProcess와 동일한 로직)
        String symbolId = null;
        String inputSymbolId = (String) drawingData.get("symbol_id");

        // symbol_id가 입력되지 않고 symbolFile이 있는 경우 createSymbol 호출
        if ((inputSymbolId == null || inputSymbolId.trim().isEmpty()) && symbolFile != null && !symbolFile.isEmpty()) {
            logger.debug("심볼 파일이 제공됨, 심볼 생성 수행: fileName={}, size={}", symbolFile.getOriginalFilename(),
                    symbolFile.getSize());

            // drawingData에서 심볼 관련 데이터 추출
            Map<String, Object> symbolData = new HashMap<>();

            // symbol_name과 symbol_code가 비어있으면 기본값 생성
            String symbolName = (String) drawingData.getOrDefault("drawing_title", "");
            String symbolCode = (String) drawingData.getOrDefault("drawing_number", "");

            // drawing_title이 비어있으면 기본값 생성 (최대 30자로 제한)
            if (symbolName == null || symbolName.trim().isEmpty()) {
                symbolName = "Drawing_" + drawingId;
                // 데이터베이스 제한에 맞게 30자로 제한
                if (symbolName.length() > 30) {
                    symbolName = symbolName.substring(0, 30);
                }
                logger.debug("drawing_title이 비어있어 기본값 생성: {}", symbolName);
            }

            // drawing_number가 비어있으면 고유한 심볼 코드 생성 (최대 30자로 제한)
            if (symbolCode == null || symbolCode.trim().isEmpty()) {
                symbolCode = "SYM_" + drawingId + "_" + System.currentTimeMillis();
                // 데이터베이스 제한에 맞게 30자로 제한
                if (symbolCode.length() > 30) {
                    symbolCode = symbolCode.substring(0, 30);
                }
                logger.debug("drawing_number가 비어있어 기본값 생성: {}", symbolCode);
            }

            // symbol_name과 symbol_code 길이 제한 (데이터베이스 제약사항)
            if (symbolName.length() > 30) {
                symbolName = symbolName.substring(0, 30);
                logger.debug("symbol_name이 30자를 초과하여 잘림: {}", symbolName);
            }
            if (symbolCode.length() > 30) {
                symbolCode = symbolCode.substring(0, 30);
                logger.debug("symbol_code가 30자를 초과하여 잘림: {}", symbolCode);
            }

            // symbol_name과 symbol_code가 여전히 비어있는지 최종 검증
            if (symbolName.trim().isEmpty() || symbolCode.trim().isEmpty()) {
                logger.warn("심볼 생성에 필요한 필수 필드가 비어있음: symbol_name='{}', symbol_code='{}'", symbolName, symbolCode);
                result.put("symbol_creation_skipped", true);
                result.put("symbol_error_message", "심볼 생성에 필요한 필수 필드(drawing_title, drawing_number)가 비어있습니다.");
            } else {
                symbolData.put("symbol_name", symbolName.trim());
                symbolData.put("symbol_code", symbolCode.trim());
                symbolData.put("symbol_type", "PROCESS");
                symbolData.put("siteFile", symbolFile);

                logger.debug("심볼 생성 데이터: symbol_name={}, symbol_code={}, symbol_type={}",
                        symbolName, symbolCode, "PROCESS");

                // createSymbol 호출하여 symbol_id 획득
                Map<String, Object> symbolResult = createSymbol(symbolData);
                if ((Boolean) symbolResult.get("success")) {
                    symbolId = (String) symbolResult.get("symbol_id");
                    logger.debug("심볼 생성 성공, symbol_id: {}", symbolId);
                } else {
                    logger.warn("심볼 생성 실패: {}", symbolResult.get("message"));
                    // 심볼 생성 실패해도 도면은 수정 계속 진행
                    // result에 심볼 생성 실패 정보를 저장하지만 도면 수정은 계속
                    result.put("symbol_creation_failed", true);
                    result.put("symbol_error_message", symbolResult.get("message"));
                }
            }
        } else if (!isEmptySymbolId(inputSymbolId)) {
            // symbol_id가 입력된 경우 기존 symbol_id 사용
            symbolId = inputSymbolId.trim();
            logger.debug("기존 symbol_id 사용: {}", symbolId);
        }

        // siteFile이 있는 경우 파일 업로드 수행
        if (siteFile != null && !siteFile.isEmpty()) {
            // 파일 업로드 함수 호출
            Map<String, Object> uploadResult = uploadDrawingFile(drawingId, siteFile, drawingData);
            if ((Boolean) uploadResult.get("success")) {
                result.put("file_uploaded", true);
            } else {
                logger.warn("도면 파일 업로드 실패: {}", uploadResult.get("message"));
                // 파일 업로드 실패해도 도면 정보는 수정 계속 진행
                result.put("file_uploaded", false);
                result.put("file_upload_error", uploadResult.get("message"));
            }
        }

        // drawingData에 실제 수정할 데이터가 있는지 확인
        boolean hasDrawingDataToUpdate = false;
        if (drawingData != null && !drawingData.isEmpty()) {
            // 실제 값이 있는 필드가 있는지 확인
            String[] fields = {
                    "drawing_number", "drawing_title", "drawing_type",
                    "drawing_status", "revision", "revision_date", "drawing_category",
                    "drawing_scale", "paper_size", "svg_content", "is_lasted",
                    "approved_at", "approved_by", "current_file_id", "description"
            };

            for (String field : fields) {
                Object value = drawingData.get(field);
                if (value != null && !value.toString().trim().isEmpty()) {
                    hasDrawingDataToUpdate = true;
                    break;
                }
            }

            // project_id가 기본값이 아닌 경우도 체크
            Object projectId = drawingData.get("project_id");
            if (projectId != null && !projectId.toString().trim().isEmpty()
                    && !"0198a173-afc7-7bb2-a294-afdf428c26f5".equals(projectId.toString())) {
                hasDrawingDataToUpdate = true;
            }
        }

        // symbol_id가 생성된 경우에도 도면 정보 수정 수행 (symbol_id를 전달하기 위해)
        if (!isEmptySymbolId(symbolId)) {
            hasDrawingDataToUpdate = true;
            logger.debug("symbol_id가 생성되어 도면 정보 수정 수행: symbol_id={}", symbolId);
        }

        // drawingData에 실제 수정할 데이터가 있는 경우에만 도면 정보 수정 수행
        if (hasDrawingDataToUpdate) {
            logger.debug("도면 데이터 수정 수행: drawing_id={}", drawingId);
        } else {
            logger.debug("도면 데이터 수정할 내용이 없어 skip: drawing_id={}", drawingId);
            // 파일 업로드만 수행된 경우 성공으로 처리
            if (siteFile != null && !siteFile.isEmpty()) {
                result.put("success", true);
                result.put("message", "도면 파일이 성공적으로 업로드되었습니다.");
                result.put("drawing_id", drawingId);
            } else {
                result.put("success", true);
                result.put("message", "수정할 내용이 없습니다.");
                result.put("drawing_id", drawingId);
            }
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/" + drawingId;

            // 요청 본문 구성 - 입력된 값이 있는 경우만 포함
            Map<String, Object> requestMap = new HashMap<>();

            // project_id 처리
            Object projectId = drawingData.get("project_id");
            requestMap.put("project_id", projectId);

            // 나머지 필드들에 대해 값이 있는 경우만 requestMap에 추가
            String[] fields = {
                    "drawing_number", "drawing_title", "drawing_type",
                    "drawing_status", "revision", "revision_date", "drawing_category",
                    "drawing_scale", "paper_size", "svg_content", "is_lasted",
                    "approved_at", "approved_by", "current_file_id", "description"
            };

            for (String field : fields) {
                Object value = drawingData.get(field);
                if (value != null && !value.toString().trim().isEmpty()) {
                    requestMap.put(field, value);
                    logger.debug("도면 수정 필드 추가: {} = {}", field, value);
                }
            }

            // symbol_id 처리 (심볼이 생성된 경우)
            if (!isEmptySymbolId(symbolId)) {
                requestMap.put("symbol_id", symbolId);
                logger.debug("도면에 symbol_id 추가: {}", symbolId);
            } else {
                logger.debug("symbol_id가 null이거나 비어있어 추가하지 않음");
            }

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("도면 수정 요청 본문: {}", requestBody);

            // HttpUtil을 사용하여 PATCH 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.patch(apiUrl, "application/json", requestBody);

            if (httpResult.isSuccess()) {
                logger.debug("도면 수정 성공: drawing_id={}", drawingId);
                result.put("success", true);
                result.put("message", "도면이 성공적으로 수정되었습니다.");
                result.put("drawing_id", drawingId);

                // 응답 본문이 있는 경우 포함
                if (httpResult.getBody() != null && !httpResult.getBody().trim().isEmpty()) {
                    try {
                        Map<String, Object> responseData = JsonUtil.parseJson(httpResult.getBody());
                        result.put("response", responseData);
                        logger.debug("도면 수정 응답: {}", responseData);
                    } catch (Exception e) {
                        logger.debug("응답 본문 파싱 실패, 원본 응답 사용: {}", httpResult.getBody());
                        result.put("response", httpResult.getBody());
                    }
                }
            } else {
                logger.warn("도면 수정 실패: drawing_id={}, status={}, body={}",
                        drawingId, httpResult.getStatus(), httpResult.getBody());
                result.put("success", false);
                result.put("message", "도면 수정에 실패했습니다: " + httpResult.getBody());
                result.put("drawing_id", drawingId);
            }

        } catch (Exception e) {
            logger.error("도면 수정 API 호출 중 오류 발생: drawing_id={}", drawingId, e);
            result.put("success", false);
            result.put("message", "도면 수정 중 오류가 발생했습니다: " + e.getMessage());
            result.put("drawing_id", drawingId);
        }

        return result;
    }

    /**
     * 도면 파일 업로드
     * 
     * @param drawingId   도면 ID
     * @param file        업로드할 파일
     * @param drawingData 도면 데이터 (drawing_version 등 포함)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadDrawingFile(String drawingId, org.springframework.web.multipart.MultipartFile file,
            Map<String, Object> drawingData) {
        Map<String, Object> result = new HashMap<>();

        if (drawingId == null || drawingId.trim().isEmpty()) {
            logger.warn("도면 파일 업로드 실패: drawing_id가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "도면 ID가 필요합니다.");
            return result;
        }

        if (file == null || file.isEmpty()) {
            logger.warn("도면 파일 업로드 실패: 파일이 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "업로드할 파일이 필요합니다.");
            return result;
        }

        try {
            // 파일 크기 검증 (100MB = 100 * 1024 * 1024 bytes)
            long maxFileSize = 100L * 1024 * 1024; // 100MB
            if (file.getSize() > maxFileSize) {
                logger.warn("파일 크기 초과: {} bytes (최대: {} bytes)", file.getSize(), maxFileSize);
                result.put("success", false);
                result.put("message", "파일 크기가 너무 큽니다. 최대 100MB까지 업로드 가능합니다.");
                return result;
            }

            // 파일 업로드용 데이터 구성
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("file", file);
            uploadData.put("drawing_version", drawingData.getOrDefault("drawing_version", "1.0.0"));

            String uploadApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/upload/" + drawingId;

            // HttpUtil을 사용하여 파일 업로드
            HttpUtil.HttpResult uploadResult = HttpUtil.postMultipart(uploadApiUrl, uploadData);

            if (uploadResult.isSuccess()) {
                logger.debug("도면 파일 업로드 성공");
                result.put("success", true);
                result.put("status", uploadResult.getStatus());
                result.put("message", "도면 파일이 성공적으로 업로드되었습니다.");
                result.put("drawing_id", drawingId);

                // 응답 본문이 있는 경우 포함
                if (uploadResult.getBody() != null && !uploadResult.getBody().trim().isEmpty()) {
                    try {
                        Map<String, Object> responseData = JsonUtil.parseJson(uploadResult.getBody());
                        result.put("response", responseData);
                    } catch (Exception e) {
                        logger.debug("응답 본문 파싱 실패, 원본 응답 사용: {}", uploadResult.getBody());
                        result.put("response", uploadResult.getBody());
                    }
                }
            } else {
                logger.warn("도면 파일 업로드 실패: {}", uploadResult.getExtractedErrorMessage());
                result.put("success", false);
                result.put("status", uploadResult.getStatus());
                result.put("message", "도면 파일 업로드에 실패했습니다: " + uploadResult.getExtractedErrorMessage());
                result.put("drawing_id", drawingId);

                // 응답 본문이 있는 경우 포함
                if (uploadResult.getBody() != null && !uploadResult.getBody().trim().isEmpty()) {
                    try {
                        Map<String, Object> responseData = JsonUtil.parseJson(uploadResult.getBody());
                        result.put("response", responseData);
                    } catch (Exception e) {
                        logger.debug("응답 본문 파싱 실패, 원본 응답 사용: {}", uploadResult.getBody());
                        result.put("response", uploadResult.getBody());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("도면 파일 업로드 API 호출 중 오류 발생: drawing_id={}", drawingId, e);
            result.put("success", false);
            result.put("message", "도면 파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            result.put("drawing_id", drawingId);
        }

        return result;
    }

    /**
     * 도면파일 목록조회
     * 
     * @param searchParams 검색 파라미터
     * @return 검색 결과
     */
    public Map<String, Object> getDrawingFilesList(Map<String, Object> searchParams) {
        Map<String, Object> result = new HashMap<>();

        // URL 구성 - Drawing Masters API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/list";
        logger.debug("도면 검색 API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("drawing_id", searchParams.getOrDefault("drawing_id", ""));
        requestMap.put("project_id", searchParams.getOrDefault("project_id", ""));
        requestMap.put("process_id", searchParams.getOrDefault("process_id", ""));

        // drawing_type이 지정되지 않았거나 빈 값인 경우 모든 타입 검색
        String drawingType = (String) searchParams.getOrDefault("drawing_type", "");
        if (drawingType != null && !drawingType.trim().isEmpty()) {
            requestMap.put("drawing_type", drawingType);
        }
        // drawing_type이 없으면 모든 타입 검색 (필터링 제거)

        requestMap.put("is_current", searchParams.getOrDefault("is_current", true));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("도면 검색 요청 본문: {}", requestBody);

        // GET 요청에서 쿼리 파라미터로 필터링 적용
        StringBuilder queryParams = new StringBuilder();
        boolean firstParam = true;

        for (Map.Entry<String, Object> entry : requestMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().trim().isEmpty()) {
                if (firstParam) {
                    queryParams.append("?");
                    firstParam = false;
                } else {
                    queryParams.append("&");
                }
                queryParams.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        String fullUrl = apiUrl + queryParams.toString();
        logger.debug("도면 검색 전체 URL: {}", fullUrl);

        // HttpUtil을 사용하여 GET 요청 수행 (쿼리 파라미터 포함)
        HttpUtil.HttpResult httpResult = HttpUtil.get(fullUrl, "application/json", null);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("도면 검색 응답 파싱 성공");
            } else {
                logger.warn("도면 검색 응답 파싱 실패: {}", responseBody);
            }

            // 특정 키만 남기기
            if (parsedResponse != null) {
                List<Map<String, Object>> filteredResponse = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        Map<String, Object> filteredItem = new HashMap<>();

                        // 파일 관련 정보 추가
                        if (item.containsKey("file_id")) {
                            filteredItem.put("file_id", item.get("file_id"));
                        }
                        if (item.containsKey("drawing_id")) {
                            filteredItem.put("drawing_id", item.get("drawing_id"));
                        }
                        if (item.containsKey("drawing_version")) {
                            filteredItem.put("drawing_version", item.get("drawing_version"));
                        }
                        if (item.containsKey("file_name")) {
                            filteredItem.put("file_name", item.get("file_name"));
                        }
                        if (item.containsKey("file_uri")) {
                            filteredItem.put("file_uri", item.get("file_uri"));
                        }
                        if (item.containsKey("file_size")) {
                            filteredItem.put("file_size", item.get("file_size"));
                        }
                        if (item.containsKey("file_type")) {
                            filteredItem.put("file_type", item.get("file_type"));
                        }
                        if (item.containsKey("mime_type")) {
                            filteredItem.put("mime_type", item.get("mime_type"));
                        }
                        if (item.containsKey("file_hash")) {
                            filteredItem.put("file_hash", item.get("file_hash"));
                        }
                        if (item.containsKey("is_current")) {
                            filteredItem.put("is_current", item.get("is_current"));
                        }
                        if (item.containsKey("uploaded_at")) {
                            String formattedDate = formatDateToYYYYMMDD(item.get("uploaded_at"));
                            filteredItem.put("uploaded_at", formattedDate);
                        }
                        if (item.containsKey("uploaded_by")) {
                            filteredItem.put("uploaded_by", item.get("uploaded_by"));
                        }
                        if (item.containsKey("created_at")) {
                            String formattedDate = formatDateToYYYYMMDD(item.get("created_at"));
                            filteredItem.put("created_at", formattedDate);
                        }
                        if (item.containsKey("created_by")) {
                            filteredItem.put("created_by", item.get("created_by"));
                        }

                        // drawing_master 객체에서 도면 마스터 정보 추출
                        if (item.containsKey("drawing_master") && item.get("drawing_master") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> drawingMaster = (Map<String, Object>) item.get("drawing_master");

                            if (drawingMaster.containsKey("project_id")) {
                                filteredItem.put("project_id", drawingMaster.get("project_id"));
                            }
                            if (drawingMaster.containsKey("drawing_number")) {
                                filteredItem.put("drawing_number", drawingMaster.get("drawing_number"));
                            }
                            if (drawingMaster.containsKey("drawing_title")) {
                                filteredItem.put("drawing_title", drawingMaster.get("drawing_title"));
                            }
                            if (drawingMaster.containsKey("drawing_type")) {
                                filteredItem.put("drawing_type", drawingMaster.get("drawing_type"));
                            }
                            if (drawingMaster.containsKey("drawing_status")) {
                                filteredItem.put("drawing_status", drawingMaster.get("drawing_status"));
                            }
                            if (drawingMaster.containsKey("revision")) {
                                filteredItem.put("revision", drawingMaster.get("revision"));
                            }
                        }

                        filteredResponse.add(filteredItem);
                    }
                }

                // 성공으로 설정
                result.put("success", true);
                result.put("response", filteredResponse);

                // 데이터가 없는 경우 메시지 추가
                if (filteredResponse.isEmpty()) {
                    result.put("message", "messages.error.noDrawingFound");
                    logger.debug("도면 검색 결과: 데이터 없음 - process_id={}, drawing_type={}",
                            searchParams.get("process_id"), searchParams.get("drawing_type"));
                } else {
                    result.put("message", "messages.success.drawingSearchComplete");
                    logger.debug("도면 검색 결과: {}개 도면 발견", filteredResponse.size());
                }

            } else {
                // parsedResponse가 null인 경우
                result.put("success", false);
                result.put("response", new HashMap<>());
                return result;
            }

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus()); // HTTP 상태 코드 추가

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("도면 검색 에러 응답 파싱 성공");
            } else {
                logger.warn("도면 검색 에러 응답 파싱 실패: {}", responseBody);
            }

            // HTTP 에러 응답에서 message 또는 detail 메시지 추출
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.drawingSearchFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);
        }

        return result;
    }

    // Create Process Formula
    public Map<String, Object> createFormula(Map<String, Object> formulaData,
            org.springframework.web.multipart.MultipartFile siteFile) {
        logger.debug("계산식 등록 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();

        if (formulaData == null) {
            logger.warn("계산식 등록 실패: 계산식 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidFormulaData");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/minio/formula/upload";
        logger.debug("계산식 등록 URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("formula_scope", formulaData.getOrDefault("formula_scope", "PROCESS"));
        requestMap.put("process_id", formulaData.getOrDefault("process_id", null));

        // python_file이 필수이므로 파일이 없으면 에러 반환
        if (siteFile == null || siteFile.isEmpty()) {
            logger.warn("계산식 등록 실패: python_file이 필요합니다");
            result.put("success", false);
            result.put("message", "python_file이 필요합니다");
            return result;
        }

        logger.debug("파일 업로드: fileName={}, size={}", siteFile.getOriginalFilename(), siteFile.getSize());

        // multipart 데이터 구성
        Map<String, Object> multipartData = new HashMap<>();
        multipartData.put("python_file", siteFile); // 외부 API가 기대하는 키명

        // 기존 파라미터들을 multipart 데이터에 추가
        for (Map.Entry<String, Object> entry : requestMap.entrySet()) {
            if (entry.getValue() != null) {
                multipartData.put(entry.getKey(), entry.getValue());
            }
        }

        logger.debug("multipart 데이터: {}", multipartData);

        // HttpUtil을 사용하여 multipart 전송
        HttpUtil.HttpResult httpResult = null;
        try {
            httpResult = HttpUtil.postMultipart(apiUrl, multipartData);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // HTTP 클라이언트 오류 (4xx) 처리 - 응답 본문 추출
            String responseBody = e.getResponseBodyAsString();
            int statusCode = e.getStatusCode().value();
            logger.error("계산식 등록 API 호출 중 HTTP 오류 발생: status={}, body={}", statusCode, responseBody);

            // 응답 본문에서 에러 메시지 추출
            String errorMessage = null;
            if (responseBody != null && !responseBody.isEmpty()) {
                // JSON 응답에서 detail.message 또는 detail, message 추출 시도
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                if (parsedResponse != null) {
                    Object detailObj = parsedResponse.get("detail");
                    if (detailObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> detail = (Map<String, Object>) detailObj;
                        Object messageObj = detail.get("message");
                        if (messageObj != null) {
                            errorMessage = messageObj.toString();
                        }
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "HTTP " + statusCode + " 오류가 발생했습니다.";
            }

            result.put("success", false);
            result.put("status", statusCode);
            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", JsonUtil.parseJson(responseBody));

            logger.warn("계산식 등록 실패: status={}, message={}", statusCode, errorMessage);
            return result;
        } catch (Exception e) {
            logger.error("계산식 등록 API 호출 중 예기치 않은 오류 발생", e);
            result.put("success", false);
            result.put("message", "계산식 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }

        // 응답 처리
        // httpResult가 null이거나 status가 -1인 경우 실패로 처리
        if (httpResult == null || httpResult.getStatus() == -1) {
            result.put("success", false);
            result.put("status", httpResult != null ? httpResult.getStatus() : -1);

            String errorMessage = null;
            if (httpResult != null) {
                String responseBody = httpResult.getBody();
                if (responseBody != null && !responseBody.isEmpty()) {
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    if (parsedResponse != null) {
                        Object detailObj = parsedResponse.get("detail");
                        if (detailObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> detail = (Map<String, Object>) detailObj;
                            Object messageObj = detail.get("message");
                            if (messageObj != null) {
                                errorMessage = messageObj.toString();
                            }
                        }
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "message");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = httpResult.getExtractedErrorMessage();
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = httpResult.getErrorMessage();
                }
            }

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "계산식 등록 API 호출 중 오류가 발생했습니다.";
            }

            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", httpResult != null ? JsonUtil.parseJson(httpResult.getBody()) : null);

            logger.warn("외부 인증 서버 계산식 등록 실패: status={}, message={}",
                    httpResult != null ? httpResult.getStatus() : -1, errorMessage);
            return result;
        }

        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅: 응답 내용 로깅
            logger.debug("계산식 등록 응답 내용: {}", parsedResponse);

            // 응답코드가 200인 경우만 성공으로 처리
            if (httpResult.getStatus() == 200) {
                // 계산식 등록 성공 여부 확인 - data.formula_id가 있는지 확인
                logger.debug("계산식 등록 응답 검증 시작 - HTTP 상태: {}", httpResult.getStatus());
                boolean hasFormulaId = false;
                if (parsedResponse != null && parsedResponse.containsKey("data")
                        && parsedResponse.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) parsedResponse.get("data");
                    hasFormulaId = data.containsKey("formula_id");
                }
                logger.debug("계산식 등록 응답 검증 결과 - formula_id 존재: {}", hasFormulaId);

                if (hasFormulaId) {
                    logger.debug("계산식 등록 성공");
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "messages.success.formulaCreateSuccess");
                    result.put("response", parsedResponse);

                    // formula_id 추출하여 결과에 포함
                    if (parsedResponse != null && parsedResponse.containsKey("data")
                            && parsedResponse.get("data") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) parsedResponse.get("data");
                        if (data.containsKey("formula_id")) {
                            result.put("formula_id", data.get("formula_id"));
                            logger.debug("계산식 등록 성공 - formula_id: {}", data.get("formula_id"));
                        }
                    }
                } else {
                    // 응답 코드는 성공이지만 내용에 문제가 있는 경우 (중복 파일 등)
                    String errorMessage = null;

                    // 1. 중첩된 detail.message 추출 시도
                    if (parsedResponse != null) {
                        Object detailObj = parsedResponse.get("detail");
                        if (detailObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> detail = (Map<String, Object>) detailObj;
                            Object messageObj = detail.get("message");
                            if (messageObj != null) {
                                errorMessage = messageObj.toString();
                            }
                        }
                    }

                    // 2. 최상위 message 또는 detail 추출
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "message");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }

                    // 3. warnings 배열에서 메시지 추출
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        if (parsedResponse != null && parsedResponse.containsKey("warnings")
                                && parsedResponse.get("warnings") instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> warnings = (List<String>) parsedResponse.get("warnings");
                            if (!warnings.isEmpty()) {
                                errorMessage = String.join(" ", warnings);
                            }
                        }
                    }

                    // 4. 기본 메시지
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "계산식 등록에 실패했습니다.";
                    }

                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("errorMessage", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("계산식 등록 실패 (응답 내용 검증 실패): {}", errorMessage);
                    return result;
                }
            } else {
                // 응답코드가 200이 아닌 경우 에러로 처리
                String errorMessage = null;

                // 1. 중첩된 detail.message 추출 시도
                if (parsedResponse != null) {
                    Object detailObj = parsedResponse.get("detail");
                    if (detailObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> detail = (Map<String, Object>) detailObj;
                        Object messageObj = detail.get("message");
                        if (messageObj != null) {
                            errorMessage = messageObj.toString();
                        }
                    }
                }

                // 2. 최상위 message 또는 detail 추출
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                // 3. 기본 메시지
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "messages.error.formulaCreateFail";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("errorMessage", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("계산식 등록 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                return result;
            }
        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            logger.debug("계산식 등록 실패 응답 본문: {}", responseBody);

            Map<String, Object> parsedResponse = null;
            if (responseBody != null && !responseBody.isEmpty()) {
                parsedResponse = JsonUtil.parseJson(responseBody);
            }

            // 에러 메시지 추출 (중첩된 JSON 구조 지원)
            String errorMessage = null;

            // 1. 중첩된 detail.message 추출 시도 (우선순위 높음)
            if (parsedResponse != null) {
                Object detailObj = parsedResponse.get("detail");
                if (detailObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detail = (Map<String, Object>) detailObj;
                    Object messageObj = detail.get("message");
                    if (messageObj != null) {
                        errorMessage = messageObj.toString();
                        logger.debug("중첩된 detail.message 추출 성공: {}", errorMessage);
                    }
                }
            }

            // 2. HttpUtil에서 추출된 에러 메시지 사용
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = httpResult.getExtractedErrorMessage();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    logger.debug("HttpUtil에서 추출된 에러 메시지: {}", errorMessage);
                }
            }

            // 3. 최상위 message 또는 detail 추출
            if ((errorMessage == null || errorMessage.isEmpty()) && responseBody != null && !responseBody.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
            }

            // 4. 기본 메시지
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "messages.error.formulaCreateFail";
                logger.warn("에러 메시지를 추출하지 못하여 기본 메시지 사용");
            }

            result.put("message", errorMessage);
            result.put("errorMessage", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 계산식 등록 실패: status={}, message={}", httpResult.getStatus(), errorMessage);
        }

        return result;
    }

    /**
     * 심볼 생성
     * 
     * @param symbolData 생성할 심볼 데이터
     * @return 생성 결과
     */
    public Map<String, Object> createSymbol(Map<String, Object> symbolData) {
        logger.debug("심볼 등록 API 호출");
        logger.debug("입력 symbolData: {}", symbolData);
        Map<String, Object> result = new HashMap<>();

        try {
            // symbol_code를 파라미터 + YYMMDDHHMMSS 형식으로 수정 (최대 30자 제한)
            String originalSymbolCode = (String) symbolData.getOrDefault("symbol_code", "");
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));

            // 총 길이가 30자를 넘지 않도록 원본 코드 길이 조정
            int maxOriginalLength = 30 - timestamp.length(); // 30 - 12 = 18자
            if (originalSymbolCode.length() > maxOriginalLength) {
                originalSymbolCode = originalSymbolCode.substring(0, maxOriginalLength);
                logger.debug("symbol_code 원본이 너무 길어 {}자로 자름: {}", maxOriginalLength, originalSymbolCode);
            }

            String modifiedSymbolCode = originalSymbolCode + timestamp;
            logger.debug("symbol_code 수정: {} -> {} (길이: {})",
                    (String) symbolData.getOrDefault("symbol_code", ""), modifiedSymbolCode,
                    modifiedSymbolCode.length());

            String apiUrl = authServerBaseUrl + "/api/v1/minio/symbols/upload";
            logger.debug("심볼 등록 URL: {}", apiUrl);

            // MultipartFile이 있는지 확인
            Object symbolFile = symbolData.get("siteFile");
            if (symbolFile instanceof org.springframework.web.multipart.MultipartFile) {
                org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) symbolFile;
                if (file != null && !file.isEmpty()) {
                    // 파일이 있는 경우 multipart로 전송
                    logger.debug("파일 업로드: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

                    // 외부 API가 요구하는 'file' 필드명으로 데이터 구성
                    Map<String, Object> uploadData = new HashMap<>();
                    uploadData.put("symbol_name", symbolData.getOrDefault("symbol_name", ""));
                    uploadData.put("symbol_code", modifiedSymbolCode);
                    uploadData.put("symbol_type", symbolData.getOrDefault("symbol_type", "PROCESS"));
                    uploadData.put("file", file);

                    logger.debug("multipart 업로드 데이터: symbol_name={}, symbol_code={}, symbol_type={}",
                            uploadData.get("symbol_name"), uploadData.get("symbol_code"),
                            uploadData.get("symbol_type"));

                    HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(apiUrl, uploadData);

                    if (httpResult.isSuccess()) {
                        String responseBody = httpResult.getBody();
                        Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                        logger.debug("심볼 등록 응답 내용: {}", parsedResponse);

                        // symbol_id 추출
                        String symbolId = JsonUtil.extractValue(responseBody, "symbol_id");
                        if (symbolId == null || symbolId.isEmpty()) {
                            // 다른 가능한 필드명들도 확인
                            symbolId = JsonUtil.extractValue(responseBody, "id");
                        }

                        logger.debug("심볼 등록 API 성공");
                        result.put("success", true);
                        result.put("status", httpResult.getStatus());
                        result.put("message", "messages.success.symbolRegisterSuccess");
                        result.put("response", parsedResponse);
                        if (!isEmptySymbolId(symbolId)) {
                            result.put("symbol_id", symbolId);
                        }
                    } else {
                        String responseBody = httpResult.getBody();
                        Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                        logger.debug("심볼 등록 실패 응답: HTTP {}, Body: {}", httpResult.getStatus(), parsedResponse);

                        String errorMessage = JsonUtil.extractValue(responseBody, "message");
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = JsonUtil.extractValue(responseBody, "detail");
                        }

                        // 중복 키 오류인지 확인 (symbol_code 중복)
                        if (httpResult.getStatus() == 500 && errorMessage != null &&
                                errorMessage.contains("duplicate key value violates unique constraint") &&
                                errorMessage.contains("symbols_symbol_code_key")) {

                            logger.debug("심볼 코드 중복 감지, 기존 심볼 조회 시도: {}", modifiedSymbolCode);

                            // 기존 심볼 조회 시도 (수정된 symbol_code 사용)
                            if (modifiedSymbolCode != null && !modifiedSymbolCode.isEmpty()) {
                                Map<String, Object> existingSymbol = getSymbolByCode(modifiedSymbolCode);
                                if (existingSymbol != null && (Boolean) existingSymbol.get("success")) {
                                    String existingSymbolId = (String) existingSymbol.get("symbol_id");
                                    if (existingSymbolId != null && !existingSymbolId.isEmpty()) {
                                        logger.debug("기존 심볼 발견, symbol_id 반환: {}", existingSymbolId);
                                        result.put("success", true);
                                        result.put("status", 200);
                                        result.put("message", "기존 심볼을 사용합니다.");
                                        result.put("symbol_id", existingSymbolId);
                                        result.put("response", existingSymbol.get("response"));
                                        return result;
                                    }
                                }
                            }
                        }

                        result.put("success", false);
                        result.put("status", httpResult.getStatus());
                        result.put("message", errorMessage != null ? errorMessage : "심볼 등록에 실패했습니다.");
                        result.put("response", parsedResponse);

                        logger.warn("심볼 등록 API 실패: {}", errorMessage);
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "messages.error.noFileToRegister");
                }
            } else {
                // 파일이 없는 경우 일반 JSON으로 전송
                // 요청 본문 구성 - 화면에서 받은 파라미터 사용
                Map<String, Object> requestMap = new HashMap<>();

                // 기본값 설정
                requestMap.put("symbol_code", modifiedSymbolCode);
                requestMap.put("symbol_name", symbolData.getOrDefault("symbol_name", ""));
                requestMap.put("symbol_type", symbolData.getOrDefault("symbol_type", "PROCESS"));
                requestMap.put("symbol_uri", symbolData.getOrDefault("symbol_uri", ""));
                requestMap.put("symbol_color", symbolData.getOrDefault("symbol_color", "#3498db"));
                requestMap.put("svg_content", symbolData.getOrDefault("svg_content", ""));
                requestMap.put("symbol_description", symbolData.getOrDefault("symbol_description", ""));

                String requestBody = JsonUtil.objectMapToJson(requestMap);
                logger.debug("심볼 등록 요청 본문 (파일 없음): {}", requestBody);

                HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

                if (httpResult.isSuccess()) {
                    String responseBody = httpResult.getBody();
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    logger.debug("심볼 등록 응답 내용 (파일 없음): {}", parsedResponse);

                    // symbol_id 추출
                    String symbolId = JsonUtil.extractValue(responseBody, "symbol_id");
                    if (symbolId == null || symbolId.isEmpty()) {
                        // 다른 가능한 필드명들도 확인
                        symbolId = JsonUtil.extractValue(responseBody, "id");
                    }

                    logger.debug("심볼 등록 API 성공 (파일 없음)");
                    result.put("success", true);
                    result.put("status", httpResult.getStatus());
                    result.put("message", "심볼이 성공적으로 등록되었습니다.");
                    result.put("response", parsedResponse);
                    if (!isEmptySymbolId(symbolId)) {
                        result.put("symbol_id", symbolId);
                    }
                } else {
                    result.put("success", false);
                    result.put("status", httpResult.getStatus());

                    String responseBody = httpResult.getBody();
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
                    logger.debug("심볼 등록 실패 응답 (파일 없음): HTTP {}, Body: {}", httpResult.getStatus(), parsedResponse);

                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }

                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "심볼 등록에 실패했습니다.";
                    }

                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("심볼 등록 API 실패: {}", errorMessage);
                }
            }

        } catch (Exception e) {
            logger.error("심볼 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "심볼 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process 심볼 수정
     * 
     * @param symbolId 심볼 ID
     * @param siteFile 업로드할 파일
     * @return 수정 결과
     */
    public Map<String, Object> updateSymbol(String symbolId, org.springframework.web.multipart.MultipartFile siteFile) {
        Map<String, Object> result = new HashMap<>();

        if (symbolId == null || symbolId.trim().isEmpty()) {
            logger.warn("심볼 수정 실패: 심볼 ID가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "messages.error.symbolIdRequired");
            return result;
        }

        if (siteFile == null || siteFile.isEmpty()) {
            logger.warn("심볼 수정 실패: 업로드할 파일이 없음");
            result.put("success", false);
            result.put("message", "messages.error.uploadFileRequired");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/symbols/upload/" + symbolId;
            logger.debug("심볼 수정 URL: {}", apiUrl);

            // 파일 업로드용 데이터 구성
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("file", siteFile);

            // HttpUtil을 사용하여 multipart 전송
            HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(apiUrl, uploadData);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("심볼 수정 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.symbolUpdateSuccess");
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
                    errorMessage = "심볼 수정에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("심볼 수정 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("심볼 수정 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "심볼 수정 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 심볼 삭제
     * 
     * @param symbolId 심볼 ID
     * @return 삭제 결과
     */
    public Map<String, Object> deleteSymbol(String symbolId) {
        Map<String, Object> result = new HashMap<>();

        if (symbolId == null || symbolId.trim().isEmpty()) {
            logger.warn("심볼 삭제 실패: 심볼 ID가 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "messages.error.symbolIdRequired");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/common/symbols/" + symbolId;
            logger.debug("심볼 삭제 URL: {}", apiUrl);

            // HttpUtil을 사용하여 DELETE 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", null);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("심볼 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.symbolDeleteSuccess");
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
                    errorMessage = "심볼 삭제에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("심볼 삭제 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("심볼 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "심볼 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도면 생성 - 2단계 프로세스 (마스터 데이터 생성 + 파일 업로드)
     * 
     * @param drawingData 도면 데이터 (parent_drawing_id 포함)
     * @param siteFile    도면 파일 (선택사항)
     * @param symbolFile  심볼 파일 (선택사항)
     * @return 생성 결과
     */
    public Map<String, Object> createDrawing(Map<String, Object> drawingData,
            org.springframework.web.multipart.MultipartFile siteFile,
            org.springframework.web.multipart.MultipartFile symbolFile) {
        Map<String, Object> result = new HashMap<>();

        if (drawingData == null) {
            logger.warn("도면 등록 실패: 도면 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidDrawingData");
            return result;
        }

        // 디버깅용: drawingData 입력변수 출력
        logger.debug("createDrawing 입력 drawingData: {}", drawingData);

        // 중첩된 drawingData 구조 처리
        Map<String, Object> actualDrawingData = drawingData;
        if (drawingData.containsKey("drawingData")) {
            Object nestedData = drawingData.get("drawingData");
            if (nestedData instanceof String) {
                // JSON 문자열인 경우 파싱
                try {
                    actualDrawingData = com.wai.admin.util.JsonUtil.parseJson((String) nestedData);
                    logger.debug("중첩된 drawingData JSON 파싱 완료: {}", actualDrawingData);
                } catch (Exception e) {
                    logger.warn("drawingData JSON 파싱 실패: {}", e.getMessage());
                    actualDrawingData = drawingData;
                }
            } else if (nestedData instanceof Map) {
                // 이미 Map인 경우
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nestedData;
                actualDrawingData = nestedMap;
                logger.debug("중첩된 drawingData Map 사용: {}", actualDrawingData);
            }
        }

        // drawingType 미리 추출 및 검증
        Object drawingTypeObj = actualDrawingData.get("drawing_type");
        String drawingType = (drawingTypeObj != null && !drawingTypeObj.toString().trim().isEmpty())
                ? drawingTypeObj.toString()
                : "";

        // parentDrawingId 추출 및 검증 (빈 문자열, null, 공백만 있는 경우 처리)
        Object parentDrawingIdObj = actualDrawingData.get("parent_drawing_id");
        String parentDrawingId = null;
        if (parentDrawingIdObj != null) {
            String tempId = parentDrawingIdObj.toString().trim();
            if (!tempId.isEmpty()) {
                parentDrawingId = tempId;
            }
        }

        logger.debug("createDrawing 추출된 값들 - drawingType: {}, parentDrawingId: {}, process_id: {}",
                drawingType, parentDrawingId, actualDrawingData.get("process_id"));

        // symbolFile이 있는 경우 심볼 생성 수행 (createProcess와 동일한 로직)
        String symbolId = null;
        logger.debug("symbolFile 확인 - symbolFile != null: {}, isEmpty: {}",
                symbolFile != null, symbolFile != null ? symbolFile.isEmpty() : "N/A");

        if (symbolFile != null && !symbolFile.isEmpty()) {
            logger.debug("심볼 파일이 제공됨, 심볼 생성 수행: fileName={}, size={}", symbolFile.getOriginalFilename(),
                    symbolFile.getSize());

            // drawingData에서 심볼 관련 데이터 추출
            Map<String, Object> symbolData = new HashMap<>();

            // symbol_name과 symbol_code가 비어있으면 기본값 생성
            String symbolName = (String) drawingData.getOrDefault("drawing_title", "");
            String symbolCode = (String) drawingData.getOrDefault("drawing_number", "");

            // drawing_title이 비어있으면 기본값 생성 (최대 30자로 제한)
            if (symbolName == null || symbolName.trim().isEmpty()) {
                symbolName = "Drawing_" + System.currentTimeMillis();
                // 데이터베이스 제한에 맞게 30자로 제한
                if (symbolName.length() > 30) {
                    symbolName = symbolName.substring(0, 30);
                }
                logger.debug("drawing_title이 비어있어 기본값 생성: {}", symbolName);
            }

            // drawing_number가 비어있으면 고유한 심볼 코드 생성 (최대 30자로 제한)
            if (symbolCode == null || symbolCode.trim().isEmpty()) {
                symbolCode = "SYM_" + System.currentTimeMillis();
                // 데이터베이스 제한에 맞게 30자로 제한
                if (symbolCode.length() > 30) {
                    symbolCode = symbolCode.substring(0, 30);
                }
                logger.debug("drawing_number가 비어있어 기본값 생성: {}", symbolCode);
            }

            // symbol_name과 symbol_code 길이 제한 (데이터베이스 제약사항)
            if (symbolName.length() > 30) {
                symbolName = symbolName.substring(0, 30);
                logger.debug("symbol_name이 30자를 초과하여 잘림: {}", symbolName);
            }
            if (symbolCode.length() > 30) {
                symbolCode = symbolCode.substring(0, 30);
                logger.debug("symbol_code가 30자를 초과하여 잘림: {}", symbolCode);
            }

            // symbol_name과 symbol_code가 여전히 비어있는지 최종 검증
            if (symbolName.trim().isEmpty() || symbolCode.trim().isEmpty()) {
                logger.warn("심볼 생성에 필요한 필수 필드가 비어있음: symbol_name='{}', symbol_code='{}'", symbolName, symbolCode);
                result.put("symbol_creation_skipped", true);
                result.put("symbol_error_message", "심볼 생성에 필요한 필수 필드(drawing_title, drawing_number)가 비어있습니다.");
            } else {
                symbolData.put("symbol_name", symbolName.trim());
                symbolData.put("symbol_code", symbolCode.trim());
                symbolData.put("symbol_type", "PROCESS");
                symbolData.put("siteFile", symbolFile);

                logger.debug("심볼 생성 데이터: symbol_name={}, symbol_code={}, symbol_type={}",
                        symbolName, symbolCode, "EQUIPMENT");

                logger.debug("심볼 생성 시작 - symbolData: {}", symbolData);

                // createSymbol 호출하여 symbol_id 획득
                Map<String, Object> symbolResult = createSymbol(symbolData);
                if ((Boolean) symbolResult.get("success")) {
                    symbolId = (String) symbolResult.get("symbol_id");
                    logger.debug("심볼 생성 성공, symbol_id: {}", symbolId);
                    logger.debug("심볼 생성 결과: {}", symbolResult);
                } else {
                    logger.warn("심볼 생성 실패: {}", symbolResult.get("message"));
                    logger.warn("심볼 생성 실패 상세 결과: {}", symbolResult);
                    // 심볼 생성 실패해도 도면은 생성 계속 진행
                    // result에 심볼 생성 실패 정보를 저장하지만 도면 생성은 계속
                    result.put("symbol_creation_failed", true);
                    result.put("symbol_error_message", symbolResult.get("message"));
                }
            }
        } else {
            logger.debug("symbolFile이 제공되지 않음 - 심볼 생성 건너뜀");
        }

        try {
            // 외부 인증 서버 URL 구성
            String masterApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/masters";

            // 1단계: 마스터 데이터 생성 (JSON POST)
            Map<String, Object> requestMap = new HashMap<>();

            // 기본값 설정 - 빈 값이 아닌 경우에만 추가
            Object processId = drawingData.get("process_id");
            if (processId != null) {
                requestMap.put("process_id", processId);
            }

            // project_id 처리
            Object projectId = drawingData.get("project_id");
            requestMap.put("project_id", projectId);

            // 필수 필드들은 항상 포함 (null 값인 경우 기본값 사용)
            requestMap.put("drawing_type", drawingType);

            Object drawingNumberObj = drawingData.get("drawing_number");
            String drawingNumber = (drawingNumberObj != null && !drawingNumberObj.toString().trim().isEmpty())
                    ? drawingNumberObj.toString()
                    : "";
            requestMap.put("drawing_number", drawingNumber);

            Object drawingTitleObj = drawingData.get("drawing_title");
            String drawingTitle = (drawingTitleObj != null && !drawingTitleObj.toString().trim().isEmpty())
                    ? drawingTitleObj.toString()
                    : "";
            requestMap.put("drawing_title", drawingTitle);

            // 선택적 필드들
            Object descriptionObj = drawingData.get("description");
            String description = (descriptionObj != null && !descriptionObj.toString().trim().isEmpty())
                    ? descriptionObj.toString()
                    : "";
            if (!description.isEmpty()) {
                requestMap.put("description", description);
            }
            Object drawingStatusObj = drawingData.get("drawing_status");
            String drawingStatus = (drawingStatusObj != null && !drawingStatusObj.toString().trim().isEmpty())
                    ? drawingStatusObj.toString()
                    : "";
            requestMap.put("drawing_status", drawingStatus);

            Object revisionObj = drawingData.get("revision");
            String revision = (revisionObj != null && !revisionObj.toString().trim().isEmpty())
                    ? revisionObj.toString()
                    : "";
            requestMap.put("revision", revision);

            // parent_drawing_id 처리
            if (parentDrawingId != null && !parentDrawingId.trim().isEmpty()) {
                requestMap.put("parent_drawing_id", parentDrawingId);
            }

            // symbol_id 처리 (심볼이 생성된 경우)
            if (!isEmptySymbolId(symbolId)) {
                requestMap.put("symbol_id", symbolId);
                logger.debug("Drawing Master 생성 시 symbol_id 추가: {}", symbolId);
            } else {
                logger.debug("symbol_id가 null이거나 비어있어 추가하지 않음");
            }

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("도면 마스터 생성 요청 본문: {}", requestBody);

            HttpUtil.HttpResult masterResult = HttpUtil.post(masterApiUrl, "application/json", requestBody);

            if (masterResult.isSuccess()) {
                String responseBody = masterResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // symbol_id 처리 결과 확인
                Object responseSymbolId = parsedResponse.get("symbol_id");
                boolean symbolIdProcessed = false;

                if (!isEmptySymbolId(symbolId)) {
                    if (responseSymbolId == null) {
                        logger.warn("외부 API에서 symbol_id를 무시함 - 요청: {}, 응답: null", symbolId);
                        logger.debug("Drawing Master 생성 후 symbol_id 업데이트 시도");
                        // TODO: 별도 API로 symbol_id 업데이트 시도할 수 있음
                    } else if (!symbolId.equals(responseSymbolId.toString())) {
                        logger.warn("외부 API에서 symbol_id가 변경됨 - 요청: {}, 응답: {}", symbolId, responseSymbolId);
                    } else {
                        logger.debug("symbol_id가 정상적으로 처리됨: {}", symbolId);
                        symbolIdProcessed = true;
                    }
                }

                // drawing_id 추출
                String drawingId = extractDrawingIdFromResponse(parsedResponse, responseBody);
                if (drawingId != null && !drawingId.trim().isEmpty()) {

                    // 2단계: 도면 파일 업로드 (Multipart POST)
                    if (siteFile != null && !siteFile.isEmpty()) {
                        // 파일 크기 검증 (100MB = 100 * 1024 * 1024 bytes)
                        long maxFileSize = 100L * 1024 * 1024; // 100MB
                        if (siteFile.getSize() > maxFileSize) {
                            logger.warn("파일 크기 초과: {} bytes (최대: {} bytes)", siteFile.getSize(), maxFileSize);
                            result.put("success", false);
                            result.put("message", "messages.error.fileSizeExceeded");
                            result.put("drawing_id", drawingId);
                            return result;
                        }

                        // 파일 업로드 함수 호출
                        Map<String, Object> uploadResult = uploadDrawingFile(drawingId, siteFile, drawingData);

                        if ((Boolean) uploadResult.get("success")) {
                            // 도면 타입별 처리
                            if ("PNID".equals(drawingType)) {
                                // PNID 도면인 경우 관계 생성 (parent_drawing_id와 child_drawing_id가 모두 존재해야 함)
                                if (parentDrawingId != null && drawingId != null && !drawingId.trim().isEmpty()) {
                                    Map<String, Object> relationshipData = new HashMap<>();
                                    relationshipData.put("parent_drawing_id", parentDrawingId);
                                    relationshipData.put("child_drawing_id", drawingId);
                                    relationshipData.put("relationship_type", "PFDCARD_TO_PNID");
                                    relationshipData.put("sequence_order", 1);

                                    Map<String, Object> relationshipResult = createRelationship(relationshipData);
                                    if (relationshipResult.get("success") != null
                                            && (Boolean) relationshipResult.get("success")) {
                                        result.put("relationship_created", true);
                                    } else {
                                        logger.error("PNID 도면 관계 생성 실패: {}", relationshipResult.get("message"));
                                        result.put("relationship_created", false);
                                        result.put("relationship_error", relationshipResult.get("message"));
                                    }
                                } else {
                                    logger.error(
                                            "PNID 도면 관계 생성 실패: parent_drawing_id 또는 child_drawing_id가 유효하지 않음. parent_drawing_id={}, child_drawing_id={}",
                                            parentDrawingId, drawingId);
                                    result.put("relationship_created", false);
                                    result.put("relationship_error",
                                            "parent_drawing_id 또는 child_drawing_id가 유효하지 않습니다.");
                                }
                            } else if ("PFDCARD".equals(drawingType)) {
                                // PFD 도면인 경우 특별 처리
                                result.put("pfd_file_uploaded", true);
                            }

                            result.put("success", true);
                            result.put("status", uploadResult.get("status"));
                            result.put("message", "messages.success.drawingCreateSuccess");
                            result.put("drawing_id", drawingId);
                            result.put("response", uploadResult.get("response"));

                            // symbol_id 처리 결과 추가
                            if (!isEmptySymbolId(symbolId)) {
                                result.put("symbol_id_requested", symbolId);
                                result.put("symbol_id_processed", symbolIdProcessed);
                                if (!symbolIdProcessed) {
                                    result.put("symbol_warning", "심볼이 생성되었지만 외부 API에서 처리되지 않았습니다.");
                                }
                            }
                        } else {
                            logger.warn("도면 파일 업로드 실패: {}", uploadResult.get("message"));
                            result.put("success", false);
                            result.put("status", uploadResult.get("status"));
                            result.put("message", "도면 마스터는 생성되었지만 파일 업로드에 실패했습니다: " + uploadResult.get("message"));
                            result.put("drawing_id", drawingId);
                            result.put("response", uploadResult.get("response"));
                        }
                    } else {
                        // 파일이 없는 경우 마스터 데이터만 성공으로 처리

                        // 도면 타입별 관계 생성
                        if ("PNID".equals(drawingType)) {
                            // PNID 도면인 경우 관계 생성 (parent_drawing_id와 child_drawing_id가 모두 존재해야 함)
                            if (parentDrawingId != null && drawingId != null && !drawingId.trim().isEmpty()) {
                                Map<String, Object> relationshipData = new HashMap<>();
                                relationshipData.put("parent_drawing_id", parentDrawingId);
                                relationshipData.put("child_drawing_id", drawingId);
                                relationshipData.put("relationship_type", "PFDCARD_TO_PNID");
                                relationshipData.put("sequence_order", 1);

                                logger.debug("PNID 도면 관계 생성 시작: parent_drawing_id={}, child_drawing_id={}",
                                        parentDrawingId, drawingId);

                                Map<String, Object> relationshipResult = createRelationship(relationshipData);
                                if (relationshipResult.get("success") != null
                                        && (Boolean) relationshipResult.get("success")) {
                                    logger.debug("PNID 도면 관계 생성 성공");
                                    result.put("relationship_created", true);
                                } else {
                                    logger.warn("PNID 도면 관계 생성 실패: {}", relationshipResult.get("message"));
                                    result.put("relationship_created", false);
                                    result.put("relationship_error", relationshipResult.get("message"));
                                }
                            } else {
                                logger.error(
                                        "PNID 도면 관계 생성 실패: parent_drawing_id 또는 child_drawing_id가 유효하지 않음. parent_drawing_id={}, child_drawing_id={}",
                                        parentDrawingId, drawingId);
                                result.put("relationship_created", false);
                                result.put("relationship_error", "parent_drawing_id 또는 child_drawing_id가 유효하지 않습니다.");
                            }
                        }

                        result.put("success", true);
                        result.put("status", masterResult.getStatus());
                        result.put("message", "messages.success.drawingMasterCreateSuccessNoFile");
                        result.put("drawing_id", drawingId);
                        result.put("response", parsedResponse);

                        // symbol_id 처리 결과 추가
                        if (!isEmptySymbolId(symbolId)) {
                            result.put("symbol_id_requested", symbolId);
                            result.put("symbol_id_processed", symbolIdProcessed);
                            if (!symbolIdProcessed) {
                                result.put("symbol_warning", "심볼이 생성되었지만 외부 API에서 처리되지 않았습니다.");
                            }
                        }
                    }
                } else {
                    logger.warn("도면 마스터 생성 응답에서 drawing_id를 찾을 수 없음");
                    result.put("success", false);
                    result.put("message", "messages.error.drawingMasterCreateSuccessButIdNotFound");
                    result.put("response", parsedResponse);
                }
            } else {
                // 마스터 데이터 생성 실패
                String responseBody = masterResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = masterResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "도면 마스터 생성에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", masterResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 도면 마스터 생성 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("도면 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "도면 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 자식 도면 업로드 - createDrawing 기반 (symbolFile 제외)
     * 
     * @param drawingData 도면 데이터 (parent_drawing_id 포함)
     * @param siteFile    도면 파일 (필수)
     * @return 업로드 결과
     */
    public Map<String, Object> uploadChildDrawing(Map<String, Object> drawingData,
            org.springframework.web.multipart.MultipartFile siteFile) {
        Map<String, Object> result = new HashMap<>();

        if (drawingData == null) {
            logger.warn("자식 도면 업로드 실패: 도면 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidDrawingData");
            return result;
        }

        // 디버깅용: drawingData 입력변수 출력
        logger.debug("uploadChildDrawing 입력 drawingData: {}", drawingData);

        if (siteFile == null || siteFile.isEmpty()) {
            logger.warn("자식 도면 업로드 실패: siteFile이 필수값입니다");
            result.put("success", false);
            result.put("message", "자식 도면 업로드에는 파일이 필수입니다.");
            return result;
        }

        // drawingType 미리 추출 및 검증
        Object drawingTypeObj = drawingData.get("drawing_type");
        String drawingType = (drawingTypeObj != null && !drawingTypeObj.toString().trim().isEmpty())
                ? drawingTypeObj.toString()
                : "";

        // parentDrawingId 추출
        String parentDrawingId = (String) drawingData.get("parent_drawing_id");

        // drawing_id 추출 (기존 drawing_id가 있는지 확인)
        String existingDrawingId = (String) drawingData.get("drawing_id");

        logger.debug("자식 도면 타입 확인: {}, 부모 도면 ID: {}, 기존 drawing_id: {}", drawingType, parentDrawingId,
                existingDrawingId);

        try {
            // drawing_id가 이미 있는 경우: 직접 업로드 API 호출
            if (existingDrawingId != null && !existingDrawingId.trim().isEmpty()) {
                logger.debug("기존 drawing_id 사용하여 직접 업로드: {}", existingDrawingId);

                // /api/v1/minio/drawing_files/upload/{drawing_id} API 호출
                String uploadApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/upload/" + existingDrawingId;
                logger.debug("도면 파일 직접 업로드 API 호출: {}", uploadApiUrl);

                // multipart 요청 데이터 구성
                Map<String, Object> uploadRequestMap = new HashMap<>();
                uploadRequestMap.put("drawing_id", existingDrawingId);
                uploadRequestMap.put("file", siteFile);
                uploadRequestMap.put("drawing_version", "1.0.0");

                logger.debug("직접 업로드 요청 데이터: {}", uploadRequestMap);

                // multipart 요청 실행
                HttpUtil.HttpResult uploadResult = HttpUtil.postMultipart(uploadApiUrl, uploadRequestMap);

                if (uploadResult.isSuccess()) {
                    String responseBody = uploadResult.getBody();
                    Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                    logger.debug("도면 파일 직접 업로드 성공: {}", responseBody);

                    result.put("success", true);
                    result.put("status", uploadResult.getStatus());
                    result.put("message", "도면 파일이 성공적으로 업로드되었습니다.");
                    result.put("drawing_id", existingDrawingId);
                    result.put("response", parsedResponse);

                } else {
                    // 업로드 실패
                    String responseBody = uploadResult.getBody();
                    String errorMessage = uploadResult.getExtractedErrorMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "message");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "도면 파일 업로드에 실패했습니다.";
                    }

                    logger.warn("도면 파일 직접 업로드 실패: {}", errorMessage);
                    result.put("success", false);
                    result.put("status", uploadResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("response", JsonUtil.parseJson(responseBody));
                }

                return result;
            }

            // drawing_id가 없는 경우: 기존 로직 수행 (마스터 생성 → 파일 업로드 → 관계 생성)
            // 외부 인증 서버 URL 구성
            String masterApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/masters";

            // 1단계: 마스터 데이터 생성 (JSON POST)
            Map<String, Object> requestMap = new HashMap<>();

            // 기본값 설정 - 빈 값이 아닌 경우에만 추가
            Object processId = drawingData.get("process_id");
            if (processId != null) {
                requestMap.put("process_id", processId);
            }

            // project_id 처리
            Object projectId = drawingData.get("project_id");
            requestMap.put("project_id", projectId);

            // 필수 필드들은 항상 포함 (null 값인 경우 기본값 사용)
            requestMap.put("drawing_type", drawingType);

            Object drawingNumberObj = drawingData.get("drawing_number");
            String drawingNumber = (drawingNumberObj != null && !drawingNumberObj.toString().trim().isEmpty())
                    ? drawingNumberObj.toString()
                    : drawingType + "001"; // 기본값: drawing_type + "001"
            requestMap.put("drawing_number", drawingNumber);

            Object drawingTitleObj = drawingData.get("drawing_title");
            String drawingTitle = (drawingTitleObj != null && !drawingTitleObj.toString().trim().isEmpty())
                    ? drawingTitleObj.toString()
                    : "도면 제목"; // 기본값: "도면 제목"
            requestMap.put("drawing_title", drawingTitle);

            // 선택적 필드들
            Object descriptionObj = drawingData.get("description");
            String description = (descriptionObj != null && !descriptionObj.toString().trim().isEmpty())
                    ? descriptionObj.toString()
                    : "";
            if (!description.isEmpty()) {
                requestMap.put("description", description);
            }

            Object drawingStatusObj = drawingData.get("drawing_status");
            String drawingStatus = (drawingStatusObj != null && !drawingStatusObj.toString().trim().isEmpty())
                    ? drawingStatusObj.toString()
                    : "DRAFT"; // 기본값: "DRAFT"
            requestMap.put("drawing_status", drawingStatus);

            Object revisionObj = drawingData.get("revision");
            String revision = (revisionObj != null && !revisionObj.toString().trim().isEmpty())
                    ? revisionObj.toString()
                    : "A"; // 기본값: "A"
            requestMap.put("revision", revision);

            // parent_drawing_id 처리
            if (parentDrawingId != null && !parentDrawingId.trim().isEmpty()) {
                requestMap.put("parent_drawing_id", parentDrawingId);
            }

            String requestBody = JsonUtil.objectMapToJson(requestMap);

            HttpUtil.HttpResult masterResult = HttpUtil.post(masterApiUrl, "application/json", requestBody);

            if (masterResult.isSuccess()) {
                String responseBody = masterResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // drawing_id 추출
                String drawingId = JsonUtil.extractValue(responseBody, "drawing_id");
                if (drawingId != null && !drawingId.trim().isEmpty()) {
                    logger.debug("자식 도면 마스터 생성 성공, drawing_id: {}", drawingId);

                    // 파일 업로드 수행 (siteFile 필수)
                    Map<String, Object> uploadResult = uploadDrawingFile(drawingId, siteFile, drawingData);

                    if ((Boolean) uploadResult.get("success")) {
                        // 도면 타입별 처리
                        if ("PNID".equals(drawingType)) {
                            // PNID 도면인 경우 관계 생성
                            if (parentDrawingId != null && !parentDrawingId.trim().isEmpty()) {
                                Map<String, Object> relationshipData = new HashMap<>();
                                relationshipData.put("parent_drawing_id", parentDrawingId);
                                relationshipData.put("child_drawing_id", drawingId);
                                relationshipData.put("relationship_type", "PFDCARD_TO_PNID");
                                relationshipData.put("sequence_order", 1);

                                Map<String, Object> relationshipResult = createRelationship(relationshipData);
                                if (relationshipResult.get("success") != null
                                        && (Boolean) relationshipResult.get("success")) {
                                    result.put("relationship_created", true);
                                } else {
                                    logger.warn("PNID 도면 관계 생성 실패: {}", relationshipResult.get("message"));
                                    result.put("relationship_created", false);
                                    result.put("relationship_error", relationshipResult.get("message"));
                                }
                            }
                        } else if ("SVG".equals(drawingType)) {
                            // SVG 도면인 경우 관계 생성
                            if (parentDrawingId != null && !parentDrawingId.trim().isEmpty()) {
                                Map<String, Object> relationshipData = new HashMap<>();
                                relationshipData.put("parent_drawing_id", parentDrawingId);
                                relationshipData.put("child_drawing_id", drawingId);
                                relationshipData.put("relationship_type", "PNID_TO_SVG");
                                relationshipData.put("sequence_order", 1);

                                Map<String, Object> relationshipResult = createRelationship(relationshipData);
                                if (relationshipResult.get("success") != null
                                        && (Boolean) relationshipResult.get("success")) {
                                    result.put("relationship_created", true);
                                } else {
                                    logger.warn("SVG 도면 관계 생성 실패: {}", relationshipResult.get("message"));
                                    result.put("relationship_created", false);
                                    result.put("relationship_error", relationshipResult.get("message"));
                                }
                            }
                        }

                        result.put("success", true);
                        result.put("status", uploadResult.get("status"));
                        result.put("message", "messages.success.drawingCreateSuccess");
                        result.put("drawing_id", drawingId);
                        result.put("response", uploadResult.get("response"));
                    } else {
                        logger.warn("자식 도면 파일 업로드 실패: {}", uploadResult.get("message"));
                        result.put("success", false);
                        result.put("status", uploadResult.get("status"));
                        result.put("message", "도면 마스터는 생성되었지만 파일 업로드에 실패했습니다: " + uploadResult.get("message"));
                        result.put("drawing_id", drawingId);
                        result.put("response", uploadResult.get("response"));
                    }
                } else {
                    logger.warn("자식 도면 마스터 생성 응답에서 drawing_id를 찾을 수 없음");
                    result.put("success", false);
                    result.put("message", "messages.error.drawingMasterCreateSuccessButIdNotFound");
                    result.put("response", parsedResponse);
                }
            } else {
                // 마스터 데이터 생성 실패
                String responseBody = masterResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = masterResult.getExtractedErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "message");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "자식 도면 마스터 생성에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", masterResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 자식 도면 마스터 생성 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("자식 도면 업로드 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "자식 도면 업로드 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 컴포넌트 생성
     * 
     * @param componentData 생성할 컴포넌트 데이터
     * @return 생성 결과
     */
    public Map<String, Object> createComponent(Map<String, Object> componentData) {
        Map<String, Object> result = new HashMap<>();

        if (componentData == null) {
            logger.warn("컴포넌트 등록 실패: 컴포넌트 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidComponentData");
            return result;
        }

        try {
            // components 배열이 있는지 확인
            Object componentsObj = componentData.get("components");
            if (componentsObj instanceof List) {
                // 배열 형태의 데이터 처리
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> componentsList = (List<Map<String, Object>>) componentsObj;
                return createComponentsBatch(componentsList);
            } else {
                // 단일 컴포넌트 데이터 처리
                return createSingleComponent(componentData);
            }
        } catch (Exception e) {
            logger.error("컴포넌트 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "컴포넌트 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }
    }

    /**
     * 단일 컴포넌트 생성
     */
    private Map<String, Object> createSingleComponent(Map<String, Object> componentData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/process/pid_components/";
            logger.debug("단일 컴포넌트 등록 URL: {}", apiUrl);

            // 요청 본문 구성 - 입력 값이 있는 경우만 포함
            Map<String, Object> requestMap = new HashMap<>();

            // 새로운 파라미터 구조에 맞게 수정 - 입력 값이 있는 경우만 포함
            if (componentData.containsKey("component_code") && componentData.get("component_code") != null) {
                requestMap.put("component_code", componentData.get("component_code"));
            }
            if (componentData.containsKey("project_id") && componentData.get("project_id") != null) {
                requestMap.put("project_id", componentData.get("project_id"));
            }
            if (componentData.containsKey("process_id") && componentData.get("process_id") != null) {
                requestMap.put("process_id", componentData.get("process_id"));
            }
            if (componentData.containsKey("mapping_type") && componentData.get("mapping_type") != null) {
                requestMap.put("mapping_type", componentData.get("mapping_type"));
            }
            if (componentData.containsKey("pid_id") && componentData.get("pid_id") != null) {
                requestMap.put("pid_id", componentData.get("pid_id"));
            }
            if (componentData.containsKey("pid_number") && componentData.get("pid_number") != null) {
                requestMap.put("pid_number", componentData.get("pid_number"));
            }
            if (componentData.containsKey("input_poc") && componentData.get("input_poc") != null) {
                requestMap.put("input_poc", componentData.get("input_poc"));
            }
            if (componentData.containsKey("output_poc") && componentData.get("output_poc") != null) {
                requestMap.put("output_poc", componentData.get("output_poc"));
            }
            if (componentData.containsKey("component_type") && componentData.get("component_type") != null) {
                requestMap.put("component_type", componentData.get("component_type"));
            }
            if (componentData.containsKey("component_hierachy") && componentData.get("component_hierachy") != null) {
                requestMap.put("component_hierachy", componentData.get("component_hierachy"));
            }
            if (componentData.containsKey("equipment_id") && componentData.get("equipment_id") != null) {
                requestMap.put("equipment_id", componentData.get("equipment_id"));
            }
            if (componentData.containsKey("structure_id") && componentData.get("structure_id") != null) {
                requestMap.put("structure_id", componentData.get("structure_id"));
            }

            // 숫자 필드들 - 입력 값이 있는 경우만 포함
            if (componentData.containsKey("standard_quantity") && componentData.get("standard_quantity") != null) {
                requestMap.put("standard_quantity", componentData.get("standard_quantity"));
            }
            if (componentData.containsKey("spare_quantity") && componentData.get("spare_quantity") != null) {
                requestMap.put("spare_quantity", componentData.get("spare_quantity"));
            }
            if (componentData.containsKey("is_active") && componentData.get("is_active") != null) {
                requestMap.put("is_active", componentData.get("is_active"));
            }
            if (componentData.containsKey("created_by") && componentData.get("created_by") != null) {
                Object createdBy = componentData.get("created_by");
                requestMap.put("created_by", createdBy);
                logger.debug("created_by 필드 추가: {} (타입: {})", createdBy, createdBy.getClass().getSimpleName());
            }

            logger.debug("컴포넌트 등록 요청 데이터: {}", JsonUtil.objectMapToJson(requestMap));
            logger.debug("원본 입력 데이터: {}", JsonUtil.objectMapToJson(componentData));

            // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
            String requestJson = JsonUtil.objectMapToJson(requestMap);
            HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestJson);

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // 응답 내용 검증
                if (isValidComponentCreateResponse(parsedResponse, responseBody)) {
                    String componentId = extractComponentIdFromResponse(parsedResponse, responseBody);
                    if (componentId != null && !componentId.trim().isEmpty()) {
                        logger.debug("컴포넌트 등록 성공: component_id={}", componentId);
                        result.put("success", true);
                        result.put("status", httpResult.getStatus());
                        result.put("message", "messages.success.componentCreateSuccess");
                        result.put("component_id", componentId);
                        result.put("response", parsedResponse);
                    } else {
                        String errorMessage = "컴포넌트 등록 응답에서 component_id를 찾을 수 없습니다.";
                        result.put("success", false);
                        result.put("status", httpResult.getStatus());
                        result.put("message", errorMessage);
                        result.put("response", parsedResponse);

                        logger.warn("컴포넌트 등록 실패 (응답 내용 검증 실패): {}", errorMessage);
                        return result;
                    }
                } else {
                    // 응답코드가 201이 아닌 경우 에러로 처리
                    String errorMessage = JsonUtil.extractValue(responseBody, "message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = JsonUtil.extractValue(responseBody, "detail");
                    }
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "컴포넌트 등록에 실패했습니다.";
                    }

                    result.put("success", false);
                    result.put("status", httpResult.getStatus());
                    result.put("message", errorMessage);
                    result.put("response", parsedResponse);

                    logger.warn("컴포넌트 등록 실패 (상태코드: {}): {}", httpResult.getStatus(), errorMessage);
                    return result;
                }
            } else {
                // 에러 응답 처리
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
                    errorMessage = "컴포넌트 등록에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                // 상세한 오류 정보 로깅
                logger.warn("외부 인증 서버 컴포넌트 등록 실패: {}", errorMessage);
                logger.warn("HTTP 상태 코드: {}", httpResult.getStatus());
                logger.warn("응답 본문: {}", responseBody);

                // AttributeError 관련 특별 처리
                if (responseBody != null && responseBody.contains("AttributeError")
                        && responseBody.contains("'dict' object has no attribute")) {
                    logger.error("========== 외부 API 오류 분석 ==========");
                    logger.error("오류 유형: AttributeError - 딕셔너리 속성 접근 오류");
                    logger.error("오류 위치: /app/app/api/v1/endpoints/process/process_pid_components.py:51");
                    logger.error("오류 내용: current_user.user_id 접근 시 'dict' object has no attribute 'user_id'");
                    logger.error("원인: current_user가 딕셔너리로 전달되었는데 객체 속성으로 접근 시도");
                    logger.error("해결방안: 외부 API에서 current_user['user_id'] 또는 getattr() 사용 필요");
                    logger.error("전송된 요청 데이터: {}", requestJson);
                    logger.error("=====================================");
                    result.put("message", "외부 API 내부 오류: 사용자 정보 처리 문제 (AttributeError)");
                }
            }

        } catch (Exception e) {
            logger.error("단일 컴포넌트 등록 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "단일 컴포넌트 등록 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 여러 컴포넌트 일괄 생성
     */
    private Map<String, Object> createComponentsBatch(List<Map<String, Object>> componentsList) {
        Map<String, Object> result = new HashMap<>();
        List<String> createdComponentIds = new ArrayList<>();
        List<String> failedComponentIds = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        logger.debug("컴포넌트 일괄 등록 시작: 총 {}개", componentsList.size());

        for (int i = 0; i < componentsList.size(); i++) {
            Map<String, Object> componentData = componentsList.get(i);
            logger.debug("컴포넌트 {} 등록 시작", i + 1);

            try {
                Map<String, Object> singleResult = createSingleComponent(componentData);

                if ((Boolean) singleResult.get("success")) {
                    String componentId = (String) singleResult.get("component_id");
                    if (componentId != null && !componentId.trim().isEmpty()) {
                        createdComponentIds.add(componentId);
                        successCount++;
                        logger.debug("컴포넌트 {} 등록 성공: component_id={}", i + 1, componentId);
                    } else {
                        failedComponentIds.add("component_" + (i + 1));
                        failureCount++;
                        logger.warn("컴포넌트 {} 등록 실패: component_id 없음", i + 1);
                    }
                } else {
                    failedComponentIds.add("component_" + (i + 1));
                    failureCount++;
                    logger.warn("컴포넌트 {} 등록 실패: {}", i + 1, singleResult.get("message"));
                }
            } catch (Exception e) {
                failedComponentIds.add("component_" + (i + 1));
                failureCount++;
                logger.error("컴포넌트 {} 등록 중 예외 발생", i + 1, e);
            }
        }

        // 결과 구성
        result.put("success", failureCount == 0); // 모든 컴포넌트가 성공한 경우에만 true
        result.put("total_count", componentsList.size());
        result.put("success_count", successCount);
        result.put("failure_count", failureCount);
        result.put("created_component_ids", createdComponentIds);
        result.put("failed_component_ids", failedComponentIds);

        if (failureCount == 0) {
            result.put("message", "모든 컴포넌트가 성공적으로 등록되었습니다.");
        } else if (successCount == 0) {
            result.put("message", "모든 컴포넌트 등록에 실패했습니다.");
        } else {
            result.put("message", String.format("컴포넌트 등록 완료: 성공 %d개, 실패 %d개", successCount, failureCount));
        }

        logger.debug("컴포넌트 일괄 등록 완료: 성공={}, 실패={}", successCount, failureCount);
        return result;
    }

    /**
     * 심볼 다운로드 URL 조회
     * 
     * @param symbolId 다운로드할 심볼 ID
     * @return 다운로드 URL과 파일 정보
     */
    public Map<String, Object> getSymbolDownload(String symbolId) {
        logger.debug("심볼 다운로드 URL 조회 API 호출: symbol_id={}", symbolId);
        Map<String, Object> result = new HashMap<>();

        if (symbolId == null || symbolId.trim().isEmpty()) {
            logger.warn("심볼 다운로드 URL 조회 실패: 심볼 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidSymbolId");
            return result;
        }

        try {
            // symbolId를 문자열로 명시적 변환 (UUID 객체 처리 문제 해결)
            String symbolIdStr = symbolId.toString().trim();
            logger.debug("변환된 symbolId: {}", symbolIdStr);

            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/symbols/download/" + symbolIdStr;
            logger.debug("심볼 다운로드 URL: {}", apiUrl);

            // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();

                logger.debug("심볼 다운로드 URL 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.symbolDownloadUrlSuccess");
                result.put("download_url", apiUrl);
                result.put("response_body", responseBody);

                // 다운로드 URL만 반환 (헤더 정보는 HttpUtil에서 제공하지 않음)
                logger.debug("다운로드 URL 생성 완료: {}", apiUrl);

            } else {
                // 에러 응답 처리
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
                    errorMessage = "심볼 다운로드 URL 조회에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 심볼 다운로드 URL 조회 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("심볼 다운로드 URL 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "심볼 다운로드 URL 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 계산식 다운로드 URL 조회
     * 
     * @param formulaId 다운로드할 계산식 ID
     * @return 다운로드 URL과 파일 정보
     */
    public Map<String, Object> getFormulaDownload(String formulaId) {
        logger.debug("계산식 다운로드 URL 조회 API 호출: formula_id={}", formulaId);
        Map<String, Object> result = new HashMap<>();

        if (formulaId == null || formulaId.trim().isEmpty()) {
            logger.warn("계산식 다운로드 URL 조회 실패: 계산식 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidFormulaId");
            return result;
        }

        try {
            // formulaId를 문자열로 명시적 변환 (UUID 객체 처리 문제 해결)
            String formulaIdStr = formulaId.toString().trim();
            logger.debug("변환된 formulaId: {}", formulaIdStr);

            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/formula/download/" + formulaIdStr;
            logger.debug("계산식 다운로드 URL: {}", apiUrl);

            // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

            // 디버깅을 위한 상세 로깅
            logger.debug("HTTP 응답 상태: {}", httpResult.getStatus());
            logger.debug("HTTP 응답 본문: {}", httpResult.getBody());
            logger.debug("HTTP 응답 성공 여부: {}", httpResult.isSuccess());

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();

                logger.debug("계산식 다운로드 URL 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.formulaDownloadUrlSuccess");
                result.put("download_url", apiUrl);
                result.put("response_body", responseBody);

                // 다운로드 URL만 반환 (헤더 정보는 HttpUtil에서 제공하지 않음)
                logger.debug("다운로드 URL 생성 완료: {}", apiUrl);

            } else {
                // 에러 응답 처리
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
                    errorMessage = "계산식 다운로드 URL 조회에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 계산식 다운로드 URL 조회 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("계산식 다운로드 URL 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "계산식 다운로드 URL 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도면 다운로드 URL 조회
     * 
     * @param drawingId 다운로드할 도면 ID
     * @return 다운로드 URL과 파일 정보
     */
    public Map<String, Object> getDrawingDownload(String drawingId) {
        logger.debug("도면 다운로드 URL 조회 API 호출: drawing_id={}", drawingId);
        Map<String, Object> result = new HashMap<>();

        if (drawingId == null || drawingId.trim().isEmpty()) {
            logger.warn("도면 다운로드 URL 조회 실패: 도면 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidDrawingId");
            return result;
        }

        try {
            // drawingId를 문자열로 명시적 변환 (UUID 객체 처리 문제 해결)
            String drawingIdStr = drawingId.toString().trim();
            logger.debug("변환된 drawingId: {}", drawingIdStr);

            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/download/" + drawingIdStr;
            logger.debug("도면 다운로드 URL: {}", apiUrl);

            // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

            // 디버깅을 위한 상세 로깅
            logger.debug("HTTP 응답 상태: {}", httpResult.getStatus());
            logger.debug("HTTP 응답 본문: {}", httpResult.getBody());
            logger.debug("HTTP 응답 성공 여부: {}", httpResult.isSuccess());

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();

                logger.debug("도면 다운로드 URL 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.drawingDownloadUrlSuccess");
                result.put("download_url", apiUrl);
                result.put("response_body", responseBody);

                // 다운로드 URL만 반환 (헤더 정보는 HttpUtil에서 제공하지 않음)
                logger.debug("다운로드 URL 생성 완료: {}", apiUrl);

            } else {
                // 에러 응답 처리
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
                    errorMessage = "도면 다운로드 URL 조회에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 도면 다운로드 URL 조회 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("도면 다운로드 URL 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "도면 다운로드 URL 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 컴포넌트 등록 응답이 유효한지 검증하는 메서드
     * 
     * @param parsedResponse 파싱된 응답 데이터
     * @param responseBody   원본 응답 본문
     * @return 유효성 검증 결과
     */
    private boolean isValidComponentCreateResponse(Map<String, Object> parsedResponse, String responseBody) {
        if (parsedResponse == null) {
            return false;
        }

        // component_id가 있는지 확인
        Object componentId = parsedResponse.get("component_id");
        if (componentId != null && !componentId.toString().trim().isEmpty()) {
            return true;
        }

        // data 객체 내부에 component_id가 있는지 확인
        Object data = parsedResponse.get("data");
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object dataComponentId = dataMap.get("component_id");
            if (dataComponentId != null && !dataComponentId.toString().trim().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 응답에서 component_id를 추출하는 헬퍼 메서드
     */
    private String extractComponentIdFromResponse(Map<String, Object> parsedResponse, String responseBody) {
        if (parsedResponse == null) {
            return null;
        }

        // 1. 최상위 레벨에서 component_id 확인
        Object componentId = parsedResponse.get("component_id");
        if (componentId != null && !componentId.toString().trim().isEmpty()) {
            logger.debug("최상위 레벨에서 component_id 발견: {}", componentId);
            return componentId.toString();
        }

        // 2. data 객체 내부에서 component_id 확인
        Object data = parsedResponse.get("data");
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object dataComponentId = dataMap.get("component_id");
            if (dataComponentId != null && !dataComponentId.toString().trim().isEmpty()) {
                logger.debug("data 객체에서 component_id 발견: {}", dataComponentId);
                return dataComponentId.toString();
            }
        }

        // 3. items 배열에서 첫 번째 항목의 component_id 확인
        Object items = parsedResponse.get("items");
        if (items instanceof List && !((List<?>) items).isEmpty()) {
            Object firstItem = ((List<?>) items).get(0);
            if (firstItem instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) firstItem;
                Object itemComponentId = itemMap.get("component_id");
                if (itemComponentId != null && !itemComponentId.toString().trim().isEmpty()) {
                    logger.debug("items 배열 첫 번째 항목에서 component_id 발견: {}", itemComponentId);
                    return itemComponentId.toString();
                }
            }
        }

        logger.warn("응답에서 component_id를 찾을 수 없음");
        return null;
    }

    /**
     * 응답에서 drawing_id를 추출하는 헬퍼 메서드
     */
    private String extractDrawingIdFromResponse(Map<String, Object> parsedResponse, String responseBody) {
        if (parsedResponse == null) {
            return null;
        }

        // 1. 최상위 레벨에서 drawing_id 확인
        Object drawingId = parsedResponse.get("drawing_id");
        if (drawingId != null && !drawingId.toString().trim().isEmpty()) {
            logger.debug("최상위 레벨에서 drawing_id 발견: {}", drawingId);
            return drawingId.toString();
        }

        // 2. data 객체에서 drawing_id 확인
        Object dataValue = parsedResponse.get("data");
        if (dataValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) dataValue;
            Object dataDrawingId = dataMap.get("drawing_id");
            if (dataDrawingId != null && !dataDrawingId.toString().trim().isEmpty()) {
                logger.debug("data 객체에서 drawing_id 발견: {}", dataDrawingId);
                return dataDrawingId.toString();
            }
        }

        // 3. response 객체에서 drawing_id 확인
        Object responseValue = parsedResponse.get("response");
        if (responseValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) responseValue;
            Object responseDrawingId = responseMap.get("drawing_id");
            if (responseDrawingId != null && !responseDrawingId.toString().trim().isEmpty()) {
                logger.debug("response 객체에서 drawing_id 발견: {}", responseDrawingId);
                return responseDrawingId.toString();
            }

            // 4. response.data 객체에서 drawing_id 확인
            Object responseDataValue = responseMap.get("data");
            if (responseDataValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseDataMap = (Map<String, Object>) responseDataValue;
                Object responseDataDrawingId = responseDataMap.get("drawing_id");
                if (responseDataDrawingId != null && !responseDataDrawingId.toString().trim().isEmpty()) {
                    logger.debug("response.data 객체에서 drawing_id 발견: {}", responseDataDrawingId);
                    return responseDataDrawingId.toString();
                }
            }
        }

        // 5. 문자열에서 직접 검색 (마지막 수단)
        if (responseBody != null && responseBody.contains("drawing_id")) {
            logger.debug("응답 본문에서 drawing_id 문자열 발견, 직접 파싱 시도");
            // 간단한 문자열 파싱 시도
            try {
                int startIndex = responseBody.indexOf("\"drawing_id\":");
                if (startIndex > 0) {
                    startIndex = responseBody.indexOf("\"", startIndex + 14);
                    if (startIndex > 0) {
                        int endIndex = responseBody.indexOf("\"", startIndex + 1);
                        if (endIndex > 0) {
                            String extractedId = responseBody.substring(startIndex + 1, endIndex);
                            if (!extractedId.trim().isEmpty()) {
                                logger.debug("문자열 파싱으로 drawing_id 추출: {}", extractedId);
                                return extractedId;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("문자열 파싱으로 drawing_id 추출 실패: {}", e.getMessage());
            }
        }

        logger.warn("응답에서 drawing_id를 찾을 수 없음");
        return null;
    }

    /**
     * 심볼 코드로 심볼 정보 조회
     */
    public Map<String, Object> getSymbolByCode(String symbolCode) {
        logger.debug("심볼 코드로 심볼 조회 API 호출: {}", symbolCode);
        Map<String, Object> result = new HashMap<>();

        try {
            String apiUrl = authServerBaseUrl + "/api/v1/common/symbols?symbol_code=" + symbolCode;
            logger.debug("심볼 조회 URL: {}", apiUrl);

            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                // symbol_id 추출
                String symbolId = JsonUtil.extractValue(responseBody, "symbol_id");
                if (symbolId == null || symbolId.isEmpty()) {
                    // 다른 가능한 필드명들도 확인
                    symbolId = JsonUtil.extractValue(responseBody, "id");
                }

                logger.debug("심볼 조회 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "심볼 조회가 성공적으로 완료되었습니다.");
                result.put("response", parsedResponse);
                if (!isEmptySymbolId(symbolId)) {
                    result.put("symbol_id", symbolId);
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
                    errorMessage = "심볼 조회에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("심볼 조회 API 실패: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("심볼 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "심볼 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도면 관계 생성
     * 
     * @param relationshipData 관계 데이터
     * @return 생성 결과
     */
    public Map<String, Object> createRelationship(Map<String, Object> relationshipData) {
        Map<String, Object> result = new HashMap<>();

        if (relationshipData == null) {
            logger.warn("도면 관계 등록 실패: 관계 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidRelationshipData");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String relationshipApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/relationships";
            logger.debug("도면 관계 등록 URL: {}", relationshipApiUrl);

            // 요청 데이터 구성
            Map<String, Object> requestMap = new HashMap<>();

            // 필수 필드 검증 및 설정
            Object parentDrawingIdObj = relationshipData.get("parent_drawing_id");
            if (parentDrawingIdObj == null || parentDrawingIdObj.toString().trim().isEmpty()) {
                logger.warn("도면 관계 등록 실패: parent_drawing_id가 필수입니다");
                result.put("success", false);
                result.put("message", "parent_drawing_id는 필수 입력값입니다.");
                return result;
            }
            requestMap.put("parent_drawing_id", parentDrawingIdObj.toString());

            Object childDrawingIdObj = relationshipData.get("child_drawing_id");
            if (childDrawingIdObj == null || childDrawingIdObj.toString().trim().isEmpty()) {
                logger.error("도면 관계 등록 실패: child_drawing_id가 입력되지 않았습니다. relationshipData={}", relationshipData);
                result.put("success", false);
                result.put("message", "child_drawing_id는 필수 입력값입니다.");
                return result;
            }
            requestMap.put("child_drawing_id", childDrawingIdObj.toString());

            // relationship_type 설정 (기본값: PFDCARD_TO_PNID)
            Object relationshipTypeObj = relationshipData.get("relationship_type");
            String relationshipType = (relationshipTypeObj != null && !relationshipTypeObj.toString().trim().isEmpty())
                    ? relationshipTypeObj.toString()
                    : "PFDCARD_TO_PNID";
            requestMap.put("relationship_type", relationshipType);

            // sequence_order 설정 (기본값: 1)
            Object sequenceOrderObj = relationshipData.get("sequence_order");
            Integer sequenceOrder = (sequenceOrderObj != null && !sequenceOrderObj.toString().trim().isEmpty())
                    ? Integer.parseInt(sequenceOrderObj.toString())
                    : 1;
            requestMap.put("sequence_order", sequenceOrder);

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("도면 관계 등록 요청: URL={}, Body={}", relationshipApiUrl, requestBody);

            // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult relationshipResult = HttpUtil.post(relationshipApiUrl, "application/json", requestBody);

            if (relationshipResult.isSuccess()) {
                String responseBody = relationshipResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("도면 관계 등록 성공");
                result.put("success", true);
                result.put("status", relationshipResult.getStatus());
                result.put("message", "도면 관계가 성공적으로 생성되었습니다.");
                result.put("response", parsedResponse);
            } else {
                String responseBody = relationshipResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "도면 관계 등록에 실패했습니다.";
                }

                logger.warn("도면 관계 등록 실패: {}", errorMessage);
                result.put("success", false);
                result.put("status", relationshipResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
            }
        } catch (Exception e) {
            logger.error("도면 관계 등록 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "도면 관계 등록 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도면 관계 조회 - PFD와 P&ID 매핑 목록 조회
     * 
     * @param searchParams 검색 파라미터 (parent_drawing_id, child_drawing_id,
     *                     relationship_type 등)
     * @return 조회 결과
     */
    public Map<String, Object> getDrawingRelationships(Map<String, Object> searchParams) {
        logger.debug("도면 관계 조회 시작: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        if (searchParams == null) {
            logger.warn("도면 관계 조회 실패: 검색 파라미터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidSearchParams");
            return result;
        }

        try {
            // 외부 인증 서버 URL 구성
            String relationshipApiUrl = authServerBaseUrl + "/api/v1/minio/drawing_files/relationships/search";
            logger.debug("도면 관계 조회 URL: {}", relationshipApiUrl);

            // 요청 데이터 구성
            Map<String, Object> requestMap = new HashMap<>();

            // 검색 필드들 추가
            String[] searchFields = {
                    "parent_drawing_id", "child_drawing_id", "relationship_type",
                    "sequence_order", "created_at", "updated_at"
            };

            for (String field : searchFields) {
                Object value = searchParams.get(field);
                if (value != null && !value.toString().trim().isEmpty()) {
                    requestMap.put(field, value);
                    logger.debug("검색 필드 추가: {} = {}", field, value);
                }
            }

            // 페이징 파라미터
            requestMap.put("page", searchParams.getOrDefault("page", 1));
            requestMap.put("page_size", searchParams.getOrDefault("page_size", 100));
            requestMap.put("order_by", searchParams.getOrDefault("order_by", "created_at"));
            requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "desc"));

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            logger.debug("도면 관계 조회 요청: URL={}, Body={}", relationshipApiUrl, requestBody);

            // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.post(relationshipApiUrl, "application/json", requestBody);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("도면 관계 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "도면 관계 조회가 성공적으로 완료되었습니다.");
                result.put("response", parsedResponse);
            } else {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                String errorMessage = JsonUtil.extractValue(responseBody, "message");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = JsonUtil.extractValue(responseBody, "detail");
                }

                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "도면 관계 조회에 실패했습니다.";
                }

                logger.warn("도면 관계 조회 실패: {}", errorMessage);
                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);
            }
        } catch (Exception e) {
            logger.error("도면 관계 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "도면 관계 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process 계산식 삭제 (검색 후 삭제)
     * 
     * @param searchParams 검색 파라미터
     * @return 삭제 결과
     */
    public Map<String, Object> deleteProcessFormula(Map<String, Object> searchParams) {
        logger.debug("Process 계산식 삭제 시작: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        if (searchParams == null) {
            logger.warn("계산식 삭제 실패: 검색 파라미터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidSearchParams");
            return result;
        }

        // URL 구성 - Formula API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/common/formula_library/search";
        logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", "") + "");
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                List<String> deletedFormulaIds = new ArrayList<>();
                List<String> failedFormulaIds = new ArrayList<>();

                // item 항목이 JSON list인 경우 순환하면서 처리
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        // formula_id 값만 추출
                        if (item.containsKey("formula_id")) {
                            String formulaId = (String) item.get("formula_id");
                            logger.debug("삭제할 계산식 ID: {}", formulaId);

                            // deleteFormula 함수 호출
                            Map<String, Object> deleteResult = deleteFormula(formulaId);
                            if ((Boolean) deleteResult.get("success")) {
                                deletedFormulaIds.add(formulaId);
                                logger.debug("계산식 삭제 성공: {}", formulaId);
                            } else {
                                failedFormulaIds.add(formulaId);
                                logger.warn("계산식 삭제 실패: {}, 오류: {}", formulaId, deleteResult.get("message"));
                            }
                        }
                    }
                }

                // 결과 구성
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.processFormulaDeleteSuccess");
                result.put("deleted_count", deletedFormulaIds.size());
                result.put("failed_count", failedFormulaIds.size());
                result.put("deleted_formula_ids", deletedFormulaIds);
                result.put("failed_formula_ids", failedFormulaIds);

                if (failedFormulaIds.size() > 0) {
                    result.put("warning", "일부 계산식 삭제에 실패했습니다.");
                }

                logger.debug("Process 계산식 삭제 완료: 성공={}, 실패={}", deletedFormulaIds.size(), failedFormulaIds.size());

            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
                result.put("success", false);
                result.put("message", "messages.error.responseParseFail");
                return result;
            }

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
                errorMessage = "messages.error.processFormulaDeleteFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("Process 계산식 삭제 API 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * Process 도면 삭제 (검색 후 삭제)
     * 
     * @param searchParams 검색 파라미터
     * @return 삭제 결과
     */
    public Map<String, Object> deleteProcessDrawing(Map<String, Object> searchParams) {
        logger.debug("Process 도면 삭제 시작: params={}", searchParams);
        Map<String, Object> result = new HashMap<>();

        if (searchParams == null) {
            logger.warn("도면 삭제 실패: 검색 파라미터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidSearchParams");
            return result;
        }

        // URL 구성 - Drawing Masters API 엔드포인트 사용
        String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/search";
        logger.debug("도면 Master 검색 API URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();

        // 추가 검색 필드들 - searchParams에 값이 있는 경우에만 추가
        String[] additionalFields = {
                "search_field", "search_value",
                "drawing_id", "project_id", "process_id", "drawing_number",
                "drawing_title", "drawing_type", "drawing_status", "revision",
                "drawing_category", "drawing_scale"
        };

        for (String field : additionalFields) {
            Object value = searchParams.get(field);
            if (value != null && !value.toString().trim().isEmpty()) {
                requestMap.put(field, value);
                logger.debug("추가 필드 추가: {} = {}", field, value);
            }
        }
        requestMap.put("page", 1);
        requestMap.put("page_size", 100); // 삭제할 도면이 많을 수 있으므로 더 큰 페이지 사이즈
        requestMap.put("order_by", "");
        requestMap.put("order_direction", "asc");

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("도면 Master 검색 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null) {
                List<String> deletedDrawingIds = new ArrayList<>();
                List<String> failedDrawingIds = new ArrayList<>();

                // items 배열에서 도면 정보 추출
                if (parsedResponse.containsKey("items") && parsedResponse.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        if (item.containsKey("drawing_id")) {
                            String drawingId = (String) item.get("drawing_id");
                            String drawingType = (String) item.get("drawing_type");
                            boolean isSuccess = false;

                            logger.debug("도면 삭제 시작 - drawing_id: {}, type: {}", drawingId, drawingType);

                            // deleteDrawing 함수에서 drawing_type에 따른 처리 수행
                            Map<String, Object> deleteResult = deleteDrawing(drawingId);
                            if ((Boolean) deleteResult.get("success")) {
                                isSuccess = true;
                                logger.debug("도면 삭제 성공: {}", drawingId);
                            } else {
                                logger.warn("도면 삭제 실패: {}, 오류: {}", drawingId, deleteResult.get("message"));
                            }

                            // 결과 처리
                            if (isSuccess) {
                                deletedDrawingIds.add(drawingId);
                            } else {
                                failedDrawingIds.add(drawingId);
                            }
                        }
                    }
                }

                // 결과 구성
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "messages.success.processDrawingDeleteSuccess");
                result.put("deleted_count", deletedDrawingIds.size());
                result.put("failed_count", failedDrawingIds.size());
                result.put("deleted_drawing_ids", deletedDrawingIds);
                result.put("failed_drawing_ids", failedDrawingIds);

                if (failedDrawingIds.size() > 0) {
                    result.put("warning", "일부 도면 삭제에 실패했습니다.");
                }

                logger.debug("Process 도면 삭제 완료: 성공={}, 실패={}", deletedDrawingIds.size(), failedDrawingIds.size());

            } else {
                logger.warn("응답 파싱 실패: {}", responseBody);
                result.put("success", false);
                result.put("message", "messages.error.responseParseFail");
                return result;
            }

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
                errorMessage = "messages.error.processDrawingDeleteFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("Process 도면 삭제 API 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 날짜를 YYYY-MM-DD HH:mm 형식으로 변환하는 유틸리티 메서드
     */
    private String formatDateToYYYYMMDD(Object dateValue) {
        if (dateValue == null) {
            return null;
        }

        try {
            String dateStr = dateValue.toString().trim();
            logger.info("formatDateToYYYYMMDD 입력값: [{}]", dateStr);

            // 이미 YYYY-MM-DD HH:mm 형식인 경우 그대로 반환
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                logger.info("formatDateToYYYYMMDD - 이미 올바른 형식, 출력값: [{}]", dateStr);
                return dateStr;
            }

            // 형식: 2025-10-14 11:23:53.196 +0900 또는 2025-10-14 11:23:53.196
            // 또는 2025-10-14 11:23:53
            java.util.regex.Pattern pattern = java.util.regex.Pattern
                    .compile("(\\d{4}-\\d{2}-\\d{2}) (\\d{2}):(\\d{2}):\\d{2}(?:\\.\\d+)?(?: [+-]\\d{4})?");
            java.util.regex.Matcher matcher = pattern.matcher(dateStr);
            if (matcher.matches()) {
                String datePart = matcher.group(1); // YYYY-MM-DD
                String hour = matcher.group(2); // HH
                String minute = matcher.group(3); // mm
                String result = datePart + " " + hour + ":" + minute;
                logger.info("formatDateToYYYYMMDD - 정규식 매칭 성공, 추출값 [datePart={}, hour={}, minute={}], 출력값: [{}]",
                        datePart, hour, minute, result);
                return result;
            } else {
                logger.info("formatDateToYYYYMMDD - 정규식 매칭 실패: [{}]", dateStr);
            }

            // ISO 8601 형식 (YYYY-MM-DDTHH:mm:ss.sssZ)인 경우 날짜와 시간 추출
            if (dateStr.contains("T")) {
                String[] parts = dateStr.split("T");
                if (parts.length >= 2) {
                    String datePart = parts[0]; // YYYY-MM-DD
                    String timePart = parts[1];
                    // 시간 부분에서 초와 밀리초 제거 (HH:mm:ss.sssZ -> HH:mm)
                    if (timePart.contains(":")) {
                        String[] timeParts = timePart.split(":");
                        if (timeParts.length >= 2) {
                            String hour = timeParts[0];
                            String minute = timeParts[1];
                            String result = datePart + " " + hour + ":" + minute;
                            logger.info("formatDateToYYYYMMDD - ISO 8601 형식 처리, 출력값: [{}]", result);
                            return result;
                        }
                    }
                    String result = datePart + " " + timePart;
                    logger.info("formatDateToYYYYMMDD - ISO 8601 형식 처리(기본), 출력값: [{}]", result);
                    return result;
                }
            }

            // YYYY-MM-DD 형식인 경우 00:00 추가
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String result = dateStr + " 00:00";
                logger.info("formatDateToYYYYMMDD - YYYY-MM-DD 형식에 시간 추가, 출력값: [{}]", result);
                return result;
            }

            // 다른 형식의 경우 기본값 반환
            logger.info("formatDateToYYYYMMDD - 알 수 없는 형식, 원본 반환, 출력값: [{}]", dateStr);
            return dateStr;
        } catch (Exception e) {
            logger.warn("날짜 형식 변환 실패: 입력값={}", dateValue, e);
            return dateValue.toString();
        }
    }

    /**
     * symbol_id가 null이거나 빈 값인지 확인 (빈 UUID 포함)
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
     * 도면 정보 조회 - drawingId로 도면 상세 정보 조회
     */
    public Map<String, Object> getDrawingInfo(String drawingId) {
        logger.debug("도면 정보 조회 시작: drawingId={}", drawingId);
        Map<String, Object> result = new HashMap<>();

        // URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/project/drawing_masters/" + drawingId;
        logger.debug("도면 정보 조회 API URL: {}", apiUrl);

        // HttpUtil을 사용하여 GET 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("response", parsedResponse);

            logger.debug("도면 정보 조회 성공: {}", drawingId);
        } else {
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "도면 정보 조회에 실패했습니다.";
            }

            result.put("message", errorMessage);
            logger.error("도면 정보 조회 실패: status={}, response={}", httpResult.getStatus(), responseBody);
        }

        return result;
    }

    /**
     * Process Component 업데이트 - PATCH API 호출
     */
    public Map<String, Object> updateComponent(String componentId, Map<String, Object> componentData) {
        Map<String, Object> result = new HashMap<>();

        // URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/pid_components/" + componentId;

        // 요청 본문 구성 - 새로운 데이터 구조에 맞게 수정
        Map<String, Object> requestMap = new HashMap<>();

        // 필수 및 선택적 필드들 추가
        if (componentData.containsKey("component_code") && componentData.get("component_code") != null) {
            requestMap.put("component_code", componentData.get("component_code"));
        }
        if (componentData.containsKey("project_id") && componentData.get("project_id") != null) {
            requestMap.put("project_id", componentData.get("project_id"));
        }
        if (componentData.containsKey("process_id") && componentData.get("process_id") != null) {
            requestMap.put("process_id", componentData.get("process_id"));
        }
        if (componentData.containsKey("mapping_type") && componentData.get("mapping_type") != null) {
            requestMap.put("mapping_type", componentData.get("mapping_type"));
        }
        if (componentData.containsKey("pid_id") && componentData.get("pid_id") != null) {
            requestMap.put("pid_id", componentData.get("pid_id"));
        }
        if (componentData.containsKey("pid_number") && componentData.get("pid_number") != null) {
            requestMap.put("pid_number", componentData.get("pid_number"));
        }
        if (componentData.containsKey("input_poc") && componentData.get("input_poc") != null) {
            requestMap.put("input_poc", componentData.get("input_poc"));
        }
        if (componentData.containsKey("output_poc") && componentData.get("output_poc") != null) {
            requestMap.put("output_poc", componentData.get("output_poc"));
        }
        if (componentData.containsKey("component_type") && componentData.get("component_type") != null) {
            requestMap.put("component_type", componentData.get("component_type"));
        }
        if (componentData.containsKey("component_hierachy") && componentData.get("component_hierachy") != null) {
            requestMap.put("component_hierachy", componentData.get("component_hierachy"));
        }
        if (componentData.containsKey("equipment_id") && componentData.get("equipment_id") != null) {
            requestMap.put("equipment_id", componentData.get("equipment_id"));
        }
        if (componentData.containsKey("structure_id") && componentData.get("structure_id") != null) {
            requestMap.put("structure_id", componentData.get("structure_id"));
        }
        if (componentData.containsKey("standard_quantity") && componentData.get("standard_quantity") != null) {
            requestMap.put("standard_quantity", componentData.get("standard_quantity"));
        }
        if (componentData.containsKey("spare_quantity") && componentData.get("spare_quantity") != null) {
            requestMap.put("spare_quantity", componentData.get("spare_quantity"));
        }
        if (componentData.containsKey("total_quantity") && componentData.get("total_quantity") != null) {
            requestMap.put("total_quantity", componentData.get("total_quantity"));
        }
        if (componentData.containsKey("is_active") && componentData.get("is_active") != null) {
            requestMap.put("is_active", componentData.get("is_active"));
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);

        // HttpUtil을 사용하여 PATCH 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.patch(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "Component가 성공적으로 업데이트되었습니다.");
            result.put("response", parsedResponse);

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Component 업데이트에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.error("Component 업데이트 실패: status={}, response={}", httpResult.getStatus(), responseBody);
        }

        return result;
    }

    /**
     * Process Component 검색 - pid_id로 component_id 조회
     */
    public Map<String, Object> searchComponentsByPidId(String pidId) {
        logger.debug("Process Component 검색 시작: pidId={}", pidId);
        Map<String, Object> result = new HashMap<>();

        // URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_components/search";
        logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("search_field", "pid_id");
        requestMap.put("search_value", pidId);
        requestMap.put("page", 1);
        requestMap.put("page_size", 100);
        requestMap.put("order_by", "created_at");
        requestMap.put("order_direction", "asc");

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("Component 검색 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null && parsedResponse.containsKey("items")
                    && parsedResponse.get("items") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                List<String> componentIds = new ArrayList<>();
                for (Map<String, Object> item : items) {
                    if (item.containsKey("component_id")) {
                        componentIds.add((String) item.get("component_id"));
                    }
                }

                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("component_ids", componentIds);
                result.put("response", parsedResponse);

                logger.debug("Component 검색 성공: {}개 component_id 발견", componentIds.size());
            } else {
                result.put("success", false);
                result.put("message", "Component 검색 결과가 없습니다.");
            }
        } else {
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            String responseBody = httpResult.getBody();
            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Component 검색에 실패했습니다.";
            }

            result.put("message", errorMessage);
            logger.error("Component 검색 실패: status={}, response={}", httpResult.getStatus(), responseBody);
        }

        return result;
    }

    /**
     * Process Component 삭제 - DELETE API 호출
     */
    public Map<String, Object> deleteComponent(String componentId) {
        logger.debug("Process Component 삭제 시작: componentId={}", componentId);
        Map<String, Object> result = new HashMap<>();

        // URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/pid_components/" + componentId;
        logger.debug("API URL: {}", apiUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            logger.debug("Component 삭제 성공: {}", responseBody);

            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "Component가 성공적으로 삭제되었습니다.");
            result.put("response", parsedResponse);

        } else {
            // 에러 응답 처리
            result.put("success", false);
            result.put("status", httpResult.getStatus());

            // 에러 메시지 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            String errorMessage = JsonUtil.extractValue(responseBody, "message");
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = JsonUtil.extractValue(responseBody, "detail");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Component 삭제에 실패했습니다.";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.error("Component 삭제 실패: status={}, response={}", httpResult.getStatus(), responseBody);
        }

        return result;
    }

    /**
     * process_dependencies 파싱하여 equipments_set과 structures_set을 통합된 리스트로 변환
     * 
     * @param processDependencies 원본 process_dependencies 데이터
     * @return 변환된 의존성 리스트
     */
    private List<Map<String, Object>> parseProcessDependencies(Object processDependencies) {
        List<Map<String, Object>> result = new ArrayList<>();

        logger.debug("parseProcessDependencies 시작 - 입력 데이터 타입: {}",
                processDependencies != null ? processDependencies.getClass().getSimpleName() : "null");

        if (processDependencies == null) {
            logger.debug("process_dependencies가 null입니다.");
            return result;
        }

        if (!(processDependencies instanceof Map)) {
            logger.warn("process_dependencies가 Map 타입이 아닙니다: {}", processDependencies.getClass().getSimpleName());
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> depsMap = (Map<String, Object>) processDependencies;

        logger.debug("process_dependencies 원본 데이터: {}", depsMap);

        // equipments_set 처리
        if (depsMap.containsKey("equipments_set")) {
            Object equipmentsSet = depsMap.get("equipments_set");
            logger.debug("equipments_set 발견 - 타입: {}",
                    equipmentsSet != null ? equipmentsSet.getClass().getSimpleName() : "null");

            if (equipmentsSet instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> equipmentItemsMap = (Map<String, Object>) equipmentsSet;
                logger.debug("equipments_set 처리 시작 - 아이템 수: {}", equipmentItemsMap.size());

                for (Map.Entry<String, Object> entry : equipmentItemsMap.entrySet()) {
                    String itemKey = entry.getKey();
                    Object itemValue = entry.getValue();

                    if (itemValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> equipmentItem = (Map<String, Object>) itemValue;
                        Map<String, Object> restructuredItem = new HashMap<>();

                        // 기본 필드 변환
                        restructuredItem.put("code", equipmentItem.get("equipment_code"));
                        restructuredItem.put("value", equipmentItem.get("equipment_value"));
                        restructuredItem.put("value_en", equipmentItem.get("equipment_value_en"));
                        restructuredItem.put("code_level", equipmentItem.get("equipment_code_level"));

                        // code_hierarchy 처리
                        if (equipmentItem.containsKey("code_hierarchy")
                                && equipmentItem.get("code_hierarchy") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> hierarchy = (Map<String, Object>) equipmentItem.get("code_hierarchy");

                            // level1 처리
                            if (hierarchy.containsKey("level1") && hierarchy.get("level1") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level1 = (Map<String, Object>) hierarchy.get("level1");
                                restructuredItem.put("level1_code", level1.get("code"));
                                restructuredItem.put("level1_value", level1.get("value"));
                                restructuredItem.put("level1_value_en", level1.get("value_en"));
                            }

                            // level2 처리
                            if (hierarchy.containsKey("level2") && hierarchy.get("level2") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level2 = (Map<String, Object>) hierarchy.get("level2");
                                restructuredItem.put("level2_code", level2.get("code"));
                                restructuredItem.put("level2_value", level2.get("value"));
                                restructuredItem.put("level2_value_en", level2.get("value_en"));
                            }

                            // level3 처리
                            if (hierarchy.containsKey("level3") && hierarchy.get("level3") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level3 = (Map<String, Object>) hierarchy.get("level3");
                                restructuredItem.put("level3_code", level3.get("code"));
                                restructuredItem.put("level3_value", level3.get("value"));
                                restructuredItem.put("level3_value_en", level3.get("value_en"));
                            }

                            // level4 처리
                            if (hierarchy.containsKey("level4") && hierarchy.get("level4") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level4 = (Map<String, Object>) hierarchy.get("level4");
                                restructuredItem.put("level4_code", level4.get("code"));
                                restructuredItem.put("level4_value", level4.get("value"));
                                restructuredItem.put("level4_value_en", level4.get("value_en"));
                            }
                        }

                        result.add(restructuredItem);
                        logger.debug("equipment 아이템 변환 완료 ({}): {}", itemKey, restructuredItem);
                    }
                }
                logger.debug("equipments_set 처리 완료 - 변환된 아이템 수: {}", equipmentItemsMap.size());
            } else {
                logger.warn("equipments_set이 Map 타입이 아닙니다: {}", equipmentsSet.getClass().getSimpleName());
            }
        } else {
            logger.debug("equipments_set이 존재하지 않습니다.");
        }

        // structures_set 처리
        if (depsMap.containsKey("structures_set")) {
            Object structuresSet = depsMap.get("structures_set");
            logger.debug("structures_set 발견 - 타입: {}",
                    structuresSet != null ? structuresSet.getClass().getSimpleName() : "null");

            if (structuresSet instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> structureItemsMap = (Map<String, Object>) structuresSet;
                logger.debug("structures_set 처리 시작 - 아이템 수: {}", structureItemsMap.size());

                for (Map.Entry<String, Object> entry : structureItemsMap.entrySet()) {
                    String itemKey = entry.getKey();
                    Object itemValue = entry.getValue();

                    if (itemValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> structureItem = (Map<String, Object>) itemValue;
                        Map<String, Object> restructuredItem = new HashMap<>();

                        // 기본 필드 변환
                        restructuredItem.put("code", structureItem.get("structure_code"));
                        restructuredItem.put("value", structureItem.get("structure_value"));
                        restructuredItem.put("value_en", structureItem.get("structure_value_en"));
                        restructuredItem.put("code_level", structureItem.get("structure_code_level"));

                        // code_hierarchy 처리
                        if (structureItem.containsKey("code_hierarchy")
                                && structureItem.get("code_hierarchy") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> hierarchy = (Map<String, Object>) structureItem.get("code_hierarchy");

                            // level1 처리
                            if (hierarchy.containsKey("level1") && hierarchy.get("level1") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level1 = (Map<String, Object>) hierarchy.get("level1");
                                restructuredItem.put("level1_code", level1.get("code"));
                                restructuredItem.put("level1_value", level1.get("value"));
                                restructuredItem.put("level1_value_en", level1.get("value_en"));
                            }

                            // level2 처리
                            if (hierarchy.containsKey("level2") && hierarchy.get("level2") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level2 = (Map<String, Object>) hierarchy.get("level2");
                                restructuredItem.put("level2_code", level2.get("code"));
                                restructuredItem.put("level2_value", level2.get("value"));
                                restructuredItem.put("level2_value_en", level2.get("value_en"));
                            }

                            // level3 처리
                            if (hierarchy.containsKey("level3") && hierarchy.get("level3") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level3 = (Map<String, Object>) hierarchy.get("level3");
                                restructuredItem.put("level3_code", level3.get("code"));
                                restructuredItem.put("level3_value", level3.get("value"));
                                restructuredItem.put("level3_value_en", level3.get("value_en"));
                            }

                            // level4 처리
                            if (hierarchy.containsKey("level4") && hierarchy.get("level4") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> level4 = (Map<String, Object>) hierarchy.get("level4");
                                restructuredItem.put("level4_code", level4.get("code"));
                                restructuredItem.put("level4_value", level4.get("value"));
                                restructuredItem.put("level4_value_en", level4.get("value_en"));
                            }
                        }

                        result.add(restructuredItem);
                        logger.debug("structure 아이템 변환 완료 ({}): {}", itemKey, restructuredItem);
                    }
                }
                logger.debug("structures_set 처리 완료 - 변환된 아이템 수: {}", structureItemsMap.size());
            } else {
                logger.warn("structures_set이 Map 타입이 아닙니다: {}", structuresSet.getClass().getSimpleName());
            }
        } else {
            logger.debug("structures_set이 존재하지 않습니다.");
        }

        logger.debug("parseProcessDependencies 완료 - 최종 결과 아이템 수: {}", result.size());
        logger.debug("parseProcessDependencies 최종 결과: {}", result);

        return result;
    }

    /**
     * CCS(용량계산서) 엑셀파일 업로드 - 외부 API 호출 (Multipart)
     */
    public Map<String, Object> uploadCcs(String tableName, String pkValue,
            org.springframework.web.multipart.MultipartFile file) {
        logger.debug("외부 인증 서버 용량계산서 파일 업로드 시도: server={}, tableName={}, pkValue={}", authServerBaseUrl, tableName,
                pkValue);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String uploadUrl = authServerBaseUrl + "/api/v1/minio/upload/" + tableName + "/" + pkValue;
        logger.debug("용량계산서 파일 업로드 URL: {}", uploadUrl);

        // HttpUtil을 사용하여 Multipart POST 요청 수행
        Map<String, Object> formData = new HashMap<>();
        if (file != null) {
            formData.put("file", file);
        }
        formData.put("file_category", "process_ccs");

        HttpUtil.HttpResult httpResult = HttpUtil.postMultipart(uploadUrl, formData);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 응답 형식을 info 레벨로 로그 출력
            if (parsedResponse != null) {
                // API 응답 형식으로 로그 출력 (status, message, data 포함)
                Map<String, Object> apiResponse = new HashMap<>();
                apiResponse.put("status", parsedResponse.getOrDefault("status", "success"));
                apiResponse.put("message", parsedResponse.getOrDefault("message", "용량계산서 파일 업로드가 완료되었습니다."));
                apiResponse.put("data", parsedResponse.getOrDefault("data", parsedResponse));
                logger.info("용량계산서 파일 업로드 API 응답: {}", JsonUtil.objectMapToJson(apiResponse));

                result.put("success", true);
                result.put("data", parsedResponse);
                result.put("message", "용량계산서 파일 업로드가 완료되었습니다.");
            } else {
                logger.error("용량계산서 파일 업로드 응답 파싱 실패");
                result.put("success", false);
                result.put("message", "응답 파싱에 실패했습니다.");
            }
        } else {
            // 실패한 응답 처리
            logger.error("용량계산서 파일 업로드 실패: statusCode={}, body={}", httpResult.getStatus(), httpResult.getBody());
            result.put("success", false);

            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // HttpUtil에서 추출된 에러 메시지 사용
            String errorMessage = httpResult.getExtractedErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "용량계산서 파일 업로드에 실패했습니다.";
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
     * 용량계산서(CCS) 파일 다운로드 URL 조회
     * 
     * @param tableName 테이블명
     * @param pkValue   기본키 값
     * @return 다운로드 URL과 파일 정보
     */
    public Map<String, Object> getCcsDownload(String tableName, String pkValue) {
        logger.debug("용량계산서 파일 다운로드 URL 조회 API 호출: tableName={}, pkValue={}", tableName, pkValue);
        Map<String, Object> result = new HashMap<>();

        if (tableName == null || tableName.trim().isEmpty()) {
            logger.warn("용량계산서 파일 다운로드 URL 조회 실패: 테이블명이 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "테이블명이 필요합니다.");
            return result;
        }

        if (pkValue == null || pkValue.trim().isEmpty()) {
            logger.warn("용량계산서 파일 다운로드 URL 조회 실패: 기본키 값이 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "기본키 값이 필요합니다.");
            return result;
        }

        try {
            // 파라미터를 문자열로 명시적 변환
            String tableNameStr = tableName.toString().trim();
            String pkValueStr = pkValue.toString().trim();
            logger.debug("변환된 tableName: {}, pkValue: {}", tableNameStr, pkValueStr);

            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/download/" + tableNameStr + "/" + pkValueStr;
            logger.debug("용량계산서 파일 다운로드 URL: {}", apiUrl);

            // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
            HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", null);

            // 응답 처리
            if (httpResult.isSuccess()) {
                // 성공적인 응답 처리
                String responseBody = httpResult.getBody();

                logger.debug("용량계산서 파일 다운로드 URL 조회 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "용량계산서 파일 다운로드 URL 조회가 완료되었습니다.");
                result.put("download_url", apiUrl);
                result.put("response_body", responseBody);

                // 다운로드 URL만 반환 (헤더 정보는 HttpUtil에서 제공하지 않음)
                logger.debug("다운로드 URL 생성 완료: {}", apiUrl);

            } else {
                // 에러 응답 처리
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
                    errorMessage = "용량계산서 파일 다운로드 URL 조회에 실패했습니다.";
                }

                result.put("success", false);
                result.put("status", httpResult.getStatus());
                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("외부 인증 서버 용량계산서 파일 다운로드 URL 조회 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("용량계산서 파일 다운로드 URL 조회 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "용량계산서 파일 다운로드 URL 조회 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 용량계산서(CCS) 파일 삭제
     * 
     * @param tableName 테이블명
     * @param pkValue   기본키 값
     * @param filename  파일명
     * @return 삭제 결과
     */
    public Map<String, Object> deleteCcs(String tableName, String pkValue, String filename) {
        Map<String, Object> result = new HashMap<>();

        if (tableName == null || tableName.trim().isEmpty()) {
            logger.warn("용량계산서 파일 삭제 실패: 테이블명이 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "테이블명이 필요합니다.");
            return result;
        }

        if (pkValue == null || pkValue.trim().isEmpty()) {
            logger.warn("용량계산서 파일 삭제 실패: 기본키 값이 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "기본키 값이 필요합니다.");
            return result;
        }

        if (filename == null || filename.trim().isEmpty()) {
            logger.warn("용량계산서 파일 삭제 실패: 파일명이 null이거나 빈 값");
            result.put("success", false);
            result.put("message", "파일명이 필요합니다.");
            return result;
        }

        try {
            // 파라미터를 문자열로 명시적 변환
            String tableNameStr = tableName.toString().trim();
            String pkValueStr = pkValue.toString().trim();
            String filenameStr = filename.toString().trim();
            logger.debug("변환된 tableName: {}, pkValue: {}, filename: {}", tableNameStr, pkValueStr, filenameStr);

            // 외부 인증 서버 URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/minio/files/" + tableNameStr + "/" + pkValueStr + "/"
                    + filenameStr;
            logger.debug("용량계산서 파일 삭제 URL: {}", apiUrl);

            // HttpUtil을 사용하여 DELETE 요청 수행
            HttpUtil.HttpResult httpResult = HttpUtil.delete(apiUrl, "application/json", null);

            if (httpResult.isSuccess()) {
                String responseBody = httpResult.getBody();
                Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

                logger.debug("용량계산서 파일 삭제 API 성공");
                result.put("success", true);
                result.put("status", httpResult.getStatus());
                result.put("message", "용량계산서 파일 삭제가 완료되었습니다.");
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
                    errorMessage = "용량계산서 파일 삭제에 실패했습니다.";
                }

                result.put("message", errorMessage);
                result.put("response", parsedResponse);

                logger.warn("용량계산서 파일 삭제 API 실패: {}", errorMessage);
            }

        } catch (Exception e) {
            logger.error("용량계산서 파일 삭제 API 호출 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "용량계산서 파일 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

}