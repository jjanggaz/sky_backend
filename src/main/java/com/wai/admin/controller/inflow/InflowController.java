package com.wai.admin.controller.inflow;

import com.wai.admin.service.inflow.InflowService;
import com.wai.admin.service.user.UserService;
import com.wai.admin.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inflow")
public class InflowController {

    @Autowired
    private UserService userService;

    @Autowired
    private InflowService inflowService;

    /**
     * 공통코드 조회
     */
    @GetMapping("/common/codes")
    public ResponseEntity<Map<String, Object>> getCommonCodes(
            @RequestParam(required = false) String code_group,
            @RequestParam(required = false) String parent_key) {
        Map<String, Object> result = inflowService.getCommonCodes(code_group, parent_key);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 유입종류별 파라미터 목록 조회
     */
    @PostMapping("/parameters")
    public ResponseEntity<Map<String, Object>> getWaterFlowTypeParameters(
            @RequestBody Map<String, Object> searchParams) {
          
        Map<String, Object> result = inflowService.getWaterFlowTypeParameters(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 유입종류 목록 조회
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllWaterFlowTypes(
            @RequestBody Map<String, Object> searchParams,
            HttpServletRequest request) {
        Map<String, Object> result = inflowService.getAllWaterFlowTypes(searchParams, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 유입종류 등록
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createWaterFlowType(
            @RequestParam("waterFlowTypeData") String waterFlowTypeDataJson,
            @RequestParam(value = "symbolFile", required = false) MultipartFile symbolFile,
            @RequestParam(value = "metricFile", required = false) MultipartFile metricFile,
            @RequestParam(value = "uscsFile", required = false) MultipartFile uscsFile) {
        
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> waterFlowTypeData = JsonUtil.parseJson(waterFlowTypeDataJson);
            
            // 파일이 있으면 waterFlowTypeData에 추가
            if (symbolFile != null && !symbolFile.isEmpty()) {
                waterFlowTypeData.put("symbolFile", symbolFile);
            }
            if (metricFile != null && !metricFile.isEmpty()) {
                waterFlowTypeData.put("metricFile", metricFile);
            }
            if (uscsFile != null && !uscsFile.isEmpty()) {
                waterFlowTypeData.put("uscsFile", uscsFile);
            }
            
            Map<String, Object> result = inflowService.createWaterFlowType(waterFlowTypeData);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "messages.error.waterFlowTypeCreateError");
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 수질 파라미터 목록 조회
     */
    @PostMapping("/listLookup")
    public ResponseEntity<Map<String, Object>> getWaterQualityParameters(@RequestBody Map<String, Object> parameterData) {
        Map<String, Object> result = inflowService.getWaterQualityParameters(parameterData);
        
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

    /**
     * 유입종류 수정
     */
    @PostMapping("/update/{flowTypeId}")
    public ResponseEntity<Map<String, Object>> updateWaterFlowType(
            @PathVariable String flowTypeId,
            @RequestParam("waterFlowTypeData") String waterFlowTypeDataJson,
            @RequestParam(value = "symbolFile", required = false) MultipartFile symbolFile,
            @RequestParam(value = "metricFile", required = false) MultipartFile metricFile,
            @RequestParam(value = "uscsFile", required = false) MultipartFile uscsFile) {
        
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> waterFlowTypeData = JsonUtil.parseJson(waterFlowTypeDataJson);
            
            // 파일이 있으면 waterFlowTypeData에 추가
            if (symbolFile != null && !symbolFile.isEmpty()) {
                waterFlowTypeData.put("symbolFile", symbolFile);
            }
            if (metricFile != null && !metricFile.isEmpty()) {
                waterFlowTypeData.put("metricFile", metricFile);
            }
            if (uscsFile != null && !uscsFile.isEmpty()) {
                waterFlowTypeData.put("uscsFile", uscsFile);
            }
            
            Map<String, Object> result = inflowService.updateWaterFlowType(flowTypeId, waterFlowTypeData);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "messages.error.waterFlowTypeUpdateError");
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 유입종류 삭제
     */
    @PostMapping("/delete/{flowTypeId}")
    public ResponseEntity<Map<String, Object>> deleteWaterFlowType(@PathVariable String flowTypeId, @RequestBody Map<String, Object> parameterData) {
        Map<String, Object> result = inflowService.deleteWaterFlowType(flowTypeId, parameterData);

        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 심볼 파일 정보 조회
     */
    @GetMapping("/fileInfo/{symbolId}")
    public ResponseEntity<Map<String, Object>> getSymbolFileInfo(@PathVariable String symbolId) {
        Map<String, Object> result = inflowService.getSymbolFileInfo(symbolId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 계산식 추출
     */
    @PostMapping("/formula/extract")
    public ResponseEntity<Map<String, Object>> extractFormula(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = inflowService.extractFormula(file);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "messages.error.formulaExtractError");
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 수질 파라미터 등록
     */
    @PostMapping("/codeInsert")
    public ResponseEntity<Map<String, Object>> insertWaterQualityParameter(@RequestBody Map<String, Object> parameterData) {
        Map<String, Object> result = inflowService.insertWaterQualityParameter(parameterData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 수질 파라미터 수정
     */
    @PostMapping("/codeUpdate/{parameterId}")
    public ResponseEntity<Map<String, Object>> updateWaterQualityParameter(
            @PathVariable String parameterId,
            @RequestBody Map<String, Object> parameterData) {
        Map<String, Object> result = inflowService.updateWaterQualityParameter(parameterId, parameterData);
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(result);
        } else {
            Integer status = (Integer) result.get("status");
            if (status != null) {
                return ResponseEntity.status(status).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
    }

    /**
     * 수질 파라미터 삭제
     */
    @DeleteMapping("/codeDelete/{parameterId}")
    public ResponseEntity<Map<String, Object>> deleteWaterQualityParameter(@PathVariable String parameterId) {
        Map<String, Object> result = inflowService.deleteWaterQualityParameter(parameterId);
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(result);
        } else {
            Integer status = (Integer) result.get("status");
            if (status != null) {
                return ResponseEntity.status(status).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
    }

    /**
     * MinIO에서 계산식 파일 삭제
     */
    @PostMapping("/formula/{formulaId}")
    public ResponseEntity<Map<String, Object>> deleteMinIOFormula(
            @PathVariable String formulaId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        Map<String, Object> result = inflowService.deleteMinIOFormula(formulaId, requestBody);
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(result);
        } else {
            Integer status = (Integer) result.get("status");
            if (status != null) {
                return ResponseEntity.status(status).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
    }

    /**
     * MinIO에서 심볼 파일 삭제 및 재생성
     */
    @PostMapping("/symbols/{symbolId}")
    public ResponseEntity<Map<String, Object>> deleteSymbol(
            @PathVariable String symbolId,
            @RequestBody Map<String, Object> waterFlowTypeData) {
        Map<String, Object> result = inflowService.deleteSymbol(symbolId, waterFlowTypeData);
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(result);
        } else {
            Integer status = (Integer) result.get("status");
            if (status != null) {
                return ResponseEntity.status(status).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
    }
} 