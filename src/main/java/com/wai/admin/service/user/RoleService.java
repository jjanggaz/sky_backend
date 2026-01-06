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
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    // 외부 인증 서버 설정
    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * 권한 조회 - 외부 API 호출
     */
    public Map<String, Object> searchRoles(Map<String, Object> searchParams, HttpServletRequest request) {
        logger.debug("외부 인증 서버 권한 조회 시도: server={}, params={}", authServerBaseUrl, searchParams);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String rolesUrl = authServerBaseUrl + "/api/v1/auth/roles/search";
        logger.debug("권한 조회 URL: {}", rolesUrl);
        
        // 요청 본문 구성 - 화면에서 받은 파라미터 사용
        Map<String, Object> requestMap = new HashMap<>();
        
        // 기본값 설정
        requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
        requestMap.put("search_value", searchParams.getOrDefault("search_value", ""));
        requestMap.put("page", searchParams.getOrDefault("page", 1));
        requestMap.put("page_size", searchParams.getOrDefault("page_size", 20));
        requestMap.put("order_by", searchParams.getOrDefault("order_by", ""));
        requestMap.put("order_direction", searchParams.getOrDefault("order_direction", "asc"));
        
        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("권한 조회 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(rolesUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("권한 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
            } else {
                logger.warn("권한 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("권한 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleSearchSuccess");
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
                errorMessage = "messages.error.roleSearchFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 권한 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 권한 등록 - 외부 API 호출
     */
    public Map<String, Object> createRole(Map<String, Object> roleData) {
        logger.debug("외부 인증 서버 권한 등록 시도: server={}", authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (roleData == null) {
            logger.warn("권한 등록 실패: 권한 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidRoleData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String rolesUrl = authServerBaseUrl + "/api/v1/auth/roles/";
        logger.debug("권한 등록 URL: {}", rolesUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(roleData);
        logger.debug("권한 등록 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.post(rolesUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("권한 등록 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleCreateSuccess");
            result.put("response", parsedResponse);
            
            // 권한 등록 성공 후 메뉴 권한 할당 API 호출
            try {
                // parsedResponse에서 role_id 추출
                Object roleIdObj = parsedResponse.get("role_id");
                if (roleIdObj != null) {
                    String roleId = roleIdObj.toString();
                    logger.debug("메뉴 권한 할당 시도: role_id={}", roleId);
                    
                    // roleData에서 직접 menus 추출
                    try {
                        Object menusObj = roleData.get("menus");
                        
                        // menus 데이터 타입 확인 및 로깅
                        logger.debug("menus 데이터 타입: {}, 값: {}", 
                            menusObj != null ? menusObj.getClass().getSimpleName() : "null", 
                            menusObj);
                        
                        if (menusObj != null) {
                            // 메뉴 권한 할당 API 호출
                            String assignMenuPermissionsUrl = authServerBaseUrl + "/api/v1/auth/role_permissions/assign_menu_permissions";
                            
                            // 요청 데이터 구성
                            Map<String, Object> menuPermissionData = new HashMap<>();
                            menuPermissionData.put("role_id", roleId);
                            menuPermissionData.put("menus", menusObj);
                            
                            String menuPermissionRequestBody = JsonUtil.objectMapToJson(menuPermissionData);
                            logger.debug("메뉴 권한 할당 요청 본문: {}", menuPermissionRequestBody);
                            
                            // POST 요청으로 메뉴 권한 할당
                            HttpUtil.HttpResult menuPermissionResult = HttpUtil.post(assignMenuPermissionsUrl, "application/json", menuPermissionRequestBody);
                            
                            if (menuPermissionResult.isSuccess()) {
                                logger.debug("메뉴 권한 할당 성공");
                                result.put("success", true);
                                result.put("status", menuPermissionResult.getStatus());
                                result.put("message", "messages.success.menuPermissionAssignSuccess");
                                
                            } else {
                                logger.warn("메뉴 권한 할당 실패: status={}, message={}", 
                                    menuPermissionResult.getStatus(), menuPermissionResult.getExtractedErrorMessage());
                                result.put("success", false);
                                result.put("status", menuPermissionResult.getStatus());
                                result.put("message", "messages.error.menuPermissionAssignFail");
                                
                            }
                        } else {
                            logger.debug("메뉴 권한 할당 건너뜀: menus 데이터가 없음");
                            result.put("success", false);
                            result.put("message", "messages.error.noMenuDataSkipPermission");
                        }
                    } catch (Exception e) {
                        logger.error("메뉴 권한 할당 처리 중 오류 발생", e);
                        result.put("success", false);
                        result.put("message", "메뉴 권한 할당 중 오류가 발생했습니다: " + e.getMessage());
                    }
                } else {
                    logger.warn("메뉴 권한 할당 실패: role_id를 찾을 수 없음");
                    result.put("success", false);
                    result.put("message", "messages.error.roleIdNotFoundSkipPermission");
                }
            } catch (Exception e) {
                logger.error("메뉴 권한 할당 중 오류 발생", e);
                result.put("success", false);
                result.put("message", "메뉴 권한 할당 중 오류가 발생했습니다: " + e.getMessage());
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
                errorMessage = "messages.error.roleCreateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 권한 등록 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 권한 수정 - 외부 API 호출
     */
    public Map<String, Object> updateRole(String roleId, Map<String, Object> roleData) {
        logger.debug("외부 인증 서버 권한 수정 시도: server={}, roleId={}", authServerBaseUrl, roleId);
        Map<String, Object> result = new HashMap<>();
        
        if (roleData == null) {
            logger.warn("권한 수정 실패: 권한 데이터가 null");
            result.put("success", false);
            result.put("message", "messages.error.invalidRoleData");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String rolesUrl = authServerBaseUrl + "/api/v1/auth/roles/" + roleId;
        logger.debug("권한 수정 URL: {}", rolesUrl);
        
        // 요청 본문 구성
        String requestBody = JsonUtil.objectMapToJson(roleData);
        logger.debug("권한 수정 요청 본문: {}", requestBody);
        
        // HttpUtil을 사용하여 PATCH 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.patch(rolesUrl, "application/json", requestBody);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("권한 수정 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleUpdateSuccess");
            result.put("response", parsedResponse);
            
            // 권한 수정 성공 후 메뉴 권한 할당 업데이트 API 호출
            try {
                // roleData에서 menus 추출
                Object menusObj = roleData.get("menus");
                
                // menus 데이터 타입 확인 및 로깅
                logger.debug("menus 데이터 타입: {}, 값: {}", 
                    menusObj != null ? menusObj.getClass().getSimpleName() : "null", 
                    menusObj);
                
                if (menusObj != null) {
                    // 메뉴 권한 할당 업데이트 API 호출
                    String updateMenuPermissionsUrl = authServerBaseUrl + "/api/v1/auth/role_permissions/assign_menu_permissions/update";
                    
                    // 요청 데이터 구성
                    Map<String, Object> menuPermissionData = new HashMap<>();
                    menuPermissionData.put("role_id", roleId);
                    menuPermissionData.put("menus", menusObj);
                    
                    String menuPermissionRequestBody = JsonUtil.objectMapToJson(menuPermissionData);
                    logger.debug("메뉴 권한 할당 업데이트 요청 본문: {}", menuPermissionRequestBody);
                    
                    // PUT 요청으로 메뉴 권한 할당 업데이트
                    HttpUtil.HttpResult menuPermissionResult = HttpUtil.put(updateMenuPermissionsUrl, "application/json", menuPermissionRequestBody);
                    
                    if (menuPermissionResult.isSuccess()) {
                        logger.debug("메뉴 권한 할당 업데이트 성공");
                        result.put("success", true);
                        result.put("status", menuPermissionResult.getStatus());
                        result.put("message", "messages.success.roleAndMenuPermissionUpdateSuccess");
                        
                    } else {
                        logger.warn("메뉴 권한 할당 업데이트 실패: status={}, message={}", 
                            menuPermissionResult.getStatus(), menuPermissionResult.getExtractedErrorMessage());
                        result.put("success", false);
                        result.put("status", menuPermissionResult.getStatus());
                        result.put("message", "messages.error.roleUpdateSuccessButMenuPermissionFail");
                        
                    }
                } else {
                    logger.debug("메뉴 권한 할당 업데이트 건너뜀: menus 데이터가 없음");
                    result.put("success", true);
                    result.put("message", "messages.success.roleUpdateSuccessNoMenuData");
                }
            } catch (Exception e) {
                logger.error("메뉴 권한 할당 업데이트 처리 중 오류 발생", e);
                result.put("success", false);
                result.put("message", "권한은 수정되었지만 메뉴 권한 할당 업데이트 중 오류가 발생했습니다: " + e.getMessage());
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
                errorMessage = "messages.error.roleUpdateFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 권한 수정 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 권한 삭제 - 외부 API 호출
     */
    public Map<String, Object> deleteRole(String roleId) {
        logger.debug("외부 인증 서버 권한 삭제 시도: server={}, roleId={}", authServerBaseUrl, roleId);
        Map<String, Object> result = new HashMap<>();
        
        // 외부 인증 서버 URL 구성
        String rolesUrl = authServerBaseUrl + "/api/v1/auth/roles/" + roleId;
        logger.debug("권한 삭제 URL: {}", rolesUrl);
        
        // HttpUtil을 사용하여 DELETE 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.delete(rolesUrl, "application/json", "");
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            logger.debug("권한 삭제 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleDeleteSuccess");
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
                errorMessage = "messages.error.roleDeleteFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 권한 삭제 실패: {}", errorMessage);
        }
        
        return result;
    }

    /**
     * 특정 권한 조회(화면 테이블 보기버튼 누르면 호출) - 외부 API 호출
     */
    public Map<String, Object> getRoleById(String roleId, HttpServletRequest request) {
        logger.debug("외부 인증 서버 특정 권한 조회 시도: roleId={}, server={}", roleId, authServerBaseUrl);
        Map<String, Object> result = new HashMap<>();
        
        if (roleId == null || roleId.isEmpty()) {
            logger.warn("특정 권한 조회 실패: 권한 ID가 null이거나 비어있음");
            result.put("success", false);
            result.put("message", "messages.error.invalidRoleId");
            return result;
        }
        
        // 외부 인증 서버 URL 구성
        String roleUrl = authServerBaseUrl + "/api/v1/auth/roles/" + roleId;
        logger.debug("특정 권한 조회 URL: {}", roleUrl);
        
        // HttpUtil을 사용하여 GET 요청 수행 (자동 토큰 추출)
        HttpUtil.HttpResult httpResult = HttpUtil.get(roleUrl, "application/json", null);
        
        // 응답 처리
        if (httpResult.isSuccess()) {
            // 성공적인 응답 처리
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
            
            // 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
            if (parsedResponse != null) {
                logger.debug("특정 권한 조회 응답:\n{}", JsonUtil.mapToString(parsedResponse));
                
                // 헤더의 wai_lang 값에 따라 menu_name 설정
                String language = request.getHeader("wai_lang");
                language = (language != null && !language.trim().isEmpty()) ? language.trim() : null;
                logger.debug("현재 헤더 언어: {}", language);
                
                // parsedResponse에서 menus 배열을 찾아서 언어별 menu_name 설정
                setMenuNamesByLanguage(parsedResponse, language);

                // menus를 최상위 메뉴-하위 children 구조로 재배열
                rearrangeMenusHierarchy(parsedResponse);
            } else {
                logger.warn("특정 권한 조회 응답 파싱 실패: {}", responseBody);
            }
            
            logger.debug("특정 권한 조회 성공");
            result.put("success", true);
            result.put("status", httpResult.getStatus());
            result.put("message", "messages.success.roleGetSuccess");
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
                errorMessage = "messages.error.roleGetFail";
            }
            
            result.put("message", errorMessage);
            result.put("response", parsedResponse);
            
            logger.warn("외부 인증 서버 특정 권한 조회 실패: {}", errorMessage);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private void rearrangeMenusHierarchy(Map<String, Object> response) {
        if (response == null || !response.containsKey("menus")) {
            return;
        }
        Object menusObj = response.get("menus");
        if (!(menusObj instanceof java.util.List)) {
            return;
        }

        java.util.List<Map<String, Object>> menus = (java.util.List<Map<String, Object>>) menusObj;
        if (menus.isEmpty()) {
            return;
        }

        // 루트 메뉴(최상위) 식별: parent_menu.parent_menu_id == null 이거나 menu_type == "ROOT"
        Map<String, Map<String, Object>> rootById = new HashMap<>();
        for (Map<String, Object> menu : menus) {
            Map<String, Object> parentMenu = null;
            Object parentObj = menu.get("parent_menu");
            if (parentObj instanceof Map) {
                parentMenu = (Map<String, Object>) parentObj;
            }
            Object parentId = parentMenu != null ? parentMenu.get("parent_menu_id") : null;
            String menuType = menu.get("menu_type") != null ? String.valueOf(menu.get("menu_type")) : null;
            boolean isRoot = parentId == null || "ROOT".equalsIgnoreCase(menuType);
            if (isRoot) {
                // children 초기화
                if (!menu.containsKey("children") || !(menu.get("children") instanceof java.util.List)) {
                    menu.put("children", new java.util.ArrayList<Map<String, Object>>());
                }
                String menuId = String.valueOf(menu.get("menu_id"));
                if (menuId != null) {
                    rootById.put(menuId, menu);
                }
            }
        }

        // 하위 메뉴들을 루트의 children으로 배치: parent_menu.parent_menu_id == 어떤 루트의 menu_id
        for (Map<String, Object> menu : menus) {
            Map<String, Object> parentMenu = null;
            Object parentObj = menu.get("parent_menu");
            if (parentObj instanceof Map) {
                parentMenu = (Map<String, Object>) parentObj;
            }
            String parentId = parentMenu != null && parentMenu.get("parent_menu_id") != null
                ? String.valueOf(parentMenu.get("parent_menu_id"))
                : null;
            if (parentId != null && rootById.containsKey(parentId)) {
                Map<String, Object> root = rootById.get(parentId);
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> children = (java.util.List<Map<String, Object>>) root.get("children");
                if (children == null) {
                    children = new java.util.ArrayList<>();
                    root.put("children", children);
                }
                // 자기 자신이 루트가 아닌 경우에만 추가
                String menuId = String.valueOf(menu.get("menu_id"));
                if (menuId != null && !menuId.equals(parentId)) {
                    children.add(menu);
                }
            }
        }

        // 최상위 메뉴만 남기고 정렬, 각 children도 정렬
        java.util.List<Map<String, Object>> newRootList = new java.util.ArrayList<>(rootById.values());
        newRootList.sort((a, b) -> {
            Integer ao = a.get("menu_order") instanceof Number ? ((Number) a.get("menu_order")).intValue() : Integer.MAX_VALUE;
            Integer bo = b.get("menu_order") instanceof Number ? ((Number) b.get("menu_order")).intValue() : Integer.MAX_VALUE;
            return Integer.compare(ao, bo);
        });
        for (Map<String, Object> root : newRootList) {
            Object childrenObj = root.get("children");
            if (childrenObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> children = (java.util.List<Map<String, Object>>) childrenObj;
                children.sort((a, b) -> {
                    Integer ao = a.get("menu_order") instanceof Number ? ((Number) a.get("menu_order")).intValue() : Integer.MAX_VALUE;
                    Integer bo = b.get("menu_order") instanceof Number ? ((Number) b.get("menu_order")).intValue() : Integer.MAX_VALUE;
                    return Integer.compare(ao, bo);
                });
            }
        }

        response.put("menus", newRootList);
        logger.debug("메뉴 계층 재배열 완료: 루트 {}개", newRootList.size());
    }

    /**
     * 언어에 따라 메뉴 이름을 설정하는 메서드
     * @param response 응답 데이터
     * @param language 언어 코드 (ko, en)
     */
    @SuppressWarnings("unchecked")
    private void setMenuNamesByLanguage(Map<String, Object> response, String language) {
        if (response == null || !response.containsKey("menus")) {
            return;
        }
        
        Object menusObj = response.get("menus");
        if (!(menusObj instanceof java.util.List)) {
            return;
        }
        
        java.util.List<Map<String, Object>> menus = (java.util.List<Map<String, Object>>) menusObj;
        
        for (Map<String, Object> menu : menus) {
            if (menu.containsKey("multilingual_terms")) {
                Object multilingualTermsObj = menu.get("multilingual_terms");
                if (multilingualTermsObj instanceof Map) {
                    Map<String, Object> multilingualTerms = (Map<String, Object>) multilingualTermsObj;
                    
                    String menuName = null;
                    if ("ko".equals(language) && multilingualTerms.containsKey("ko")) {
                        menuName = (String) multilingualTerms.get("ko");
                    } else if ("en".equals(language) && multilingualTerms.containsKey("en")) {
                        menuName = (String) multilingualTerms.get("en");
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
            }
        }
    }
} 