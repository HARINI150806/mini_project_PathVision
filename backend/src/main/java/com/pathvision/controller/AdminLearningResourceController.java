package com.pathvision.controller;

import com.pathvision.dto.CreateLearningResourceRequest;
import com.pathvision.dto.LearningResourceResponse;
import com.pathvision.dto.LearningResourceUploadResponse;
import com.pathvision.service.LearningResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/resources")
public class AdminLearningResourceController {

    private final LearningResourceService learningResourceService;

    public AdminLearningResourceController(LearningResourceService learningResourceService) {
        this.learningResourceService = learningResourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningResourceResponse create(@RequestBody @Valid CreateLearningResourceRequest request) {
        return learningResourceService.create(request);
    }

    @GetMapping
    public List<LearningResourceResponse> getAll() {
        return learningResourceService.getAll();
    }

    @PostMapping("/upload")
    public LearningResourceUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return learningResourceService.uploadCsv(file);
    }

    @PostMapping("/sync")
    public Map<String, Object> syncNow() {
        int count = learningResourceService.syncAllSources();
        return Map.of(
                "message", "Sync completed",
                "updatedCount", count
        );
    }
}
