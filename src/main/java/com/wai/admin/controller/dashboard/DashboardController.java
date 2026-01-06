package com.wai.admin.controller.dashboard;

import com.wai.admin.controller.dashboard.system.DashboardData;
import com.wai.admin.controller.dashboard.system.DashboardResponse;
import com.wai.admin.service.dashboard.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * 서버 정보를 포함한 대시보드 데이터를 조회합니다.
     */
    @GetMapping("/server")
    public ResponseEntity<DashboardResponse> getServerDashboardData() {
        try {
            DashboardData dashboardData = dashboardService.collectDashboardData();
            
            DashboardResponse response = DashboardResponse.builder()
                .success(true)
                .message("대시보드 데이터를 성공적으로 가져왔습니다.")
                .data(dashboardData)
                .build();
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Dashboard API error", e);
            
            DashboardResponse errorResponse = DashboardResponse.builder()
                .success(false)
                .message("서버 내부 오류가 발생했습니다.")
                .error(e.getMessage())
                .build();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

} 