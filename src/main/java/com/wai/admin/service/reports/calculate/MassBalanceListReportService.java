package com.wai.admin.service.reports.calculate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
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
public class MassBalanceListReportService {

    private final ObjectMapper objectMapper;

    // 특수 문자 넘버링 배열
    private static final String[] CIRCLE_NUMBERS = {
        "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
        "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳",
        "㉑", "㉒", "㉓", "㉔", "㉕", "㉖", "㉗", "㉘", "㉙", "㉚",
        "㉛", "㉜", "㉝", "㉞", "㉟", "㊱", "㊲", "㊳", "㊴", "㊵",
        "㊶", "㊷", "㊸", "㊹", "㊺", "㊻", "㊼", "㊽", "㊾", "㊿"
    };

    public MassBalanceListReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 물질수지도 엑셀 파일 생성
     */
    public byte[] generateMassBalanceListExcel(Map<String, Object> params) throws Exception {
        System.out.println("generateMassBalanceListExcel 시작");
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("물질수지도");

            // JSON 데이터 로드
            Map<String, Object> jsonData = loadMassBalanceJson(params);

            // mass_balance 데이터 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> massBalanceList = (List<Map<String, Object>>) jsonData.get("mass_balance");

            if (massBalanceList == null || massBalanceList.isEmpty()) {
                throw new Exception("mass_balance 데이터가 없습니다.");
            }

            // 스타일 정의
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle unitHeaderStyle = createUnitHeaderStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);  // 숫자용 스타일 (소수점 1자리)

            int currentRow = 0;

            // Row 1: 빈 행
            sheet.createRow(currentRow++);

            // 공정 개수에 따라 병합 범위 결정 (각 공정당 3열)
            int totalColumns = 21;  //massBalanceList.size() * 1;

