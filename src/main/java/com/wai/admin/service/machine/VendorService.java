package com.wai.admin.service.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.Map;

@Service
public class VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 벤더 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getVendorList(Map<String, Object> searchParams) {
        logger.debug("외부 인증 서버 벤더 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String vendorUrl = authServerBaseUrl + "/api/v1/equipment/vendors/search";
        logger.debug("벤더 목록 조회 URL: {}", vendorUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 분기처리
        Object keyword = searchParams.get("keyword");
        if (keyword != null && !keyword.toString().trim().isEmpty()) {
            // keyword가 있는 경우 keyword 조건만 적용
            requestMap.put("keyword", keyword);
        } else {
            // keyword가 없는 경우 search_field, search_value 조건 적용
            requestMap.put("search_field", searchParams.getOrDefault("search_field", "vendor_id"));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        }

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));

        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.info("벤더 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(vendorUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("벤더 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("벤더 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            logger.debug("벤더 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.vendorListSuccess");
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
                errorMessage = "messages.error.vendorListFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 벤더 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 벤더 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteVendor(String vendorId) {
        logger.debug("외부 인증 서버 벤더 삭제 시도: server={}, vendorId={}", authServerBaseUrl, vendorId);
        Map<String, Object> result = new HashMap<>();

        if (vendorId == null || vendorId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "vendor_id 파라미터가 필요합니다.");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String vendorUrl = authServerBaseUrl + "/api/v1/equipment/vendors/" + vendorId;
        logger.debug("벤더 삭제 URL: {}", vendorUrl);

        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(vendorUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("벤더 삭제 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
            }

            logger.debug("벤더 삭제 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.vendorDeleteSuccess");
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
                errorMessage = "messages.error.vendorDeleteFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 벤더 삭제 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 벤더 생성 - 외부 API 호출
     */
    public Map<String, Object> createVendor(Map<String, Object> createParams) {
        logger.debug("외부 인증 서버 벤더 생성 시도: server={}, params={}", authServerBaseUrl, createParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String vendorUrl = authServerBaseUrl + "/api/v1/equipment/vendors/";
        logger.debug("벤더 생성 URL: {}", vendorUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        String requestBody = JsonUtil.objectMapToJson(createParams);
        logger.debug("벤더 생성 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(vendorUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("벤더 생성 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
            }

            logger.debug("벤더 생성 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.vendorCreateSuccess");
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
                errorMessage = "messages.error.vendorCreateFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 벤더 생성 실패: {}", errorMessage);
        }

        return result;
    }

    /**
     * 벤더 수정 - 외부 API 호출
     */
    public Map<String, Object> updateVendor(String vendorId, Map<String, Object> updateParams) {
        logger.debug("외부 인증 서버 벤더 수정 시도: server={}, vendorId={}, params={}", authServerBaseUrl, vendorId, updateParams);
        Map<String, Object> result = new HashMap<>();

        if (vendorId == null || vendorId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "vendor_id 파라미터가 필요합니다.");
            return result;
        }

        // 외부 인증 서버 URL 구성
        String vendorUrl = authServerBaseUrl + "/api/v1/equipment/vendors/" + vendorId;
        logger.debug("벤더 수정 URL: {}", vendorUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        String requestBody = JsonUtil.objectMapToJson(updateParams);
        logger.debug("벤더 수정 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(vendorUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("벤더 수정 성공 응답: {}", JsonUtil.objectMapToJson(parsedResponse));
            }

            logger.debug("벤더 수정 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.vendorUpdateSuccess");
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
                errorMessage = "messages.error.vendorUpdateFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 벤더 수정 실패: {}", errorMessage);
        }

        return result;
    }
}
