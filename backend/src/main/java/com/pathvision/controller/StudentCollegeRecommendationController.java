package com.pathvision.controller;

import com.pathvision.dto.CollegeRecommendationRequest;
import com.pathvision.dto.CollegeRecommendationResponse;
import com.pathvision.entity.User;
import com.pathvision.service.CollegeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/college-recommendations")
public class StudentCollegeRecommendationController {

    private final CollegeService collegeService;

    public StudentCollegeRecommendationController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @PostMapping
    public ResponseEntity<?> recommend(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CollegeRecommendationRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<CollegeRecommendationResponse> result = collegeService.recommendColleges(request, user.getId());
        return ResponseEntity.ok(result);
    }
}
