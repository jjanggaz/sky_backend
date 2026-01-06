package com.wai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wai.admin.util.JsonUtil;
import com.wai.admin.util.HttpUtil;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;


@Service
public class MainService {

	private static final Logger logger = LoggerFactory.getLogger(MainService.class);

	// 외부 인증 서버 설정
	@Value("${auth.server.base-url}")
	private String authServerBaseUrl;

	private static final Map<String, String> USERS = new HashMap<>();
	private static final Map<String, String> TOKENS = new HashMap<>(); // token -> username

	/**
	 * 로그인 처리 - 외부 인증 서버 호출
	 * @param system_code 
	 */
	public Map<String, Object> login(String username, String password, String system_code, HttpServletResponse response) {
		logger.debug("외부 인증 서버 로그인 시도: username={}, server={}", username, authServerBaseUrl);
		Map<String, Object> result = new HashMap<>();
		
		if (username == null || password == null) {
			logger.warn("로그인 실패: 사용자명 또는 비밀번호가 null");
			result.put("success", false);
			result.put("message", "placeholder.loginUsername");
			return result;
		}
		
		// 외부 인증 서버 URL 구성
		String loginUrl = authServerBaseUrl + "/api/v1/auth/login";
		logger.debug("인증 서버 URL: {}", loginUrl);
		
		// 요청 본문 구성 - Map 사용
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("username", username);
		requestMap.put("password", password);
		
		String requestBody = JsonUtil.mapToJson(requestMap);
		logger.debug("요청 본문: {}", requestBody);
		
		// HttpUtil을 사용하여 POST 요청 수행 (로그인은 Authorization 헤더 없음)
		HttpUtil.HttpResult httpResult = HttpUtil.post(loginUrl, "application/json", requestBody);
		
		// 응답 처리
		if (httpResult.isSuccess()) {
			// 성공적인 응답 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
			if (parsedResponse != null) {
				logger.debug("파싱된 응답:\n{}", JsonUtil.mapToString(parsedResponse));
			} else {
				logger.warn("응답 파싱 실패: {}", responseBody);
			}
			
			// 특정 키만 남기기
			if (parsedResponse != null) {
				Map<String, Object> filteredResponse = new HashMap<>();
				
				// 메뉴 권한 확인
				boolean hasMenuPermissions = false;
				if (parsedResponse.containsKey("accessible_menus")) { //user_menu_permissions에서 accessible_menus로 변경
					Object accessibleMenus = parsedResponse.get("accessible_menus");
					// accessible_menus는 항상 배열로 들어오므로 배열이 비어있지 않으면 권한이 있다고 판단
					hasMenuPermissions = accessibleMenus instanceof java.util.List && !((java.util.List<?>) accessibleMenus).isEmpty();
					
					if (hasMenuPermissions) {
						filteredResponse.put("menus", accessibleMenus);
					}
				}
				
				// 메뉴 권한이 없으면 접근 권한 없음 처리
				if (!hasMenuPermissions) {
					result.put("success", false);
					result.put("status", 403); // Forbidden
					result.put("message", "messages.error.noAccessPermission");
					result.put("response", filteredResponse);
					logger.warn("로그인 실패: 메뉴 접근 권한이 없습니다. username={}", username);
					return result;
				}

                // 원하는 키만 추가
				if (parsedResponse.containsKey("user_info")) {
					Object userInfoObj = parsedResponse.get("user_info");
					filteredResponse.put("user_info", userInfoObj);
					// user_id는 세션에 저장하지 않고 result에만 저장
					if (userInfoObj instanceof java.util.Map) {
						Object userId = ((java.util.Map<?, ?>) userInfoObj).get("user_id");
						if (userId != null) {
							result.put("user_id", String.valueOf(userId));
							logger.debug("user_id 저장: {}", userId);
						}
					}
				}
				
				// 메뉴 권한이 있으면 성공으로 설정
				result.put("success", true);
				result.put("status", httpResult.getStatus());
				result.put("message", "messages.success.loginSuccess");
				result.put("response", filteredResponse);
			} else {
				// parsedResponse가 null인 경우도 메뉴 권한이 없는 것으로 처리
				result.put("success", false);
				result.put("status", 403); // Forbidden
				result.put("message", "messages.error.noAccessPermission");
				result.put("response", new HashMap<>());
				logger.warn("로그인 실패: 응답 파싱 실패로 인한 메뉴 접근 권한 없음. username={}", username);
				return result;
			}
			
			// 토큰 추출 및 쿠키 설정
			if (parsedResponse != null) {
				// Access Token 쿠키 설정
				String accessToken = (String) parsedResponse.get("access_token");
				if (accessToken != null && !accessToken.isEmpty()) {
					Object accessExpiresIn = parsedResponse.get("access_token_expires_in");
					int accessMaxAge = 18000; // 기본값: 5시간
					
					if (accessExpiresIn != null) {
						if (accessExpiresIn instanceof Number) {
							accessMaxAge = ((Number) accessExpiresIn).intValue();
						} else if (accessExpiresIn instanceof String) {
							try {
								accessMaxAge = Integer.parseInt((String) accessExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("access_token_expires_in 값을 정수로 변환할 수 없습니다: {}", accessExpiresIn);
							}
						}
					}
					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_access", accessToken, accessMaxAge);
					} else {
						setCookie(response, "webView_access", accessToken, accessMaxAge);
					}
					
				}
				
				// Refresh Token 쿠키 설정
				String refreshToken = (String) parsedResponse.get("refresh_token");
				if (refreshToken != null && !refreshToken.isEmpty()) {
					Object refreshExpiresIn = parsedResponse.get("refresh_token_expires_in");
					int refreshMaxAge = 604800; // 기본값: 7일
					
					if (refreshExpiresIn != null) {
						if (refreshExpiresIn instanceof Number) {
							refreshMaxAge = ((Number) refreshExpiresIn).intValue();
						} else if (refreshExpiresIn instanceof String) {
							try {
								refreshMaxAge = Integer.parseInt((String) refreshExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("refresh_token_expires_in 값을 정수로 변환할 수 없습니다: {}", refreshExpiresIn);
							}
						}
					}
					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_refresh", refreshToken, refreshMaxAge);
					} else {
						setCookie(response, "webView_refresh", refreshToken, refreshMaxAge);
					}
				}
				
				// Session Token 쿠키 설정
				String sessionToken = (String) parsedResponse.get("session_id");
				if (sessionToken != null && !sessionToken.isEmpty()) {
					Object sessionExpiresIn = parsedResponse.get("session_expires_in");
					int sessionMaxAge = 18000; // 기본값: 5시간
					
					if (sessionExpiresIn != null) {
						if (sessionExpiresIn instanceof Number) {
							sessionMaxAge = ((Number) sessionExpiresIn).intValue();
						} else if (sessionExpiresIn instanceof String) {
							try {
								sessionMaxAge = Integer.parseInt((String) sessionExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("session_expires_in 값을 정수로 변환할 수 없습니다: {}", sessionExpiresIn);
							}
						}
					}
					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_session", sessionToken, sessionMaxAge);
					} else {
						setCookie(response, "webView_session", sessionToken, sessionMaxAge);
					}
				}
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
				errorMessage = "messages.error.loginFail";
			}
			
			result.put("message", errorMessage);
			result.put("response", parsedResponse);
		}
		
		return result;
	}

