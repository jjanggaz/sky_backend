package com.wai.admin.controller.reports.calculate;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.reports.calculate.ProcessCapaReportService;

@RestController
@RequestMapping("/api/report/")
public class ProcessCapaReportController {

	@Autowired
	private ProcessCapaReportService processCapaReportService;

	
	@PostMapping(value = "/v1/processcapa")
	public ResponseEntity<byte[]> downloadIntegrationXlsx(@RequestBody Map<String, Object> searchParams) throws Exception {
		byte[] reportBytes = processCapaReportService.generateIntegrationXlsx(searchParams);

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processcapacity.xlsx")
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(reportBytes);
	}
}