            // Row 2: MASS BALANCE 타이틀
            XSSFRow titleRow = sheet.createRow(currentRow++);
            // 병합 영역의 모든 셀에 스타일 적용 (테두리가 제대로 표시되도록)
            for (int i = 0; i < totalColumns; i++) {
                XSSFCell cell = titleRow.createCell(i);
                if (i == 0) {
                    cell.setCellValue("MASS BALANCE");
                }
                cell.setCellStyle(titleStyle);
            }
            if (totalColumns > 1) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, totalColumns - 1));
            }

            // 타이틀과 이미지 사이 빈 행 추가
            sheet.createRow(currentRow++);

            // Row 4~: 이미지 삽입
            String imageUrl = (String) jsonData.get("mass_balance_image_url");
            int imageEndRow = currentRow;

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    imageEndRow = insertImageFromUrl(workbook, sheet, imageUrl, currentRow);
                    System.out.println("imageEndRow >>> " + imageEndRow);
                } catch (Exception e) {
                    System.out.println("이미지 로드 실패: " + e.getMessage());
                    // 이미지 로드 실패 시 기본 15행 공간 확보
                    imageEndRow = currentRow + 15;
                }
            } else {
                // 이미지 URL이 없으면 기본 15행 공간 확보
                imageEndRow = currentRow + 15;
            }

            // 이미지 영역 빈 행 생성
            for (int i = currentRow; i < imageEndRow; i++) {
                if (sheet.getRow(i) == null) {
                    sheet.createRow(i);
                }
                sheet.getRow(i).setHeightInPoints(26.25f);
            }
            currentRow = imageEndRow;

            // 이미지와 테이블 사이 빈 행 추가
            sheet.createRow(currentRow++);

            // 데이터 항목 추출 (influents 객체 내부의 키들 추출)
            Set<String> itemKeysSet = new LinkedHashSet<>();
            for (Map<String, Object> process : massBalanceList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> influents = (Map<String, Object>) process.get("influents");
                if (influents != null) {
                    for (String key : influents.keySet()) {
                        itemKeysSet.add(key);
                    }
                }
            }
            List<String> itemKeys = new ArrayList<>(itemKeysSet);

            // 데이터 행들 (q 제외한 항목들)
            // 먼저 모든 고유한 name 값들을 수집하고, unit별로 mg/L, kg/d 값을 분리 저장
            // 각 공정별로 {mgL: value, kgd: value} 형태로 저장 (값이 없으면 null)
            Map<String, List<Map<String, Double>>> itemDataByName = new LinkedHashMap<>();

            for (String key : itemKeys) {
                if (key.equals("q")) continue; // Q는 별도 처리

                for (int i = 0; i < massBalanceList.size(); i++) {
                    Map<String, Object> process = massBalanceList.get(i);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> influents = (Map<String, Object>) process.get("influents");
                    if (influents == null) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemData = (Map<String, Object>) influents.get(key);

                    if (itemData != null) {
                        String name = getString(itemData, "name", key.toUpperCase());
                        String unit = getString(itemData, "unit", "");
                        double value = getDoubleValue(itemData.get("value"));

                        if (!itemDataByName.containsKey(name)) {
                            // 각 공정별 데이터를 저장할 리스트 초기화 (null로 초기화하여 값 없음 구분)
                            List<Map<String, Double>> processDataList = new ArrayList<>();
                            for (int j = 0; j < massBalanceList.size(); j++) {
                                Map<String, Double> values = new LinkedHashMap<>();
                                values.put("mgL", null);
                                values.put("kgd", null);
                                processDataList.add(values);
                            }
                            itemDataByName.put(name, processDataList);
                        }

                        // 해당 공정의 데이터를 unit에 따라 저장
                        Map<String, Double> processValues = itemDataByName.get(name).get(i);
                        if ("mg/L".equalsIgnoreCase(unit) || "㎎/L".equals(unit)) {
                            processValues.put("mgL", value);
                        } else if ("kg/d".equalsIgnoreCase(unit) || "㎏/d".equals(unit)) {
                            processValues.put("kgd", value);
                        } else {
                            // 다른 단위인 경우 mg/L로 기본 처리
                            processValues.put("mgL", value);
                        }
                    }
                }
            }

            // 10개씩 나눠서 테이블 생성
            int maxColumnsPerTable = 10;
            int totalProcesses = massBalanceList.size();
            int tableCount = (int) Math.ceil((double) totalProcesses / maxColumnsPerTable);

            for (int tableIdx = 0; tableIdx < tableCount; tableIdx++) {
                int startIdx = tableIdx * maxColumnsPerTable;
                int endIdx = Math.min(startIdx + maxColumnsPerTable, totalProcesses);
                int processCountInTable = endIdx - startIdx;

                // 각 테이블 행의 첫 번째 공정만 3열 (Item명 포함), 나머지는 2열
                // 총 열 수 = 1개(첫번째) * 3열 + (나머지) * 2열 = 3 + (processCountInTable - 1) * 2
                int columnsInTable = 3 + (processCountInTable - 1) * 2;

                // 테이블 간 빈 행 추가 (첫 번째 테이블 제외)
                if (tableIdx > 0) {
                    sheet.createRow(currentRow++);
                }

                // 헤더 행: 공정명 (① Influent, ② SBR Input, ...)
                XSSFRow processHeaderRow = sheet.createRow(currentRow++);
                int colIdx = 0;
                for (int i = startIdx; i < endIdx; i++) {
                    Map<String, Object> process = massBalanceList.get(i);
                    String processName = (String) process.get("process_name");
                    String circleNum = i < CIRCLE_NUMBERS.length ? CIRCLE_NUMBERS[i] : "(" + (i + 1) + ")";

                    // 첫 번째 공정은 3열, 나머지는 2열
                    boolean isFirstInRow = (i == startIdx);
                    int colsForThisProcess = isFirstInRow ? 3 : 2;

                    // 병합 영역의 모든 셀에 스타일 적용 (테두리가 제대로 표시되도록)
                    for (int j = 0; j < colsForThisProcess; j++) {
                        XSSFCell cell = processHeaderRow.createCell(colIdx + j);
                        if (j == 0) {
                            cell.setCellValue(circleNum + " " + processName);
                        }
                        cell.setCellStyle(headerStyle);
                    }

                    // 병합 처리
                    if (colsForThisProcess > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, colIdx, colIdx + colsForThisProcess - 1));
                    }

                    colIdx += colsForThisProcess;
                }

                // Q 행 (유량)
                XSSFRow qRow = sheet.createRow(currentRow++);
                colIdx = 0;
                for (int i = startIdx; i < endIdx; i++) {
                    Map<String, Object> process = massBalanceList.get(i);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> influents = (Map<String, Object>) process.get("influents");

                    String name = "Q";
                    double numValue = 0;
                    String unit = "㎥/d";

                    if (influents != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> qData = (Map<String, Object>) influents.get("q");
                        if (qData != null) {
                            name = getString(qData, "name", "Q");
                            numValue = getDoubleValue(qData.get("value"));
                            unit = getString(qData, "unit", "㎥/d");
                        }
                    }

                    // 각 테이블의 첫 번째 공정에서만 Item명 출력
                    boolean isFirstInRow = (i == startIdx);
                    if (isFirstInRow) {
                        createCell(qRow, colIdx++, name, centerStyle);
                    }
                    createNumericCell(qRow, colIdx++, numValue, numericStyle, centerStyle);
                    createCell(qRow, colIdx++, unit, centerStyle);
                }

                // Item 헤더 행 (Item | mg/L | kg/d 반복)
                XSSFRow itemHeaderRow = sheet.createRow(currentRow++);
                colIdx = 0;
                for (int i = startIdx; i < endIdx; i++) {
                    // 각 테이블의 첫 번째 공정에서만 Item 출력
                    boolean isFirstInRow = (i == startIdx);
                    if (isFirstInRow) {
                        createCell(itemHeaderRow, colIdx++, "Item", unitHeaderStyle);
                    }
                    createCell(itemHeaderRow, colIdx++, "㎎/L", unitHeaderStyle);
                    createCell(itemHeaderRow, colIdx++, "㎏/d", unitHeaderStyle);
                }

                // 각 항목별로 행 생성
                for (Map.Entry<String, List<Map<String, Double>>> entry : itemDataByName.entrySet()) {
                    String itemName = entry.getKey();
                    List<Map<String, Double>> processDataList = entry.getValue();

                    XSSFRow dataRow = sheet.createRow(currentRow++);
                    colIdx = 0;

                    for (int i = startIdx; i < endIdx; i++) {
                        Map<String, Double> values = processDataList.get(i);

                        Double mgLValue = values.get("mgL");
                        Double kgdValue = values.get("kgd");

                        // 각 테이블의 첫 번째 공정에서만 Item명 출력
                        boolean isFirstInRow = (i == startIdx);
                        if (isFirstInRow) {
                            createCell(dataRow, colIdx++, itemName, dataStyle);
                        }
                        createNumericCell(dataRow, colIdx++, mgLValue, numericStyle, centerStyle);
                        createNumericCell(dataRow, colIdx++, kgdValue, numericStyle, centerStyle);
                    }
                }

                // 컬럼 너비 설정 (각 테이블마다)
                for (int i = 0; i < columnsInTable; i++) {
                    if (i == 0) {
                        sheet.setColumnWidth(i, 3500);  // Item 컬럼 (첫 번째 열)
                    } else {
                        sheet.setColumnWidth(i, 3000);  // 값 컬럼
                    }
                }
            }

            // ========== Removal Efficiency (%) 테이블 ==========
            // efficiency 객체가 있는지 확인
            boolean hasEfficiency = false;
            for (Map<String, Object> process : massBalanceList) {
                if (process.get("efficiency") != null) {
                    hasEfficiency = true;
                    break;
                }
            }

            if (hasEfficiency) {
                // 빈 행 추가 (influents 테이블과 구분)
                sheet.createRow(currentRow++);

                // efficiency 객체가 있는 공정만 필터링
                List<Map<String, Object>> efficiencyProcessList = new ArrayList<>();
                for (Map<String, Object> process : massBalanceList) {
                    if (process.get("efficiency") != null) {
                        efficiencyProcessList.add(process);
                    }
                }

                // efficiency 항목 키 수집 (순서 유지)
                Set<String> efficiencyKeysSet = new LinkedHashSet<>();
                for (Map<String, Object> process : efficiencyProcessList) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> efficiency = (Map<String, Object>) process.get("efficiency");
                    if (efficiency != null) {
                        for (String key : efficiency.keySet()) {
                            efficiencyKeysSet.add(key);
                        }
                    }
                }
                List<String> efficiencyKeys = new ArrayList<>(efficiencyKeysSet);

                // efficiency 항목별 name 수집 (표시용)
                Map<String, String> efficiencyNames = new LinkedHashMap<>();
                for (String key : efficiencyKeys) {
                    for (Map<String, Object> process : efficiencyProcessList) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> efficiency = (Map<String, Object>) process.get("efficiency");
                        if (efficiency != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemData = (Map<String, Object>) efficiency.get(key);
                            if (itemData != null) {
                                String name = getString(itemData, "name", key.toUpperCase());
                                efficiencyNames.put(key, name);
                                break;
                            }
                        }
                    }
                }

                // efficiency가 있는 공정 수 기준으로 테이블 생성
                int effTotalProcesses = efficiencyProcessList.size();
                int effTableCount = (int) Math.ceil((double) effTotalProcesses / maxColumnsPerTable);

                // 10개씩 나눠서 Efficiency 테이블 생성
                for (int tableIdx = 0; tableIdx < effTableCount; tableIdx++) {
                    int startIdx = tableIdx * maxColumnsPerTable;
                    int endIdx = Math.min(startIdx + maxColumnsPerTable, effTotalProcesses);
                    int processCountInTable = endIdx - startIdx;

                    // 테이블 간 빈 행 추가 (첫 번째 테이블 제외)
                    if (tableIdx > 0) {
                        sheet.createRow(currentRow++);
                    }

                    // 1행: "Removal Efficiency (%)" 타이틀 (횡 병합)
                    int effTitleRowIdx = currentRow;
                    XSSFRow effTitleRow = sheet.createRow(currentRow++);
                    int totalEffCols = 1 + processCountInTable; // Item열 + 공정수
                    for (int i = 0; i < totalEffCols; i++) {
                        XSSFCell cell = effTitleRow.createCell(i);
                        if (i == 0) {
                            cell.setCellValue("Removal Efficiency (%)");
                        }
                        cell.setCellStyle(headerStyle);
                    }
                    if (totalEffCols > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(effTitleRowIdx, effTitleRowIdx, 0, totalEffCols - 1));
                    }

                    // 2행: "Item" + 각 공정명 (번호 없이 process_name만)
                    XSSFRow effHeaderRow = sheet.createRow(currentRow++);
                    int colIdx = 0;
                    createCell(effHeaderRow, colIdx++, "Item", unitHeaderStyle);
                    for (int i = startIdx; i < endIdx; i++) {
                        Map<String, Object> process = efficiencyProcessList.get(i);
                        String processName = (String) process.get("process_name");
                        createCell(effHeaderRow, colIdx++, processName, unitHeaderStyle);
                    }

                    // efficiency 데이터 행 생성
                    for (String key : efficiencyKeys) {
                        XSSFRow dataRow = sheet.createRow(currentRow++);
                        colIdx = 0;

                        // Item명 (첫 번째 열)
                        String itemName = efficiencyNames.getOrDefault(key, key.toUpperCase());
                        createCell(dataRow, colIdx++, itemName, dataStyle);

                        // 각 공정의 efficiency 값 (efficiency가 있는 공정만)
                        for (int i = startIdx; i < endIdx; i++) {
                            Map<String, Object> process = efficiencyProcessList.get(i);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> efficiency = (Map<String, Object>) process.get("efficiency");

                            String displayValue = "-";
                            if (efficiency != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> itemData = (Map<String, Object>) efficiency.get(key);
                                if (itemData != null) {
                                    double value = getDoubleValue(itemData.get("value"));
                                    String unit = getString(itemData, "unit", "%");
                                    // 소수점 1자리까지 표시 + unit 연결
                                    displayValue = String.format("%.1f", value) + unit;
                                }
                            }
                            createCell(dataRow, colIdx++, displayValue, centerStyle);
                        }
                    }
                }
            }

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
     * URL에서 이미지 다운로드 후 엑셀에 삽입 (영역에 맞게 비율 유지하며 최적화)
     */
    private int insertImageFromUrl(XSSFWorkbook workbook, XSSFSheet sheet, String imageUrl, int startRow) throws Exception {
        InputStream imageStream = null;
        try {

            System.out.println("imageUrl >>>> " + imageUrl);

            URL url = new URL(imageUrl);
            imageStream = url.openStream();
            byte[] imageBytes = IOUtils.toByteArray(imageStream);

            // 이미지 타입 결정
            int pictureType = Workbook.PICTURE_TYPE_PNG;
            if (imageUrl.toLowerCase().contains(".jpg") || imageUrl.toLowerCase().contains(".jpeg")) {
                pictureType = Workbook.PICTURE_TYPE_JPEG;
            }

            // 이미지 크기 읽기
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            int imageWidth = bufferedImage.getWidth();
            int imageHeight = bufferedImage.getHeight();

            // 고정 영역 크기 (너비 17열, 높이 15행)
            int targetCols = 17;
            int targetRows = 15;

            // 열 너비 계산 (기본 열 너비는 약 64 픽셀, 문자 단위로 8.43 * 7.5 ≈ 63)
            // 행 높이는 26.25 포인트 = 약 35 픽셀
            double colWidthPx = 64.0;  // 기본 열 너비 (픽셀)
            double rowHeightPx = 35.0; // 행 높이 26.25pt ≈ 35px

            double targetWidthPx = targetCols * colWidthPx;
            double targetHeightPx = targetRows * rowHeightPx;

            // 비율 계산하여 영역에 맞게 축소/확대
            double scaleX = targetWidthPx / imageWidth;
            double scaleY = targetHeightPx / imageHeight;
            double scale = Math.min(scaleX, scaleY);  // 비율 유지를 위해 작은 값 선택

            // 최종 이미지 크기 (픽셀)
            double finalWidthPx = imageWidth * scale;
            double finalHeightPx = imageHeight * scale;

            // 최종 크기를 열/행 수로 변환
            double finalCols = finalWidthPx / colWidthPx;
            double finalRows = finalHeightPx / rowHeightPx;

            int pictureIdx = workbook.addPicture(imageBytes, pictureType);

            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();

            // 앵커 타입 설정 (이미지가 셀과 함께 이동하고 크기 조정됨)
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

            anchor.setCol1(0);
            anchor.setRow1(startRow);

            // 정수 부분과 소수 부분 분리하여 정밀한 위치 설정
            int endCol = (int) finalCols;
            int endRow = (int) finalRows;

            // 소수점 부분을 EMU(English Metric Units)로 변환하여 오프셋 설정
            // 1열 = 1023 * 256 EMU, 1행 = 255 * 256 EMU (근사값)
            int dx2 = (int) ((finalCols - endCol) * 1023 * 256);
            int dy2 = (int) ((finalRows - endRow) * 255 * 256);

            anchor.setCol2(endCol);
            anchor.setRow2(startRow + endRow);
            anchor.setDx2(dx2);
            anchor.setDy2(dy2);

            Picture picture = drawing.createPicture(anchor, pictureIdx);

            System.out.println("이미지 원본 크기: " + imageWidth + "x" + imageHeight);
            System.out.println("스케일: " + scale + ", 최종 열/행: " + finalCols + "/" + finalRows);

            return startRow + targetRows;  // 고정된 영역 높이 반환
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    // 무시
                }
            }
        }
    }

    /**
     * JSON 파일 또는 URL에서 mass_balance 데이터 로드
     */
    private Map<String, Object> loadMassBalanceJson(Map<String, Object> params) throws Exception {
        try {
            String jsonUrl = params != null ? (String) params.get("jsonUrl") : null;
            InputStream inputStream = null;

            if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
                // URL에서 JSON 데이터 로드
                URL url = new URL(jsonUrl);
                inputStream = url.openStream();
                System.out.println("URL에서 JSON 로드: " + jsonUrl);
            } else {
                // 기본 파일에서 로드
                ClassPathResource resource = new ClassPathResource("json/waterflow.json");
                if (!resource.exists()) {
                    throw new Exception("waterflow.json 파일을 찾을 수 없습니다.");
                }
                inputStream = resource.getInputStream();
                System.out.println("기본 파일에서 JSON 로드: waterflow.json");
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
        // 상하좌우 테두리 추가 (검정색)
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    /**
     * 헤더 스타일 생성 (특수문자 ①② 들어가는 공정명 헤더)
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
        // 배경색 흰색으로 변경
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // 상하좌우 테두리 (검정색)
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    /**
     * 단위 헤더 스타일 생성 (Item, mg/L, kg/d)
     */
    private CellStyle createUnitHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        // 배경색 흰색으로 변경
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 데이터 스타일 생성 (Item명 등 좌측 열용 - 중앙정렬)
     */
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 9);
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
     * 중앙 정렬 스타일 생성
     */
    private CellStyle createCenterStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 9);
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
     * 숫자 스타일 생성 (소수점 1자리까지 표시)
     */
    private CellStyle createNumericStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        // 소수점 1자리까지 표시하는 포맷 설정
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
        return style;
    }

    /**
     * 셀 생성 헬퍼 메서드 (문자열)
     */
    private void createCell(XSSFRow row, int column, String value, CellStyle style) {
        XSSFCell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * 숫자 셀 생성 헬퍼 메서드 (실수값 저장, 값이 없으면 '-' 표시)
     */
    private void createNumericCell(XSSFRow row, int column, Double value, CellStyle style, CellStyle textStyle) {
        XSSFCell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("-");
            cell.setCellStyle(textStyle);
        } else {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    /**
     * Map에서 문자열 값 가져오기
     */
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    /**
     * Object에서 double 값 추출
     */
    private double getDoubleValue(Object value) {
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
