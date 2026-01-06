package com.wai.admin.util;

import java.util.Map;

/**
 * JSON 변환 유틸리티 클래스
 */
public class JsonUtil {
    
    /**
     * Map을 JSON 문자열로 변환
     * @param map 변환할 Map
     * @return JSON 문자열
     */
    public static String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Object Map을 JSON 문자열로 변환 (다양한 타입 지원)
     * @param map 변환할 Map
     * @return JSON 문자열
     */
    public static String objectMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(entry.getKey())).append("\":")
                .append(valueToJson(entry.getValue()));
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Object 값을 JSON 형태로 변환
     * @param value 변환할 값
     * @return JSON 문자열
     */
    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof java.util.Collection) {
            // Collection (List, Set 등) 처리
            return collectionToJson((java.util.Collection<?>) value);
        } else if (value.getClass().isArray()) {
            // 배열 처리
            return arrayToJson(value);
        } else if (value instanceof java.util.Map) {
            // Map 처리
            return objectMapToJson((java.util.Map<String, Object>) value);
        } else if (value instanceof org.springframework.web.multipart.MultipartFile) {
            // MultipartFile 처리 - 파일 정보를 JSON 객체로 변환
            org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) value;
            if (file != null && !file.isEmpty()) {
                StringBuilder fileJson = new StringBuilder("{");
                fileJson.append("\"originalFilename\":\"").append(escapeJson(file.getOriginalFilename())).append("\",");
                fileJson.append("\"contentType\":\"").append(escapeJson(file.getContentType())).append("\",");
                fileJson.append("\"size\":").append(file.getSize()).append(",");
                fileJson.append("\"name\":\"").append(escapeJson(file.getName())).append("\"");
                fileJson.append("}");
                return fileJson.toString();
            } else {
                return "null";
            }
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }
    
    /**
     * JSON 문자열에서 특수 문자 이스케이프 처리
     * @param str 원본 문자열
     * @return 이스케이프된 문자열
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\b", "\\b")
                 .replace("\f", "\\f")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * 가변 인자로 key-value 쌍을 받아서 JSON 생성
     * @param keyValues key1, value1, key2, value2, ... 형태의 가변 인자
     * @return JSON 문자열
     */
    public static String createJson(String... keyValues) {
        if (keyValues == null || keyValues.length == 0 || keyValues.length % 2 != 0) {
            return "{}";
        }
        
        Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                map.put(keyValues[i], keyValues[i + 1]);
            }
        }
        
        return mapToJson(map);
    }
    
    /**
     * Object 가변 인자로 key-value 쌍을 받아서 JSON 생성
     * @param keyValues key1, value1, key2, value2, ... 형태의 가변 인자
     * @return JSON 문자열
     */
    public static String createObjectJson(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0 || keyValues.length % 2 != 0) {
            return "{}";
        }
        
        Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length && keyValues[i] instanceof String) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        
        return objectMapToJson(map);
    }
    
    /**
     * JSON 문자열에서 특정 키의 값을 추출 (문자열 값만)
     * @param json JSON 문자열
     * @param key 추출할 키
     * @return 키에 해당하는 값 (없으면 null)
     */
    public static String extractValue(String json, String key) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        
        // 간단한 JSON 파싱 (중괄호와 따옴표만 처리)
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
    
    /**
     * JSON 문자열을 Map으로 파싱
     * @param json JSON 문자열
     * @return 파싱된 Map (실패 시 null)
     */
    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            Map<String, Object> result = new java.util.HashMap<>();
            json = json.trim();
            
            // 최상위 객체가 {}로 감싸져 있는지 확인
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return null;
            }
            
            // 중괄호 제거
            json = json.substring(1, json.length() - 1).trim();
            
            if (json.isEmpty()) {
                return result;
            }
            
            // 쉼표로 분리하여 각 키-값 쌍 처리
            String[] pairs = splitByComma(json);
            
            for (String pair : pairs) {
                String[] keyValue = splitKeyValue(pair.trim());
                if (keyValue != null && keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    // 키에서 따옴표 제거
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    
                    // 값 파싱
                    Object parsedValue = parseValue(value);
                    result.put(key, parsedValue);
                }
            }
            
            return result;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * JSON 값을 파싱 (문자열, 숫자, 불린, null, 객체, 배열)
     * @param value JSON 값 문자열
     * @return 파싱된 값
     */
    private static Object parseValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        // null 체크
        if ("null".equals(value)) {
            return null;
        }
        
        // 불린 체크
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        
        // 숫자 체크
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                // 숫자 파싱 실패 시 문자열로 처리
            }
        }
        
        // 문자열 체크
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        
        // 객체 체크
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseJson(value);
        }
        
        // 배열 체크 (간단한 구현)
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseArray(value);
        }
        
        // 기본적으로 문자열로 처리
        return value;
    }
    
    /**
     * JSON 배열을 파싱 (간단한 구현)
     * @param arrayStr 배열 문자열
     * @return 파싱된 배열 (List)
     */
    private static java.util.List<Object> parseArray(String arrayStr) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        
        try {
            // 대괄호 제거
            String content = arrayStr.substring(1, arrayStr.length() - 1).trim();
            
            if (content.isEmpty()) {
                return result;
            }
            
            // 쉼표로 분리하여 각 요소 처리
            String[] elements = splitByComma(content);
            
            for (String element : elements) {
                Object parsedElement = parseValue(element.trim());
                result.add(parsedElement);
            }
            
            return result;
        } catch (Exception e) {
            return result;
        }
    }
    
    /**
     * JSON 배열 문자열을 List로 파싱 (public 메서드)
     * @param json JSON 배열 문자열
     * @return 파싱된 List (실패 시 null)
     */
    public static java.util.List<Object> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            json = json.trim();
            
            // 배열 체크
            if (!json.startsWith("[") || !json.endsWith("]")) {
                return null;
            }
            
            return parseArray(json);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 쉼표로 분리 (중첩된 객체/배열 내의 쉼표는 무시)
     * @param json JSON 문자열
     * @return 분리된 문자열 배열
     */
    private static String[] splitByComma(String json) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char lastChar = 0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '"' && lastChar != '\\') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                } else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
            lastChar = c;
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * 키-값 쌍을 분리
     * @param pair 키-값 쌍 문자열
     * @return [키, 값] 배열
     */
    private static String[] splitKeyValue(String pair) {
        int colonIndex = -1;
        boolean inString = false;
        char lastChar = 0;
        
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            
            if (c == '"' && lastChar != '\\') {
                inString = !inString;
            } else if (!inString && c == ':') {
                colonIndex = i;
                break;
            }
            
            lastChar = c;
        }
        
        if (colonIndex == -1) {
            return null;
        }
        
        return new String[]{
            pair.substring(0, colonIndex),
            pair.substring(colonIndex + 1)
        };
    }
    
    /**
     * Map을 보기 좋은 형태의 문자열로 변환 (디버깅용)
     * @param map 변환할 Map
     * @return 보기 좋은 문자열
     */
    public static String mapToString(Map<String, Object> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("  \"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                sb.append(mapToString(nestedMap));
            } else if (value instanceof java.util.List) {
                sb.append(listToString((java.util.List<?>) value));
            } else {
                sb.append(value.toString());
            }
            sb.append(",\n");
        }
        
        // 마지막 쉼표 제거
        if (sb.charAt(sb.length() - 2) == ',') {
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * List를 보기 좋은 형태의 문자열로 변환 (디버깅용)
     * @param list 변환할 List
     * @return 보기 좋은 문자열
     */
    public static String listToString(java.util.List<?> list) {
        if (list == null) {
            return "null";
        }
        if (list.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        
        for (Object item : list) {
            sb.append("  ");
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(item).append("\"");
            } else if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                sb.append(mapToString(map));
            } else if (item instanceof java.util.List) {
                sb.append(listToString((java.util.List<?>) item));
            } else {
                sb.append(item.toString());
            }
            sb.append(",\n");
        }
        
        // 마지막 쉼표 제거
        if (sb.charAt(sb.length() - 2) == ',') {
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Collection을 JSON 배열 형태로 변환
     * @param collection 변환할 Collection
     * @return JSON 배열 문자열
     */
    private static String collectionToJson(java.util.Collection<?> collection) {
        if (collection == null) {
            return "[]";
        }
        if (collection.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Object item : collection) {
            if (!first) {
                json.append(",");
            }
            json.append(valueToJson(item));
            first = false;
        }
        
        json.append("]");
        return json.toString();
    }
    
    /**
     * 배열을 JSON 배열 형태로 변환
     * @param array 변환할 배열
     * @return JSON 배열 문자열
     */
    private static String arrayToJson(Object array) {
        if (array == null) {
            return "[]";
        }
        
        if (!array.getClass().isArray()) {
            return "[]";
        }
        
        int length = java.lang.reflect.Array.getLength(array);
        if (length == 0) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                json.append(",");
            }
            Object item = java.lang.reflect.Array.get(array, i);
            json.append(valueToJson(item));
        }
        
        json.append("]");
        return json.toString();
    }
} 