package com.wai.admin.controller;

import com.wai.admin.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/main")
public class MainController {

    @Autowired
    private MainService mainService;

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthy() {
        return ResponseEntity.ok("OK");
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> loginRequest, 
            HttpServletResponse response) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        String system_code = loginRequest.get("system_code");
        
        Map<String, Object> result = mainService.login(username, password, system_code, response);
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            // 로그인 성공 - language는 클라이언트에서 헤더로 관리
            return ResponseEntity.ok(result);
        } else {
            // status가 있으면 해당 상태 코드로 반환, 없으면 400 Bad Request
            Integer status = (Integer) result.get("status");
            if (status != null) {
                return ResponseEntity.status(status).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> logoutRequest, HttpServletResponse response) {
        String system_code = logoutRequest.get("system_code");
        Map<String, Object> result = mainService.logout(system_code, response);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
        @RequestBody Map<String, String> refreshRequest,
            @CookieValue(value = "admin_refresh", required = false) String adminRefreshToken,
            @CookieValue(value = "webView_refresh", required = false) String webViewRefreshToken,
            HttpServletResponse response) {
        String system_code = refreshRequest.get("system_code");
        String token = "";
        if(system_code.equals("WAI_WEB_ADMIN")){
            token = adminRefreshToken;
        } else {
            token = webViewRefreshToken;
        }
        
        Map<String, Object> result = mainService.refreshToken(token, system_code, response);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 토큰 검증
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletResponse response) {
        // get방식으로 system_code를 받으나 자동처리 해놓아서 사용을 안함
        Map<String, Object> result = mainService.verifyToken(response);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(401).body(result);
        }
    }


} 