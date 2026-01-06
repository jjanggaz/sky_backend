package com.wai.admin.service.test;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestService {

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
} 