	/**
	 * 로그아웃 처리 - 외부 인증 서버 호출
	 * @param system_code 
	 */
	public Map<String, Object> logout(String system_code, HttpServletResponse response) {
		logger.debug("외부 인증 서버 로그아웃 시도: server={}", authServerBaseUrl);
		Map<String, Object> result = new HashMap<>();
		
		// 외부 인증 서버 URL 구성
		String logoutUrl = authServerBaseUrl + "/api/v1/auth/logout";
		logger.debug("로그아웃 서버 URL: {}", logoutUrl);
		
		// HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 주입)
		HttpUtil.HttpResult httpResult = HttpUtil.post(logoutUrl, "application/json", "");
		
		// 응답 처리
		if (httpResult.isSuccess()) {
			// 성공적인 로그아웃 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
			if (parsedResponse != null) {
				logger.debug("로그아웃 응답:\n{}", JsonUtil.mapToString(parsedResponse));
			} else {
				logger.warn("로그아웃 응답 파싱 실패: {}", responseBody);
			}
			
			result.put("success", true);
			result.put("status", httpResult.getStatus());
			result.put("message", "messages.success.logoutSuccess");
			result.put("response", parsedResponse);
			
			logger.debug("외부 인증 서버 로그아웃 성공");
		} else {
			// 에러 응답 처리
			result.put("success", false);
			result.put("status", httpResult.getStatus());
			
			// 에러 메시지 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// HTTP 에러 응답에서 message 또는 detail 메시지 추출
			String errorMessage = JsonUtil.extractValue(responseBody, "message");
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = JsonUtil.extractValue(responseBody, "detail");
			}
			
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = "messages.error.logoutFail";
			}
			
