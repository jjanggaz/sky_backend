package com.wai.admin.controller.machine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wai.admin.service.machine.MachineService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/machine")
public class MachineController {

    @Autowired
    private MachineService machineService;

    /**
     * 공통 코드 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/common/code")
    public ResponseEntity<Map<String, Object>> getCommonCodes(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = machineService.getCommonCodes(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 수정할때 각 depth별로 본인부터 상위 level 끝까지 조회
     * 
     * @param search_value
     * @return
     */
    @GetMapping("/common/code/{search_value}")
    public ResponseEntity<Map<String, Object>> getCommonDepthCode(@PathVariable String search_value) {
        Map<String, Object> result;

        result = machineService.getCommonDepthCode(search_value);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 등록화면 depth별 코드 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/depth")
    public ResponseEntity<Map<String, Object>> getDepth(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = machineService.getDepth(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 상세검색 콤보박스 세팅용
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/depth/detail")
    public ResponseEntity<Map<String, Object>> getDepthDetail(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = machineService.getDepthDetail(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 상세검색 콤보박스 Onchange 시 호출 하위 inputBox 세팅용 show/hide 하기위해서
     * 
     * @param searchParams
     * @return
     */
    @GetMapping("/depth/detail/searchType/{search_key}")
    public ResponseEntity<Map<String, Object>> getDepthDetailSearchType(@PathVariable String search_key) {
        Map<String, Object> result = machineService.getDepthDetailSearchType(search_key);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 기계 목록 조회
     * 
     * @param searchParams
     * @param waiLang 언어 헤더
     * @return
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> getSearchList(
            @RequestBody Map<String, Object> searchParams,
            @RequestHeader(value = "wai_lang", required = false) String waiLang) {
        Map<String, Object> result;

        result = machineService.getMachineList(searchParams, waiLang);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 기계 등록 (엑셀파일 업로드)
     * 
     * @param machine_name 기계 타입
     * @param excelFile 엑셀 파일
     * @return
     */
    @PostMapping(value = "/uploadModelExcel/{machine_name}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadModelExcel(
            @PathVariable String machine_name,
            @RequestParam(value = "excel_file", required = false) MultipartFile excelFile) {
        Map<String, Object> result = machineService.uploadModelExcel(machine_name, excelFile);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 엑셀 템플릿 파일 다운로드
     * 
     * @param machine_name 기계 타입
     * @return
     */
    @GetMapping(value = "/tempExcel/{machine_name}")
    public ResponseEntity<Map<String, Object>> tempExcel(
            @PathVariable String machine_name) {
        Map<String, Object> result = machineService.downloadTemplateExcel(machine_name);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 모델 등록 (Zip 파일 업로드)
     * 
     * @param equipmentType 장비 타입
     * @param allFile Zip 파일
     * @return
     */
    @PostMapping(value = "/uploadModelZip/{machine_name}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadModelZip(
            @PathVariable String machine_name,
            @RequestParam(value = "all_file", required = false) MultipartFile allFile) {
        Map<String, Object> result = machineService.uploadModelZip(machine_name, allFile);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 기계 정보 업데이트
     * 
     * @param id 기계 ID
     * @param updateParamsJson 업데이트할 정보 (JSON 문자열)
     * @param formulaFile Formula 파일 (Model로 업로드)
     * @param dtdModelFile DTD Model 파일 (Symbol로 업로드)
     * @param thumbnailFile Thumbnail 파일
     * @param revitModelFile Revit Model 파일 (RVT로 업로드)
     * @return
     */
    @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateMachine(
            @PathVariable String id,
            @RequestParam(value = "updateParams") String updateParamsJson,
            @RequestParam(value = "dtd_model_file", required = false) MultipartFile dtdModelFile,
            @RequestParam(value = "thumbnail_file", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "revit_model_file", required = false) MultipartFile revitModelFile,
            @RequestParam(value = "symbol_file", required = false) MultipartFile symbolFile) {

        Map<String, Object> result = machineService.updateMachine(id, updateParamsJson, dtdModelFile, thumbnailFile, revitModelFile, symbolFile);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 기계 삭제
     * 
     * @param id 기계 ID
     * @param deleteParams 삭제할 파일 ID 정보
     * @return
     */
    @PostMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteMachine(
        @PathVariable String id,
        @RequestBody Map<String, Object> deleteParams) {
        Map<String, Object> result = machineService.deleteMachine(id, deleteParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 기계 상세, 파일 정보 조회
     * 
     * @param id 기계 ID
     * @param fileParams 다운로드할 파일 ID 정보
     * @return
     */
    @PostMapping("/detail/files/{equipment_id}")
    public ResponseEntity<Map<String, Object>> getDetailFiles(
        @PathVariable String equipment_id,
        @RequestBody Map<String, Object> fileParams) {
        Map<String, Object> result = machineService.getDetailFiles(equipment_id, fileParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 기계 상세 업체 정보 조회
     * 
     * @param equipment_type 장비 타입
     * @return
     */
    @PostMapping("/detail/common/{equipment_type}")
    public ResponseEntity<Map<String, Object>> getDetailCommon(
        @PathVariable String equipment_type) {
        Map<String, Object> result = machineService.getDetailCommon(equipment_type);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * Formula 검색
     * 
     * @param searchParams 검색 파라미터 (equipment_type 포함)
     * @return
     */
    @PostMapping("/search/formula")
    public ResponseEntity<Map<String, Object>> searchMachineFormula(@RequestBody Map<String, Object> searchParams) {
        String equipmentType = (String) searchParams.get("equipment_type");
        
        if (equipmentType == null || equipmentType.trim().isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "equipment_type 파라미터가 필요합니다.");
            return ResponseEntity.badRequest().body(errorResult);
        }
        
        Map<String, Object> result = machineService.getFormulaByEquipmentType(equipmentType);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * Formula 파일 생성
     * 
     * @param pythonFile Python 파일
     * @param formulaId Formula ID (선택)
     * @return
     */
    @PostMapping(value = "/create/formula", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> createMachineFormula(
            @RequestParam(value = "python_file") MultipartFile pythonFile,
            @RequestParam(value = "formula_id", required = false) String formulaId,
            @RequestParam(value = "equipment_type", required = false) String equipmentType) {
        Map<String, Object> result = machineService.createMachineFormula(pythonFile, formulaId, equipmentType);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * Formula 삭제
     * 
     * @param id Formula ID
     * @return
     */
    @DeleteMapping("/delete/formula/{id}")
    public ResponseEntity<Map<String, Object>> deleteMachineFormula(@PathVariable String id) {
        Map<String, Object> result = machineService.deleteMachineFormula(id);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 장비 가격 이력 등록
     * 
     * @param priceParams 가격 정보 파라미터
     * @return
     */
    @PostMapping("/price/history")
    public ResponseEntity<Map<String, Object>> createEquipmentPriceHistory(@RequestBody Map<String, Object> priceParams) {
        Map<String, Object> result = machineService.createEquipmentPriceHistory(priceParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 카탈로그 목록 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/catalog/search")
    public ResponseEntity<Map<String, Object>> getCatalogList(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = machineService.getCatalogList(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 카탈로그 컬럼 목록 조회 - 상세검색 필드 조회용
     * 
     * @param search_key
     * @return
     */
    @GetMapping("/catalog/column/{search_key}")
    public ResponseEntity<Map<String, Object>> getCatalogColumnList(@PathVariable String search_key) {
        Map<String, Object> result = machineService.getCatalogColumnList(search_key);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

}