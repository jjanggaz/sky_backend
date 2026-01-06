package com.wai.admin.controller.machine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wai.admin.service.machine.StructureService;
import com.wai.admin.util.JsonUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/structure")
public class StructureController {

    @Autowired
    private StructureService structureService;

    @PostMapping("/common/code")
    public ResponseEntity<Map<String, Object>> getCommonCodes(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = structureService.getCommonCodes(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/depth")
    public ResponseEntity<Map<String, Object>> getDepth(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = structureService.getDepth(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/common/code/{search_value}")
    public ResponseEntity<Map<String, Object>> getCommonDepthCode(@PathVariable String search_value) {
        Map<String, Object> result;

        result = structureService.getCommonDepthCode(search_value);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> getSearchList(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = structureService.getStructureList(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> createStructure(
            @RequestParam(value = "createParams") String createParamsJson,
            @RequestParam(value = "all_file", required = false) MultipartFile allFile) {
        Map<String, Object> result;

        // JSON 문자열을 Map으로 파싱
        Map<String, Object> createParams = JsonUtil.parseJson(createParamsJson);
        String structureType = (String) createParams.getOrDefault("structure_type_detail", "");
        String rootStructureType = (String) createParams.getOrDefault("root_structure_type", "");

        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("root_structure_type", rootStructureType);
        searchParams.put("structure_type", structureType);

        String remarks = (String) createParams.getOrDefault("remarks", "");

        Map<String, Object> structureList = structureService.getStructureList(searchParams);

        Map<String, Object> response = (Map<String, Object>) structureList.get("response");
        List<?> items = null;
        if (response != null) {
            items = (List<?>) response.get("items");
        }
        if (items != null && !items.isEmpty()) {
            structureList.put("success", false);
            structureList.put("status", 400);
            structureList.put("message", "messages.error.structureTypeAlreadyExists");
            return ResponseEntity.badRequest().body(structureList);
        }

        // 파일 업로드 호출
        result = structureService.uploadStructureFiles(structureType, allFile);
        
        // result에서 structure_id 추출
        String structureId = "";
        if ((Boolean) result.get("success") && result.get("data") != null) {
            Map<String, Object> outerData = (Map<String, Object>) result.get("data");
            Map<String, Object> innerData = (Map<String, Object>) outerData.get("data");
            if (innerData != null) {
                structureId = (String) innerData.get("structure_id");
            }
        }
        
        if(remarks != null && !remarks.isEmpty()){
            structureService.updateStructureRemark(structureId, remarks);
        }
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateStructure(
            @PathVariable String id,
            @RequestParam(value = "updateParams") String updateParamsJson,
            @RequestParam(value = "formula_file", required = false) MultipartFile formulaFile,
            @RequestParam(value = "dtd_model_file", required = false) MultipartFile dtdModelFile,
            @RequestParam(value = "thumbnail_file", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "revit_model_file", required = false) MultipartFile revitModelFile) {
        Map<String, Object> result;

        // JSON 문자열을 Map으로 파싱
        Map<String, Object> updateParams = JsonUtil.parseJson(updateParamsJson);
        
        // 구조체 업데이트 호출 (파일들과 함께)
        result = structureService.updateStructure(id, updateParams, formulaFile, dtdModelFile, thumbnailFile, revitModelFile);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteStructure(@PathVariable String id, @RequestBody Map<String, Object> deleteParams) {
        Map<String, Object> result;
        
        result = structureService.deleteStructure(id, deleteParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/search/formula")
    public ResponseEntity<Map<String, Object>> searchStructureFormula(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = structureService.searchStructureFormula(searchParams);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/delete/formula")
    public ResponseEntity<Map<String, Object>> deleteStructureFormula(@RequestBody Map<String, Object> deleteParams) {
        Map<String, Object> result;

        // deleteParams에서 structure_id와 formula_id 추출
        String structureId = (String) deleteParams.get("structure_id");
        String formulaId = (String) deleteParams.get("formula_id");
        
        result = structureService.deleteStructureFormula(structureId, formulaId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 