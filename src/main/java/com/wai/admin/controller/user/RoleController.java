package com.wai.admin.controller.user;

import com.wai.admin.service.user.RoleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 권한 조회
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> searchRoles(
            @RequestBody Map<String, Object> searchParams,
            HttpServletRequest request) {
        Map<String, Object> result = roleService.searchRoles(searchParams, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 권한 등록
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRole(
            @RequestBody Map<String, Object> roleData) {
        Map<String, Object> result = roleService.createRole(roleData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 권한 수정
     */
    @PatchMapping("/update/{role_id}")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable String role_id,
            @RequestBody Map<String, Object> roleData) {
        Map<String, Object> result = roleService.updateRole(role_id, roleData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 권한 삭제
     */
    @DeleteMapping("/delete/{role_id}")
    public ResponseEntity<Map<String, Object>> deleteRole(
            @PathVariable String role_id) {
        Map<String, Object> result = roleService.deleteRole(role_id);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 특정 권한 조회(화면 테이블 보기버튼 누르면 호출)
     */
    @GetMapping("/{role_id}")
    public ResponseEntity<Map<String, Object>> getRoleById(
            @PathVariable String role_id,
            HttpServletRequest request) {
        Map<String, Object> result = roleService.getRoleById(role_id, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 