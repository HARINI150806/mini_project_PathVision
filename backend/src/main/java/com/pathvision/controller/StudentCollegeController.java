package com.pathvision.controller;

import com.pathvision.dto.CollegeResponse;
import com.pathvision.service.CollegeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/colleges")
public class StudentCollegeController {

    private final CollegeService collegeService;

    public StudentCollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public List<CollegeResponse> getAllCollegesForStudent(
            @RequestParam(value = "community", required = false) String community
    ) {
        return collegeService.getAllColleges(community);
    }
}
