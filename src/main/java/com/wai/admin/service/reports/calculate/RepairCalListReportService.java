package com.wai.admin.service.reports.calculate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
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
public class RepairCalListReportService {

    private final ObjectMapper objectMapper;

    public RepairCalListReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 수리계산서 엑셀 파일 생성 (원본과 동일한 17개 컬럼 구조)
     */
    public byte[] generateRepairCalExcel() throws Exception {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("수리계산서");

            // 스타일 정의
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle defaultStyle = createDefaultStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;

            // Row 1: 제목 행 "수리계산서"
            XSSFRow titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(25);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("수리계산서");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 16));

            // Row 2: 빈 행
            sheet.createRow(rowNum++);

            // Row 3-4: 헤더 행 (2행으로 병합)
            XSSFRow headerRow1 = sheet.createRow(rowNum++);
            XSSFRow headerRow2 = sheet.createRow(rowNum++);
            headerRow1.setHeightInPoints(20);
            headerRow2.setHeightInPoints(20);

            // 헤더 1: "산출근거" (A3:H4 병합)
            XSSFCell header1 = headerRow1.createCell(0);
            header1.setCellValue("산출근거");
            header1.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 3, 0, 7));

            // 헤더 2: "L.W.L\n(일평균)" (I3:K4 병합)
            XSSFCell header2 = headerRow1.createCell(8);
            header2.setCellValue("L.W.L\n(일평균)");
            header2.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 3, 8, 10));

            // 헤더 3: "D.W.L\n(일최대)" (L3:N4 병합)
            XSSFCell header3 = headerRow1.createCell(11);
            header3.setCellValue("D.W.L\n(일최대)");
            header3.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 3, 11, 13));

            // 헤더 4: "H.W.L\n(시간최대)" (O3:Q4 병합)
            XSSFCell header4 = headerRow1.createCell(14);
            header4.setCellValue("H.W.L\n(시간최대)");
            header4.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 3, 14, 16));

            // JSON 데이터 로드
            List<Map<String, Object>> repairCalList = loadRepairCalJson();

            // 데이터 행 추가 (Row 5부터 시작)
            for (Map<String, Object> item : repairCalList) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(20);

                // 17개 컬럼 처리
                for (int col = 1; col <= 17; col++) {
                    String colKey = "Col" + col;
                    Object cellValue = item.get(colKey);

                    XSSFCell cell = dataRow.createCell(col - 1);

                    if (cellValue != null) {
                        if (cellValue instanceof Number) {
                            // 숫자인 경우
                            double numValue = ((Number) cellValue).doubleValue();
                            if (numValue == Math.floor(numValue) && numValue < 1000000) {
                                // 정수인 경우
                                cell.setCellValue(numValue);
                            } else {
                                // 소수인 경우
                                cell.setCellValue(numValue);
                            }
                            cell.setCellStyle(numericStyle);
                        } else if (cellValue instanceof String) {
                            String strValue = (String) cellValue;
                            if (!strValue.trim().isEmpty()) {
                                cell.setCellValue(strValue);
                                cell.setCellStyle(defaultStyle);
                            }
                        }
                    }
                }
            }

            // 컬럼 너비 설정 (원본과 유사하게)
            sheet.setColumnWidth(0, 1000);   // Col1 - 항목명
            sheet.setColumnWidth(1, 5000);   // Col2 - 설명
            sheet.setColumnWidth(2, 1500);   // Col3
            sheet.setColumnWidth(3, 1500);   // Col4
            sheet.setColumnWidth(4, 1500);   // Col5
            sheet.setColumnWidth(5, 3000);   // Col6 - 값
            sheet.setColumnWidth(6, 1500);   // Col7
            sheet.setColumnWidth(7, 1500);   // Col8
            sheet.setColumnWidth(8, 1500);   // Col9
            sheet.setColumnWidth(9, 1500);   // Col10
            sheet.setColumnWidth(10, 1500);  // Col11
            sheet.setColumnWidth(11, 1500);  // Col12
            sheet.setColumnWidth(12, 2000);  // Col13 - 계산값
            sheet.setColumnWidth(13, 1500);  // Col14 - 단위
            sheet.setColumnWidth(14, 1500);  // Col15
            sheet.setColumnWidth(15, 1500);  // Col16
            sheet.setColumnWidth(16, 1500);  // Col17

            // 테두리 그리기
            drawBorders(sheet, rowNum);


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
     * JSON 파일 로드
     */
    private List<Map<String, Object>> loadRepairCalJson() throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("json/repaircal.json");
            if (!resource.exists()) {
                throw new Exception("repaircal.json 파일을 찾을 수 없습니다.");
            }

            try (InputStream inputStream = resource.getInputStream()) {
                String jsonContent = StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, Object> jsonData = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

                // "repairCalList" 키에서 배열 추출
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> repairCalList = (List<Map<String, Object>>) jsonData.get("repairCalList");

                if (repairCalList == null) {
                    throw new Exception("repaircal.json 파일에 'repairCalList' 키가 없습니다.");
                }

                return repairCalList;
            }
        } catch (Exception e) {
            throw new Exception("JSON 파일 로드 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Draw borders for header and data columns
     */
    private void drawBorders(XSSFSheet sheet, int lastDataRow) {
        CellRangeAddress[] headerRanges = {
            new CellRangeAddress(2, 3, 0, 7),
            new CellRangeAddress(2, 3, 8, 10),
            new CellRangeAddress(2, 3, 11, 13),
            new CellRangeAddress(2, 3, 14, 16)
        };

        for (CellRangeAddress range : headerRanges) {
            RegionUtil.setBorderTop(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, range, sheet);
        }

        int startRow = 2;
        int endRow = lastDataRow;

        for (int rowIdx = startRow; rowIdx < endRow; rowIdx++) {
            XSSFRow row = sheet.getRow(rowIdx);
            if (row == null) {
                row = sheet.createRow(rowIdx);
            }

            XSSFCell cellH = row.getCell(7);
            if (cellH == null) cellH = row.createCell(7);
            CellStyle styleH = sheet.getWorkbook().createCellStyle();
            styleH.cloneStyleFrom(cellH.getCellStyle() != null ? cellH.getCellStyle() : sheet.getWorkbook().createCellStyle());
            styleH.setBorderRight(BorderStyle.THIN);
            cellH.setCellStyle(styleH);

            XSSFCell cellK = row.getCell(10);
            if (cellK == null) cellK = row.createCell(10);
            CellStyle styleK = sheet.getWorkbook().createCellStyle();
            styleK.cloneStyleFrom(cellK.getCellStyle() != null ? cellK.getCellStyle() : sheet.getWorkbook().createCellStyle());
            styleK.setBorderRight(BorderStyle.THIN);
            cellK.setCellStyle(styleK);

            XSSFCell cellN = row.getCell(13);
            if (cellN == null) cellN = row.createCell(13);
            CellStyle styleN = sheet.getWorkbook().createCellStyle();
            styleN.cloneStyleFrom(cellN.getCellStyle() != null ? cellN.getCellStyle() : sheet.getWorkbook().createCellStyle());
            styleN.setBorderRight(BorderStyle.THIN);
            cellN.setCellStyle(styleN);

            XSSFCell cellQ = row.getCell(16);
            if (cellQ == null) cellQ = row.createCell(16);
            CellStyle styleQ = sheet.getWorkbook().createCellStyle();
            styleQ.cloneStyleFrom(cellQ.getCellStyle() != null ? cellQ.getCellStyle() : sheet.getWorkbook().createCellStyle());
            styleQ.setBorderRight(BorderStyle.THIN);
            cellQ.setCellStyle(styleQ);
        }

        XSSFRow lastRow = sheet.getRow(lastDataRow - 1);
        if (lastRow != null) {
            for (int colIdx = 0; colIdx <= 16; colIdx++) {
                XSSFCell cell = lastRow.getCell(colIdx);
                if (cell == null) cell = lastRow.createCell(colIdx);
                CellStyle style = sheet.getWorkbook().createCellStyle();
                if (cell.getCellStyle() != null) {
                    style.cloneStyleFrom(cell.getCellStyle());
                }
                style.setBorderBottom(BorderStyle.THIN);
                cell.setCellStyle(style);
            }
        }
    }

    /**
     * 제목 스타일 생성
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 헤더 스타일 생성 (테두리 없음)
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);  // 헤더는 줄바꿈 허용
        return style;
    }

    /**
     * 기본 데이터 스타일 생성 (문자열용, 테두리 없음)
     */
    private CellStyle createDefaultStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);  // 자동 줄바꿈 비활성화
        return style;
    }

    /**
     * 숫자 데이터 스타일 생성 (테두리 없음)
     */
    private CellStyle createNumericStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);  // 자동 줄바꿈 비활성화
        return style;
    }
}