			result.put("message", errorMessage);
			result.put("response", parsedResponse);
			
			logger.warn("외부 인증 서버 로그아웃 실패: {}", errorMessage);
		}
		
		// 토큰 쿠키 완전 삭제 (성공/실패 관계없이)
		if(system_code.equals("WAI_WEB_ADMIN")){
			deleteCookie(response, "admin_access");
			deleteCookie(response, "admin_refresh");
			deleteCookie(response, "admin_session");
		} else {
			deleteCookie(response, "webView_access");
			deleteCookie(response, "webView_refresh");
			deleteCookie(response, "webView_session");
		}
		logger.debug("로그아웃 처리 완료 - 토큰 쿠키 삭제됨");
		
		return result;
	}

	/**
	 * 쿠키 설정 유틸리티 메서드
	 */
	private void setCookie(HttpServletResponse response, String cookieName, String cookieValue, int maxAge) {
		Cookie cookie = new Cookie(cookieName, cookieValue);
		cookie.setHttpOnly(true);
		// Secure;가 설정돼있으면 Http에서 사용불가하여 false로 설정
		cookie.setSecure(false); // HTTPS에서만 전송
		cookie.setPath("/"); // 모든 경로에서 접근 가능
		cookie.setMaxAge(maxAge);
		
		// SameSite=Lax 설정
		String sameSiteAttribute = "SameSite=Lax";
		// Secure;가 설정돼있으면 Http에서 사용불가하여 제거
		String cookieHeader = String.format("%s=%s; Path=%s; HttpOnly; Max-Age=%d; %s", 
			cookieName, cookieValue, "/", maxAge, sameSiteAttribute);
		response.addHeader("Set-Cookie", cookieHeader);
		
		logger.debug("쿠키 설정 완료: {} (만료시간: {}초, SameSite=Lax)", cookieName, maxAge);
	}

	/**
	 * 쿠키 완전 삭제 유틸리티 메서드
	 */
	private void deleteCookie(HttpServletResponse response, String cookieName) {
		Cookie cookie = new Cookie(cookieName, "");
		cookie.setHttpOnly(false); // 클라이언트에서 삭제가 가능하도록 False 처리
		// Secure;가 설정돼있으면 Http에서 사용불가하여 false로 설정
		cookie.setSecure(false);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		cookie.setValue("");
		
		// SameSite=Lax 설정으로 쿠키 삭제
		String sameSiteAttribute = "SameSite=Lax";
		// Secure;가 설정돼있으면 Http에서 사용불가하여 제거
		String cookieHeader = String.format("%s=; Path=%s; HttpOnly; Max-Age=0; %s", 
			cookieName, "/", sameSiteAttribute);
		response.addHeader("Set-Cookie", cookieHeader);
		
		logger.debug("쿠키 삭제 완료: {} (SameSite=Lax)", cookieName);
	}

	/**
	 * 토큰 갱신 처리 - 외부 인증 서버 호출
	 * @param system_code 
	 */
	public Map<String, Object> refreshToken(String token, String system_code, HttpServletResponse response) {
		logger.debug("외부 인증 서버 토큰 갱신 시도: server={}", authServerBaseUrl);
		Map<String, Object> result = new HashMap<>();
		
		if (token == null || token.isEmpty()) {
			logger.warn("토큰 갱신 실패: 토큰이 null이거나 비어있음");
			result.put("success", false);
			result.put("message", "messages.error.invalidToken");
			return result;
		}
		
		// 외부 인증 서버 URL 구성
		String refreshUrl = authServerBaseUrl + "/api/v1/auth/refresh";
		logger.debug("토큰 갱신 서버 URL: {}", refreshUrl);
		
		// 요청 본문 구성 - refresh token을 포함
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("refresh_token", token); // 갱신할때는 refresh_token으로 보내야함
		String requestBody = JsonUtil.mapToJson(requestMap);
		logger.debug("토큰 갱신 요청 본문: {}", requestBody);
		
		// HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 추출)
		// 이제 wai_refresh 토큰도 자동으로 헤더에 포함됨
		HttpUtil.HttpResult httpResult = HttpUtil.post(refreshUrl, "application/json", requestBody);
		
		// 응답 처리
		if (httpResult.isSuccess()) {
			// 성공적인 토큰 갱신 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
			if (parsedResponse != null) {
				logger.debug("토큰 갱신 응답:\n{}", JsonUtil.mapToString(parsedResponse));
			} else {
				logger.warn("토큰 갱신 응답 파싱 실패: {}", responseBody);
			}
			
			result.put("success", true);
			result.put("status", httpResult.getStatus());
			result.put("message", "messages.success.tokenRefreshSuccess");
			
			// 새로운 토큰 추출 및 쿠키 설정
			if (parsedResponse != null) {
				// 새로운 Access Token 쿠키 설정
				String newAccessToken = (String) parsedResponse.get("access_token");
				if (newAccessToken != null && !newAccessToken.isEmpty()) {
					Object accessExpiresIn = parsedResponse.get("access_token_expires_in");
					int accessMaxAge = 18000; // 기본값: 5시간
					
					if (accessExpiresIn != null) {
						if (accessExpiresIn instanceof Number) {
							accessMaxAge = ((Number) accessExpiresIn).intValue();
						} else if (accessExpiresIn instanceof String) {
							try {
								accessMaxAge = Integer.parseInt((String) accessExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("access_token_expires_in 값을 정수로 변환할 수 없습니다: {}", accessExpiresIn);
							}
						}
					}

					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_access", newAccessToken, accessMaxAge);
					} else {
						setCookie(response, "webView_access", newAccessToken, accessMaxAge);
					}
					logger.debug("새로운 Access Token 쿠키 설정 완료");
				}
				
				// 새로운 Refresh Token 쿠키 설정
				String newRefreshToken = (String) parsedResponse.get("refresh_token");
				if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
					Object refreshExpiresIn = parsedResponse.get("refresh_token_expires_in");
					int refreshMaxAge = 604800; // 기본값: 7일
					
					if (refreshExpiresIn != null) {
						if (refreshExpiresIn instanceof Number) {
							refreshMaxAge = ((Number) refreshExpiresIn).intValue();
						} else if (refreshExpiresIn instanceof String) {
							try {
								refreshMaxAge = Integer.parseInt((String) refreshExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("refresh_token_expires_in 값을 정수로 변환할 수 없습니다: {}", refreshExpiresIn);
							}
						}
					}
					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_refresh", newRefreshToken, refreshMaxAge);
					} else {
						setCookie(response, "webView_refresh", newRefreshToken, refreshMaxAge);
					}
					logger.debug("새로운 Refresh Token 쿠키 설정 완료 (만료시간: {}초)", refreshMaxAge);
				}
				
				// 새로운 Session Token 쿠키 설정
				String sessionId = (String) parsedResponse.get("session_id");
				if (sessionId != null && !sessionId.isEmpty()) {
					Object sessionExpiresIn = parsedResponse.get("session_expires_in");
					int sessionMaxAge = 18000; // 기본값: 5시간
					
					if (sessionExpiresIn != null) {
						if (sessionExpiresIn instanceof Number) {
							sessionMaxAge = ((Number) sessionExpiresIn).intValue();
						} else if (sessionExpiresIn instanceof String) {
							try {
								sessionMaxAge = Integer.parseInt((String) sessionExpiresIn);
							} catch (NumberFormatException e) {
								logger.warn("session_expires_in 값을 정수로 변환할 수 없습니다: {}", sessionExpiresIn);
							}
						}
					}
					if(system_code.equals("WAI_WEB_ADMIN")){
						setCookie(response, "admin_session", sessionId, sessionMaxAge);
					} else {
						setCookie(response, "webView_session", sessionId, sessionMaxAge);
					}
					logger.debug("새로운 Session Token 쿠키 설정 완료 (session_id: {}, 만료시간: {}초)", sessionId, sessionMaxAge);
				}
				
				result.put("response", "success"); //값이 있으나 반환 안할거임
			} else {
				result.put("response", parsedResponse);
			}
			
			logger.debug("외부 인증 서버 토큰 갱신 성공");
		} else {
			// 에러 응답 처리
			result.put("success", false);
			result.put("status", httpResult.getStatus());
			
			// 에러 메시지 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// HTTP 에러 응답에서 message 또는 detail 메시지 추출
			String errorMessage = JsonUtil.extractValue(responseBody, "message");
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = JsonUtil.extractValue(responseBody, "detail");
			}
			
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = "messages.error.tokenRefreshFail";
			}
			
			result.put("message", errorMessage);
			result.put("response", parsedResponse);
			
			logger.warn("외부 인증 서버 토큰 갱신 실패: {}", errorMessage);
		}
		
		return result;
	}

	/**
	 * 토큰 검증 처리 - 외부 인증 서버 호출
	 */
	public Map<String, Object> verifyToken(HttpServletResponse response) {
		logger.debug("외부 인증 서버 토큰 검증 시도: server={}", authServerBaseUrl);
		Map<String, Object> result = new HashMap<>();
		
		// 외부 인증 서버 URL 구성
		String verifyUrl = authServerBaseUrl + "/api/v1/auth/verify";
		logger.debug("토큰 검증 서버 URL: {}", verifyUrl);
		
		// HttpUtil을 사용하여 POST 요청 수행 (자동 토큰 주입)
		HttpUtil.HttpResult httpResult = HttpUtil.get(verifyUrl, "application/json", "");
		
		// 응답 처리
		if (httpResult.isSuccess()) {
			// 성공적인 토큰 검증 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// 디버깅용: 파싱된 응답을 보기 좋게 로그 출력
			if (parsedResponse != null) {
				logger.debug("토큰 검증 응답:\n{}", JsonUtil.mapToString(parsedResponse));
			} else {
				logger.warn("토큰 검증 응답 파싱 실패: {}", responseBody);
			}
			
			result.put("success", true);
			result.put("status", httpResult.getStatus());
			result.put("message", "messages.success.tokenVerifySuccess");
			
			// valid 값만 반환
			if (parsedResponse != null && parsedResponse.containsKey("valid")) {
				Map<String, Object> filteredResponse = new HashMap<>();
				filteredResponse.put("valid", parsedResponse.get("valid"));
				result.put("response", filteredResponse);
			} else {
				result.put("response", parsedResponse);
			}
			
			logger.debug("외부 인증 서버 토큰 검증 성공");
		} else {
			// 에러 응답 처리
			result.put("success", false);
			result.put("status", httpResult.getStatus());
			
			// 에러 메시지 처리
			String responseBody = httpResult.getBody();
			Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);
			
			// HTTP 에러 응답에서 message 또는 detail 메시지 추출
			String errorMessage = JsonUtil.extractValue(responseBody, "message");
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = JsonUtil.extractValue(responseBody, "detail");
			}
			
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = "messages.error.tokenVerifyFail";
			}
			
			result.put("message", errorMessage);
			result.put("response", parsedResponse);
			
			logger.warn("외부 인증 서버 토큰 검증 실패: {}", errorMessage);
		}
		
		return result;
	}

} 