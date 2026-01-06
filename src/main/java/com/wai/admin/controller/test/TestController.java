package com.wai.admin.controller.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wai.admin.service.test.TestService;

import com.wai.admin.vo.test.Sky1Vo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private TestService testService;
    
    private Properties apiProperties;
    
    public TestController() {
        loadApiProperties();
    }
    
    private void loadApiProperties() {
        apiProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("api.properties")) {
            if (input == null) {
                throw new RuntimeException("api.properties 파일을 찾을 수 없습니다.");
            }
            apiProperties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("api.properties 파일을 로드할 수 없습니다.", e);
        }
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return testService.getHealthStatus();
    }

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = testService.getApplicationInfo();
        
        // API 설정 정보 추가
        String apiBaseUrl = apiProperties.getProperty("api.base-url");
        String apiVersion = apiProperties.getProperty("api.version");
        String testEndpoint = apiProperties.getProperty("api.endpoint.test");
        
        info.put("apiBaseUrl", apiBaseUrl);
        info.put("apiVersion", apiVersion);
        info.put("testEndpoint", testEndpoint);
        info.put("fullTestUrl", apiBaseUrl + testEndpoint);
        
        return info;
    }
    
    @GetMapping("/config")
    public Map<String, Object> getApiConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Properties 파일에서 직접 가져온 값
        config.put("baseUrl", apiProperties.getProperty("api.base-url"));
        config.put("version", apiProperties.getProperty("api.version"));
        
        // 엔드포인트들
        config.put("testEndpoint", apiProperties.getProperty("api.endpoint.test"));
        config.put("userEndpoint", apiProperties.getProperty("api.endpoint.user"));
        config.put("projectEndpoint", apiProperties.getProperty("api.endpoint.project"));
        config.put("machineEndpoint", apiProperties.getProperty("api.endpoint.machine"));
        config.put("modelEndpoint", apiProperties.getProperty("api.endpoint.model"));
        config.put("processEndpoint", apiProperties.getProperty("api.endpoint.process"));
        config.put("costEndpoint", apiProperties.getProperty("api.endpoint.cost"));
        config.put("dashboardEndpoint", apiProperties.getProperty("api.endpoint.dashboard"));
        config.put("codeEndpoint", apiProperties.getProperty("api.endpoint.code"));
        
        return config;
    }

    /**
     * sky1 테이블에서 id, name 컬럼을 조회하여 반환
     * @return List<Sky1Vo> - Sky1Vo 리스트
     */
    @GetMapping("/sky1")
    public List<Sky1Vo> getSky1Data() {
        return testService.getSky1Data();
    }
} 