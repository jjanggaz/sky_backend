package com.wai.admin.service.cost;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

@Service
public class CostService {

    private static final Logger logger = LoggerFactory.getLogger(CostService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 단가 목록 조회 - 외부 API 호출
     */
    public Map<String, Object> getCostList(Map<String, Object> searchParams) {
        logger.info("외부 인증 서버 단가 목록 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();

        // 외부 인증 서버 URL 구성
        String costUrl = authServerBaseUrl + "/api/v1/equipment/equipment_price_history/search";
        logger.info("단가 목록 조회 URL: {}", costUrl);

        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();

        // 검색 조건 분기처리
        Object keyword = searchParams.get("keyword");
        if (keyword != null && !keyword.toString().trim().isEmpty()) {
            // keyword가 있는 경우 keyword 조건만 적용
            requestMap.put("keyword", keyword);
        } else {
            // keyword가 없는 경우 search_field, search_value 조건 적용
            requestMap.put("search_field", searchParams.getOrDefault("search_field", "history_id"));
            requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        }

        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 10));

        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.info("단가 목록 조회 요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(costUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.info("단가 목록 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("단가 목록 조회 응답 파싱 실패: {}", responseBody);
            }

            logger.info("단가 목록 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.costListSuccess");
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
                errorMessage = "messages.error.costListFail";
            }

            result.put("message", errorMessage);
            result.put("response", parsedResponse);

            logger.warn("외부 인증 서버 단가 목록 조회 실패: {}", errorMessage);
        }

        return result;
    }

    public Map<String, Object> getAllCosts() {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("costs", "비용 목록 데이터");
        return data;
    }

    public Map<String, Object> getCostById(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("costId", id);
        data.put("costType", "비용 유형");
        data.put("amount", 1000000);
        data.put("currency", "KRW");
        return data;
    }

    public Map<String, Object> createCost(Map<String, Object> costData) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("createdCost", costData);
        data.put("createdAt", System.currentTimeMillis());
        return data;
    }

    public Map<String, Object> updateCost(String id, Map<String, Object> costData) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("updatedCostId", id);
        data.put("updatedData", costData);
        data.put("updatedAt", System.currentTimeMillis());
        return data;
    }

    public Map<String, Object> deleteCost(String id) {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCostId", id);
        data.put("deletedAt", System.currentTimeMillis());
        return data;
    }

    public Map<String, Object> getCostSummary() {
        // TODO: 외부 API 호출 로직 구현
        Map<String, Object> data = new HashMap<>();
        data.put("totalCost", 5000000);
        data.put("monthlyCost", 1000000);
        data.put("costBreakdown", "비용 분석 데이터");
        return data;
    }
}