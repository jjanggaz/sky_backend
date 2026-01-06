package com.wai.admin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 공통 유틸리티 클래스
 * 
 * 다양한 공통 함수들을 제공합니다.
 */
public class CommonUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);
        
    /**
     * 문자열이 null이거나 비어있는지 확인
     * 
     * @param str 확인할 문자열
     * @return null이거나 비어있으면 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이거나 비어있지 않은지 확인
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 비어있지 않으면 true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 객체가 null인지 확인
     * 
     * @param obj 확인할 객체
     * @return null이면 true
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * 객체가 null이 아닌지 확인
     * 
     * @param obj 확인할 객체
     * @return null이 아니면 true
     */
    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }
    
    /**
     * 디버그용 맵 출력
     * 
     * @param map 출력할 맵
     * @param title 제목
     */
    public static void debugMap(Map<String, Object> map, String title) {
        if (logger.isDebugEnabled()) {
            logger.debug("=== {} ===", title);
            if (map != null) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    logger.debug("{}: {}", entry.getKey(), entry.getValue());
                }
            } else {
                logger.debug("맵이 null입니다.");
            }
            logger.debug("================");
        }
    }
    
    /**
     * 디버그용 맵 출력 (제목 없음)
     * 
     * @param map 출력할 맵
     */
    public static void debugMap(Map<String, Object> map) {
        debugMap(map, "맵 내용");
    }
} 