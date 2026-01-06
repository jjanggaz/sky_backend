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
public class MaschineListReportPreviewService {

    private final ObjectMapper objectMapper;

    public MaschineListReportPreviewService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 기계리스트 HTML 생성
     */
    public String generateMachineListHtml(Map<String, Object> params) throws Exception {
        // JSON 데이터 로드
        Map<String, Object> machineData = loadMachineListJson(params);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ko\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>기계리스트</title>\n");
        html.append("    <style>\n");
        html.append("        html, body { \n");
        html.append("            font-family: 'Malgun Gothic', 'Arial', sans-serif; \n");
        html.append("            margin: 0; \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        #app, #app > main { \n");
        html.append("            height: 100%; \n");
        html.append("        }\n");
        html.append("        table { \n");
        html.append("            width: 100%; \n");
        html.append("            border-collapse: collapse; \n");
        html.append("            margin-top: 10px; \n");
        html.append("            font-size: 11pt; \n");
        html.append("        }\n");
        html.append("        .title { \n");
        html.append("            text-align: center; \n");
        html.append("            font-size: 16pt; \n");
        html.append("            font-weight: bold; \n");
        html.append("            padding: 15px 0; \n");
        html.append("            margin-bottom: 10px; \n");
        html.append("        }\n");
        html.append("        th { \n");
        html.append("            background-color: #D9D9D9; \n");
        html.append("            padding: 10px 5px; \n");
        html.append("            border: 1px solid #000000; \n");
        html.append("            font-weight: bold; \n");
        html.append("            text-align: center; \n");
        html.append("            vertical-align: middle; \n");
        html.append("            font-size: 10pt; \n");
        html.append("        }\n");
        html.append("        td { \n");
        html.append("            padding: 6px 8px; \n");
        html.append("            border: 1px solid #000000; \n");
        html.append("            vertical-align: middle; \n");
        html.append("            font-size: 10pt; \n");
        html.append("        }\n");
        html.append("        .group-header { \n");
        html.append("            font-weight: bold; \n");
        html.append("            background-color: #FFFFFF; \n");
        html.append("            padding: 12px 8px; \n");
        html.append("            border: 1px solid #000000; \n");
        html.append("            text-align: left; \n");
        html.append("            font-size: 11pt; \n");
        html.append("        }\n");
        html.append("        .center { \n");
        html.append("            text-align: center; \n");
        html.append("            vertical-align: middle; \n");
        html.append("        }\n");
        html.append("        .left { \n");
        html.append("            text-align: left; \n");
        html.append("            vertical-align: middle; \n");
        html.append("        }\n");
        html.append("        @media print {\n");
        html.append("            body { margin: 0; padding: 10mm; }\n");
        html.append("            .title { page-break-after: avoid; }\n");
        html.append("            .group-header { page-break-after: avoid; }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 제목
        html.append("    <div class=\"title\">기계리스트</div>\n");

        // 테이블 시작
        html.append("    <table class=\"tbl-preview\">\n");

        // 헤더
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>Tag No.</th>\n");
        html.append("                <th>Item</th>\n");
        html.append("                <th>Specification</th>\n");
        html.append("                <th>Vendor</th>\n");
        html.append("                <th>MODEL</th>\n");
        html.append("                <th>Material of Construction</th>\n");
        html.append("                <th>Power<br>(kw)</th>\n");
        html.append("                <th>Duty</th>\n");
        html.append("                <th>Sty</th>\n");
        html.append("                <th>Tot</th>\n");
        html.append("                <th>Unit</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");

        // equipments 배열 데이터 처리
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
                html.append("            <tr>\n");
                html.append("                <td colspan=\"11\" class=\"group-header\">■ ").append(processName)
                        .append("</td>\n");
                html.append("            </tr>\n");

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

                        html.append("            <tr>\n");
                        html.append("                <td class=\"center\">").append(getString(item, "tag_number"))
                                .append("</td>\n");
                        html.append("                <td class=\"left\">").append(getString(item, "name"))
                                .append("</td>\n");
                        html.append("                <td class=\"left\">").append(specification)
                                .append("</td>\n");
                        html.append("                <td class=\"left\">").append(getString(item, "manufacturer"))
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getString(item, "model_name"))
                                .append("</td>\n");
                        html.append("                <td class=\"left\">")
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getNumber(item, "power_kW"))
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getNumber(item, "normal_count"))
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getNumber(item, "spare_count"))
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getNumber(item, "qty"))
                                .append("</td>\n");
                        html.append("                <td class=\"center\">").append(getString(item, "unit"))
                                .append("</td>\n");
                        html.append("            </tr>\n");
                    }
                }
            }
        }

        html.append("        </tbody>\n");
        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }


    /**
     * JSON 파일 또는 URL에서 데이터 로드
     */
    private Map<String, Object> loadMachineListJson(Map<String, Object> params) throws Exception {
        try {
            String jsonUrl = (String) params.get("jsonUrl");
            InputStream inputStream = null;

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
        if (value == null)
            return "";
        return value.toString();
    }

    /**
     * Map에서 숫자 값 가져오기
     */
    private String getNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return "";
        if (value.toString().equals("null"))
            return "";
        return value.toString();
    }
}
