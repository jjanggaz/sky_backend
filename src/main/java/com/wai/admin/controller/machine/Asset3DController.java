package com.wai.admin.controller.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wai.admin.service.machine.Asset3DService;
import com.wai.admin.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/asset3D")
public class Asset3DController {

    private static final Logger logger = LoggerFactory.getLogger(Asset3DController.class);

    @Autowired
    private Asset3DService asset3DService;

    /**
     * 공통 코드 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/common/code")
    public ResponseEntity<Map<String, Object>> getCommonCodes(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = asset3DService.getCommonCodes(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 공통 코드 트리 조회
     *
     * @param searchParams
     * @return
     */
    @PostMapping("/common/codeTree")
    public ResponseEntity<Map<String, Object>> getCodeTree(
            @RequestBody(required = false) Map<String, Object> searchParams) {
        Map<String, Object> result = asset3DService.getCodeTree(searchParams);
        logger.info("POST /common/codeTree 응답: {}", result);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 공통 코드 트리 조회 (GET) - 기본 조건
     *
     * @return
     */
    @GetMapping("/common/codeTree")
    public ResponseEntity<Map<String, Object>> getCodeTree() {
        Map<String, Object> result = asset3DService.getCodeTree(null);
        logger.info("GET /common/codeTree 응답: {}", result);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 400);
            return ResponseEntity.status(statusCode).body(result);
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
        Map<String, Object> result = asset3DService.getDepth(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 프리셋 마스터 목록 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/search/{type}")
    public ResponseEntity<Map<String, Object>> getSearchList(
            @PathVariable String type,
            @RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        if ("PRESET".equalsIgnoreCase(type)) {
            result = asset3DService.getPresetList(searchParams);
        } else if ("3D_LIBRARY".equalsIgnoreCase(type)) {
            result = asset3DService.getLibraryList(searchParams);
        } else {
            result = new HashMap<>();
            result.put("success", false);
            result.put("message", "지원하지 않는 타입입니다: " + type);
            return ResponseEntity.badRequest().body(result);
        }

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 프리셋 마스터 생성 (multipart/form-data 지원)
     * 
     * @param presetData 프리셋 데이터
     * @param siteFile   썸네일 파일 (선택)
     * @return 생성 결과
     */
    @PostMapping(value = "/preset/create", consumes = { "multipart/form-data", "multipart/mixed" })
    public ResponseEntity<Map<String, Object>> createPresetMaster(
            @RequestParam Map<String, String> presetData,
            @RequestParam(value = "siteFile", required = false) MultipartFile siteFile) {

        logger.info("프리셋 마스터 생성 요청 (multipart): presetData={}, siteFile={}",
                presetData, siteFile != null ? siteFile.getOriginalFilename() : null);

        // Map<String, String>을 Map<String, Object>로 변환
        Map<String, Object> presetDataMap = new HashMap<>(presetData);

        // metadata가 JSON 문자열로 전달된 경우 파싱
        if (presetDataMap.containsKey("metadata") && presetDataMap.get("metadata") instanceof String) {
            String metadataStr = (String) presetDataMap.get("metadata");
            if (metadataStr != null && !metadataStr.trim().isEmpty()) {
                try {
                    Object metadataObj = JsonUtil.parseJson(metadataStr);
                    if (metadataObj != null) {
                        presetDataMap.put("metadata", metadataObj);
                    }
                } catch (Exception e) {
                    logger.warn("metadata 파싱 실패, 문자열로 유지: {}", e.getMessage());
                }
            }
        }

        Map<String, Object> result = asset3DService.createPresetMaster(presetDataMap, siteFile);

        logger.info("프리셋 마스터 생성 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("statusCode", 500);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 썸네일 업로드
     * 
     * @param file     썸네일 파일
     * @param presetId 프리셋 ID (UUID)
     * @return 업로드 결과
     */
    @PostMapping(value = "/preset/thumbnail/upload", consumes = { "multipart/form-data", "multipart/mixed" })
    public ResponseEntity<Map<String, Object>> uploadPresetThumbnail(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "preset_id", required = true) String presetId) {

        logger.info("프리셋 썸네일 업로드 요청: presetId={}, fileName={}, size={}",
                presetId, file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = asset3DService.uploadPresetThumbnail(file, presetId);

        logger.info("프리셋 썸네일 업로드 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 썸네일 삭제
     * 
     * @param presetId 프리셋 ID (UUID)
     * @return 삭제 결과
     */
    @DeleteMapping("/preset/thumbnail/delete/{preset_id}")
    public ResponseEntity<Map<String, Object>> deletePresetThumbnail(
            @PathVariable("preset_id") String presetId) {

        logger.info("프리셋 썸네일 삭제 요청: presetId={}", presetId);

        Map<String, Object> result = asset3DService.deletePresetThumbnail(presetId);

        logger.info("프리셋 썸네일 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 썸네일 삭제
     * 
     * @param libraryId 라이브러리 ID (UUID)
     * @return 삭제 결과
     */
    @DeleteMapping("/library/thumbnail/delete/{library_id}")
    public ResponseEntity<Map<String, Object>> deleteLibraryThumbnail(
            @PathVariable("library_id") String libraryId) {

        logger.info("3D 라이브러리 썸네일 삭제 요청: libraryId={}", libraryId);

        Map<String, Object> result = asset3DService.deleteLibraryThumbnail(libraryId);

        logger.info("3D 라이브러리 썸네일 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 썸네일 업로드
     * 
     * @param file      썸네일 파일
     * @param libraryId 라이브러리 ID (UUID)
     * @return 업로드 결과
     */
    @PostMapping(value = "/library/thumbnail/upload", consumes = { "multipart/form-data", "multipart/mixed" })
    public ResponseEntity<Map<String, Object>> uploadLibraryThumbnail(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "library_id", required = true) String libraryId) {

        logger.info("3D 라이브러리 썸네일 업로드 요청: libraryId={}, fileName={}, size={}",
                libraryId, file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = asset3DService.uploadLibraryThumbnail(file, libraryId);

        logger.info("3D 라이브러리 썸네일 업로드 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 Model 파일 업로드
     * 
     * @param file      Model 파일
     * @param libraryId 라이브러리 ID (UUID)
     * @return 업로드 결과
     */
    @PostMapping(value = "/library/model/upload", consumes = { "multipart/form-data", "multipart/mixed" })
    public ResponseEntity<Map<String, Object>> uploadLibraryModel(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "library_id", required = true) String libraryId) {

        logger.info("3D 라이브러리 Model 파일 업로드 요청: libraryId={}, fileName={}, size={}",
                libraryId, file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = asset3DService.uploadLibraryModel(file, libraryId);

        logger.info("3D 라이브러리 Model 파일 업로드 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 마스터 수정
     * 
     * @param presetId   프리셋 ID
     * @param presetData 프리셋 데이터
     * @return 수정 결과
     */
    @RequestMapping(value = "/preset/update/{preset_id}", method = { RequestMethod.PATCH }, consumes = {
            "application/json" })
    public ResponseEntity<Map<String, Object>> updatePresetMaster(
            @PathVariable("preset_id") String presetId,
            @RequestBody Map<String, Object> presetData) {

        logger.info("프리셋 수정 요청: preset_id={}", presetId);

        Map<String, Object> result = asset3DService.updatePresetMaster(presetId, presetData);

        logger.info("프리셋 수정 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 상세 생성
     *
     * @param presetId   프리셋 ID
     * @param detailData 상세 데이터
     * @return 생성 결과
     */
    @PostMapping(value = "/preset/{preset_id}/detail", consumes = { "application/json" })
    public ResponseEntity<Map<String, Object>> createPresetDetail(
            @PathVariable("preset_id") String presetId,
            @RequestBody Map<String, Object> detailData) {

        logger.info("프리셋 상세 생성 요청: preset_id={}, detailData={}", presetId, detailData);

        Map<String, Object> result = asset3DService.createPresetDetail(presetId, detailData);

        logger.info("프리셋 상세 생성 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 상세 목록 조회
     *
     * @param presetId 프리셋 ID
     * @return 프리셋 상세 목록 결과
     */
    @GetMapping(value = "/preset/{preset_id}/detail")
    public ResponseEntity<Map<String, Object>> getPresetDetail(
            @PathVariable("preset_id") String presetId) {

        logger.info("프리셋 상세 목록 조회 요청: preset_id={}", presetId);

        Map<String, Object> result = asset3DService.getPresetDetail(presetId);

        logger.info("프리셋 상세 목록 조회 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 상세 수정
     *
     * @param presetId   프리셋 ID
     * @param detailId   상세 ID
     * @param detailData 프리셋 상세 데이터
     * @return 수정 결과
     */
    @RequestMapping(value = "/preset/{preset_id}/detail/{detail_id}", method = { RequestMethod.PATCH }, consumes = {
            "application/json" })
    public ResponseEntity<Map<String, Object>> updatePresetDetail(
            @PathVariable("preset_id") String presetId,
            @PathVariable("detail_id") String detailId,
            @RequestBody Map<String, Object> detailData) {

        logger.info("프리셋 상세 수정 요청: preset_id={}, detail_id={}", presetId, detailId);

        Map<String, Object> result = asset3DService.updatePresetDetail(presetId, detailId, detailData);

        logger.info("프리셋 상세 수정 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 상세 삭제
     *
     * @param presetId 프리셋 ID
     * @param detailId 상세 ID
     * @return 삭제 결과
     */
    @DeleteMapping(value = "/preset/{preset_id}/detail/{detail_id}")
    public ResponseEntity<Map<String, Object>> deletePresetDetail(
            @PathVariable("preset_id") String presetId,
            @PathVariable("detail_id") String detailId) {

        logger.info("프리셋 상세 삭제 요청: preset_id={}, detail_id={}", presetId, detailId);

        Map<String, Object> result = asset3DService.deletePresetDetail(presetId, detailId);

        logger.info("프리셋 상세 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 상세 순서 변경 (swap_sequence)
     *
     * @param presetId 프리셋 ID
     * @param swapData 교체할 상세 ID 정보 (detail_id_1, detail_id_2)
     * @return 순서 변경 결과
     */
    @PostMapping(value = "/preset/{preset_id}/details/swap_sequence", consumes = { "application/json" })
    public ResponseEntity<Map<String, Object>> swapSequence(
            @PathVariable("preset_id") String presetId,
            @RequestBody Map<String, Object> swapData) {

        logger.info("프리셋 상세 순서 변경 요청: preset_id={}, swapData={}", presetId, swapData);

        Map<String, Object> result = asset3DService.swapSequence(presetId, swapData);

        logger.info("프리셋 상세 순서 변경 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 생성 (multipart/form-data 지원)
     *
     * @param libraryData   라이브러리 데이터
     * @param modelFile     Model 파일 (선택)
     * @param thumbnailFile 썸네일 파일 (선택)
     * @return 생성 결과
     */
    @PostMapping(value = "/library/create", consumes = { "multipart/form-data", "multipart/mixed" })
    public ResponseEntity<Map<String, Object>> createLibrary(
            @RequestParam Map<String, String> libraryData,
            @RequestParam(value = "modelFile", required = false) MultipartFile modelFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {

        logger.info("3D 라이브러리 생성 요청 (multipart): libraryData={}, modelFile={}, thumbnailFile={}",
                libraryData, modelFile != null ? modelFile.getOriginalFilename() : null,
                thumbnailFile != null ? thumbnailFile.getOriginalFilename() : null);

        // Map<String, String>을 Map<String, Object>로 변환
        Map<String, Object> libraryDataMap = new HashMap<>(libraryData);

        // metadata가 JSON 문자열로 전달된 경우 파싱
        if (libraryDataMap.containsKey("metadata") && libraryDataMap.get("metadata") instanceof String) {
            String metadataStr = (String) libraryDataMap.get("metadata");
            if (metadataStr != null && !metadataStr.trim().isEmpty()) {
                try {
                    Object metadataObj = JsonUtil.parseJson(metadataStr);
                    if (metadataObj != null) {
                        libraryDataMap.put("metadata", metadataObj);
                    }
                } catch (Exception e) {
                    logger.warn("metadata 파싱 실패, 문자열로 유지: {}", e.getMessage());
                }
            }
        }

        Map<String, Object> result = asset3DService.createLibrary(libraryDataMap, modelFile, thumbnailFile);

        logger.info("3D 라이브러리 생성 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 생성 (JSON 지원)
     *
     * @param libraryData 라이브러리 데이터
     * @return 생성 결과
     */
    @PostMapping(value = "/library/create/json", consumes = { "application/json" })
    public ResponseEntity<Map<String, Object>> createLibraryJson(@RequestBody Map<String, Object> libraryData) {
        logger.info("3D 라이브러리 생성 요청 (JSON): libraryData={}", libraryData);

        Map<String, Object> result = asset3DService.createLibrary(libraryData, null, null);

        logger.info("3D 라이브러리 생성 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 수정
     *
     * @param libraryId   라이브러리 ID
     * @param libraryData 라이브러리 데이터
     * @return 수정 결과
     */
    @RequestMapping(value = "/library/update/{library_id}", method = { RequestMethod.PATCH }, consumes = {
            "application/json" })
    public ResponseEntity<Map<String, Object>> updateLibrary(
            @PathVariable("library_id") String libraryId,
            @RequestBody Map<String, Object> libraryData) {

        logger.info("3D 라이브러리 수정 요청: library_id={}", libraryId);

        Map<String, Object> result = asset3DService.updateLibrary(libraryId, libraryData);

        logger.info("3D 라이브러리 수정 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 마스터 삭제 (Path Parameter 지원)
     * 
     * @param presetId    프리셋 ID
     * @param thumbnailId 썸네일 ID (선택)
     * @return 삭제 결과
     */
    @DeleteMapping("/preset/delete/{preset_id}")
    public ResponseEntity<Map<String, Object>> deletePresetMasterById(
            @PathVariable("preset_id") String presetId,
            @RequestParam(value = "thumbnail_id", required = false) String thumbnailId) {

        logger.info("프리셋 삭제 요청: preset_id={}, thumbnail_id={}", presetId, thumbnailId);

        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("preset_id", presetId);

        // thumbnail_id 처리
        if (thumbnailId != null && !thumbnailId.trim().isEmpty()) {
            deleteData.put("thumbnail_id", thumbnailId);
        }

        Map<String, Object> result = asset3DService.deletePresetMaster(deleteData);

        logger.info("프리셋 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 프리셋 마스터 삭제 (Request Body 지원)
     * 
     * @param deleteData 삭제 데이터 (preset_id, thumbnail_id 포함)
     * @return 삭제 결과
     */
    @DeleteMapping("/preset/delete")
    public ResponseEntity<Map<String, Object>> deletePresetMaster(@RequestBody Map<String, Object> deleteData) {

        logger.info("프리셋 삭제 요청: deleteData={}", deleteData);

        Map<String, Object> result = asset3DService.deletePresetMaster(deleteData);

        logger.info("프리셋 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 삭제 (Path Parameter 지원)
     * 
     * @param libraryId   라이브러리 ID
     * @param thumbnailId 썸네일 ID (선택)
     * @return 삭제 결과
     */
    @DeleteMapping("/library/delete/{library_id}")
    public ResponseEntity<Map<String, Object>> deleteLibraryById(
            @PathVariable("library_id") String libraryId,
            @RequestParam(value = "thumbnail_id", required = false) String thumbnailId) {

        logger.info("3D 라이브러리 삭제 요청: library_id={}, thumbnail_id={}", libraryId, thumbnailId);

        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("library_id", libraryId);

        // thumbnail_id 처리
        if (thumbnailId != null && !thumbnailId.trim().isEmpty()) {
            deleteData.put("thumbnail_id", thumbnailId);
        }

        Map<String, Object> result = asset3DService.deleteLibrary(deleteData);

        logger.info("3D 라이브러리 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
            return ResponseEntity.status(statusCode).body(result);
        }
    }

    /**
     * 3D 라이브러리 삭제 (Request Body 지원)
     * 
     * @param deleteData 삭제 데이터 (library_id, thumbnail_id 포함)
     * @return 삭제 결과
     */
    @DeleteMapping("/library/delete")
    public ResponseEntity<Map<String, Object>> deleteLibrary(@RequestBody Map<String, Object> deleteData) {

        logger.info("3D 라이브러리 삭제 요청: deleteData={}", deleteData);

        Map<String, Object> result = asset3DService.deleteLibrary(deleteData);

        logger.info("3D 라이브러리 삭제 응답: success={}, message={}",
                result.get("success"), result.get("message"));

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            int statusCode = (Integer) result.getOrDefault("status", 400);
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
        Map<String, Object> result = asset3DService.getDetailCommon(equipment_type);

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

        result = asset3DService.getCatalogList(searchParams);

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
        Map<String, Object> result = asset3DService.getCatalogColumnList(search_key);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

}