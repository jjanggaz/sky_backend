package com.wai.admin.service.reports.calculate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StructureListReportService {

    private final ObjectMapper objectMapper;

    public StructureListReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 구조물 리스트 엑셀 파일 생성 (jsonUrl 기반)
     */
    public byte[] generateStructureListExcel(Map<String, Object> params) throws Exception {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("구조물 리스트");

            // 스타일 정의
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle rightAlignStyle = createRightAlignStyle(workbook);
            CellStyle noBorderStyle = createNoBorderStyle(workbook);

            int rowNum = 0;

            // 제목 행
            XSSFRow titleRow = sheet.createRow(rowNum++);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("구조물 리스트");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // 빈 행
            rowNum++;

            // JSON 데이터 로드
            Map<String, Object> structureData = loadStructureListJson(params);

            // structures 배열 데이터 처리 (StructureListReportPreviewService와 동일한 구조)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> structures = (List<Map<String, Object>>) structureData.get("structures");

            if (structures != null) {
                // code_key별로 데이터를 분류
                Map<String, List<Map<String, Object>>> groupedStructures = new LinkedHashMap<>();
                groupedStructures.put("S_CONC", new ArrayList<>());
                groupedStructures.put("S_BUILD", new ArrayList<>());
                groupedStructures.put("S_STSREC", new ArrayList<>());
                groupedStructures.put("S_STSCIR", new ArrayList<>());

                // structures 배열을 순회하며 STC.로 시작하는 구조물 객체 추출
                for (Map<String, Object> structure : structures) {
                    for (Map.Entry<String, Object> entry : structure.entrySet()) {
                        String key = entry.getKey();
                        if (key.startsWith("STC.")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> item = (Map<String, Object>) entry.getValue();
                            String codeKey = getString(item, "code_key");

                            if (groupedStructures.containsKey(codeKey)) {
                                groupedStructures.get(codeKey).add(item);
                            }
                        }
                    }
                }

                int sectionNumber = 1;

                // 1. 토목 (S_CONC)
                if (!groupedStructures.get("S_CONC").isEmpty()) {
                    rowNum = createStructureSection(sheet, sectionNumber++ + ". 토목", groupedStructures.get("S_CONC"),
                            "S_CONC", rowNum, sectionStyle, headerStyle, dataStyle, centerStyle, rightAlignStyle);
                    rowNum++; // 섹션 간 빈 행
                }

                // 2. 건축 (S_BUILD)
                if (!groupedStructures.get("S_BUILD").isEmpty()) {
                    rowNum = createStructureSection(sheet, sectionNumber++ + ". 건축", groupedStructures.get("S_BUILD"),
                            "S_BUILD", rowNum, sectionStyle, headerStyle, dataStyle, centerStyle, rightAlignStyle);
                    rowNum++;
                }

                // 3. 각형탱크(STS) (S_STSREC)
                if (!groupedStructures.get("S_STSREC").isEmpty()) {
                    rowNum = createStructureSection(sheet, sectionNumber++ + ". 각형탱크(STS)", groupedStructures.get("S_STSREC"),
                            "S_STSREC", rowNum, sectionStyle, headerStyle, dataStyle, centerStyle, rightAlignStyle);
                    rowNum++;
                }

                // 4. 원형탱크(STS) (S_STSCIR)
                if (!groupedStructures.get("S_STSCIR").isEmpty()) {
                    rowNum = createStructureSection(sheet, sectionNumber++ + ". 원형탱크(STS)", groupedStructures.get("S_STSCIR"),
                            "S_STSCIR", rowNum, sectionStyle, headerStyle, dataStyle, centerStyle, rightAlignStyle);
                    rowNum++;
                }
            }

            // 푸터 메시지
            XSSFRow footerRow = sheet.createRow(rowNum);
            XSSFCell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("소요 및 적용 면적은 개략검토된 내용으로써 상세 설계를 통한 산정필요");
            footerCell.setCellStyle(noBorderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 9));

            // 컬럼 너비 설정
            sheet.setColumnWidth(0, 6000);  // 구조물 이름
            sheet.setColumnWidth(1, 4000);  // 소요 Volume
            sheet.setColumnWidth(2, 4000);  // 적용 Volume
            sheet.setColumnWidth(3, 2500);  // W(m)
            sheet.setColumnWidth(4, 2500);  // L(m)
            sheet.setColumnWidth(5, 2500);  // He(m)
            sheet.setColumnWidth(6, 2500);  // H(m)
            sheet.setColumnWidth(7, 2500);  // 지수(EA)
            sheet.setColumnWidth(8, 3000);  // Value
            sheet.setColumnWidth(9, 3000);  // Unit

            // 바이트 배열로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            return baos.toByteArray();
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    // 로그만 기록 (무시)
                }
            }
        }
    }

    /**
     * 구조물 섹션 생성 (code_key별)
     */
    private int createStructureSection(XSSFSheet sheet, String sectionTitle, List<Map<String, Object>> items,
            String codeKey, int startRow, CellStyle sectionStyle, CellStyle headerStyle, CellStyle dataStyle,
            CellStyle centerStyle, CellStyle rightAlignStyle) {

        int rowNum = startRow;

        // 섹션 헤더
        XSSFRow sectionRow = sheet.createRow(rowNum++);
        XSSFCell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue(sectionTitle);
        sectionCell.setCellStyle(sectionStyle);

        // code_key에 따라 헤더 구조가 다름
        if ("S_BUILD".equals(codeKey)) {
            // 건축 영역
            // 헤더 행 1
            XSSFRow headerRow1 = sheet.createRow(rowNum++);
            createCell(headerRow1, 0, "구조물 이름", headerStyle);
            createMergedCell(headerRow1, sheet, 1, 5, "적용 구조물 내부 규격", headerStyle);
//            createMergedCell(headerRow1, sheet, 6, 7, "Design Criteria", headerStyle);
            createCell(headerRow1, 6, "비고", headerStyle);

            // 헤더 행 2
            XSSFRow headerRow2 = sheet.createRow(rowNum++);
            createCell(headerRow2, 0, "", headerStyle);
            createCell(headerRow2, 1, "W(m)", headerStyle);
            createCell(headerRow2, 2, "L(m)", headerStyle);
            createCell(headerRow2, 3, "He(m)", headerStyle);
            createCell(headerRow2, 4, "H(m)", headerStyle);
            createCell(headerRow2, 5, "지수(EA)", headerStyle);
//            createCell(headerRow2, 6, "Value", headerStyle);
//            createCell(headerRow2, 7, "Unit", headerStyle);
            createCell(headerRow2, 6, "", headerStyle);

            // 세로 병합 (구조물 이름, 비고)
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 8, 8));

            // 데이터 행 생성
            for (Map<String, Object> item : items) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                createCell(dataRow, 0, getString(item, "name"), dataStyle);
                createCell(dataRow, 1, getNumber(item, "W"), centerStyle);
                createCell(dataRow, 2, getNumber(item, "L"), centerStyle);
                createCell(dataRow, 3, "-", centerStyle);
                createCell(dataRow, 4, getNumber(item, "height"), centerStyle);
                createCell(dataRow, 5, getNumber(item, "tank_number"), centerStyle);
