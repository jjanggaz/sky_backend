package com.wai.admin.service.test;

import com.wai.admin.mapper.test.TestMapper;
import com.wai.admin.vo.test.Sky1Vo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestService {

    private static final Logger logger = LoggerFactory.getLogger(TestService.class);
    
    private final TestMapper testMapper;

    public TestService(TestMapper testMapper) {
        this.testMapper = testMapper;
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "OK");
        data.put("message", "WAI Admin Backend is running");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    public Map<String, Object> getApplicationInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("application", "WAI Admin Backend");
        data.put("version", "1.0.0");
        data.put("description", "WAI Admin Backend API");
        return data;
    }

    /**
     * sky1 테이블에서 id, name 컬럼을 조회하여 반환
     * @return List<Sky1Vo> - Sky1Vo 리스트
     */
    public List<Sky1Vo> getSky1Data() {
        logger.debug("sky1 테이블 조회 시작");
        
        try {
            List<Sky1Vo> result = testMapper.selectSky1List();
            logger.debug("sky1 테이블 조회 성공: {} 건", result.size());
            return result;
        } catch (Exception e) {
            logger.error("sky1 테이블 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("sky1 테이블 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 