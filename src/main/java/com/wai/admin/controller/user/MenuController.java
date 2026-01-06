package com.wai.admin.controller.user;

import com.wai.admin.service.user.MenuService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 메뉴 조회
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> searchMenus(
            @RequestBody Map<String, Object> searchParams,
            HttpServletRequest request) {
        Map<String, Object> result = menuService.searchMenus(searchParams, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 메뉴 수정
     */
    @PatchMapping("/update/{menu_id}")
    public ResponseEntity<Map<String, Object>> updateMenu(
            @PathVariable String menu_id,
            @RequestBody Map<String, Object> menuData) {
        Map<String, Object> result = menuService.updateMenu(menu_id, menuData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 