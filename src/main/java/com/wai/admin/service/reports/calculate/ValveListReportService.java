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
public class ValveListReportService {

    private final ObjectMapper objectMapper;

    public ValveListReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 밸브리스트 엑셀 파일 생성 (원본 Excel 형식 그대로)
     */
    public byte[] generateValveListExcel() throws Exception {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("밸브리스트");

            // 스타일 정의
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            int rowNum = 0;

            // Row 1: 제목 행
            XSSFRow titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(25);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("밸브리스트");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

            // Row 2: 헤더 행
            XSSFRow headerRow = sheet.createRow(rowNum++);
            headerRow.setHeightInPoints(40);
            String[] headers = {
                "Tag No.",
                "Total Quantity",
                "Size",
                "Service",
                "Location",
                "USE",
                "Valve Type",
                "Valve Style",
                "Operation",
                "Actuator Type",
                "Loss of signal",
                "Loss of Air",
                "Comments"
            };

            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // JSON 데이터 로드
            List<Map<String, Object>> valveList = loadValveListJson();

            // 데이터 행 추가
            for (Map<String, Object> valve : valveList) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(30);

                // Tag No.
                createCell(dataRow, 0, getString(valve, "tagNo"), centerStyle);
                // Total Quantity
                createCell(dataRow, 1, getNumber(valve, "totalQuantity"), centerStyle);
                // Size
                createCell(dataRow, 2, getString(valve, "size"), centerStyle);
                // Service
                createCell(dataRow, 3, getString(valve, "service"), centerStyle);
                // Location
                createCell(dataRow, 4, getString(valve, "location"), dataStyle);
                // USE
                createCell(dataRow, 5, getString(valve, "use"), dataStyle);
                // Valve Type
                createCell(dataRow, 6, getString(valve, "valveType"), centerStyle);
                // Valve Style
                createCell(dataRow, 7, getString(valve, "valveStyle"), centerStyle);
                // Operation
                createCell(dataRow, 8, getString(valve, "operation"), centerStyle);
                // Actuator Type
                createCell(dataRow, 9, getString(valve, "actuatorType"), centerStyle);
                // Loss of signal
                createCell(dataRow, 10, getString(valve, "lossOfSignal"), centerStyle);
                // Loss of Air
                createCell(dataRow, 11, getString(valve, "lossOfAir"), centerStyle);
                // Comments
                createCell(dataRow, 12, getString(valve, "comments"), dataStyle);
            }

            // 컬럼 너비 설정 (원본 Excel과 유사하게)
            sheet.setColumnWidth(0, 3500);   // Tag No.
            sheet.setColumnWidth(1, 3000);   // Total Quantity
            sheet.setColumnWidth(2, 2500);   // Size
            sheet.setColumnWidth(3, 3500);   // Service
            sheet.setColumnWidth(4, 8000);   // Location
            sheet.setColumnWidth(5, 7000);   // USE
            sheet.setColumnWidth(6, 3000);   // Valve Type
            sheet.setColumnWidth(7, 3000);   // Valve Style
            sheet.setColumnWidth(8, 3500);   // Operation
            sheet.setColumnWidth(9, 3500);   // Actuator Type
            sheet.setColumnWidth(10, 3000);  // Loss of signal
            sheet.setColumnWidth(11, 3000);  // Loss of Air
            sheet.setColumnWidth(12, 5000);  // Comments

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
     * JSON 파일 로드 (객체 안의 배열 형식)
     */
    private List<Map<String, Object>> loadValveListJson() throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("json/valve.json");
            if (!resource.exists()) {
                throw new Exception("valve.json 파일을 찾을 수 없습니다.");
            }

            try (InputStream inputStream = resource.getInputStream()) {
                String jsonContent = StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, Object> jsonData = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

                // "valveList" 키에서 배열 추출
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> valveList = (List<Map<String, Object>>) jsonData.get("valveList");

                if (valveList == null) {
                    throw new Exception("valve.json 파일에 'valveList' 키가 없습니다.");
                }

                return valveList;
            }
        } catch (Exception e) {
            throw new Exception("JSON 파일 로드 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 제목 스타일 생성
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
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
     * 데이터 스타일 생성 (좌측 정렬)
     */
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
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
     * 셀 생성 헬퍼 메서드
     */
    private void createCell(XSSFRow row, int column, String value, CellStyle style) {
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
        if (value.toString().trim().isEmpty()) return "";
        return value.toString();
    }

    /**
     * Map에서 숫자 값 가져오기
     */
    private String getNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "";
        if (value.toString().equals("null")) return "";
        if (value.toString().trim().isEmpty()) return "";
        return value.toString();
    }
}
