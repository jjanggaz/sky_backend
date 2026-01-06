package com.wai.admin.controller.reports.calculate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.reports.calculate.RepairCalListReportService;

/**
 * 수리계산서 리포트 컨트롤러
 */
@RestController
public class RepairCalListReportController {

    private final RepairCalListReportService repairCalListService;

    public RepairCalListReportController(RepairCalListReportService repairCalListService) {
        this.repairCalListService = repairCalListService;
    }

    /**
     * 수리계산서 엑셀 다운로드
     *
     */
    @GetMapping("/v1/repaircallist.xlsx")
    public ResponseEntity<byte[]> downloadRepairCalListExcel() throws IOException {
        try {
            byte[] excelBytes = repairCalListService.generateRepairCalExcel();

            // 파일명 생성 (타임스탬프 포함)
            String fileName = "Repair_Cal_List_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            throw new IOException("수리계산서 엑셀 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
