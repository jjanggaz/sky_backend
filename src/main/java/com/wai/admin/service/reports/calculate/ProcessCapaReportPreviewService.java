package com.wai.admin.service.reports.calculate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wai.admin.controller.reports.calculate.FileTabInfo;
import com.wai.admin.controller.reports.calculate.TabNamesResponseData;
import com.wai.admin.util.HttpUtil;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.vo.report.calculate.ProcessInfoVo;

@Service
public class ProcessCapaReportPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCapaReportPreviewService.class);

    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    
    private List<String> getFileList(Map<String, Object> searchParams) {
        logger.debug("파일 목록 조회 시작: server={}, params={}", authServerBaseUrl, searchParams);
       
        List<String> fileUrls = new ArrayList<>();

        // API URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/search/enhanced";

        logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();

        if (searchParams != null) {
            //requestMap.putAll(searchParams);
            //requestMap.put("project_id", searchParams.getOrDefault("project_id", ""));
            requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));
            requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
            requestMap.put("ccs_file_only", searchParams.getOrDefault("ccs_file_only", ""));

        }


        String requestBody = JsonUtil.objectMapToJson(requestMap);

        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        
        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null && parsedResponse.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                for (Map<String, Object> item : items) {
                    // ccs_file_info 추출 (배열)
                    if (item.containsKey("ccs_file_info") && item.get("ccs_file_info") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> ccsFileInfoList = (List<Map<String, Object>>) item.get("ccs_file_info");

                        // 각 파일 정보에서 download_url 추출
                        for (Map<String, Object> ccsFileInfo : ccsFileInfoList) {
                            if (ccsFileInfo.containsKey("download_url")) {
                                String downloadUrl = (String) ccsFileInfo.get("download_url");
                                if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
                                    fileUrls.add(downloadUrl);
                                    logger.debug("파일 URL 추가: {}", downloadUrl);
                                }
                            }
                        }
                    }
                }

                logger.info("총 {} 개의 파일 URL을 찾았습니다.", fileUrls.size());
            } else {
                logger.warn("응답에서 items를 찾을 수 없습니다.");
            }
        } else {
            // 에러 응답 처리
            logger.error("API 호출 실패: status={}", httpResult.getStatus());
            String responseBody = httpResult.getBody();
            logger.error("에러 응답: {}", responseBody);
        }

        return fileUrls;
    }

    /**
     * Download Excel file from URL
     */
    private Workbook downloadExcelFile(String url) throws IOException {
        URL fileUrl = new URL(url);
        InputStream inputStream = fileUrl.openStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        inputStream.close();
        return workbook;
    }

    /**
     * Copy entire sheet from source to target
     */
    private void copySheet(Sheet sourceSheet, Sheet targetSheet, Workbook sourceWorkbook, Workbook targetWorkbook) {
        int maxRowNum = sourceSheet.getLastRowNum();

        for (int i = 0; i <= maxRowNum; i++) {
            Row sourceRow = sourceSheet.getRow(i);
            if (sourceRow != null) {
                Row targetRow = targetSheet.createRow(i);

                int maxCellNum = sourceRow.getLastCellNum();
                if (maxCellNum > 0) {
                    for (int j = 0; j < maxCellNum; j++) {
                        Cell sourceCell = sourceRow.getCell(j);
                        if (sourceCell != null) {
                            Cell targetCell = targetRow.createCell(j);
                            copyCell(sourceCell, targetCell, sourceWorkbook, targetWorkbook);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy individual cell with value and style
     */
    private void copyCell(Cell sourceCell, Cell targetCell, Workbook sourceWorkbook, Workbook targetWorkbook) {
        switch (sourceCell.getCellType()) {
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(sourceCell)) {
                    targetCell.setCellValue(sourceCell.getDateCellValue());
                } else {
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case FORMULA:
                targetCell.setCellFormula(sourceCell.getCellFormula());
                break;
            case BLANK:
                targetCell.setBlank();
                break;
            default:
                break;
        }

        CellStyle sourceStyle = sourceCell.getCellStyle();
        if (sourceStyle != null) {
            CellStyle newStyle = targetWorkbook.createCellStyle();
            newStyle.cloneStyleFrom(sourceStyle);
            targetCell.setCellStyle(newStyle);
        }

        Row sourceRow = sourceCell.getRow();
        Row targetRow = targetCell.getRow();
        if (sourceRow != null && targetRow != null) {
            targetRow.setHeight(sourceRow.getHeight());
        }

        int columnIndex = sourceCell.getColumnIndex();
        targetCell.getSheet().setColumnWidth(columnIndex, sourceCell.getSheet().getColumnWidth(columnIndex));
    }

    /**
     * Populate DATA IN sheet from JSON URL (세로 그룹화 형식)
     * searchParams의 processId, processNo와 일치하는 데이터만 추가
     */
    private void populateDataInSheet(Sheet sheet, Workbook workbook, String jsonUrl, Map<String, Object> searchParams) throws Exception {
        logger.info("DATA IN sheet 데이터 입력 시작: {}", jsonUrl);

        // searchParams에서 processId, processNo 추출
        String targetProcessId = (String) searchParams.get("processId");
        String targetProcessNo = (String) searchParams.get("processNo");

        logger.info("타겟 processId: {}, processNo: {}", targetProcessId, targetProcessNo);

        // JSON URL에서 데이터 다운로드
        URL url = new URL(jsonUrl);
        InputStream inputStream = url.openStream();
        String jsonContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        inputStream.close();

        // JSON 파싱
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonContent);

        // values 배열 추출
        JsonNode values = root.get("values");
        if (values == null || !values.isArray() || values.size() == 0) {
            logger.warn("values 배열이 비어있거나 없습니다.");
            return;
        }

        // 스타일 설정
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle normalStyle = workbook.createCellStyle();

        int rowIndex = 0;

        // 각 process별로 세로 그룹화 형식으로 데이터 추가
        for (JsonNode valueNode : values) {
            // JSON에서 process_id, process_no 추출
            String jsonProcessId = valueNode.has("process_id") ? valueNode.get("process_id").asText() : "";
            String jsonProcessNo = valueNode.has("process_no") ? valueNode.get("process_no").asText() : "";

            // processId와 processNo가 일치하는 경우만 처리
            if (targetProcessId != null && targetProcessNo != null) {
                if (!targetProcessId.equals(jsonProcessId) || !targetProcessNo.equals(jsonProcessNo)) {
                    continue;  // 일치하지 않으면 스킵
                }
            }

            String processName = valueNode.has("process_name") ? valueNode.get("process_name").asText() : "Unknown";

            // process_name을 헤더로 추가 (검정 배경)
            Row headerRow = sheet.createRow(rowIndex++);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(processName);
            headerCell.setCellStyle(headerStyle);

            // 빈 셀도 같은 스타일 적용 (선택사항)
            Cell emptyHeaderCell = headerRow.createCell(1);
            emptyHeaderCell.setCellStyle(normalStyle);

            // 모든 필드를 세로로 나열 (process_id, process_no, process_name 제외)
            Iterator<String> fieldNames = valueNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                // process_id, process_no, process_name 제외
                if ("process_id".equals(fieldName) || "process_no".equals(fieldName) || "process_name".equals(fieldName)) {
                    continue;
                }

                Row dataRow = sheet.createRow(rowIndex++);

                // 첫 번째 열: 필드명
                Cell nameCell = dataRow.createCell(0);
                nameCell.setCellValue(fieldName);
                nameCell.setCellStyle(normalStyle);

                // 두 번째 열: 값
                Cell valueCell = dataRow.createCell(1);
                JsonNode fieldValue = valueNode.get(fieldName);
                if (fieldValue != null && !fieldValue.isNull()) {
                    if (fieldValue.isNumber()) {
                        valueCell.setCellValue(fieldValue.asDouble());
                    } else {
                        valueCell.setCellValue(fieldValue.asText());
                    }
                }
                valueCell.setCellStyle(normalStyle);
            }

            logger.info("Process 추가: {}, processId: {}, processNo: {}, 행 개수: {}", processName, jsonProcessId, jsonProcessNo, rowIndex);
        }

        // 컬럼 너비 조정
        sheet.setColumnWidth(0, 8000);  // 필드명 컬럼
        sheet.setColumnWidth(1, 6000);  // 값 컬럼

        logger.info("DATA IN sheet 데이터 입력 완료: {} rows", rowIndex);
    }

    /**
     * Extract sheet tab names from Excel files (ProcessCapaReportService 로직 적용)
     */
    public TabNamesResponseData extractTabNames(Map<String, Object> searchParams) throws Exception {
        List<FileTabInfo> tablist = new ArrayList<>();

        // searchParams에서 project_id와 unit_system_code 추출
        String projectId = (String) searchParams.get("project_id");
        String unitSystemCode = (String) searchParams.get("unit_system_code");
        String jsonUrl = (String) searchParams.get("jsonUrl");

        // process_id별 정보를 저장하는 Map
        Map<String, ProcessInfoVo> processInfoMap = new HashMap<>();

        // jsonUrl이 있으면 해당 로직 사용
        if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
            // URL에서 JSON 데이터 로드
            URL url = new URL(jsonUrl);
            InputStream inputStream = url.openStream();
            String jsonContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonContent);
            JsonNode valuesNode = root.get("values");

            // values 배열에서 process_id 중복 체크
            if (valuesNode != null && valuesNode.isArray()) {
                for (JsonNode valueNode : valuesNode) {
                    String processId2 = valueNode.has("process_id") ? valueNode.get("process_id").asText() : null;
                    String processName = valueNode.has("process_name") ? valueNode.get("process_name").asText() : "";
                    String processNo = valueNode.has("process_no") ? valueNode.get("process_no").asText() : "";

                    if (processId2 != null && !processId2.isEmpty()) {
                        if (processInfoMap.containsKey(processId2)) {
                            processInfoMap.get(processId2).addProcessNo(processNo);
                        } else {
                            processInfoMap.put(processId2, new ProcessInfoVo(processId2, processName, processNo));
                        }
                    }
                }
            }

            logger.info("총 고유 process_id 수: {}", processInfoMap.size());

            // API URL 구성
            String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/search/enhanced";

            // 각 processInfo에 대해 download_url 조회
            for (Map.Entry<String, ProcessInfoVo> entry : processInfoMap.entrySet()) {
                ProcessInfoVo processInfo = entry.getValue();
                String procId = processInfo.getProcessId();

                for (String processNo : processInfo.getProcessNoList()) {
                    String downloadUrl = findDownloadUrlByProcessId(apiUrl, procId, searchParams);
                    if (downloadUrl != null) {
                        processInfo.addDownloadUrl(downloadUrl);
                    } else {
                        processInfo.addDownloadUrl("");
                    }
                }
            }

            // 엑셀 다운로드 및 시트명 추출
            for (Map.Entry<String, ProcessInfoVo> entry : processInfoMap.entrySet()) {
                ProcessInfoVo processInfo = entry.getValue();
                String processName = processInfo.getProcessName();
                List<String> processNoList = processInfo.getProcessNoList();
                List<String> downloadUrlList = processInfo.getDownloadUrlList();
                int duplicateCount = processInfo.getDuplicateCount();

                for (int idx = 0; idx < processNoList.size(); idx++) {
                    String processNo = processNoList.get(idx);
                    String downloadUrl = (idx < downloadUrlList.size()) ? downloadUrlList.get(idx) : "";

                    if (downloadUrl == null || downloadUrl.isEmpty()) {
                        continue;
                    }

                    try {
                        // Excel 파일 다운로드
                        Workbook workbook = downloadExcelFile(downloadUrl);

                        // 시트명 리스트 생성 (_DATAIN 제외)
                        List<String> sheetNames = new ArrayList<>();
                        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                            String originalSheetName = workbook.getSheetAt(i).getSheetName();

                            // "_DATAIN"이 포함된 시트는 제외
                            // ///////////////////////////////////(2025-12-04)
                            /* 잠시 테스트 기간에만 해당 조건 주석처리
                            if (originalSheetName.toUpperCase().contains("DATAIN")) {
                                continue;
                            }
                            */
                            // ///////////////////////////////////

                            // 시트명 결정 (duplicateCount에 따라)
                            String newSheetName;
                            if (duplicateCount == 1) {
                                newSheetName = originalSheetName;
                            } else {
                                newSheetName = originalSheetName + "_" + processNo;
                            }
                            sheetNames.add(newSheetName);
                        }

                        workbook.close();

                        // 파일명 결정 (duplicateCount에 따라)
                        String fileName;
                        fileName = processName;
                        
                        /*/
                        if (duplicateCount == 1) {
                            fileName = processName;
                        } else {
                            fileName = processName + "_" + processNo;
                        }
                        */

                        // FileTabInfo 객체 생성 (processId, processNo, excelUrl, duplicateCnt 포함)
                        FileTabInfo fileTabInfo = new FileTabInfo(fileName, processInfo.getProcessId(), processNo, downloadUrl, duplicateCount, sheetNames);
                        tablist.add(fileTabInfo);

                        logger.info("파일: {}, 시트 개수: {}", fileName, sheetNames.size());

                    } catch (Exception e) {
                        logger.error("파일 처리 중 오류 발생: {}", downloadUrl, e);
                    }
                }
            }
        } else {
            // 기존 로직: jsonUrl이 없는 경우
            List<String> fileUrls = getFileList(searchParams);
            logger.info("탭명 추출을 위한 파일 URL 개수: {}", fileUrls.size());

            for (String fileUrl : fileUrls) {
                try {
                    String fileName = extractFileNameFromUrl(fileUrl);
                    Workbook workbook = downloadExcelFile(fileUrl);

                    List<String> sheetNames = new ArrayList<>();
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        String sheetName = workbook.getSheetAt(i).getSheetName();
                        if (!sheetName.toUpperCase().contains("DATAIN")) {
                            sheetNames.add(sheetName);
                        }
                    }

                    workbook.close();

                    // FileTabInfo 객체 생성 (jsonUrl 없는 경우 processId, processNo는 빈값, duplicateCnt는 1)
                    FileTabInfo fileTabInfo = new FileTabInfo(fileName, "", "", fileUrl, 1, sheetNames);
                    tablist.add(fileTabInfo);

                    logger.info("파일: {}, 시트 개수: {}", fileName, sheetNames.size());

                } catch (Exception e) {
                    logger.error("파일 처리 중 오류 발생: {}", fileUrl, e);
                }
            }
        }

        return new TabNamesResponseData(projectId, unitSystemCode, tablist);
    }

    /**
     * API를 호출하여 process_id가 일치하는 download_url 찾기
     */
    private String findDownloadUrlByProcessId(String apiUrl, String processId, Map<String, Object> searchParams) {
        try {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));
            requestMap.put("search_field", "process_code");
            requestMap.put("search_value", processId);
            requestMap.put("ccs_file_only", "true");

            String requestBody = JsonUtil.objectMapToJson(requestMap);
            HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

            if (httpResult.isSuccess()) {
                Map<String, Object> parsedResponse = JsonUtil.parseJson(httpResult.getBody());

                if (parsedResponse != null && parsedResponse.containsKey("items")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                    for (Map<String, Object> item : items) {
                        // process_info 객체에서 process_code 추출
                        String itemProcessCode = "";
                        if (item.containsKey("process_info") && item.get("process_info") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> processInfoObj = (Map<String, Object>) item.get("process_info");
                            itemProcessCode = processInfoObj.get("process_code") != null
                                ? processInfoObj.get("process_code").toString() : "";
                        }

                        if (processId.equals(itemProcessCode)) {
                            if (item.containsKey("ccs_file_info") && item.get("ccs_file_info") instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> ccsFileInfoList = (List<Map<String, Object>>) item.get("ccs_file_info");

                                if (!ccsFileInfoList.isEmpty() && ccsFileInfoList.get(0).containsKey("download_url")) {
                                    return (String) ccsFileInfoList.get(0).get("download_url");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("download_url 조회 실패 - processId: {}, error: {}", processId, e.getMessage());
        }
        return null;
    }

    /**
     * Extract file name from URL
     */
    private String extractFileNameFromUrl(String url) {
        try {
            // URL에서 마지막 슬래시 이후의 파일명 추출
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
                String fileName = url.substring(lastSlashIndex + 1);

                // 쿼리 파라미터 제거 (? 이후)
                int queryIndex = fileName.indexOf('?');
                if (queryIndex != -1) {
                    fileName = fileName.substring(0, queryIndex);
                }

                // .xlsx 확장자 제거
                if (fileName.endsWith(".xlsx")) {
                    fileName = fileName.substring(0, fileName.length() - 5);
                }

                return fileName;
            }
        } catch (Exception e) {
            logger.warn("파일명 추출 실패: {}", url, e);
        }

        return "Unknown";
    }

    /**
     * project_id를 이용해 unit_system 조회
     */
    public String getUnitSystem(String project_id) {
        logger.debug("unit_system 조회 시작: server={}, project_id={}", authServerBaseUrl, project_id);

        // API URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/project/projects/" + project_id;
        logger.debug("API URL: {}", apiUrl);

        // HttpUtil을 사용하여 GET 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.get(apiUrl, "application/json", "");

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null && parsedResponse.containsKey("unit_system")) {
                String unitSystem = (String) parsedResponse.get("unit_system");
                logger.info("조회된 unit_system: {}", unitSystem);
                return unitSystem;
            } else {
                logger.warn("응답에서 unit_system을 찾을 수 없습니다.");
                return null;
            }
        } else {
            // 에러 응답 처리
            logger.error("API 호출 실패: status={}", httpResult.getStatus());
            String responseBody = httpResult.getBody();
            logger.error("에러 응답: {}", responseBody);
            return null;
        }
    }

    /**
     * fileName으로 파일 URL 필터링
     */
    public String getFileFilter(Map<String, Object> searchParams) throws Exception {
        String fileName = (String) searchParams.get("fileName");

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("fileName이 제공되지 않았습니다.");
        }

        // .xlsx 확장자 제거 (비교용)
        String fileNameWithoutExt = fileName;
        if (fileName.endsWith(".xlsx")) {
            fileNameWithoutExt = fileName.substring(0, fileName.length() - 5);
        }

        logger.info("필터링할 파일명: {}", fileNameWithoutExt);

        // API URL 구성
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/search/enhanced";
        logger.debug("API URL: {}", apiUrl);

        // 요청 본문 구성
        Map<String, Object> requestMap = new HashMap<>();
        if (searchParams != null) {
            requestMap.put("unit_system_code", searchParams.getOrDefault("unit_system_code", ""));
            requestMap.put("search_field", searchParams.getOrDefault("search_field", ""));
            requestMap.put("ccs_file_only", searchParams.getOrDefault("ccs_file_only", "true"));
        }

        String requestBody = JsonUtil.objectMapToJson(requestMap);
        logger.debug("요청 본문: {}", requestBody);

        // HttpUtil을 사용하여 POST 요청 수행
        HttpUtil.HttpResult httpResult = HttpUtil.post(apiUrl, "application/json", requestBody);

        // 응답 처리
        if (httpResult.isSuccess()) {
            String responseBody = httpResult.getBody();
            Map<String, Object> parsedResponse = JsonUtil.parseJson(responseBody);

            if (parsedResponse != null && parsedResponse.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) parsedResponse.get("items");

                for (Map<String, Object> item : items) {
                    // ccs_file_info 추출 (배열)
                    if (item.containsKey("ccs_file_info") && item.get("ccs_file_info") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> ccsFileInfoList = (List<Map<String, Object>>) item.get("ccs_file_info");

                        // 각 파일 정보에서 original_filename과 download_url 추출
                        for (Map<String, Object> ccsFileInfo : ccsFileInfoList) {
                            if (ccsFileInfo.containsKey("original_filename") && ccsFileInfo.containsKey("download_url")) {
                                String originalFilename = (String) ccsFileInfo.get("original_filename");
                                String downloadUrl = (String) ccsFileInfo.get("download_url");

                                if (originalFilename != null && downloadUrl != null) {
                                    // .xlsx 확장자 제거 후 비교
                                    String compareFilename = originalFilename;
                                    if (originalFilename.endsWith(".xlsx")) {
                                        compareFilename = originalFilename.substring(0, originalFilename.length() - 5);
                                    }

                                    // 파일명 비교
                                    if (compareFilename.equals(fileNameWithoutExt)) {
                                        logger.info("일치하는 파일 발견: {}, URL: {}", originalFilename, downloadUrl);
                                        return downloadUrl;
                                    }
                                }
                            }
                        }
                    }
                }

                logger.warn("일치하는 파일을 찾을 수 없습니다: {}", fileName);
                throw new Exception("파일을 찾을 수 없습니다: " + fileName);
            } else {
                logger.warn("응답에서 items를 찾을 수 없습니다.");
                throw new Exception("API 응답에서 파일 정보를 찾을 수 없습니다.");
            }
        } else {
            logger.error("API 호출 실패: status={}", httpResult.getStatus());
            throw new Exception("API 호출 실패: " + httpResult.getStatus());
        }
    }

    /**
     * Excel Sheet를 HTML로 변환 (수식 재계산 포함)
     */
    public byte[] generateTabContentHtml(Map<String, Object> searchParams) throws Exception {
        String fileName = (String) searchParams.get("fileName");
        String tabName = (String) searchParams.get("tabName");
        String excelUrl = (String) searchParams.get("excelUrl");
        int duplicateCnt = (Integer) searchParams.get("duplicateCnt");
        String processNo = (String) searchParams.get("processNo");

        logger.info("HTML 생성 요청 - fileName: {}, tabName: {}", fileName, tabName);

        if (tabName == null || tabName.trim().isEmpty()) {
            throw new Exception("tabName이 제공되지 않았습니다.");
        }

        // 1. 파일 URL 가져오기
        String fileUrl = excelUrl ; //getFileFilter(searchParams);
        logger.info("다운로드할 파일 URL: {}", fileUrl);

        // 2. Excel 파일 다운로드
        Workbook workbook = downloadExcelFile(fileUrl);

        // 3. _DATAIN Sheet 찾기 및 생성하고 데이터 넣기
        String jsonUrl = (String) searchParams.get("jsonUrl");
        if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
            // 원본 _DATAIN 시트명 저장 (수식 참조 유지를 위해)
            String originalDataInSheetName = null;

            // 기존 _DATAIN 이름이 있는 sheet 찾아서 삭제 및 시트명 변경
            for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
                Sheet currentSheet = workbook.getSheetAt(i);
                String sheetName = currentSheet.getSheetName();
                logger.info("시트 Name >>>> {}", sheetName);

                if (sheetName.toUpperCase().contains("DATAIN")) {
                    // 원본 _DATAIN 시트명 저장
                    originalDataInSheetName = sheetName;
                    // _DATAIN 시트는 삭제
                    workbook.removeSheetAt(i);
                    logger.info("원본 _DATAIN 시트 삭제: {}", sheetName);
                } else if (duplicateCnt > 1) {
                    // duplicateCnt가 1보다 큰 경우, 시트명 끝에 "_" + processNo 추가
                    String newSheetName = sheetName + "_" + processNo;
                    workbook.setSheetName(i, newSheetName);
                    logger.info("시트명 변경: {} -> {}", sheetName, newSheetName);
                }
            }

            // _DATAIN sheet 생성 (원본 시트명 유지하여 수식 참조 유지)
            String dataInSheetName;
            if (originalDataInSheetName != null) {
                // 원본 시트명이 있으면 그대로 사용 (수식 참조 유지)
                dataInSheetName = originalDataInSheetName;
            } else {
                // 원본 시트명이 없으면 기본 이름 생성
                dataInSheetName = fileName + "_DATAIN";
            }
            // {공정명}DATA IN sheet 생성
            Sheet dataInSheet = workbook.createSheet(dataInSheetName);
            // {공정명}DATA IN sheet 에 데이터 입력
            populateDataInSheet(dataInSheet, workbook, jsonUrl, searchParams);
            logger.info("DATA IN sheet 추가 완료 >>>> {}", dataInSheetName);
        } else {
            logger.warn("jsonUrl이 제공되지 않아 DATA IN sheet를 추가하지 않습니다.");
        }

        // 4. 수식 강제 재계산 (매우 중요!)
        workbook.setForceFormulaRecalculation(true);
        logger.info("수식 재계산 설정 완료");

        // 5. Excel 파일을 임시로 저장했다가 다시 로드 (수식 재평가를 위해)
        ByteArrayOutputStream tempBaos = new ByteArrayOutputStream();
        workbook.write(tempBaos);
        workbook.close();

        // 임시 저장한 Excel을 다시 로드
        ByteArrayInputStream tempBais = new ByteArrayInputStream(tempBaos.toByteArray());
        workbook = new XSSFWorkbook(tempBais);
        tempBaos.close();
        tempBais.close();

        logger.info("Excel 파일 재로드 완료 (수식 재평가)");

        // 6. FormulaEvaluator를 사용하여 모든 수식을 명시적으로 재평가
        // 외부 워크북 참조가 있는 경우 실패할 수 있으므로 try-catch로 처리
        try {
            org.apache.poi.ss.usermodel.FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.setIgnoreMissingWorkbooks(true);
            evaluator.evaluateAll();
            logger.info("모든 수식 재평가 완료");
        } catch (Exception e) {
            logger.warn("수식 재평가 중 오류 발생 (외부 워크북 참조): {}. 캐시된 값 사용.", e.getMessage());
        }

        // 7. tabName과 동일한 Sheet 찾기
        Sheet sheet = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet currentSheet = workbook.getSheetAt(i);
            if (currentSheet.getSheetName().equals(tabName)) {
                sheet = currentSheet;
                break;
            }
        }

        if (sheet == null) {
            workbook.close();
            throw new Exception("시트를 찾을 수 없습니다: " + tabName);
        }

        logger.info("시트 발견: {}, 행 개수: {}", sheet.getSheetName(), sheet.getLastRowNum() + 1);

        // 8. Sheet를 HTML로 변환 (Excel 스타일 완벽 재현)
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(escapeHtml(tabName)).append("</title>\n");
        html.append("    <style>\n");
        html.append("        html, body { \n");
        html.append("            font-family: 'Malgun Gothic', 'Arial', sans-serif; \n");
        html.append("            margin: 0; \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        #app, #app > main { \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        table { border-collapse: collapse; background-color: white; table-layout: fixed; }\n");
        html.append("        td { border: 1px solid #d0d0d0; padding: 2px 4px; overflow: hidden; white-space: pre-wrap; word-wrap: break-word; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"excel-container\">\n");
        html.append("    <table class=\"tbl-preview\">\n");

        // 9. 컬럼 너비 설정
        html.append("        <colgroup>\n");
        int maxCol = 0;
        for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row != null && row.getLastCellNum() > maxCol) {
                maxCol = row.getLastCellNum();
            }
        }

        for (int colIdx = 0; colIdx < maxCol; colIdx++) {
            int columnWidth = sheet.getColumnWidth(colIdx);
            // Excel 단위를 픽셀로 변환 (1 Excel unit ≈ 1/256 문자, 1문자 ≈ 7픽셀)
            int widthPx = (int) (columnWidth / 256.0 * 7);
            html.append("            <col style=\"width: ").append(widthPx).append("px;\">\n");
        }
        html.append("        </colgroup>\n");

        // 10. 병합 영역 정보 수집
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

        // 병합 영역 맵 생성: key = "rowIdx,colIdx", value = CellRangeAddress
        // 병합의 시작 셀인지, 병합에 포함된 셀인지 구분
        Map<String, CellRangeAddress> mergedStartCells = new HashMap<>();  // 병합 시작 셀
        Map<String, CellRangeAddress> mergedIncludedCells = new HashMap<>();  // 병합에 포함된 셀 (시작 제외)

        for (CellRangeAddress region : mergedRegions) {
            int firstRow = region.getFirstRow();
            int lastRow = region.getLastRow();
            int firstCol = region.getFirstColumn();
            int lastCol = region.getLastColumn();

            for (int r = firstRow; r <= lastRow; r++) {
                for (int c = firstCol; c <= lastCol; c++) {
                    String key = r + "," + c;
                    if (r == firstRow && c == firstCol) {
                        // 병합 시작 셀
                        mergedStartCells.put(key, region);
                    } else {
                        // 병합에 포함된 셀 (렌더링 스킵 대상)
                        mergedIncludedCells.put(key, region);
                    }
                }
            }
        }

        logger.info("병합 영역 수: {}", mergedRegions.size());

        // 11. 행 순회하며 HTML 테이블 생성 (스타일 및 병합 처리 포함)
        for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                html.append("        <tr style=\"height: 15px;\"><td colspan=\"").append(maxCol).append("\"></td></tr>\n");
                continue;
            }

            // 행 높이 설정
            float heightInPoints = row.getHeightInPoints();
            int heightPx = (int) (heightInPoints * 4 / 3); // 포인트를 픽셀로 변환
            html.append("        <tr style=\"height: ").append(heightPx).append("px;\">\n");

            // 첫 번째 셀에 값이 있고 뒤의 모든 셀이 비어있으면 병합
            int firstCellDynamicColSpan = 1;
            Cell firstCell = row.getCell(0);
            if (firstCell != null) {
                String firstCellValue = getCellValueAsString(firstCell);
                if (firstCellValue != null && !firstCellValue.trim().isEmpty()) {
                    // 첫 번째 셀 뒤의 연속된 빈 셀 개수 계산
                    int emptyCount = 0;
                    boolean allEmpty = true;
                    for (int nextCol = 1; nextCol < maxCol; nextCol++) {
                        String nextKey = rowIdx + "," + nextCol;
                        // 병합 영역에 포함된 셀이면 중단
                        if (mergedIncludedCells.containsKey(nextKey) || mergedStartCells.containsKey(nextKey)) {
                            allEmpty = false;
                            break;
                        }
                        Cell nextCell = row.getCell(nextCol);
                        if (nextCell == null || getCellValueAsString(nextCell).trim().isEmpty()) {
                            emptyCount++;
                        } else {
                            allEmpty = false;
                            break;
                        }
                    }
                    // 뒤의 모든 셀이 비어있는 경우에만 병합
                    if (allEmpty && emptyCount > 0) {
                        firstCellDynamicColSpan = 1 + emptyCount;
                    }
                }
            }

            // 동적 병합으로 스킵할 셀 인덱스 Set
            java.util.Set<Integer> skipDynamicCells = new java.util.HashSet<>();
            if (firstCellDynamicColSpan > 1) {
                for (int i = 1; i < firstCellDynamicColSpan; i++) {
                    skipDynamicCells.add(i);
                }
            }

            // 각 셀을 개별적으로 렌더링 (병합 처리 포함)
            for (int colIdx = 0; colIdx < maxCol; colIdx++) {
                String key = rowIdx + "," + colIdx;

                // 병합에 포함된 셀 (시작 셀 제외)은 스킵
                if (mergedIncludedCells.containsKey(key)) {
                    continue;
                }

                // 동적 병합으로 스킵할 셀
                if (skipDynamicCells.contains(colIdx)) {
                    continue;
                }

                Cell cell = row.getCell(colIdx);
                String cellValue = "";
                String cellStyle = "";

                if (cell != null) {
                    cellValue = getCellValueAsString(cell);
                    CellStyle style = cell.getCellStyle();
                    cellStyle = generateCellStyle(style, workbook);
                }

                cellValue = escapeHtml(cellValue);

                // 병합 시작 셀인 경우 rowspan, colspan 추가
                if (mergedStartCells.containsKey(key)) {
                    CellRangeAddress region = mergedStartCells.get(key);
                    int rowSpan = region.getLastRow() - region.getFirstRow() + 1;
                    int colSpan = region.getLastColumn() - region.getFirstColumn() + 1;

                    html.append("            <td");
                    if (rowSpan > 1) {
                        html.append(" rowspan=\"").append(rowSpan).append("\"");
                    }
                    if (colSpan > 1) {
                        html.append(" colspan=\"").append(colSpan).append("\"");
                    }
                    html.append(" style=\"").append(cellStyle).append("\">")
                        .append(cellValue.isEmpty() ? "&nbsp;" : cellValue)
                        .append("</td>\n");
                } else if (colIdx == 0 && firstCellDynamicColSpan > 1) {
                    // 첫 번째 셀 동적 병합 (테두리 없이 텍스트만 표시)

                    // 폰트 스타일 추출 (font-family, font-size, font-weight, font-style, color, background-color)
                    StringBuilder fontStyle = new StringBuilder();
                    java.util.regex.Matcher fontFamilyMatcher = java.util.regex.Pattern.compile("font-family:\\s*([^;]+);?").matcher(cellStyle);
                    if (fontFamilyMatcher.find()) {
                        fontStyle.append("font-family: ").append(fontFamilyMatcher.group(1).trim()).append("; ");
                    }
                    java.util.regex.Matcher fontSizeMatcher = java.util.regex.Pattern.compile("font-size:\\s*([^;]+);?").matcher(cellStyle);
                    if (fontSizeMatcher.find()) {
                        fontStyle.append("font-size: ").append(fontSizeMatcher.group(1).trim()).append("; ");
                    }
                    java.util.regex.Matcher fontWeightMatcher = java.util.regex.Pattern.compile("font-weight:\\s*([^;]+);?").matcher(cellStyle);
                    if (fontWeightMatcher.find()) {
                        fontStyle.append("font-weight: ").append(fontWeightMatcher.group(1).trim()).append("; ");
                    }
                    java.util.regex.Matcher fontStyleMatcher = java.util.regex.Pattern.compile("font-style:\\s*([^;]+);?").matcher(cellStyle);
                    if (fontStyleMatcher.find()) {
                        fontStyle.append("font-style: ").append(fontStyleMatcher.group(1).trim()).append("; ");
                    }
                    java.util.regex.Matcher colorMatcher = java.util.regex.Pattern.compile("(?<!background-)color:\\s*([^;]+);?").matcher(cellStyle);
                    if (colorMatcher.find()) {
                        fontStyle.append("color: ").append(colorMatcher.group(1).trim()).append("; ");
                    }
                    // 배경색 추출
                    java.util.regex.Matcher bgColorMatcher = java.util.regex.Pattern.compile("background-color:\\s*([^;]+);?").matcher(cellStyle);
                    if (bgColorMatcher.find()) {
                        fontStyle.append("background-color: ").append(bgColorMatcher.group(1).trim()).append("; ");
                    }

                    // 병합된 td (테두리 없음)
                    html.append("            <td colspan=\"").append(firstCellDynamicColSpan).append("\" style=\"padding: 2px 4px; border: none; vertical-align: middle; ");
                    html.append(fontStyle);
                    html.append("\">")
                        .append(cellValue.isEmpty() ? "&nbsp;" : cellValue)
                        .append("</td>\n");
                } else {
                    // 일반 셀
                    html.append("            <td style=\"").append(cellStyle).append("\">")
                        .append(cellValue.isEmpty() ? "&nbsp;" : cellValue)
                        .append("</td>\n");
                }
            }

            html.append("        </tr>\n");
        }

        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        workbook.close();

        // 11. HTML을 byte[]로 변환
        byte[] htmlBytes = html.toString().getBytes(StandardCharsets.UTF_8);
        logger.info("HTML 생성 완료, 크기: {} bytes", htmlBytes.length);

        return htmlBytes;
    }

    /**
     * Excel Sheet를 DATA IN이 포함된 Excel 파일로 반환
     */
    public byte[] generateTabContentExcel(Map<String, Object> searchParams) throws Exception {
        String fileName = (String) searchParams.get("fileName");
        String tabName = (String) searchParams.get("tabName");

        logger.info("Excel 생성 요청 - fileName: {}, tabName: {}", fileName, tabName);

        if (tabName == null || tabName.trim().isEmpty()) {
            throw new Exception("tabName이 제공되지 않았습니다.");
        }

        System.out.println(">>>> url list in");
        // 1. 파일 URL 가져오기
        String fileUrl = getFileFilter(searchParams);
        logger.info("download file URL>>> {}", fileUrl);
        System.out.println(">>>> url list out");

        // 2. Excel 파일 다운로드
        Workbook workbook = downloadExcelFile(fileUrl);

        // 3. DATA IN Sheet 찾아서 삭제 후 재생성
        String jsonUrl = (String) searchParams.get("jsonUrl");
        if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
            // 기존 DATA IN sheet 찾아서 삭제
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet currentSheet = workbook.getSheetAt(i);
                if ("DATA IN".equalsIgnoreCase(currentSheet.getSheetName())) {
                    workbook.removeSheetAt(i);
                    break;
                }
            }

            // DATA IN sheet 생성 및 데이터 입력
            Sheet dataInSheet = workbook.createSheet("DATA IN");
            populateDataInSheet(dataInSheet, workbook, jsonUrl, searchParams);
            logger.info("DATA IN sheet 추가 완료");
        } else {
            logger.warn("jsonUrl이 제공되지 않아 DATA IN sheet를 추가하지 않습니다.");
        }

        // 4. Workbook을 byte[]로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        byte[] excelBytes = baos.toByteArray();
        baos.close();

        logger.info("Excel 파일 생성 완료, 크기: {} bytes", excelBytes.length);

        return excelBytes;
    }

    /**
     * 셀 값을 문자열로 추출
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // 정수인 경우 소수점 제거
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    switch (cell.getCachedFormulaResultType()) {
                        case NUMERIC:
                            double numValue = cell.getNumericCellValue();
                            if (numValue == Math.floor(numValue)) {
                                return String.valueOf((long) numValue);
                            }
                            return String.valueOf(numValue);
                        case STRING:
                            return cell.getStringCellValue();
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        case ERROR:
                            return "#ERROR";
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    logger.debug("수식 셀 값 추출 오류 (row={}, col={}): {}",
                        cell.getRowIndex(), cell.getColumnIndex(), e.getMessage());
                    return "#ERROR";
                }
            case ERROR:
                return "#ERROR";
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * Excel 셀 스타일을 CSS 스타일 문자열로 변환
     */
    private String generateCellStyle(CellStyle cellStyle, Workbook workbook) {
        if (cellStyle == null) {
            return "";
        }

        StringBuilder style = new StringBuilder();

        // 1. 폰트 스타일
        Font font = workbook.getFontAt(cellStyle.getFontIndex());
        if (font != null) {
            // 폰트 패밀리
            String fontName = font.getFontName();
            style.append("font-family: '").append(fontName).append("', Arial, sans-serif; ");

            // 폰트 크기 (포인트를 픽셀로 변환)
            short fontSize = font.getFontHeightInPoints();
            style.append("font-size: ").append(fontSize).append("pt; ");

            // 볼드
            if (font.getBold()) {
                style.append("font-weight: bold; ");
            }

            // 이탤릭
            if (font.getItalic()) {
                style.append("font-style: italic; ");
            }

            // 밑줄
            if (font.getUnderline() != Font.U_NONE) {
                style.append("text-decoration: underline; ");
            }

            // 취소선
            if (font.getStrikeout()) {
                style.append("text-decoration: line-through; ");
            }

            // 폰트 색상
            if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont) {
                org.apache.poi.xssf.usermodel.XSSFFont xssfFont = (org.apache.poi.xssf.usermodel.XSSFFont) font;
                org.apache.poi.xssf.usermodel.XSSFColor fontColor = xssfFont.getXSSFColor();
                if (fontColor != null) {
                    String colorHex = getColorHex(fontColor);
                    if (colorHex != null) {
                        style.append("color: ").append(colorHex).append("; ");
                    }
                }
            }
        }

        // 2. 배경색
        if (cellStyle.getFillPattern() == FillPatternType.SOLID_FOREGROUND) {
            String bgColorHex = null;

            // XSSFCellStyle인 경우 XSSFColor로 먼저 시도
            if (cellStyle instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle) {
                org.apache.poi.xssf.usermodel.XSSFCellStyle xssfCellStyle =
                    (org.apache.poi.xssf.usermodel.XSSFCellStyle) cellStyle;
                org.apache.poi.xssf.usermodel.XSSFColor bgColor = xssfCellStyle.getFillForegroundColorColor();
                if (bgColor != null) {
                    bgColorHex = getColorHex(bgColor);
                }
            }

            // XSSFColor로 색상을 못 가져온 경우 인덱스 색상으로 시도
            if (bgColorHex == null) {
                short colorIndex = cellStyle.getFillForegroundColor();
                if (colorIndex != IndexedColors.AUTOMATIC.getIndex()) {
                    bgColorHex = getIndexedColorHex(colorIndex);
                }
            }

            if (bgColorHex != null && !bgColorHex.equals("#FFFFFF")) { // 흰색이 아닌 경우만
                style.append("background-color: ").append(bgColorHex).append("; ");
            }
        }

        // 3. 텍스트 정렬
        switch (cellStyle.getAlignment()) {
            case LEFT:
                style.append("text-align: left; ");
                break;
            case CENTER:
                style.append("text-align: center; ");
                break;
            case RIGHT:
                style.append("text-align: right; ");
                break;
            case JUSTIFY:
                style.append("text-align: justify; ");
                break;
            default:
                break;
        }

        // 4. 수직 정렬 (기본값: 가운데 정렬)
        switch (cellStyle.getVerticalAlignment()) {
            case TOP:
                style.append("vertical-align: top; ");
                break;
            case BOTTOM:
                style.append("vertical-align: bottom; ");
                break;
            case CENTER:
            default:
                style.append("vertical-align: middle; ");
                break;
        }

        // 5. 테두리
        String borderTop = getBorderStyle(cellStyle.getBorderTop());
        String borderRight = getBorderStyle(cellStyle.getBorderRight());
        String borderBottom = getBorderStyle(cellStyle.getBorderBottom());
        String borderLeft = getBorderStyle(cellStyle.getBorderLeft());

        if (!borderTop.isEmpty()) {
            style.append("border-top: ").append(borderTop).append("; ");
        }
        if (!borderRight.isEmpty()) {
            style.append("border-right: ").append(borderRight).append("; ");
        }
        if (!borderBottom.isEmpty()) {
            style.append("border-bottom: ").append(borderBottom).append("; ");
        }
        if (!borderLeft.isEmpty()) {
            style.append("border-left: ").append(borderLeft).append("; ");
        }

        // 6. 텍스트 줄바꿈
        if (cellStyle.getWrapText()) {
            style.append("white-space: pre-wrap; ");
        }

        return style.toString();
    }

    /**
     * XSSFColor를 HEX 색상 문자열로 변환
     */
    private String getColorHex(org.apache.poi.xssf.usermodel.XSSFColor color) {
        if (color == null) {
            return null;
        }

        byte[] rgb = color.getRGB();
        if (rgb == null || rgb.length < 3) {
            return null;
        }

        return String.format("#%02X%02X%02X",
            rgb[0] & 0xFF,
            rgb[1] & 0xFF,
            rgb[2] & 0xFF);
    }

    /**
     * IndexedColors 인덱스를 HEX 색상 문자열로 변환
     */
    private String getIndexedColorHex(short colorIndex) {
        // 주요 인덱스 색상 매핑
        switch (colorIndex) {
            case 8:  // BLACK
                return "#000000";
            case 9:  // WHITE
                return "#FFFFFF";
            case 10: // RED
                return "#FF0000";
            case 11: // BRIGHT_GREEN
                return "#00FF00";
            case 12: // BLUE
                return "#0000FF";
            case 13: // YELLOW
                return "#FFFF00";
            case 14: // PINK
                return "#FF00FF";
            case 15: // TURQUOISE
                return "#00FFFF";
            case 16: // DARK_RED
                return "#800000";
            case 17: // GREEN
                return "#008000";
            case 18: // DARK_BLUE
                return "#000080";
            case 19: // DARK_YELLOW
                return "#808000";
            case 20: // VIOLET
                return "#800080";
            case 21: // TEAL
                return "#008080";
            case 22: // GREY_25_PERCENT
                return "#C0C0C0";
            case 23: // GREY_50_PERCENT
                return "#808080";
            case 64: // AUTOMATIC (시스템 기본값)
                return null;
            default:
                return null;
        }
    }

    /**
     * Excel 테두리 스타일을 CSS border 스타일로 변환
     */
    private String getBorderStyle(org.apache.poi.ss.usermodel.BorderStyle borderStyle) {
        if (borderStyle == null || borderStyle == org.apache.poi.ss.usermodel.BorderStyle.NONE) {
            return "";
        }

        switch (borderStyle) {
            case THIN:
                return "1px solid #000000";
            case MEDIUM:
                return "2px solid #000000";
            case THICK:
                return "3px solid #000000";
            case DASHED:
                return "1px dashed #000000";
            case DOTTED:
                return "1px dotted #000000";
            case DOUBLE:
                return "3px double #000000";
            default:
                return "1px solid #000000";
        }
    }

    /**
     * HTML 특수문자 이스케이프
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
