package com.wai.admin.controller.reports.calculate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.reports.calculate.StructureListReportService;


@RestController
@RequestMapping("/api/report/")
public class StructureListReportController {
	
	@Autowired
	private StructureListReportService structureListReportService;
	
	/**
	 * 구조물리스트 엑셀 다운로드 (structure.json 기반)
	 */
	@PostMapping("/v1/structurelist")
	public ResponseEntity<byte[]> downloadStructureListExcel(
		@RequestBody(required = false) Map<String, Object> params) throws IOException {

		System.out.println("structurelist.xlsx 다운로드");
		try {
			// 엑셀 파일 생성 (params 전달하여 jsonUrl 기반으로 생성)
			byte[] excelBytes = structureListReportService.generateStructureListExcel(params);
			
			// 파일명 생성
			String fileName = "Structure_List_" + 
				new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
			
			// 응답 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDispositionFormData("attachment", fileName);
			headers.setContentLength(excelBytes.length);
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(excelBytes);
			
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(("구조물리스트 엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage()).getBytes("UTF-8"));
		}
	}


}
