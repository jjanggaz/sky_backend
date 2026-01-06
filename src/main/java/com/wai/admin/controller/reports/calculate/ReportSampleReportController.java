package com.wai.admin.controller.reports.calculate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wai.admin.vo.sampleVo.PayloadVo;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;
import net.sf.jasperreports.pdf.SimplePdfReportConfiguration;

@RestController
public class ReportSampleReportController {
	
	//private final ResourceLoader resourceLoader;
	private final ObjectMapper objectMapper;
	public static String json = null;

	// Manual constructor (Lombok fallback for IDE)
	public ReportSampleReportController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	  //@CrossOrigin(origins = "*", allowedHeaders = "*", allowCredentials = "false")
	  @GetMapping(value = "/v1/sales.xlsx")	  
	  public ResponseEntity<byte[]> downloadSalesXlsx() throws Exception {
		
		// Read test.json file if no request body provided		
		ClassPathResource testJson = new ClassPathResource("json/test.json");
		InputStream inputStream = testJson.getInputStream();
		json = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		System.out.println("Loaded test.json from resources");
		
		
		System.out.println("json" + json);  
		  
	    PayloadVo payload = objectMapper.readValue(json, PayloadVo.class);
	    
	    	    
	    Map<String, Object> params = new HashMap<>();
	    params.put("reportTitle", payload.getReportTitle());
	    params.put("companyName", payload.getCompany().getName());
	    params.put("companyAddress", payload.getCompany().getAddress());
	    params.put("companyItems", payload.getCompany().getItems()); // List<Item>
	    params.put("house", payload.getHouse());              // List<HouseItem>

	    
	    //Resource jrxml = resourceLoader.getResource("classpath:reports/general_statment.jrxml");
	    //ClassPathResource jrxml = new ClassPathResource("reports/general_statment.jrxml");
	    ClassPathResource jrxml = new ClassPathResource("reports/general_statment.jrxml");
	    JasperReport jr = JasperCompileManager.compileReport(jrxml.getInputStream());
	    JasperPrint jp = JasperFillManager.fillReport(jr, params, new JREmptyDataSource(1));

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    JRXlsxExporter exporter = new JRXlsxExporter();
	    exporter.setExporterInput(new SimpleExporterInput(jp));
	    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));

	    SimpleXlsxReportConfiguration conf = new SimpleXlsxReportConfiguration();
	    conf.setDetectCellType(true);
	    conf.setRemoveEmptySpaceBetweenRows(true);
	    exporter.setConfiguration(conf);

	    exporter.exportReport();

	    
        byte[] reportBytes = baos.toByteArray();
	    
        /*
	    return ResponseEntity.ok()
	        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales.xlsx")
	        .contentType(MediaType.parseMediaType(
	            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
	        .body(baos.toByteArray());
	    */
	    
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jeneral_statment.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(reportBytes);	    
	    
	  }	

	  
	  //@CrossOrigin(origins = "*", allowedHeaders = "*", allowCredentials = "false")
	  @GetMapping(value = "/v1/sales.pdf")	  
	  public ResponseEntity<byte[]> downloadSalesPdf() throws Exception {
		  
		// Read test.json file if no request body provided
		ClassPathResource testJson = new ClassPathResource("json/test.json");
		InputStream inputStream = testJson.getInputStream();
		json = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		System.out.println("Loaded test.json from resources");
		  
		System.out.println("json" + json);  
		  		  
	    PayloadVo payload = objectMapper.readValue(json, PayloadVo.class);
	    	    	    
	    Map<String, Object> params = new HashMap<>();
	    params.put("reportTitle", payload.getReportTitle());
	    params.put("companyName", payload.getCompany().getName());
	    params.put("companyAddress", payload.getCompany().getAddress());
	    params.put("companyItems", payload.getCompany().getItems()); // List<Item>
	    params.put("house", payload.getHouse());              		// List<HouseItem>

	    ClassPathResource jrxml = new ClassPathResource("reports/general_statment.jrxml");
	    JasperReport jr = JasperCompileManager.compileReport(jrxml.getInputStream());
	    JasperPrint jp = JasperFillManager.fillReport(jr, params, new JREmptyDataSource(1));

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    JRPdfExporter exporter = new JRPdfExporter();
	    exporter.setExporterInput(new SimpleExporterInput(jp));
	    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));

        SimplePdfReportConfiguration reportConf = new SimplePdfReportConfiguration();
        reportConf.setForceLineBreakPolicy(true); //등 레이아웃 옵션
	    
	    SimplePdfExporterConfiguration conf = new SimplePdfExporterConfiguration();
        //conf.setMetadataAuthor("BUKWANGTECH");
        conf.setTagged(true);
        conf.setTagLanguage("KO-KR");
        conf.setMetadataTitle("General Statement");
        conf.setDisplayMetadataTitle(true);
        //conf.setEncrypted(true); // 암호화 등 사용하려 할경우 BouncyCastle(BC) 라이브러리 의존성 주입해야 함. 
        
        exporter.setConfiguration(reportConf);
        exporter.setConfiguration(conf);
        
	    exporter.exportReport();

//	    logger.info("Excel report generated successfully");
	    
        byte[] reportBytes = baos.toByteArray();
//        logger.info("Generated Excel file size: {} bytes", reportBytes.length);	    
	    
        /*
	    return ResponseEntity.ok()
	        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales.xlsx")
	        .contentType(MediaType.parseMediaType(
	            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
	        .body(baos.toByteArray());
	    */
	    
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jeneral_statment.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportBytes);	    
	    
	  }		  
	  
	  
	
}
