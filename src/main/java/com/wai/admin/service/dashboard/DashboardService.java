package com.wai.admin.service.dashboard;

import com.wai.admin.controller.dashboard.system.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    /**
     * 서버 정보를 포함한 대시보드 데이터를 수집합니다.
     */
    public DashboardData collectDashboardData() {
        // 서버 상태 정보 수집
        ServerStatus serverStatus = collectServerStatus();
        
        // 프로젝트 통계 수집
        ProjectStats projectStats = collectProjectStats();
        
        // 3D 모델 통계 수집
        ModelStats modelStats = collectModelStats();
        
        // 승인 통계 수집
        ApprovalStats approvalStats = collectApprovalStats();
        
        return DashboardData.builder()
            .serverStatus(serverStatus)
            .projectStats(projectStats)
            .modelStats(modelStats)
            .approvalStats(approvalStats)
            .build();
    }

    /**
     * 서버 상태 정보를 수집합니다.
     */
    private ServerStatus collectServerStatus() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            // CPU 정보
            double cpuUsage = 0.0;
            try {
                // Java 9+에서는 다른 방법으로 CPU 사용률을 가져와야 함
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                    cpuUsage = sunOsBean.getCpuLoad() * 100;
                }
            } catch (Exception e) {
                log.warn("CPU usage information not available", e);
            }
            
            int cpuCores = osBean.getAvailableProcessors();
            double cpuTemp = getCPUTemperature();
            
            // 메모리 정보
            long totalMem = 0;
            long freeMem = 0;
            try {
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                    totalMem = sunOsBean.getTotalPhysicalMemorySize();
                    freeMem = sunOsBean.getFreePhysicalMemorySize();
                }
            } catch (Exception e) {
                log.warn("Memory information not available", e);
            }
            long usedMem = totalMem - freeMem;
            double memUsage = totalMem > 0 ? ((double) usedMem / totalMem) * 100 : 0.0;
            
            // 디스크 정보
            long diskTotal = 0;
            long diskFree = 0;
            long diskUsed = 0;
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // Windows 환경에서는 C: 드라이브 정보 사용
                    File root = new File("C:");
                    diskTotal = root.getTotalSpace();
                    diskFree = root.getFreeSpace();
                } else {
                    // Linux/Unix 환경
                    File root = new File("/");
                    diskTotal = root.getTotalSpace();
                    diskFree = root.getFreeSpace();
                }
                diskUsed = diskTotal - diskFree;
            } catch (Exception e) {
                log.warn("Disk information not available", e);
            }
            
            // 시스템 정보
            long uptime = runtimeBean.getUptime() / 1000; // 초 단위
            double[] loadAverage = new double[3];
            try {
                loadAverage[0] = osBean.getSystemLoadAverage();
                // 1분, 5분, 15분 평균을 시뮬레이션 (실제로는 OS에서 제공하는 값이 제한적)
                loadAverage[1] = loadAverage[0] * 0.8;
                loadAverage[2] = loadAverage[0] * 0.6;
            } catch (Exception e) {
                log.warn("Load average information not available", e);
                loadAverage = new double[]{0.0, 0.0, 0.0};
            }
            String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");
            
            return ServerStatus.builder()
                .capacity(Capacity.builder()
                    .total(diskTotal)
                    .used(diskUsed)
                    .free(diskFree)
                    .build())
                .cpu(CPU.builder()
                    .usage((int) Math.round(cpuUsage))
                    .cores(cpuCores)
                    .temperature((int) Math.round(cpuTemp))
                    .build())
                .ram(RAM.builder()
                    .total(totalMem)
                    .used(usedMem)
                    .free(freeMem)
                    .usage((int) Math.round(memUsage))
                    .build())
                .system(SystemInfo.builder()
                    .uptime(uptime)
                    .loadAverage(loadAverage)
                    .osInfo(osInfo)
                    .build())
                .build();
                
        } catch (Exception e) {
            log.error("Error collecting server status", e);
            return ServerStatus.builder()
                .capacity(Capacity.builder().total(0L).used(0L).free(0L).build())
                .cpu(CPU.builder().usage(0).cores(0).temperature(0).build())
                .ram(RAM.builder().total(0L).used(0L).free(0L).usage(0).build())
                .system(SystemInfo.builder().uptime(0L).loadAverage(new double[]{0, 0, 0}).osInfo("Unknown").build())
                .build();
        }
    }

    /**
     * CPU 온도를 가져옵니다.
     */
    private double getCPUTemperature() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                // Linux 시스템에서 CPU 온도 읽기
                String temp = Files.readString(Paths.get("/sys/class/thermal/thermal_zone0/temp"));
                return Double.parseDouble(temp.trim()) / 1000.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 프로젝트 통계를 수집합니다.
     */
    private ProjectStats collectProjectStats() {
        try {
            // TODO: 실제 프로젝트 리포지토리에서 데이터 조회
            long total = 10; // projectRepository.count();
            long inProgress = 5; // projectRepository.countByStatus("IN_PROGRESS");
            long completed = 3; // projectRepository.countByStatus("COMPLETED");
            
            return ProjectStats.builder()
                .total(total)
                .inProgress(inProgress)
                .completed(completed)
                .build();
        } catch (Exception e) {
            log.error("Error collecting project stats", e);
            return ProjectStats.builder().total(0L).inProgress(0L).completed(0L).build();
        }
    }

    /**
     * 3D 모델 통계를 수집합니다.
     */
    private ModelStats collectModelStats() {
        try {
            // TODO: 실제 모델 리포지토리에서 데이터 조회
            long total = 25; // modelRepository.count();
            long pending = 8; // modelRepository.countByStatus("PENDING");
            long approved = 15; // modelRepository.countByStatus("APPROVED");
            
            return ModelStats.builder()
                .total(total)
                .pending(pending)
                .approved(approved)
                .build();
        } catch (Exception e) {
            log.error("Error collecting model stats", e);
            return ModelStats.builder().total(0L).pending(0L).approved(0L).build();
        }
    }

    /**
     * 승인 통계를 수집합니다.
     */
    private ApprovalStats collectApprovalStats() {
        try {
            // TODO: 실제 승인 리포지토리에서 데이터 조회
            long total = 30; // approvalRepository.count();
            long pending = 12; // approvalRepository.countByStatus("PENDING");
            long approved = 15; // approvalRepository.countByStatus("APPROVED");
            long rejected = 3; // approvalRepository.countByStatus("REJECTED");
            
            return ApprovalStats.builder()
                .total(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .build();
        } catch (Exception e) {
            log.error("Error collecting approval stats", e);
            return ApprovalStats.builder().total(0L).pending(0L).approved(0L).rejected(0L).build();
        }
    }
} 