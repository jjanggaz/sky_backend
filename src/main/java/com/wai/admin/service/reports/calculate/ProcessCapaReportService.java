package com.wai.admin.service.reports.calculate;

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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wai.admin.util.HttpUtil;
import com.wai.admin.util.JsonUtil;
import com.wai.admin.vo.report.calculate.ProcessInfoVo;

@Service
public class ProcessCapaReportService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCapaReportService.class);

    @Value("${auth.server.base-url}")
    private String authServerBaseUrl;

    /**
     * Generate integrated Excel file with process capacity data
     */
    public byte[] generateIntegrationXlsx(Map<String, Object> searchParams) throws Exception {
        System.out.println("searchParams : " + searchParams);

        // 0. jsonUrl 호출하여 데이터 가져오기
        String jsonUrl = (String) searchParams.get("jsonUrl");
        String jsonContent = null;
        JsonNode valuesNode = null;
        ObjectMapper mapper = new ObjectMapper();

        // process_id별 정보를 저장하는 Map (key: process_id, value: ProcessInfoVo)
        Map<String, ProcessInfoVo> processInfoMap = new HashMap<>();

        if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
            // URL에서 JSON 데이터 로드
            java.net.URL url = new java.net.URL(jsonUrl);
            InputStream inputStream = url.openStream();
            jsonContent = StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
            inputStream.close();

            // JSON 파싱
            JsonNode root = mapper.readTree(jsonContent);
            valuesNode = root.get("values");

            // 1. values 배열에서 process_id 중복 체크
            if (valuesNode != null && valuesNode.isArray()) {
                for (JsonNode valueNode : valuesNode) {
                    String processId = valueNode.has("process_id") ? valueNode.get("process_id").asText() : null;
                    String processName = valueNode.has("process_name") ? valueNode.get("process_name").asText() : "";
                    String processNo = valueNode.has("process_no") ? valueNode.get("process_no").asText() : "";

                    if (processId != null && !processId.isEmpty()) {
                        // 중복 체크: 이미 존재하면 processNo 추가, 없으면 새로 추가
                        if (processInfoMap.containsKey(processId)) {
                            processInfoMap.get(processId).addProcessNo(processNo);
                            logger.info("중복 발견 - process_id: {}, processNo: {}, 현재 중복 수: {}",
                                processId, processNo, processInfoMap.get(processId).getDuplicateCount());
                        } else {
                            processInfoMap.put(processId, new ProcessInfoVo(processId, processName, processNo));
                        }
                    }
                }
            }

            logger.info("=== Process 정보 저장 결과 ===");
            logger.info("총 고유 process_id 수: {}", processInfoMap.size());
        }

        // ============================================================
        // 1차: 중첩 for문으로 processInfo에 download_url 저장
        // ============================================================
        String apiUrl = authServerBaseUrl + "/api/v1/process/process_masters/search/enhanced";

        for (Map.Entry<String, ProcessInfoVo> entry : processInfoMap.entrySet()) {
            ProcessInfoVo processInfo = entry.getValue();
            String processId = processInfo.getProcessId();

            for (String processNo : processInfo.getProcessNoList()) {
                // API 호출하여 process_code가 일치하는 download_url 찾기
                String downloadUrl = findDownloadUrlByProcessIdAndNo(apiUrl, processId, processNo, searchParams);
                if (downloadUrl != null) {
                    processInfo.addDownloadUrl(downloadUrl);
                    logger.info("download_url 저장 - processId: {}, processNo: {}, url: {}", processId, processNo, downloadUrl);
                } else {
                    processInfo.addDownloadUrl("");  // 빈 값이라도 인덱스 유지를 위해 추가
                    logger.warn("download_url을 찾지 못함 - processId: {}, processNo: {}", processId, processNo);
                }
            }
        }

        // 결과 로그 출력
        for (Map.Entry<String, ProcessInfoVo> entry : processInfoMap.entrySet()) {
            logger.info("최종 Process 정보: {}", entry.getValue());
        }

        // ============================================================
        // 2차: 중첩 for문으로 엑셀 다운로드 및 sheet명 수정, 합치기
        // ============================================================
        Workbook mergedWorkbook = new XSSFWorkbook();

        for (Map.Entry<String, ProcessInfoVo> entry : processInfoMap.entrySet()) {
            ProcessInfoVo processInfo = entry.getValue();
            String processId = processInfo.getProcessId();
            String processName = processInfo.getProcessName();
            List<String> processNoList = processInfo.getProcessNoList();
            List<String> downloadUrlList = processInfo.getDownloadUrlList();
            int duplicateCount = processInfo.getDuplicateCount();

            for (int idx = 0; idx < processNoList.size(); idx++) {
                String processNo = processNoList.get(idx);
                String downloadUrl = (idx < downloadUrlList.size()) ? downloadUrlList.get(idx) : "";

                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    logger.warn("download_url이 비어있어 건너뜁니다 - processId: {}, processNo: {}", processId, processNo);
                    continue;
                }

                try {
                    // 엑셀 파일 다운로드
                    Workbook sourceWorkbook = downloadExcelFile(downloadUrl);
                    logger.info("엑셀 다운로드 완료 - processId: {}, processNo: {}", processId, processNo);

                    // 스타일/폰트 캐시: 각 소스 워크북별로 새로 생성 (서로 다른 워크북의 스타일 인덱스 충돌 방지)
                    Map<Integer, CellStyle> styleCache = new HashMap<>();
                    Map<Integer, Font> fontCache = new HashMap<>();

                    // 원본 DATAIN 시트명 찾기 (수식 참조 치환용)
                    String oldDataInSheetName = null;
                    for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
                        String sheetName = sourceWorkbook.getSheetAt(i).getSheetName();
                        if (sheetName.toUpperCase().contains("DATAIN")) {
                            oldDataInSheetName = sheetName;
                            break;
                        }
                    }

                    // 새 DATAIN 시트명 결정
                    String newDataInSheetName;
                    if (duplicateCount == 1) {
                        newDataInSheetName = processName + "_DATAIN";
                    } else {
                        newDataInSheetName = processName + "_" + processNo + "_DATAIN";
                    }

                    logger.info("DATAIN 시트명 치환: {} -> {}", oldDataInSheetName, newDataInSheetName);

                    // sheet 순회
                    for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
                        Sheet sourceSheet = sourceWorkbook.getSheetAt(i);
                        String originalSheetName = sourceSheet.getSheetName();

                        // "DATAIN"이 포함된 시트는 건너뛰기 (나중에 새로 생성)
                        if (originalSheetName.toUpperCase().contains("DATAIN")) {
                            continue;
                        }

                        String newSheetName;
                        if (duplicateCount == 1) {
                            // 조건1: duplicateCount가 1인 경우 - sheet명 그대로 유지
                            newSheetName = originalSheetName;
                        } else {
                            // 조건2: duplicateCount가 1 이상인 경우 - sheet명 뒤에 "_" + processNo 추가
                            newSheetName = originalSheetName + "_" + processNo;
                        }

                        // 중복 시트명 처리
                        newSheetName = getUniqueSheetName(mergedWorkbook, newSheetName);

                        // 시트 복사 (DATAIN 시트 참조 치환 포함, 스타일/폰트 캐싱 적용)
                        Sheet mergedSheet = mergedWorkbook.createSheet(newSheetName);
                        copySheet(sourceSheet, mergedSheet, sourceWorkbook, mergedWorkbook,
                                  oldDataInSheetName, newDataInSheetName, styleCache, fontCache);
                        logger.info("시트 복사 완료: {} -> {}", originalSheetName, newSheetName);
                    }

                    sourceWorkbook.close();

                    // _DATAIN 시트 생성 및 데이터 입력
                    String dataInSheetName = getUniqueSheetName(mergedWorkbook, newDataInSheetName);

                    Sheet dataInSheet = mergedWorkbook.createSheet(dataInSheetName);

                    // valuesNode에서 일치하는 데이터 찾아 입력
                    populateDataInSheetFromValues(dataInSheet, mergedWorkbook, valuesNode, processId, processNo);
                    logger.info("DATAIN 시트 생성 완료: {}", dataInSheetName);

                } catch (Exception e) {
                    logger.error("엑셀 처리 중 오류 - processId: {}, processNo: {}, error: {}",
                        processId, processNo, e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // 최종 엑셀 파일 생성
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mergedWorkbook.write(baos);
        mergedWorkbook.close();
        byte[] reportBytes = baos.toByteArray();
        baos.close();

        logger.info("최종 엑셀 파일 생성 완료, 크기: {} bytes", reportBytes.length);
        return reportBytes;
    }

    /**
     * API를 호출하여 process_id와 process_no(process_code)가 일치하는 download_url 찾기
     */
    private String findDownloadUrlByProcessIdAndNo(String apiUrl, String processId, String processNo, Map<String, Object> searchParams) {
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

                        // process_code가 일치하는 경우
                        if (processId.equals(itemProcessCode)) {
                            logger.info("process_code 일치함 - itemProcessCode: {}, processId: {}", itemProcessCode, processId);

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
            logger.error("download_url 조회 실패 - processId: {}, processNo: {}, error: {}", processId, processNo, e.getMessage());
        }
        return null;
    }

    /**
     * 중복되지 않는 고유한 시트명 생성
     */
    private String getUniqueSheetName(Workbook workbook, String sheetName) {
        String uniqueName = sheetName;
        int suffix = 1;

        // 시트명 최대 길이 제한 (Excel 제한: 31자)
        if (uniqueName.length() > 31) {
            uniqueName = uniqueName.substring(0, 31);
        }

        while (workbook.getSheet(uniqueName) != null) {
            String suffixStr = "_" + suffix;
            if (sheetName.length() + suffixStr.length() > 31) {
                uniqueName = sheetName.substring(0, 31 - suffixStr.length()) + suffixStr;
            } else {
                uniqueName = sheetName + suffixStr;
            }
            suffix++;
        }
        return uniqueName;
    }

    /**
     * values 배열에서 process_id와 process_no가 일치하는 데이터를 찾아 DATAIN 시트에 입력
     */
    private void populateDataInSheetFromValues(Sheet sheet, Workbook workbook, JsonNode valuesNode,
                                                String targetProcessId, String targetProcessNo) {
        if (valuesNode == null || !valuesNode.isArray()) {
            return;
        }

        // 헤더 스타일 설정 (노란색 배경, 흰색 글자)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 일반 스타일 설정
        CellStyle normalStyle = workbook.createCellStyle();

        int rowIndex = 0;

        // values 배열에서 일치하는 데이터 찾기
        for (JsonNode valueNode : valuesNode) {
            String processId = valueNode.has("process_id") ? valueNode.get("process_id").asText() : "";
            String processNo = valueNode.has("process_no") ? valueNode.get("process_no").asText() : "";

            // process_id와 process_no가 일치하는 경우
            if (targetProcessId.equals(processId) && targetProcessNo.equals(processNo)) {
                // 첫 번째 라인: process_name 값을 A열에 입력 (배경색 검은색, 글자색 흰색)
                String processName = valueNode.has("process_name") ? valueNode.get("process_name").asText() : "";
                Row firstRow = sheet.createRow(rowIndex++);
                Cell processNameCell = firstRow.createCell(0);
                processNameCell.setCellValue(processName);
                processNameCell.setCellStyle(headerStyle);

                logger.debug("DATAIN 시트 첫 번째 라인에 process_name 입력: {}", processName);

                // 모든 필드를 순회하며 데이터 입력 (process_name, process_id, process_no 제외)
                Iterator<String> fieldNames = valueNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();

                    // process_name, process_id, process_no는 제외
                    if ("process_name".equals(fieldName) || "process_id".equals(fieldName) || "process_no".equals(fieldName)) {
                        continue;
                    }

                    Row dataRow = sheet.createRow(rowIndex++);

                    // A열: 속성명
                    Cell nameCell = dataRow.createCell(0);
                    nameCell.setCellValue(fieldName);
                    nameCell.setCellStyle(normalStyle);

                    // B열: 속성값
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
                break;  // 일치하는 데이터를 찾았으므로 종료
            }
        }

        // 컬럼 너비 조정
        sheet.setColumnWidth(0, 8000);  // A열: 속성명
        sheet.setColumnWidth(1, 6000);  // B열: 속성값

        logger.info("DATAIN 시트 데이터 입력 완료 - processId: {}, processNo: {}, 행 수: {}",
            targetProcessId, targetProcessNo, rowIndex);
    }

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
     * Copy entire sheet from source to target (DATAIN 시트 참조 치환 포함, 스타일/폰트 캐싱 적용)
     */
    private void copySheet(Sheet sourceSheet, Sheet targetSheet, Workbook sourceWorkbook, Workbook targetWorkbook,
                           String oldDataInSheetName, String newDataInSheetName,
                           Map<Integer, CellStyle> styleCache, Map<Integer, Font> fontCache) {
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
                            copyCell(sourceCell, targetCell, sourceWorkbook, targetWorkbook,
                                     oldDataInSheetName, newDataInSheetName, styleCache, fontCache);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy individual cell with value and style (DATAIN 시트 참조 치환 포함, 스타일/폰트 캐싱 적용)
     */
    private void copyCell(Cell sourceCell, Cell targetCell, Workbook sourceWorkbook, Workbook targetWorkbook,
                          String oldDataInSheetName, String newDataInSheetName,
                          Map<Integer, CellStyle> styleCache, Map<Integer, Font> fontCache) {
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
                String formula = sourceCell.getCellFormula();
                // DATAIN 시트 참조를 새 시트명으로 치환
                if (oldDataInSheetName != null && newDataInSheetName != null &&
                    !oldDataInSheetName.equals(newDataInSheetName)) {
                    // 'OldSheetName'! 형태와 OldSheetName! 형태 모두 치환
                    formula = formula.replace("'" + oldDataInSheetName + "'!", "'" + newDataInSheetName + "'!");
                    formula = formula.replace(oldDataInSheetName + "!", newDataInSheetName + "!");
                    logger.debug("수식 치환: {} -> {}", sourceCell.getCellFormula(), formula);
                }
                try {
                    targetCell.setCellFormula(formula);
                } catch (Exception e) {
                    logger.warn("수식 설정 실패, 원본 수식 사용: {}", e.getMessage());
                    targetCell.setCellFormula(sourceCell.getCellFormula());
                }
                break;
            case BLANK:
                targetCell.setBlank();
                break;
            default:
                break;
        }

        // 스타일 및 폰트 캐싱: 동일한 스타일/폰트 인덱스는 재사용
        CellStyle sourceStyle = sourceCell.getCellStyle();
        if (sourceStyle != null) {
            int styleIndex = sourceStyle.getIndex();
            CellStyle cachedStyle = styleCache.get(styleIndex);
            if (cachedStyle == null) {
                cachedStyle = targetWorkbook.createCellStyle();

                // 폰트 명시적 복사 (다른 워크북 간 폰트 복사 문제 해결)
                int sourceFontIndex = sourceStyle.getFontIndex();
                Font cachedFont = fontCache.get(sourceFontIndex);
                if (cachedFont == null) {
                    Font sourceFont = sourceWorkbook.getFontAt(sourceFontIndex);
                    cachedFont = targetWorkbook.createFont();
                    cachedFont.setFontName(sourceFont.getFontName());
                    cachedFont.setFontHeightInPoints(sourceFont.getFontHeightInPoints());
                    cachedFont.setBold(sourceFont.getBold());
                    cachedFont.setItalic(sourceFont.getItalic());
                    cachedFont.setUnderline(sourceFont.getUnderline());
                    cachedFont.setStrikeout(sourceFont.getStrikeout());
                    cachedFont.setTypeOffset(sourceFont.getTypeOffset());

                    // 폰트 색상 복사 (XSSFFont의 RGB 색상 지원)
                    if (sourceFont instanceof org.apache.poi.xssf.usermodel.XSSFFont &&
                        cachedFont instanceof org.apache.poi.xssf.usermodel.XSSFFont) {
                        org.apache.poi.xssf.usermodel.XSSFFont xssfSourceFont =
                            (org.apache.poi.xssf.usermodel.XSSFFont) sourceFont;
                        org.apache.poi.xssf.usermodel.XSSFFont xssfTargetFont =
                            (org.apache.poi.xssf.usermodel.XSSFFont) cachedFont;

                        org.apache.poi.xssf.usermodel.XSSFColor fontColor = xssfSourceFont.getXSSFColor();
                        if (fontColor != null) {
                            org.apache.poi.xssf.usermodel.XSSFColor newFontColor = copyXSSFColor(fontColor);
                            if (newFontColor != null) {
                                xssfTargetFont.setColor(newFontColor);
                            }
                        } else {
                            // XSSFColor가 없으면 인덱스 색상 사용
                            cachedFont.setColor(sourceFont.getColor());
                        }
                    } else {
                        // XSSF가 아닌 경우 인덱스 색상 사용
                        cachedFont.setColor(sourceFont.getColor());
                    }

                    fontCache.put(sourceFontIndex, cachedFont);
                }
                cachedStyle.setFont(cachedFont);

                // 기본 스타일 속성 복사
                cachedStyle.setAlignment(sourceStyle.getAlignment());
                cachedStyle.setVerticalAlignment(sourceStyle.getVerticalAlignment());
                cachedStyle.setWrapText(sourceStyle.getWrapText());
                cachedStyle.setRotation(sourceStyle.getRotation());
                cachedStyle.setIndention(sourceStyle.getIndention());

                // 테두리 복사
                cachedStyle.setBorderTop(sourceStyle.getBorderTop());
                cachedStyle.setBorderBottom(sourceStyle.getBorderBottom());
                cachedStyle.setBorderLeft(sourceStyle.getBorderLeft());
                cachedStyle.setBorderRight(sourceStyle.getBorderRight());
                cachedStyle.setTopBorderColor(sourceStyle.getTopBorderColor());
                cachedStyle.setBottomBorderColor(sourceStyle.getBottomBorderColor());
                cachedStyle.setLeftBorderColor(sourceStyle.getLeftBorderColor());
                cachedStyle.setRightBorderColor(sourceStyle.getRightBorderColor());

                // 데이터 포맷 복사
                cachedStyle.setDataFormat(sourceStyle.getDataFormat());

                // 배경색(채우기) 명시적 복사 (다른 워크북 간 색상 복사 문제 해결)
                if (sourceStyle instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle &&
                    cachedStyle instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle) {
                    org.apache.poi.xssf.usermodel.XSSFCellStyle xssfSourceStyle =
                        (org.apache.poi.xssf.usermodel.XSSFCellStyle) sourceStyle;
                    org.apache.poi.xssf.usermodel.XSSFCellStyle xssfTargetStyle =
                        (org.apache.poi.xssf.usermodel.XSSFCellStyle) cachedStyle;

                    // 원본 스타일의 실제 채우기 색상 정보 추출
                    org.apache.poi.xssf.usermodel.XSSFColor fgColor = xssfSourceStyle.getFillForegroundColorColor();
                    short fgColorIndex = xssfSourceStyle.getFillForegroundColor();
                    FillPatternType fillPattern = xssfSourceStyle.getFillPattern();

                    // 채우기 색상이 존재하는지 확인 (XSSFColor 또는 인덱스 색상)
                    boolean hasFillColor = (fgColor != null) ||
                        (fgColorIndex != IndexedColors.AUTOMATIC.getIndex() && fgColorIndex > 0);

                    // 채우기 패턴이 있거나, 채우기 색상이 있는 경우 처리
                    if ((fillPattern != null && fillPattern != FillPatternType.NO_FILL) || hasFillColor) {

                        // FillPattern 설정 (색상이 있는데 패턴이 없으면 SOLID_FOREGROUND로 설정)
                        if (fillPattern == null || fillPattern == FillPatternType.NO_FILL) {
                            if (hasFillColor) {
                                xssfTargetStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                            }
                        } else {
                            xssfTargetStyle.setFillPattern(fillPattern);
                        }

                        // FillForegroundColor (채우기 색상) 복사
                        boolean colorSet = false;

                        if (fgColor != null) {
                            org.apache.poi.xssf.usermodel.XSSFColor newFgColor = copyXSSFColor(fgColor);
                            if (newFgColor != null) {
                                xssfTargetStyle.setFillForegroundColor(newFgColor);
                                colorSet = true;
                            }
                        }

                        // XSSFColor가 null이거나 복사 실패한 경우 인덱스 색상 확인
                        if (!colorSet && fgColorIndex != IndexedColors.AUTOMATIC.getIndex() && fgColorIndex > 0) {
                            xssfTargetStyle.setFillForegroundColor(fgColorIndex);
                            colorSet = true;
                        }

                        // 그래도 색상이 설정되지 않았으면 흰색으로 기본 설정
                        if (!colorSet) {
                            xssfTargetStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                                new byte[]{(byte)255, (byte)255, (byte)255}, null));
                        }

                        // FillBackgroundColor 복사
                        org.apache.poi.xssf.usermodel.XSSFColor bgColor = xssfSourceStyle.getFillBackgroundColorColor();
                        if (bgColor != null) {
                            org.apache.poi.xssf.usermodel.XSSFColor newBgColor = copyXSSFColor(bgColor);
                            if (newBgColor != null) {
                                xssfTargetStyle.setFillBackgroundColor(newBgColor);
                            }
                        }
                    }
                } else {
                    // XSSF가 아닌 경우 기본 방식으로 복사
                    cachedStyle.setFillPattern(sourceStyle.getFillPattern());
                    cachedStyle.setFillForegroundColor(sourceStyle.getFillForegroundColor());
                    cachedStyle.setFillBackgroundColor(sourceStyle.getFillBackgroundColor());
                }

                styleCache.put(styleIndex, cachedStyle);
            }
            targetCell.setCellStyle(cachedStyle);
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
     * XSSFColor를 새 객체로 복사 (다른 워크북 간 색상 복사 문제 해결)
     */
    private org.apache.poi.xssf.usermodel.XSSFColor copyXSSFColor(org.apache.poi.xssf.usermodel.XSSFColor sourceColor) {
        if (sourceColor == null) {
            return null;
        }

        try {
            // Theme 색상인 경우 먼저 처리 (getRGBWithTint를 통해 실제 RGB 값 얻기)
            if (sourceColor.isThemed()) {
                byte[] rgbWithTint = sourceColor.getRGBWithTint();
                if (rgbWithTint != null && rgbWithTint.length >= 3) {
                    logger.debug("Theme 색상 RGB 추출 성공: [{}, {}, {}]",
                        rgbWithTint[0] & 0xFF, rgbWithTint[1] & 0xFF, rgbWithTint[2] & 0xFF);
                    return new org.apache.poi.xssf.usermodel.XSSFColor(rgbWithTint, null);
                }
                // getRGBWithTint 실패 시 ARGB 시도
                byte[] argb = sourceColor.getARGB();
                if (argb != null && argb.length >= 4) {
                    byte[] rgb = new byte[]{argb[1], argb[2], argb[3]};
                    logger.debug("Theme 색상 ARGB에서 RGB 추출: [{}, {}, {}]",
                        rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                    return new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null);
                }
                // Theme 색상인데 RGB를 얻을 수 없는 경우 흰색 반환
                logger.debug("Theme 색상 RGB 추출 실패, 흰색으로 대체");
                return new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null);
            }

            // 1. ARGB 값으로 복사 시도
            byte[] argb = sourceColor.getARGB();
            if (argb != null && argb.length >= 4) {
                byte[] rgb = new byte[]{argb[1], argb[2], argb[3]};
                logger.debug("ARGB로 색상 복사: [{}, {}, {}]", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                return new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null);
            }

            // 2. RGB 값으로 복사 시도
            byte[] rgb = sourceColor.getRGB();
            if (rgb != null && rgb.length >= 3) {
                logger.debug("RGB로 색상 복사: [{}, {}, {}]", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                return new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null);
            }

            // 3. ARGBHex로 복사 시도
            String argbHex = sourceColor.getARGBHex();
            if (argbHex != null && argbHex.length() >= 6) {
                // ARGB Hex에서 RGB 추출 (앞 2자리는 Alpha)
                String rgbHex = argbHex.length() == 8 ? argbHex.substring(2) : argbHex;
                byte[] rgbBytes = new byte[3];
                rgbBytes[0] = (byte) Integer.parseInt(rgbHex.substring(0, 2), 16);
                rgbBytes[1] = (byte) Integer.parseInt(rgbHex.substring(2, 4), 16);
                rgbBytes[2] = (byte) Integer.parseInt(rgbHex.substring(4, 6), 16);
                logger.debug("ARGBHex로 색상 복사: {}", argbHex);
                return new org.apache.poi.xssf.usermodel.XSSFColor(rgbBytes, null);
            }

            // 4. Indexed 색상으로 복사 시도
            short indexed = sourceColor.getIndexed();
            if (indexed > 0 && indexed < 64) {
                logger.debug("Indexed 색상으로 복사: index={}", indexed);
                return new org.apache.poi.xssf.usermodel.XSSFColor(IndexedColors.fromInt(indexed), null);
            }

            // 모든 방법 실패 시 흰색 반환
            logger.debug("모든 색상 추출 방법 실패, 흰색으로 대체");
            return new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null);

        } catch (Exception e) {
            logger.warn("XSSFColor 복사 중 오류: {}", e.getMessage());
            // 오류 발생 시에도 흰색 반환
            return new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null);
        }
    }

    /**
     * Populate DATA IN sheet from JSON file
     */
    private void populateDataInSheet(Sheet sheet, Workbook workbook) throws IOException {
        ClassPathResource jsonResource = new ClassPathResource("json/processcapa.json");
        InputStream inputStream = jsonResource.getInputStream();
        String jsonContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        inputStream.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonContent);

        int rowIndex = 0;

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle normalStyle = workbook.createCellStyle();

        Iterator<Map.Entry<String, JsonNode>> sections = root.fields();
        while (sections.hasNext()) {
            Map.Entry<String, JsonNode> section = sections.next();
            String sectionName = section.getKey();
            JsonNode sectionData = section.getValue();

            Row headerRow = sheet.createRow(rowIndex++);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(sectionName);
            headerCell.setCellStyle(headerStyle);

            JsonNode items = sectionData.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String name = item.get("name").asText();
                    JsonNode valueNode = item.get("value");

                    Row dataRow = sheet.createRow(rowIndex++);
                    Cell nameCell = dataRow.createCell(0);
                    Cell valueCell = dataRow.createCell(1);

                    nameCell.setCellValue(name);
                    nameCell.setCellStyle(normalStyle);

                    if (valueNode != null && !valueNode.isNull()) {
                        if (valueNode.isNumber()) {
                            valueCell.setCellValue(valueNode.asDouble());
                        } else {
                            valueCell.setCellValue(valueNode.asText());
                        }
                    }
                    valueCell.setCellStyle(normalStyle);
                }
            }
        }

        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
    }
}
