package com.wai.admin.controller.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wai.admin.service.model.ModelService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    public Map<String, Object> getAllModels() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelListSuccess");
        response.put("data", modelService.getAllModels());
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getModelById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelDetailSuccess");
        response.put("data", modelService.getModelById(id));
        return response;
    }

    @PostMapping
    public Map<String, Object> createModel(@RequestBody Map<String, Object> modelData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelCreateSuccess");
        response.put("data", modelService.createModel(modelData));
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateModel(@PathVariable String id, @RequestBody Map<String, Object> modelData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelUpdateSuccess");
        response.put("data", modelService.updateModel(id, modelData));
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteModel(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelDeleteSuccess");
        response.put("data", modelService.deleteModel(id));
        return response;
    }

    @GetMapping("/{id}/version")
    public Map<String, Object> getModelVersions(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "messages.success.modelVersionSuccess");
        response.put("data", modelService.getModelVersions(id));
        return response;
    }
} 