package com.wai.admin.controller.code;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.code.CodeService;

@RestController
@RequestMapping("/api/code")
public class CodeController {

    @Autowired
    private CodeService codeService;

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllCodes(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = codeService.getAllCodes(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCode(
            @RequestBody Map<String, Object> codeData) {
        Map<String, Object> result = codeService.createCode(codeData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/multiCreate")
    public ResponseEntity<Map<String, Object>> multiCreateCodes(
            @RequestBody List<Map<String, Object>> codesList) {
        
        System.out.println("multiCreateCodes 실행");
        System.out.println("받은 데이터: " + codesList);

        Map<String, Object> result = codeService.multiCreateCodes(codesList);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PatchMapping("/update/{code_id}")
    public ResponseEntity<Map<String, Object>> updateCode(
            @PathVariable String code_id,
            @RequestBody Map<String, Object> codeData) {
        Map<String, Object> result = codeService.updateCode(code_id, codeData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/delete/{code_id}")
    public ResponseEntity<Map<String, Object>> deleteCode(
            @PathVariable String code_id) {
        Map<String, Object> result = codeService.deleteCode(code_id);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 