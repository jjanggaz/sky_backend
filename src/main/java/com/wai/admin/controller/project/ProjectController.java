package com.wai.admin.controller.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wai.admin.service.project.ProjectService;
import com.wai.admin.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 프로젝트 목록 조회
     */
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllProjects(
            @RequestBody Map<String, Object> searchParams,
            HttpServletRequest request) {
        Map<String, Object> result = projectService.getAllProjects(searchParams, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 프로젝트 생성
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createProject(
            @RequestParam("projectData") String projectDataJson,
            @RequestParam(value = "siteFile", required = false) MultipartFile siteFile) {
        
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> projectData = JsonUtil.parseJson(projectDataJson);
            
            // 파일이 있으면 projectData에 추가
            if (siteFile != null && !siteFile.isEmpty()) {
                projectData.put("siteFile", siteFile);
            }
            
            Map<String, Object> result = projectService.createProject(projectData);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "프로젝트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 프로젝트 수정
     */
    @PostMapping("/update/{projectId}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable String projectId,
            @RequestParam("projectData") String projectDataJson,
            @RequestParam(value = "siteFile", required = false) MultipartFile siteFile,
            HttpServletRequest request) {
        
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> projectData = JsonUtil.parseJson(projectDataJson);
            
            // 파일이 있으면 projectData에 추가
            if (siteFile != null && !siteFile.isEmpty()) {
                projectData.put("siteFile", siteFile);
            }
            
            Map<String, Object> result = projectService.updateProject(projectId, projectData, request);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "프로젝트 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 프로젝트 삭제
     */
    @PostMapping("/delete/{projectId}")
    public ResponseEntity<Map<String, Object>> deleteProject(
            @PathVariable String projectId, 
            @RequestBody Map<String, Object> deleteData) {
        Map<String, Object> result = projectService.deleteProject(projectId, deleteData);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 프로젝트 관련 공통코드들을 조회합니다.
     */
    @PostMapping("/common-codes")
    public ResponseEntity<Map<String, Object>> getProjectCommonCodes() {
        Map<String, Object> result = projectService.getProjectCommonCodes();
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 부지정보 파일 조회
     */
    @GetMapping("/fileInfo/{siteId}")
    public ResponseEntity<Map<String, Object>> downloadSiteInfoFile(@PathVariable String siteId) {
        Map<String, Object> result = projectService.downloadSiteInfoFile(siteId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 부지정보 파일 삭제
     */
    @PostMapping("/deleteSiteFile/{siteId}")
    public ResponseEntity<Map<String, Object>> deleteSiteInfoFile(
            @PathVariable String siteId,
            @RequestBody(required = false) Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> result = projectService.deleteSiteInfoFile(siteId, requestData, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    
    /**
     * 추천프로젝트 목록 조회
     */
    @PostMapping("/listRec")
    public ResponseEntity<Map<String, Object>> getRecommendationProjects(
            @RequestBody(required = false) Map<String, Object> searchParams,
            HttpServletRequest request) {
        Map<String, Object> result = projectService.getRecommendationProjects(searchParams, request);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 추천프로젝트 생성
     */
    @PostMapping("/createRec")
    public ResponseEntity<Map<String, Object>> createRecommendationProject(
            @RequestBody Map<String, Object> recommendationData,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> result = projectService.createRecommendationProject(recommendationData, request);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "추천프로젝트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 추천프로젝트 삭제
     */
    @DeleteMapping("/deleteRec/{recommendationId}")
    public ResponseEntity<Map<String, Object>> deleteRecommendationProject(
            @PathVariable String recommendationId) {
        Map<String, Object> result = projectService.deleteRecommendationProject(recommendationId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 프로젝트 상태 업데이트
     */
    @PostMapping("/status/update/{projectId}")
    public ResponseEntity<Map<String, Object>> updateProjectStatus(
            @PathVariable String projectId,
            @RequestBody Map<String, Object> statusData,
            HttpServletRequest request) {
        Map<String, Object> result = projectService.updateProjectApi(projectId, statusData, request);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 