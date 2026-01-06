package com.wai.admin.controller.reports.calculate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.reports.calculate.StructureListReportPreviewService;


@RestController
@RequestMapping("/api/report/")
public class StructureListReportPreviewController {

	@Autowired
	private StructureListReportPreviewService structureListReportPreviewService;


	/**
	 * 구조물리스트 HTML 응답 (structure.json 기반)
	 */
	@PostMapping("/v1/structureListPreview")
	public ResponseEntity<String> getStructureListHtml(
		@RequestBody(required = false) Map<String, Object> params) throws IOException {

		System.out.println("json >>> " + params);

		if (params == null) {
			params = new HashMap<>();
		}

		try {
			// HTML 생성
			String html = structureListReportPreviewService.generateStructureListHtml(params);

			// 응답 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_HTML);
			headers.add("Content-Type", "text/html; charset=UTF-8");

			return ResponseEntity.ok()
					.headers(headers)
					.body(html);

		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body("<html><body><h1>오류 발생</h1><p>구조물리스트 HTML 생성 중 오류가 발생했습니다: " + e.getMessage() + "</p></body></html>");
		}
	}


}
