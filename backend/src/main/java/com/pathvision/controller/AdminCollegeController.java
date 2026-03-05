package com.pathvision.controller;

import com.pathvision.dto.CreateCollegeCutoffRequest;
import com.pathvision.dto.CollegeCutoffResponse;
import com.pathvision.dto.CollegeResponse;
import com.pathvision.dto.CollegeUploadResponse;
import com.pathvision.dto.CreateCollegeRequest;
import com.pathvision.service.CollegeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/colleges")
public class AdminCollegeController {

    private final CollegeService collegeService;

    public AdminCollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollegeResponse createCollege(@RequestBody @Valid CreateCollegeRequest request) {
        return collegeService.createCollege(request);
    }

    @GetMapping
    public List<CollegeResponse> getAllColleges() {
        return collegeService.getAllColleges();
    }

    @PostMapping("/upload")
    public CollegeUploadResponse uploadColleges(@RequestParam("file") MultipartFile file) {
        return collegeService.uploadCollegesCsv(file);
    }

    @PostMapping("/{collegeId}/cutoffs")
    @ResponseStatus(HttpStatus.CREATED)
    public void addOrUpdateCutoff(@PathVariable Long collegeId, @RequestBody @Valid CreateCollegeCutoffRequest request) {
        collegeService.addOrUpdateCutoff(collegeId, request);
    }

    @PostMapping("/cutoffs/upload")
    public CollegeUploadResponse uploadCutoffs(@RequestParam("file") MultipartFile file) {
        return collegeService.uploadCutoffsCsv(file);
    }

    @GetMapping("/cutoffs")
    public List<CollegeCutoffResponse> getAllCutoffs() {
        return collegeService.getAllCutoffs();
    }
}
