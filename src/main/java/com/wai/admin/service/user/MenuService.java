package com.wai.admin.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;


import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 메뉴 조회 - 외부 API 호출
     */
    public Map<String, Object> searchMenus(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("외부 인증 서버 메뉴 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String menusUrl = authServerBaseUrl + "/api/v1/auth/system_menus/search";
        logger.debug("메뉴 조회 URL: {}", menusUrl);
        
        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();
        
        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        requestMap.put("menu_type", searchParams.getOrDefault("menu_type", ""));
        requestMap.put("get_all", searchParams.getOrDefault("get_all", true));
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("메뉴 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(menusUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("메뉴 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
                
                // 메뉴 데이터를 계층 구조로 변환
                String menuType = (String) searchParams.getOrDefault("menu_type", "");
                parsedResponse = convertToHierarchicalMenu(parsedResponse, menuType, request);
            } else {
                logger.warn("메뉴 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("메뉴 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.menuSearchSuccess");
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
                errorMessage = "messages.error.menuSearchFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 메뉴 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 메뉴 데이터를 계층 구조로 변환하는 메서드
     * @param response 외부 API 응답 데이터
     * @param menuType 필터링할 메뉴 타입
     * @param request HttpServletRequest 객체
     * @return 계층 구조로 변환된 메뉴 데이터
     */
    private Map<String, Object> convertToHierarchicalMenu(Map<String, Object> response, String menuType, HttpServletRequest request) {
        if (response == null || !response.containsKey("items")) {
            return response;
        }

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) response.get("items");
        if (items == null || items.isEmpty()) {
            return response;
        }

        // 헤더에서 wai_lang 값 가져오기
        String language = request.getHeader("wai_lang");
        language = (language != null && !language.trim().isEmpty()) ? language.trim() : null;
        logger.debug("현재 헤더 언어: {}", language);

        // menuType과 system_code가 같은 메뉴만 필터링
        java.util.List<Map<String, Object>> filteredItems = new java.util.ArrayList<>();
        for (Map<String, Object> menu : items) {
            String itemMenuType = (String) menu.get("menu_type");
            String itemSystemCode = (String) menu.get("system_code");
            
            if (menuType.equals(itemSystemCode)) {
                // 언어에 따른 메뉴 이름 설정
                setMenuNameByLanguage(menu, language);
                filteredItems.add(menu);
            }
        }

        if (filteredItems.isEmpty()) {
            logger.debug("menu_type({})와 system_code가 같은 메뉴가 없습니다.", menuType);
            Map<String, Object> result = new HashMap<>(response);
            result.put("items", new java.util.ArrayList<>());
            return result;
        }

        // 메뉴 ID를 키로 하는 맵 생성
        Map<String, Map<String, Object>> menuMap = new HashMap<>();
        java.util.List<Map<String, Object>> rootMenus = new java.util.ArrayList<>();

        // 필터링된 메뉴를 맵에 저장하고 children 배열 초기화
        for (Map<String, Object> menu : filteredItems) {
            menu.put("children", new java.util.ArrayList<Map<String, Object>>());
            menuMap.put((String) menu.get("menu_id"), menu);
        }

        // 부모-자식 관계 설정
        for (Map<String, Object> menu : filteredItems) {
            String parentMenuId = (String) menu.get("parent_menu_id");
            
            if (parentMenuId == null) {
                // 최상위 메뉴
                rootMenus.add(menu);
            } else {
                // 하위 메뉴 - 부모 메뉴의 children에 추가
                Map<String, Object> parentMenu = menuMap.get(parentMenuId);
                if (parentMenu != null) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> children = (java.util.List<Map<String, Object>>) parentMenu.get("children");
                    children.add(menu);
                }
            }
        }

        // 계층 구조로 변환된 items로 교체
        Map<String, Object> result = new HashMap<>(response);
        result.put("items", rootMenus);
        
        logger.debug("menu_type({})에 맞는 메뉴 {}개를 계층 구조로 변환했습니다.", menuType, filteredItems.size());
        return result;
    }

    /**
     * 언어에 따라 메뉴 이름을 설정하는 메서드
     * @param menu 메뉴 데이터
     * @param language 언어 코드 (ko, en)
     */
    private void setMenuNameByLanguage(Map<String, Object> menu, String language) {
        String menuName = null;
        
        if ("ko".equals(language)) {
            menuName = (String) menu.get("term_name_ko");
        } else if ("en".equals(language)) {
            menuName = (String) menu.get("term_name_en");
        }
        
        // 언어별 term_name이 없으면 기본 menu_name 사용
        if (menuName == null || menuName.isEmpty()) {
            menuName = (String) menu.get("menu_name");
        }
        
        // menu_name 필드에 설정된 이름 저장
        menu.put("menu_name", menuName);
        
        logger.debug("메뉴 ID: {}, 언어: {}, 설정된 이름: {}", 
                   menu.get("menu_id"), language, menuName);
    }

    /**
     * 메뉴 수정 - 외부 API 호출
     */
    public Map<String, Object> updateMenu(String menuId, Map<String, Object> menuData) {
        logger.debug("외부 인증 서버 메뉴 수정 시도: server={}, menuId={}", authServerBaseUrl, menuId);
        Map<String, Object> result = new HashMap<>();
        
        if (menuData == null) {
            logger.warn("메뉴 수정 실패: 메뉴 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidMenuData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String menusUrl = authServerBaseUrl + "/api/v1/auth/system_menus/" + menuId;
        logger.debug("메뉴 수정 URL: {}", menusUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(menuData);
        logger.debug("메뉴 수정 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(menusUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("메뉴 수정 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.menuUpdateSuccess");
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
                errorMessage = "messages.error.menuUpdateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 메뉴 수정 실패: {}", errorMessage);
        }
        
        return result;
    }


}