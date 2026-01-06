package com.wai.admin.controller.machine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wai.admin.service.machine.VendorService;

import java.util.Map;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    /**
     * 벤더 목록 조회
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> getVendorList(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = vendorService.getVendorList(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 벤더 생성
     * 
     * @param createParams
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createVendor(@RequestBody Map<String, Object> createParams) {
        Map<String, Object> result;

        result = vendorService.createVendor(createParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 벤더 수정
     * 
     * @param vendorId
     * @param updateParams
     * @return
     */
    @PatchMapping("/update/{vendor_id}")
    public ResponseEntity<Map<String, Object>> updateVendor(@PathVariable("vendor_id") String vendorId,
            @RequestBody Map<String, Object> updateParams) {
        Map<String, Object> result;

        result = vendorService.updateVendor(vendorId, updateParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 벤더 삭제
     * 
     * @param vendorId
     * @return
     */
    @DeleteMapping("/delete/{vendor_id}")
    public ResponseEntity<Map<String, Object>> deleteVendor(@PathVariable("vendor_id") String vendorId) {
        Map<String, Object> result;

        result = vendorService.deleteVendor(vendorId);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

}
