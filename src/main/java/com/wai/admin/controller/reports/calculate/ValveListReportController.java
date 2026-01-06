package com.wai.admin.controller.reports.calculate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wai.admin.service.reports.calculate.ValveListReportService;


@RestController
public class ValveListReportController {

	private final ValveListReportService valveListService;

	public ValveListReportController(ValveListReportService valveListService) {
		this.valveListService = valveListService;
	}



	/**
	 * 밸브리스트 엑셀 다운로드 (valve.json 기반)
	 */
	@GetMapping("/v1/valvelist.xlsx")
	public ResponseEntity<byte[]> downloadValveListExcel() throws IOException {

		try {
			// 엑셀 파일 생성
			byte[] excelBytes = valveListService.generateValveListExcel();

			// 파일명 생성
			String fileName = "Valve_List_" +
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
					.body(("밸브리스트 엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage()).getBytes("UTF-8"));
		}
	}


}
