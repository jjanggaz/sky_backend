package com.wai.admin.controller.reports.calculate;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wai.admin.service.reports.calculate.ProcessCapaReportPreviewService;

@RestController
@RequestMapping("/api/report/")
public class ProcessCapaReportPreviewController {

	@Autowired
	private ProcessCapaReportPreviewService processCapaReportPreviewService;


	@PostMapping(value = "/v1/getTabContentHtml")
	public ResponseEntity<byte[]> getTabContentHtml(@RequestBody Map<String, Object> searchParams) throws Exception {

		System.out.println("searchParams >>> " + searchParams);

		// Service를 통해 HTML 생성
		byte[] htmlBytes = processCapaReportPreviewService.generateTabContentHtml(searchParams);

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
			.contentType(MediaType.TEXT_HTML)
			.body(htmlBytes);
	}

	@PostMapping(value = "/v1/getTestExcel")
	public ResponseEntity<byte[]> TestDownSample(@RequestBody Map<String, Object> searchParams) throws Exception {

		System.out.println("searchParams >>> " + searchParams);

		// Service를 통해 Excel 생성 (DATA IN sheet 포함)
		byte[] excelBytes = processCapaReportPreviewService.generateTabContentExcel(searchParams);

		// 파일명 생성 (fileName 파라미터가 있으면 사용, 없으면 기본값)
		String fileName = (String) searchParams.getOrDefault("fileName", "ProcessCapaReport");
		String tabName = (String) searchParams.getOrDefault("tabName", "Sheet");
		String downloadFileName = fileName + "_" + tabName + ".xlsx";

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"")
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(excelBytes);
	}

	@PostMapping(value = "/v1/getTabNames")
	public ResponseEntity<TabNamesResponse> getTabNames(@RequestBody Map<String, Object> searchParams) {
		try {

			// 1. json url 을 파싱해서 project_id 값 추출
			String project_id = projectParsing(searchParams);
			System.out.println("projectId >> " + project_id);

			// 2. project_id 값을 이용해 unit_system(METRIC/USCS) 을 조회.
			String unit_system_code = processCapaReportPreviewService.getUnitSystem(project_id);
			System.out.println("unitSystem >> " + unit_system_code);

			// searchParams에 projectId와 unitSystem 추가
			searchParams.put("project_id", project_id);
			searchParams.put("unit_system_code", unit_system_code);

			System.out.println("searchParams >> " + searchParams);

			// Service를 통해 탭명 추출
			TabNamesResponseData responseData = processCapaReportPreviewService.extractTabNames(searchParams);

			// 성공 응답 생성
			TabNamesResponse response = TabNamesResponse.builder()
				.success(true)
				.status(200)
				.message("성공")
				.response(responseData)
				.build();

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			// 에러 응답 생성
			TabNamesResponse errorResponse = TabNamesResponse.builder()
				.success(false)
				.status(500)
				.message("탭명 추출 중 오류가 발생했습니다: " + e.getMessage())
				.response(null)
				.build();

			return ResponseEntity.status(500).body(errorResponse);
		}
	}

	/**
	 * JSON URL에서 project_id 파싱
	 */
	private String projectParsing(Map<String, Object> searchParams) throws Exception {

		String jsonUrl = (String) searchParams.get("jsonUrl");

		System.out.println("jsonUrl >> " + jsonUrl);

		if (jsonUrl == null || jsonUrl.trim().isEmpty()) {
			throw new Exception("jsonUrl이 제공되지 않았습니다.");
		}

		try {
			// URL에서 JSON 데이터 다운로드
			URL url = new URL(jsonUrl);
			InputStream inputStream = url.openStream();
			String jsonContent = StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
			inputStream.close();

			// JSON 파싱
			ObjectMapper objectMapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			Map<String, Object> jsonData = objectMapper.readValue(jsonContent, Map.class);

			// project_id 추출
			String projectId = (String) jsonData.get("project_id");

			if (projectId == null || projectId.trim().isEmpty()) {
				throw new Exception("JSON 데이터에서 project_id를 찾을 수 없습니다.");
			}

			return projectId;

		} catch (Exception e) {
			throw new Exception("project_id 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}
}
