package com.wai.admin.controller.cost;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wai.admin.service.cost.CostService;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/cost")
public class CostController {

    @Autowired
    private CostService costService;

    /**
     * 벤더 목록 조회 (POST)
     * 
     * @param searchParams
     * @return
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> getCostList(@RequestBody Map<String, Object> searchParams) {
        Map<String, Object> result;

        result = costService.getCostList(searchParams);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping
    public Map<String, Object> getAllCosts() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costListSuccess");
        response.put("data", costService.getAllCosts());
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCostById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costDetailSuccess");
        response.put("data", costService.getCostById(id));
        return response;
    }

    @PostMapping
    public Map<String, Object> createCost(@RequestBody Map<String, Object> costData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costCreateSuccess");
        response.put("data", costService.createCost(costData));
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCost(@PathVariable String id, @RequestBody Map<String, Object> costData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costUpdateSuccess");
        response.put("data", costService.updateCost(id, costData));
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCost(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costDeleteSuccess");
        response.put("data", costService.deleteCost(id));
        return response;
    }

    @GetMapping("/summary")
    public Map<String, Object> getCostSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.costSummarySuccess");
        response.put("data", costService.getCostSummary());
        return response;
    }
}