//                createCell(dataRow, 6, "", centerStyle); // Value - 빈값
//                createCell(dataRow, 7, "hr(HRT)", centerStyle); // Unit
                createCell(dataRow, 6, "", dataStyle); // 비고 - 빈값
            }
        } else if("S_CONC".equals(codeKey)) {
            // 토목영역역
            // 헤더 행 1
            XSSFRow headerRow1 = sheet.createRow(rowNum++);
            createCell(headerRow1, 0, "구조물 이름", headerStyle);
            createCell(headerRow1, 1, "소요 Volume (㎥)", headerStyle);
            createCell(headerRow1, 2, "적용 유효 Volume (㎥)", headerStyle);
            createMergedCell(headerRow1, sheet, 3, 7, "적용 구조물 내부 규격", headerStyle);
            createMergedCell(headerRow1, sheet, 8, 9, "Design Criteria", headerStyle);
            createCell(headerRow1, 10, "비고", headerStyle);

            // 헤더 행 2
            XSSFRow headerRow2 = sheet.createRow(rowNum++);
            createCell(headerRow2, 0, "", headerStyle);
            createCell(headerRow2, 1, "", headerStyle);
            createCell(headerRow2, 2, "", headerStyle);
            createCell(headerRow2, 3, "W(m)", headerStyle);
            createCell(headerRow2, 4, "L(m)", headerStyle);
            createCell(headerRow2, 5, "He(m)", headerStyle);
            createCell(headerRow2, 6, "H(m)", headerStyle);
            createCell(headerRow2, 7, "지수(EA)", headerStyle);
            createCell(headerRow2, 8, "Value", headerStyle);
            createCell(headerRow2, 9, "Unit", headerStyle);
            createCell(headerRow2, 10, "", headerStyle);

            // 세로 병합 (구조물 이름, 소요 Volume, 적용 Volume)
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 10, 10));

            // 데이터 행 생성
            for (Map<String, Object> item : items) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                createCell(dataRow, 0, getString(item, "name"), dataStyle);
                createCell(dataRow, 1, getNumber(item, "required_capacity"), rightAlignStyle);
                createCell(dataRow, 2, getNumber(item, "applied_capacity"), rightAlignStyle);
                createCell(dataRow, 3, getNumber(item, "W"), centerStyle);
                createCell(dataRow, 4, getNumber(item, "L"), centerStyle);
                createCell(dataRow, 5, getNumber(item, "water_level"), centerStyle);
                createCell(dataRow, 6, getNumber(item, "height"), centerStyle);
                createCell(dataRow, 7, getNumber(item, "tank_number"), centerStyle);
                createCell(dataRow, 8, "", centerStyle); // Value - 빈값
                createCell(dataRow, 9, "hr(HRT)", centerStyle); // Unit
                createCell(dataRow, 10, "", centerStyle); // 비고 데이터 - 빈값
            }
        } else if("S_STSREC".equals(codeKey)) {
            // 각형탱크(STS) 영역역
            // 헤더 행 1
            XSSFRow headerRow1 = sheet.createRow(rowNum++);
            createCell(headerRow1, 0, "구조물 이름", headerStyle);
            createCell(headerRow1, 1, "소요 Volume (㎥)", headerStyle);
            createCell(headerRow1, 2, "적용 유효 Volume (㎥)", headerStyle);
            createMergedCell(headerRow1, sheet, 3, 7, "적용 구조물 내부 규격", headerStyle);
            createMergedCell(headerRow1, sheet, 8, 9, "Design Criteria", headerStyle);
            createCell(headerRow1, 10, "비고", headerStyle);

            // 헤더 행 2
            XSSFRow headerRow2 = sheet.createRow(rowNum++);
            createCell(headerRow2, 0, "", headerStyle);
            createCell(headerRow2, 1, "", headerStyle);
            createCell(headerRow2, 2, "", headerStyle);
            createCell(headerRow2, 3, "W(m)", headerStyle);
            createCell(headerRow2, 4, "L(m)", headerStyle);
            createCell(headerRow2, 5, "He(m)", headerStyle);
            createCell(headerRow2, 6, "H(m)", headerStyle);
            createCell(headerRow2, 7, "지수(EA)", headerStyle);
            createCell(headerRow2, 8, "Value", headerStyle);
            createCell(headerRow2, 9, "Unit", headerStyle);
            createCell(headerRow2, 10, "", headerStyle);

            // 세로 병합 (구조물 이름, 소요 Volume, 적용 Volume)
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 10, 10));

            // 데이터 행 생성
            for (Map<String, Object> item : items) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                createCell(dataRow, 0, getString(item, "name"), dataStyle);
                createCell(dataRow, 1, getNumber(item, "required_capacity"), rightAlignStyle);
                createCell(dataRow, 2, getNumber(item, "applied_capacity"), rightAlignStyle);
                createCell(dataRow, 3, getNumber(item, "W"), centerStyle);
                createCell(dataRow, 4, getNumber(item, "L"), centerStyle);
                createCell(dataRow, 5, getNumber(item, "water_level"), centerStyle);
                createCell(dataRow, 6, getNumber(item, "height"), centerStyle);
                createCell(dataRow, 7, getNumber(item, "tank_number"), centerStyle);
                createCell(dataRow, 8, "", centerStyle); // Value - 빈값
                createCell(dataRow, 9, "hr(HRT)", centerStyle); // Unit
                createCell(dataRow, 10, "", centerStyle); // 비고 데이터 - 빈값
            }
        } else if("S_STSCIR".equals(codeKey)) {
            // 원형탱크(STS) 영역
            // 헤더 행 1
            XSSFRow headerRow1 = sheet.createRow(rowNum++);
            createCell(headerRow1, 0, "구조물 이름", headerStyle);
            createCell(headerRow1, 1, "소요 Volume (㎥)", headerStyle);
            createCell(headerRow1, 2, "적용 유효 Volume (㎥)", headerStyle);
            createMergedCell(headerRow1, sheet, 3, 7, "적용 구조물 내부 규격", headerStyle);
            createMergedCell(headerRow1, sheet, 8, 9, "Design Criteria", headerStyle);
            createCell(headerRow1, 10, "비고", headerStyle);

            // 헤더 행 2
            XSSFRow headerRow2 = sheet.createRow(rowNum++);
            createCell(headerRow2, 0, "", headerStyle);
            createCell(headerRow2, 1, "", headerStyle);
            createCell(headerRow2, 2, "", headerStyle);
            createCell(headerRow2, 3, "D(m)", headerStyle);
            createCell(headerRow2, 4, "-", headerStyle);
            createCell(headerRow2, 5, "He(m)", headerStyle);
            createCell(headerRow2, 6, "H(m)", headerStyle);
            createCell(headerRow2, 7, "지수(EA)", headerStyle);
            createCell(headerRow2, 8, "Value", headerStyle);
            createCell(headerRow2, 9, "Unit", headerStyle);
            createCell(headerRow2, 10, "", headerStyle);

            // 세로 병합 (구조물 이름, 소요 Volume, 적용 Volume)
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(headerRow1.getRowNum(), headerRow2.getRowNum(), 10, 10));

            // 데이터 행 생성
            for (Map<String, Object> item : items) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                createCell(dataRow, 0, getString(item, "name"), dataStyle);
                createCell(dataRow, 1, getNumber(item, "required_capacity"), rightAlignStyle);
                createCell(dataRow, 2, getNumber(item, "applied_capacity"), rightAlignStyle);
                createCell(dataRow, 3, getNumber(item, "W"), centerStyle);
                createCell(dataRow, 4, "", centerStyle);
                createCell(dataRow, 5, getNumber(item, "water_level"), centerStyle);
                createCell(dataRow, 6, getNumber(item, "height"), centerStyle);
                createCell(dataRow, 7, getNumber(item, "tank_number"), centerStyle);
                createCell(dataRow, 8, "", centerStyle); // Value - 빈값
                createCell(dataRow, 9, "hr(HRT)", centerStyle); // Unit
                createCell(dataRow, 10, "", centerStyle); // 비고 데이터 - 빈값
            }
        }

        return rowNum;
    }

    /**
     * JSON 파일 또는 URL에서 데이터 로드
     */
    private Map<String, Object> loadStructureListJson(Map<String, Object> params) throws Exception {
        try {
            String jsonUrl = params != null ? (String) params.get("jsonUrl") : null;
            InputStream inputStream = null;

            // jsonUrl이 있으면 URL에서 로드, 없으면 기본 파일에서 로드
            if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
                // URL에서 JSON 데이터 로드
                java.net.URL url = new java.net.URL(jsonUrl);
                inputStream = url.openStream();
                System.out.println("URL에서 JSON 로드: " + jsonUrl);
            } else {
                // 기본 파일에서 로드
                ClassPathResource resource = new ClassPathResource("json/projectSampleOrg.json");
                if (!resource.exists()) {
                    throw new Exception("projectSampleOrg.json 파일을 찾을 수 없습니다.");
                }
                inputStream = resource.getInputStream();
                System.out.println("기본 파일에서 JSON 로드");
            }

            try (InputStream stream = inputStream) {
                String jsonContent = StreamUtils.copyToString(stream, java.nio.charset.StandardCharsets.UTF_8);
                return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            throw new Exception("JSON 데이터 로드 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 제목 스타일 생성
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 섹션 스타일 생성
     */
    private CellStyle createSectionStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * 데이터 스타일 생성
     */
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * 중앙 정렬 스타일 생성
     */
    private CellStyle createCenterStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 우측 정렬 스타일 생성
     */
    private CellStyle createRightAlignStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 테두리 없는 스타일 생성
     */
    private CellStyle createNoBorderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * 셀 생성 헬퍼 메서드
     */
    private void createCell(XSSFRow row, int column, String value, CellStyle style) {
        XSSFCell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * 병합된 셀 생성 헬퍼 메서드
     */
    private void createMergedCell(XSSFRow row, XSSFSheet sheet, int startCol, int endCol, String value, CellStyle style) {
        XSSFCell cell = row.createCell(startCol);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);

        // 병합 영역 내 모든 셀에 스타일 적용
        for (int col = startCol + 1; col <= endCol; col++) {
            XSSFCell emptyCell = row.createCell(col);
            emptyCell.setCellStyle(style);
        }

        if (startCol != endCol) {
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), startCol, endCol));
        }
    }

    /**
     * Map에서 문자열 값 가져오기
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "";
        return value.toString();
    }

    /**
     * Map에서 숫자 값 가져오기 (소수점 2자리 초과시만 제한)
     */
    private String getNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "";
        if (value.toString().equals("null")) return "";

        try {
            double numValue = Double.parseDouble(value.toString());
            String strValue = value.toString();
            int decimalPlaces = 0;
            if (strValue.contains(".")) {
                String[] parts = strValue.split("\\.");
                if (parts.length > 1) {
                    decimalPlaces = parts[1].length();
                }
            }

            if (decimalPlaces > 2) {
                return String.format("%.2f", numValue);
            } else {
                return value.toString();
            }
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }
}
