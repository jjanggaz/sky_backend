package com.wai.admin.service.reports.calculate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
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
public class MaschineListReportService {

    private final ObjectMapper objectMapper;

    public MaschineListReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 기계리스트 엑셀 파일 생성 (jsonUrl 기반)
     */
    public byte[] generateMachineListExcel(Map<String, Object> params) throws Exception {
        System.out.println("generateMachineListExcel 시작");
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("기계리스트");

            // 스타일 정의
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle groupStyle = createGroupStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            int rowNum = 0;

            // 제목 행
            XSSFRow titleRow = sheet.createRow(rowNum++);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("기계리스트");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            // 빈 행
            rowNum++;

            // 헤더 행
            XSSFRow headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Tag No.", "Item", "Specification", "Vendor", "MODEL",
                                "Material of Construction", "Power\n(kw)",
                                "Duty", "Sty", "Tot", "Unit"};

            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // JSON 데이터 로드
            Map<String, Object> machineData = loadMachineListJson(params);

            // equipments 배열 데이터 처리 (MaschineListReportPreviewService와 동일한 구조)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> equipments = (List<Map<String, Object>>) machineData.get("equipments");

            if (equipments != null) {
                for (Map<String, Object> equipment : equipments) {
                    // process_name을 그룹 헤더로 사용 (■ 포함)
                    String processName = (String) equipment.get("process_name");
                    if (processName == null || processName.isEmpty()) {
                        continue;
                    }

                    // 시스템 그룹 행
                    XSSFRow groupRow = sheet.createRow(rowNum++);
                    groupRow.setHeightInPoints(40);
                    XSSFCell groupCell = groupRow.createCell(0);
                    groupCell.setCellValue("■ " + processName);
                    groupCell.setCellStyle(groupStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 10));

                    // EQP.로 시작하는 장비 데이터 처리
                    for (Map.Entry<String, Object> entry : equipment.entrySet()) {
                        String key = entry.getKey();
                        // process_id, process_name, process_no를 제외하고 EQP.로 시작하는 항목만 처리
                        if (key.startsWith("EQP.")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> item = (Map<String, Object>) entry.getValue();

                            // spec1과 spec2를 연결
                            String spec1 = getString(item, "spec1");
                            String spec2 = getString(item, "spec2");
                            String specification = spec1;
                            if (!spec2.isEmpty()) {
                                specification = spec1.isEmpty() ? spec2 : spec1 + " " + spec2;
                            }

                            XSSFRow dataRow = sheet.createRow(rowNum++);

                            // Tag No
                            createCell(dataRow, 0, getString(item, "tag_number"), centerStyle);
                            // Item
                            createCell(dataRow, 1, getString(item, "name"), dataStyle);
                            // Specification
                            createCell(dataRow, 2, specification, dataStyle);
                            // Vendor
                            createCell(dataRow, 3, getString(item, "manufacturer"), dataStyle);
                            // MODEL
                            createCell(dataRow, 4, getString(item, "model_name"), centerStyle);
                            // Material of Construction
                            createCell(dataRow, 5, "", dataStyle);
                            // Power
                            createCell(dataRow, 6, getNumber(item, "power_kW"), centerStyle);
                            // Duty
                            createCell(dataRow, 7, getNumber(item, "normal_count"), centerStyle);
                            // Sty
                            createCell(dataRow, 8, getNumber(item, "spare_count"), centerStyle);
                            // Tot
                            createCell(dataRow, 9, getNumber(item, "qty"), centerStyle);
                            // Unit
                            createCell(dataRow, 10, getString(item, "unit"), centerStyle);
                        }
                    }
                }
            }

            // 컬럼 너비 설정
            sheet.setColumnWidth(0, 3000);  // Tag No
            sheet.setColumnWidth(1, 5000);  // Item
            sheet.setColumnWidth(2, 8000);  // Specification
            sheet.setColumnWidth(3, 4000);  // Vendor
            sheet.setColumnWidth(4, 3500);  // MODEL
            sheet.setColumnWidth(5, 6000);  // Material of Construction
            sheet.setColumnWidth(6, 2500);  // Power
            sheet.setColumnWidth(7, 2000);  // Duty
            sheet.setColumnWidth(8, 2000);  // Sty
            sheet.setColumnWidth(9, 2000);  // Tot
            sheet.setColumnWidth(10, 2000);  // Unit

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
     * JSON 파일 또는 URL에서 데이터 로드
     */
    private Map<String, Object> loadMachineListJson(Map<String, Object> params) throws Exception {
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
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * 그룹 스타일 생성
     */
    private CellStyle createGroupStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
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
     * 셀 생성 헬퍼 메서드
     */
    private void createCell(XSSFRow row, int column, String value,
                            CellStyle style) {
        XSSFCell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
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
     * Map에서 숫자 값 가져오기
     */
    private String getNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "";
        if (value.toString().equals("null")) return "";
        return value.toString();
    }
}
