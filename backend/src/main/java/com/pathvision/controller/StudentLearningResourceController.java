package com.pathvision.controller;

import com.pathvision.dto.LearningResourceResponse;
import com.pathvision.entity.User;
import com.pathvision.service.LearningResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/resources")
public class StudentLearningResourceController {

    private final LearningResourceService learningResourceService;

    public StudentLearningResourceController(LearningResourceService learningResourceService) {
        this.learningResourceService = learningResourceService;
    }

    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommended(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "source", defaultValue = "all") String source
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<LearningResourceResponse> result = learningResourceService.getRecommendedForStudent(user.getId(), source);
        return ResponseEntity.ok(result);
    }
}
