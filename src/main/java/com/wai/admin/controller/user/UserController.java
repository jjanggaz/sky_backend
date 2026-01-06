package com.wai.admin.controller.user;

import com.wai.admin.service.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 사용자 목록 조회
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = userService.getAllUsers(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 사용자 등록
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, Object> userData) {
        Map<String, Object> result = userService.createUser(userData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 사용자 수정
     */
    @PatchMapping("/update/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> userData) {
        Map<String, Object> result = userService.updateUser(userId, userData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 사용자 삭제
     */
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable String userId) {
        Map<String, Object> result = userService.deleteUser(userId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 사용자 아이디 중복체크
     */
    @GetMapping("/check/{userName}")
    public ResponseEntity<Map<String, Object>> checkUserNameExists(
            @PathVariable String userName) {
        Map<String, Object> result = userService.checkUserNameExists(userName);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 공통코드 조회 (역할 목록)
     */
    @PostMapping("/common/roles")
    public ResponseEntity<Map<String, Object>> getCommonRoles() {
        Map<String, Object> result = userService.getCommonRoles();
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 