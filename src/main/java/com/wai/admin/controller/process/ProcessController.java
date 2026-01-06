package com.wai.admin.controller.process;

import com.wai.admin.service.process.ProcessService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/process/")
public class ProcessController {

    @Autowired
    private ProcessService processService;

    @GetMapping("/{id}")
    public Map<String, Object> getProcessById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processDetailSuccess");
        response.put("data", processService.getProcessById(id));
        return response;
    }

    @PostMapping(value = "/master/create", consumes = { "multipart/form-data", "multipart/mixed" })
    public Map<String, Object> createProcess(
            @RequestParam(value = "process_code", required = false) String processCode,
            @RequestParam(value = "process_name", required = false) String processName,
            @RequestParam(value = "process_type_code", required = false) String processTypeCode,
            @RequestParam(value = "process_category", required = false) String processCategory,
            @RequestParam(value = "process_description", required = false) String processDescription,
            @RequestParam(value = "technology_mode_code", required = false) String technologyModeCode,
            @RequestParam(value = "is_active", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(value = "poc_input_count", required = false, defaultValue = "0") Integer pocInputCount,
            @RequestParam(value = "poc_output_count", required = false, defaultValue = "0") Integer pocOutputCount,
            @RequestParam(value = "workflow_svg_uri", required = false) String workflowSvgUri,
            @RequestParam(value = "symbol_id", required = false) String symbolId,
            @RequestParam(value = "unit_system_code", required = false, defaultValue = "METRIC") String unitSystemCode,
            @RequestParam(value = "symbol_name", required = false) String symbolName,
            @RequestParam(value = "symbol_code", required = false) String symbolCode,
            @RequestParam(value = "symbol_type", required = false, defaultValue = "PROCESS") String symbolType,
            @RequestParam(value = "siteFile", required = false) org.springframework.web.multipart.MultipartFile siteFile) {

        // 파일 처리
        org.springframework.web.multipart.MultipartFile selectedFile = siteFile;

        // processData 맵 구성
        Map<String, Object> processData = new HashMap<>();
        processData.put("process_code", processCode);
        processData.put("process_name", processName);
        processData.put("process_type_code", processTypeCode);
        processData.put("process_category", processCategory);
        processData.put("process_description", processDescription);
        processData.put("technology_mode_code", technologyModeCode);
        processData.put("is_active", isActive);
        processData.put("poc_input_count", pocInputCount);
        processData.put("poc_output_count", pocOutputCount);
        processData.put("workflow_svg_uri", workflowSvgUri);
        processData.put("symbol_id", symbolId);
        // unit_system_code를 대문자로 변환 (METRIC 형식으로 통일)
        processData.put("unit_system_code", unitSystemCode != null ? unitSystemCode.toUpperCase() : "METRIC");
        processData.put("symbol_name", symbolName);
        processData.put("symbol_code", symbolCode);
        processData.put("symbol_type", symbolType);

        if (selectedFile != null) {
            processData.put("siteFile", selectedFile);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processCreateSuccess");
        response.put("data", processService.createProcess(processData, siteFile));
        return response;
    }

    /**
     * Process 생성 (대체 multipart 처리)
     */
    @PostMapping(value = "/master/create-multipart")
    public Map<String, Object> createProcessMultipart(MultipartHttpServletRequest request) {

        // process_symbol_file 찾기
        org.springframework.web.multipart.MultipartFile siteFile = request.getFile("process_symbol_file");
        if (siteFile == null) {
            // 다른 가능한 이름들도 확인
            siteFile = request.getFile("siteFile");
            siteFile = request.getFile("file");
        }

        // processData 맵 구성
        Map<String, Object> processData = new HashMap<>();
        processData.put("process_code", request.getParameter("process_code"));
        processData.put("process_name", request.getParameter("process_name"));
        processData.put("process_type_code", request.getParameter("process_type_code"));
        processData.put("process_category", request.getParameter("process_category"));
        processData.put("process_description", request.getParameter("process_description"));
        processData.put("technology_mode_code", request.getParameter("technology_mode_code"));
        processData.put("is_active", Boolean.valueOf(request.getParameter("is_active")));
        processData.put("poc_input_count", Integer.valueOf(
                request.getParameter("poc_input_count") != null ? request.getParameter("poc_input_count") : "0"));
        processData.put("poc_output_count", Integer.valueOf(
                request.getParameter("poc_output_count") != null ? request.getParameter("poc_output_count") : "0"));
        processData.put("workflow_svg_uri", request.getParameter("workflow_svg_uri"));
        processData.put("symbol_id", request.getParameter("symbol_id"));
        processData.put("unit_system_code",
                (request.getParameter("unit_system_code") != null ? request.getParameter("unit_system_code") : "METRIC")
                        .toUpperCase());
        processData.put("symbol_name", request.getParameter("symbol_name"));
        processData.put("symbol_code", request.getParameter("symbol_code"));
        processData.put("symbol_type",
                request.getParameter("symbol_type") != null ? request.getParameter("symbol_type") : "PROCESS");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processCreateAlternativeSuccess");
        response.put("data", processService.createProcess(processData, siteFile));
        return response;
    }

    /**
     * Process 생성 (JSON 지원)
     */
    @PostMapping(value = "/master/create", consumes = { "application/json" })
    public Map<String, Object> createProcessJson(@RequestBody Map<String, Object> processData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processCreateSuccess");
        response.put("data", processService.createProcess(processData, null));
        return response;
    }

    @PutMapping(value = "/master/update/{id}", consumes = { "multipart/form-data" })
    public Map<String, Object> updateProcess(
            @PathVariable String id,
            @RequestParam(value = "process_code", required = false) String processCode,
            @RequestParam(value = "process_name", required = false) String processName,
            @RequestParam(value = "process_type_code", required = false) String processTypeCode,
            @RequestParam(value = "process_category", required = false) String processCategory,
            @RequestParam(value = "process_description", required = false) String processDescription,
            @RequestParam(value = "technology_mode_code", required = false) String technologyModeCode,
            @RequestParam(value = "is_active", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(value = "poc_input_count", required = false, defaultValue = "0") Integer pocInputCount,
            @RequestParam(value = "poc_output_count", required = false, defaultValue = "0") Integer pocOutputCount,
            @RequestParam(value = "workflow_svg_uri", required = false) String workflowSvgUri,
            @RequestParam(value = "symbol_id", required = false) String symbolId,
            @RequestParam(value = "unit_system_code", required = false, defaultValue = "METRIC") String unitSystemCode,
            @RequestParam(value = "symbol_name", required = false) String symbolName,
            @RequestParam(value = "symbol_code", required = false) String symbolCode,
            @RequestParam(value = "symbol_type", required = false, defaultValue = "PROCESS") String symbolType,
            @RequestParam(value = "siteFile", required = false) org.springframework.web.multipart.MultipartFile siteFile,
            @RequestParam(value = "process_symbol_file", required = false) org.springframework.web.multipart.MultipartFile processSymbolFile,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) {

        // 파일 우선순위: siteFile > process_symbol_file > file
        org.springframework.web.multipart.MultipartFile selectedFile = null;

        if (siteFile != null && !siteFile.isEmpty()) {
            selectedFile = siteFile;
        } else if (processSymbolFile != null && !processSymbolFile.isEmpty()) {
            selectedFile = processSymbolFile;
        } else if (file != null && !file.isEmpty()) {
            selectedFile = file;
        }

        // processData 맵 구성
        Map<String, Object> processData = new HashMap<>();
        processData.put("process_code", processCode);
        processData.put("process_name", processName);
        processData.put("process_type_code", processTypeCode);
        processData.put("process_category", processCategory);
        processData.put("process_description", processDescription);
        processData.put("technology_mode_code", technologyModeCode);
        processData.put("is_active", isActive);
        processData.put("poc_input_count", pocInputCount);
        processData.put("poc_output_count", pocOutputCount);
        processData.put("workflow_svg_uri", workflowSvgUri);
        processData.put("symbol_id", symbolId);
        // unit_system_code를 대문자로 변환 (METRIC 형식으로 통일)
        processData.put("unit_system_code", unitSystemCode != null ? unitSystemCode.toUpperCase() : "METRIC");
        processData.put("symbol_name", symbolName);
        processData.put("symbol_code", symbolCode);
        processData.put("symbol_type", symbolType);

        Map<String, Object> serviceResult = processService.updateProcess(id, processData, selectedFile);
        Map<String, Object> response = new HashMap<>();

        // 서비스 결과에 따라 응답 구성
        if ((Boolean) serviceResult.get("success")) {
            response.put("status", "success");
            response.put("message", "messages.success.processUpdateComplete");
            response.put("data", serviceResult);
        } else {
            response.put("status", "error");
            response.put("message", serviceResult.get("message"));
            response.put("data", serviceResult);
        }

        return response;
    }

    /**
     * Process 수정 (JSON 지원)
     */
    @PutMapping(value = "/master/update/{id}", consumes = { "application/json" })
    public Map<String, Object> updateProcessJson(@PathVariable String id,
            @RequestBody Map<String, Object> processData) {
        Map<String, Object> serviceResult = processService.updateProcess(id, processData, null);
        Map<String, Object> response = new HashMap<>();

        // 서비스 결과에 따라 응답 구성
        if ((Boolean) serviceResult.get("success")) {
            response.put("status", "success");
            response.put("message", "messages.success.processUpdateComplete");
            response.put("data", serviceResult);
        } else {
            response.put("status", "error");
            response.put("message", serviceResult.get("message"));
            response.put("data", serviceResult);
        }

        return response;
    }

    /**
     * Process 삭제 (Path Parameter 지원 - 하위 호환성)
     */
    @DeleteMapping("/master/delete/{id}")
    public Map<String, Object> deleteProcessById(
            @PathVariable String id,
            @RequestParam(value = "symbol_id", required = false) String symbolId) {

        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("process_id", id);

        // symbol_id 처리
        if (symbolId != null && !symbolId.trim().isEmpty()) {
            deleteData.put("symbol_id", symbolId);
        }

        Map<String, Object> serviceResult = processService.deleteProcess(deleteData);
        Map<String, Object> response = new HashMap<>();

        // 서비스 결과에 따라 응답 구성
        if ((Boolean) serviceResult.get("success")) {
            response.put("status", "success");
            response.put("message", "messages.success.processDeleteSuccess");
            response.put("data", serviceResult);
        } else {
            response.put("status", "error");
            response.put("message", serviceResult.get("message"));
            response.put("data", serviceResult);
        }

        return response;
    }

    /**
     * Process 삭제 (Request Body 지원 - 다중 Symbol ID)
     */
    @DeleteMapping("/master/delete-batch")
    public Map<String, Object> deleteProcessBatch(@RequestBody Map<String, Object> deleteData) {

        Map<String, Object> serviceResult = processService.deleteProcess(deleteData);
        Map<String, Object> response = new HashMap<>();

        // 서비스 결과에 따라 응답 구성
        if ((Boolean) serviceResult.get("success")) {
            response.put("status", "success");
            response.put("message", "messages.success.processBatchDeleteSuccess");
            response.put("data", serviceResult);
        } else {
            response.put("status", "error");
            response.put("message", serviceResult.get("message"));
            response.put("data", serviceResult);
        }

        return response;
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> getProcessStatus(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processStatusSuccess");
        response.put("data", processService.getProcessStatus(id));
        return response;
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> startProcess(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processStartSuccess");
        response.put("data", processService.startProcess(id));
        return response;
    }

    @PostMapping("/{id}/stop")
    public Map<String, Object> stopProcess(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.processStopSuccess");
        response.put("data", processService.stopProcess(id));
        return response;
    }

    /**
     * ProcessMaster 검색
     */
    @PostMapping("/master/search")
    public ResponseEntity<Map<String, Object>> getAllProcesses(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getAllProcesses(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 용 공통코드 검색
     */
    @PostMapping("/code/search")
    public ResponseEntity<Map<String, Object>> getProcessCode(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getProcessCode(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process Components 검색
     */
    @PostMapping("/components/search")
    public ResponseEntity<Map<String, Object>> getComponents(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getComponents(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 용 계산식 등록 (multipart/form-data 지원)
     */
    @PostMapping(value = "/formula/create", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> createFormulaMultipart(
            @RequestParam(value = "formula_scope", required = false, defaultValue = "PROCESS") String formulaScope,
            @RequestParam(value = "process_id", required = false) Integer processId,
            @RequestParam(value = "formula_name", required = false) String formulaName,
            @RequestParam(value = "formula_code", required = false) String formulaCode,
            @RequestParam(value = "siteFile", required = false) org.springframework.web.multipart.MultipartFile siteFile) {

        Map<String, Object> formulaData = new HashMap<>();
        formulaData.put("formula_scope", formulaScope);
        formulaData.put("process_id", processId);
        formulaData.put("formula_name", formulaName);
        formulaData.put("formula_code", formulaCode);

        Map<String, Object> serviceResult = processService.createFormula(formulaData, siteFile);
        Boolean success = (Boolean) serviceResult.get("success");

        Map<String, Object> response = new HashMap<>();
        if (success != null && success) {
            response.put("status", "success");
            response.put("message", "messages.success.formulaCreateMultipartSuccess");
            response.put("data", serviceResult);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", serviceResult.get("message"));
            response.put("data", serviceResult);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Process 컴포넌트 등록
     */
    @PostMapping(value = "/component/create", consumes = { "application/json" })
    public ResponseEntity<Map<String, Object>> createComponent(@RequestBody Map<String, Object> componentData) {
        Map<String, Object> result = processService.createComponent(componentData);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 컴포넌트 업데이트
     */
    @PatchMapping(value = "/component/update/{componentId}", consumes = { "application/json" })
    public ResponseEntity<Map<String, Object>> updateComponent(
            @PathVariable String componentId,
            @RequestBody Map<String, Object> componentData) {
        Map<String, Object> result = processService.updateComponent(componentId, componentData);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 컴포넌트 삭제
     */
    @DeleteMapping("/component/delete/{componentId}")
    public ResponseEntity<Map<String, Object>> deleteComponent(@PathVariable String componentId) {
        Map<String, Object> result = processService.deleteComponent(componentId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 심볼 다운로드 URL 조회
     */
    @GetMapping("/symbol/download/{symbolId}")
    public ResponseEntity<Map<String, Object>> getSymbolDownload(@PathVariable String symbolId) {
        Map<String, Object> result = processService.getSymbolDownload(symbolId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 계산식 다운로드 URL 조회
     */
    @GetMapping("/formula/download/{formulaId}")
    public ResponseEntity<Map<String, Object>> getFormulaDownload(@PathVariable String formulaId) {
        Map<String, Object> result = processService.getFormulaDownload(formulaId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 도면 다운로드 URL 조회
     */
    @GetMapping("/drawing/download/{drawingId}")
    public ResponseEntity<Map<String, Object>> getDrawingDownload(@PathVariable String drawingId) {
        Map<String, Object> result = processService.getDrawingDownload(drawingId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 계산식 검색
     */
    @PostMapping("/formula/search")
    public ResponseEntity<Map<String, Object>> getFormula(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getFormula(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 계산식 삭제
     */
    @DeleteMapping("/formula/delete/{formulaId}")
    public ResponseEntity<Map<String, Object>> deleteFormula(@PathVariable String formulaId) {
        Map<String, Object> result = processService.deleteFormula(formulaId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 계산식 삭제 (검색 후 삭제)
     */
    @PostMapping("/formula/delete-batch")
    public ResponseEntity<Map<String, Object>> deleteProcessFormula(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.deleteProcessFormula(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 삭제 (검색 후 삭제)
     */
    @PostMapping("/drawing/delete-batch")
    public ResponseEntity<Map<String, Object>> deleteProcessDrawing(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.deleteProcessDrawing(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 삭제
     */
    @DeleteMapping("/drawing/delete/{drawingId}")
    public ResponseEntity<Map<String, Object>> deleteDrawing(@PathVariable String drawingId) {
        Map<String, Object> result = processService.deleteDrawing(drawingId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 수정
     */
    @PatchMapping(value = "/drawing/{drawingId}", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> updateDrawing(
            @PathVariable String drawingId,
            @RequestParam Map<String, Object> drawingData,
            @RequestParam(value = "siteFile", required = false) org.springframework.web.multipart.MultipartFile siteFile,
            @RequestParam(value = "symbolFile", required = false) org.springframework.web.multipart.MultipartFile symbolFile) {
        Map<String, Object> result = processService.updateDrawing(drawingId, drawingData, siteFile, symbolFile);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 생성
     */
    @PostMapping(value = "/drawing/create", consumes = { "multipart/form-data" })
    public Map<String, Object> createDrawing(
            @RequestParam Map<String, Object> drawingData,
            @RequestParam(value = "siteFile", required = false) org.springframework.web.multipart.MultipartFile siteFile,
            @RequestParam(value = "symbolFile", required = false) org.springframework.web.multipart.MultipartFile symbolFile) {

        // 파일 크기 검증 (siteFile)
        if (siteFile != null && !siteFile.isEmpty()) {
            long maxFileSize = 100L * 1024 * 1024; // 100MB
            if (siteFile.getSize() > maxFileSize) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "messages.error.fileSizeExceeded");
                response.put("data", null);
                return response;
            }
        }

        // 파일 크기 검증 (symbolFile)
        if (symbolFile != null && !symbolFile.isEmpty()) {
            long maxFileSize = 100L * 1024 * 1024; // 100MB
            if (symbolFile.getSize() > maxFileSize) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "messages.error.symbolFileSizeExceeded");
                response.put("data", null);
                return response;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.drawingCreateSuccess");
        response.put("data", processService.createDrawing(drawingData, siteFile, symbolFile));
        return response;
    }

    /**
     * Process 자식 도면 업로드
     */
    @PostMapping(value = "/drawing/child/upload", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> uploadChildDrawing(
            @RequestParam Map<String, Object> drawingData,
            @RequestParam("siteFile") org.springframework.web.multipart.MultipartFile siteFile) {

        Map<String, Object> result = processService.uploadChildDrawing(drawingData, siteFile);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 Child 검색
     */
    @PostMapping("/drawing_child/search")
    public ResponseEntity<Map<String, Object>> getDrawingChild(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getDrawingChild(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 도면 Master 상세 검색
     */
    @PostMapping("/drawing_master/search")
    public ResponseEntity<Map<String, Object>> getDrawingMaster(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getDrawingMaster(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Minio 도면 파일 목록 검색 (프론트엔드 호출용)
     */
    @PostMapping("/drawing_files/list")
    public ResponseEntity<Map<String, Object>> getDrawingFilesList(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getDrawingFilesList(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Process 심볼 삭제
     */
    @DeleteMapping("/symbol/{symbolId}")
    public ResponseEntity<Map<String, Object>> deleteSymbol(@PathVariable String symbolId) {
        Map<String, Object> result = processService.deleteSymbol(symbolId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 도면 관계 조회 - PFD와 P&ID 매핑 목록 조회
     */
    @PostMapping("/relationships/search")
    public ResponseEntity<Map<String, Object>> getDrawingRelationships(
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result = processService.getDrawingRelationships(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 자식 Excel 업로드 - PID Excel 매핑
     */
    @PostMapping(value = "/excel/child/upload", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> uploadChildExcel(
            @RequestParam Map<String, Object> excelData,
            @RequestParam(value = "excelFile", required = false) org.springframework.web.multipart.MultipartFile excelFile) {

        Map<String, Object> result = processService.uploadChildExcel(excelData, excelFile);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.get("success"));
        response.put("message", result.get("message"));
        response.put("data", result);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 용량계산서(CCS) 엑셀 파일 업로드
     */
    @PostMapping(value = "/ccs/upload/{tableName}/{pkValue}", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> uploadCcs(
            @PathVariable String tableName,
            @PathVariable String pkValue,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) {

        Map<String, Object> result = processService.uploadCcs(tableName, pkValue, file);

        Map<String, Object> response = new HashMap<>();
        if ((Boolean) result.get("success")) {
            response.put("status", "success");
            response.put("message", result.get("message"));
            response.put("data", result.get("data"));
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", result.get("message"));
            response.put("data", result.get("data"));
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 용량계산서(CCS) 파일 다운로드 URL 조회
     */
    @GetMapping("/ccs/download/{tableName}/{pkValue}")
    public ResponseEntity<Map<String, Object>> getCcsDownload(
            @PathVariable String tableName,
            @PathVariable String pkValue) {
        Map<String, Object> result = processService.getCcsDownload(tableName, pkValue);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 용량계산서(CCS) 파일 삭제
     */
    @DeleteMapping("/ccs/delete/{tableName}/{pkValue}/{filename}")
    public ResponseEntity<Map<String, Object>> deleteCcs(
            @PathVariable String tableName,
            @PathVariable String pkValue,
            @PathVariable String filename) {
        Map<String, Object> result = processService.deleteCcs(tableName, pkValue, filename);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

}