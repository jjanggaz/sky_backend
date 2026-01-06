package com.wai.admin.service.reports.calculate;


import java.io.InputStream;
import java.util.List;
import java.util.Map;


import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StructureListReportPreviewService {

    private final ObjectMapper objectMapper;

    public StructureListReportPreviewService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 구조물 리스트 HTML 파일 생성
     */
    public String generateStructureListHtml(Map<String, Object> params) throws Exception {
        // JSON 데이터 로드
        Map<String, Object> structureData = loadStructureListJson(params);
        System.out.println("structureData >>> " + structureData);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ko\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>구조물 리스트</title>\n");
        html.append("    <style>\n");
        html.append("        html, body { \n");
        html.append("            font-family: 'Malgun Gothic', 'Arial', sans-serif; \n");
        html.append("            margin: 0; \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        #app, #app > main { \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        h1 { text-align: center; font-size: 24px; margin: 20px 0; }\n");
        html.append("        h2 { font-size: 18px; margin: 30px 0 10px 0; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin-bottom: 30px; table-layout: fixed; }\n");
        html.append("        th, td { border: 1px solid #000; padding: 8px; text-align: center; }\n");
        html.append("        th { background-color: #ffffff; font-weight: bold; }\n");
        html.append("        td.left { text-align: left; }\n");
        html.append("        td.right { text-align: right; }\n");
        html.append("        .footer { margin-top: 20px; font-size: 14px; }\n");
        html.append("        .col-name { width: 15%; }\n");
        html.append("        .col-volume { width: 10%; }\n");
        html.append("        .col-spec { width: 7%; }\n");
        html.append("        .col-design { width: 7%; }\n");
        html.append("        .col-note { width: 10%; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 제목
        html.append("    <h1>구조물 리스트</h1>\n");

        // structures 배열 데이터 처리
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> structures = (List<Map<String, Object>>) structureData.get("structures");

        if (structures != null) {
            // code_key별로 데이터를 분류
            java.util.Map<String, java.util.List<Map<String, Object>>> groupedStructures = new java.util.LinkedHashMap<>();
            groupedStructures.put("S_CONC", new java.util.ArrayList<>());
            groupedStructures.put("S_BUILD", new java.util.ArrayList<>());
            groupedStructures.put("S_STSREC", new java.util.ArrayList<>());
            groupedStructures.put("S_STSCIR", new java.util.ArrayList<>());

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

            // 각 code_key별로 테이블 생성
            int sectionNumber = 1;

            // 1. 토목 (S_CONC)
            if (!groupedStructures.get("S_CONC").isEmpty()) {
                html.append("    <h2>").append(sectionNumber++).append(". 토목</h2>\n");
                appendStructureTable(html, groupedStructures.get("S_CONC"), "S_CONC");
            }

            // 2. 건축 (S_BUILD)
            if (!groupedStructures.get("S_BUILD").isEmpty()) {
                html.append("    <h2>").append(sectionNumber++).append(". 건축</h2>\n");
                appendStructureTable(html, groupedStructures.get("S_BUILD"), "S_BUILD");
            }

            // 3. 각형탱크(STS) (S_STSREC)
            if (!groupedStructures.get("S_STSREC").isEmpty()) {
                html.append("    <h2>").append(sectionNumber++).append(". 각형탱크(STS)</h2>\n");
                appendStructureTable(html, groupedStructures.get("S_STSREC"), "S_STSREC");
            }

            // 4. 원형탱크(STS) (S_STSCIR)
            if (!groupedStructures.get("S_STSCIR").isEmpty()) {
                html.append("    <h2>").append(sectionNumber++).append(". 원형탱크(STS)</h2>\n");
                appendStructureTable(html, groupedStructures.get("S_STSCIR"), "S_STSCIR");
            }
        }

        // 푸터
        html.append("    <div class=\"footer\">소요 및 적용 면적은 개략검토된 내용으로써 상세 설계를 통한 산정필요</div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * code_key별 구조물 테이블 생성
     */
    private void appendStructureTable(StringBuilder html, java.util.List<Map<String, Object>> items, String codeKey) {
        html.append("    <table class=\"tbl-preview\">\n");
        html.append("        <thead>\n");

        System.out.println("-------------------------");
        System.out.println("codeKey >>>>>>> " + codeKey);
        System.out.println("-------------------------");

        // code_key에 따라 헤더 구조가 다름
        if ("S_BUILD".equals(codeKey)) {
            // S_BUILD 헤더 구조(건축) - Design Criteria, Value, Unit 제외
            html.append("            <tr>\n");
            html.append("                <th rowspan=\"2\">구조물 이름</th>\n");
            html.append("                <th colspan=\"5\">적용 구조물 내부 규격</th>\n");
            html.append("                <th rowspan=\"2\">비고</th>\n");
            html.append("            </tr>\n");
            html.append("            <tr>\n");
            html.append("                <th>W(m)</th>\n");
            html.append("                <th>L(m)</th>\n");
            html.append("                <th>He(m)</th>\n");
            html.append("                <th>H(m)</th>\n");
            html.append("                <th>지수(EA)</th>\n");
            html.append("            </tr>\n");
        } else if("S_CONC".equals(codeKey)){
            // S_CONC 헤더 구조(토목)
            html.append("            <tr>\n");
            html.append("                <th rowspan=\"2\">구조물 이름</th>\n");
            html.append("                <th rowspan=\"2\">소요 Volume (㎥)</th>\n");
            html.append("                <th rowspan=\"2\">적용 유효 Volume (㎥)</th>\n");
            html.append("                <th colspan=\"5\">적용 구조물 내부 규격</th>\n");
            html.append("                <th colspan=\"2\">Design Criteria</th>\n");
            html.append("                <th rowspan=\"2\">비고</th>\n");
            html.append("            </tr>\n");
            html.append("            <tr>\n");
            html.append("                <th>W(m)</th>\n");
            html.append("                <th>L(m)</th>\n");
            html.append("                <th>He(m)</th>\n");
            html.append("                <th>H(m)</th>\n");
            html.append("                <th>지수(EA)</th>\n");
            html.append("                <th>Value</th>\n");
            html.append("                <th>Unit</th>\n");
            html.append("            </tr>\n");
        } else if("S_STSREC".equals(codeKey)){
            // S_STSREC 헤더 구조(각형탱크(STS))
            html.append("            <tr>\n");
            html.append("                <th rowspan=\"2\">구조물 이름</th>\n");
            html.append("                <th rowspan=\"2\">소요 Volume (㎥)</th>\n");
            html.append("                <th rowspan=\"2\">적용 유효 Volume (㎥)</th>\n");
            html.append("                <th colspan=\"5\">적용 구조물 내부 규격</th>\n");
            html.append("                <th colspan=\"2\">Design Criteria</th>\n");
            html.append("                <th rowspan=\"2\">비고</th>\n");
            html.append("            </tr>\n");
            html.append("            <tr>\n");
            html.append("                <th>W(m)</th>\n");
            html.append("                <th>L(m)</th>\n");
            html.append("                <th>He(m)</th>\n");
            html.append("                <th>H(m)</th>\n");
            html.append("                <th>지수(EA)</th>\n");
            html.append("                <th>Value</th>\n");
            html.append("                <th>Unit</th>\n");
            html.append("            </tr>\n");
        } else if("S_STSCIR".equals(codeKey)){
            // S_STSCIR은 동일한 헤더 구조(원형태크(STS))
            html.append("            <tr>\n");
            html.append("                <th rowspan=\"2\">구조물 이름</th>\n");
            html.append("                <th rowspan=\"2\">소요 Volume (㎥)</th>\n");
            html.append("                <th rowspan=\"2\">적용 유효 Volume (㎥)</th>\n");
            html.append("                <th colspan=\"5\">적용 구조물 내부 규격</th>\n");
            html.append("                <th colspan=\"2\">Design Criteria</th>\n");
            html.append("                <th rowspan=\"2\">비고</th>\n");
            html.append("            </tr>\n");
            html.append("            <tr>\n");
            html.append("                <th>D(m)</th>\n");
            html.append("                <th>-</th>\n");
            html.append("                <th>He(m)</th>\n");
            html.append("                <th>H(m)</th>\n");
            html.append("                <th>지수(EA)</th>\n");
            html.append("                <th>Value</th>\n");
            html.append("                <th>Unit</th>\n");
            html.append("            </tr>\n");
        }

        html.append("        </thead>\n");
        html.append("        <tbody>\n");

        // 데이터 행 생성
        for (Map<String, Object> item : items) {
            html.append("            <tr>\n");
            html.append("                <td class=\"left\">").append(getString(item, "name")).append("</td>\n");

            // 건축은 소요/적용유효 컬럼이 없음
            if (!"S_BUILD".equals(codeKey)) {
                html.append("                <td class=\"right\">").append(getNumber(item, "required_capacity")).append("</td>\n");
                html.append("                <td class=\"right\">").append(getNumber(item, "applied_capacity")).append("</td>\n");
            }

            html.append("                <td>").append(getNumber(item, "W")).append("</td>\n");
            if("S_STSCIR".equals(codeKey)){
                html.append("                <td>").append("").append("</td>\n");
            }else{
                html.append("                <td>").append(getNumber(item, "L")).append("</td>\n");
            }
            // water_level "-" 처리
            if(!"S_BUILD".equals(codeKey)){
                html.append("                <td>").append(getNumber(item, "water_level")).append("</td>\n");
            }else {
                html.append("                <td>").append("-").append("</td>\n");
            }

            html.append("                <td>").append(getNumber(item, "height")).append("</td>\n");
            html.append("                <td>").append(getNumber(item, "tank_number")).append("</td>\n");

            // 건축(S_BUILD)은 Value, Unit 컬럼이 없음
            if (!"S_BUILD".equals(codeKey)) {
                html.append("                <td>").append("").append("</td>\n"); // Value - 빈값
                html.append("                <td>").append("hr(HRT)").append("</td>\n"); // Unit - hr(HRT)
            }

            html.append("                <td class=\"left\">").append("").append("</td>\n"); // 비고 - 빈값
            html.append("            </tr>\n");
        }

        html.append("        </tbody>\n");
        html.append("    </table>\n");
    }
    
  
    /**
     * JSON 파일 로드
     */
    private Map<String, Object> loadStructureListJson(Map<String, Object> params) throws Exception {

        try {
            String jsonUrl = (String) params.get("jsonUrl");
            InputStream inputStream = null;

            System.out.println("jsonUrl :" + jsonUrl);
            // jsonUrl이 있으면 URL에서 로드, 없으면 기본 파일에서 로드
            if (jsonUrl != null && !jsonUrl.trim().isEmpty()) {
                // URL에서 JSON 데이터 로드
                java.net.URL url = new java.net.URL(jsonUrl);
                inputStream = url.openStream();
                System.out.println("inputStream :  " + inputStream);
            } else {
                // 기본 파일에서 로드
                ClassPathResource resource = new ClassPathResource("json/projectSampleOrg.json");
                if (!resource.exists()) {
                    throw new Exception("projectSampleOrg.json 파일을 찾을 수 없습니다.");
                }
                inputStream = resource.getInputStream();
            }

            try (InputStream stream = inputStream) {
                String jsonContent = StreamUtils.copyToString(stream, java.nio.charset.StandardCharsets.UTF_8);
                return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception e) {
            throw new Exception("JSON 데이터 로드 중 오류가 발생했습니다: " + e.getMessage(), e);
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
            // 숫자로 변환 시도
            double numValue = Double.parseDouble(value.toString());

            // 소수점 이하 자릿수 확인
            String strValue = value.toString();
            int decimalPlaces = 0;
            if (strValue.contains(".")) {
                String[] parts = strValue.split("\\.");
                if (parts.length > 1) {
                    decimalPlaces = parts[1].length();
                }
            }

            // 소수점 2자리 초과하는 경우만 2자리로 제한
            if (decimalPlaces > 2) {
                return String.format("%.2f", numValue);
            } else {
                // 2자리 이하는 원본 그대로 반환
                return value.toString();
            }
        } catch (NumberFormatException e) {
            // 숫자가 아닌 경우 원본 문자열 반환
            return value.toString();
        }
    }
    

}
