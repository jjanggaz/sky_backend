package com.wai.admin.controller;

import com.wai.admin.controller.test.TestController;
import com.wai.admin.service.test.TestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestService testService;

    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "OK");
        mockResponse.put("message", "WAI Admin Backend is running");
        mockResponse.put("timestamp", 1234567890L);

        when(testService.getHealthStatus()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/test/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("WAI Admin Backend is running"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getInfo_ShouldReturnApplicationInfo() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("application", "WAI Admin Backend");
        mockResponse.put("version", "1.0.0");
        mockResponse.put("description", "WAI Admin Backend API");

        when(testService.getApplicationInfo()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/test/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("WAI Admin Backend"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.description").value("WAI Admin Backend API"));
    }
}
