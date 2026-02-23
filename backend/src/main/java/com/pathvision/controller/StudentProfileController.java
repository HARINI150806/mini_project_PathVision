package com.pathvision.controller;

import com.pathvision.entity.StudentProfile;
import com.pathvision.entity.User;
import com.pathvision.repository.StudentProfileRepository;
import com.pathvision.repository.UserRepository;
import com.pathvision.service.FileStorageService;
import com.pathvision.service.OcrService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentProfileController {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;
    private final OcrService ocrService;

    public StudentProfileController(StudentProfileRepository profileRepository,
                                    UserRepository userRepository,
                                    FileStorageService storageService,
                                    OcrService ocrService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.ocrService = ocrService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        var opt = profileRepository.findByUser(user);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "Profile not found")));
    }
    // ✅ Aggregate calculation
private Double computeAggregateFromMarks(Map<String, Integer> marks) {

    if (marks == null || marks.isEmpty()) {
        return 0.0;
    }

    double sum = 0;

    for (Integer mark : marks.values()) {
        if (mark != null) {
            sum += mark;
        }
    }

    double average = sum / marks.size();

    // Round to 2 decimal places
    return Math.round(average * 100.0) / 100.0;
}



    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveProfile(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "interests", required = false) String interestsJson,
            @RequestParam(value = "addressLine", required = false) String addressLine,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "pincode", required = false) String pincode,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "phone", required = false)
            @Pattern(regexp = "^[0-9]{10}$") String phone,
            @RequestParam(value = "stream", required = false) String stream,
            @RequestPart(value = "marksheet", required = false) MultipartFile marksheet
    ) {
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        User managedUser = userRepository.findById(user.getId()).orElse(null);
        if (managedUser == null)
            return ResponseEntity.status(400).body(Map.of("message", "User not found"));

        StudentProfile profile = profileRepository.findByUser(managedUser)
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setUser(managedUser);
                    return p;
                });

        if (interestsJson != null) profile.setInterestsJson(interestsJson);
        if (addressLine != null) profile.setAddressLine(addressLine);
        if (city != null) profile.setCity(city);
        if (state != null) profile.setState(state);
        if (pincode != null) profile.setPincode(pincode);
        if (gender != null) profile.setGender(gender);
        if (phone != null) profile.setPhone(phone);
        if (stream != null) profile.setStream(stream);

        if (marksheet != null && !marksheet.isEmpty()) { 
            String ct = marksheet.getContentType();
            if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("application/pdf"))) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid file type"));
            }
            if (marksheet.getSize() > 10L * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "File too large (max 10MB)"));
            }
            try {
                String filename = storageService.saveMarksheet(marksheet);
                String url = storageService.buildMarksheetUrl(filename);
                java.nio.file.Path path = storageService.getMarksheetPath(filename);
                // DigiLocker verification check
                if (!ocrService.isDigiLockerDocument(path.toFile())) {
                    return ResponseEntity.status(400).body(Map.of("message", "Only DigiLocker-verified marksheets are accepted. Please upload a valid DigiLocker PDF."));
                }
                profile.setMarksheetUrl(url);
                OcrService.CutoffResult result = ocrService.extractSubjectMarks(path.toFile());
                Map<String, Integer> subjectMarks = result.getMarks();
                profile.setMarksheetText(subjectMarks.toString());
                profile.setAggregatePercentage(computeAggregateFromMarks(subjectMarks));
                if (stream != null) {
                    if ("CSE".equalsIgnoreCase(stream.trim())) {
                        profile.setCsCutoff(result.getCsCutoff());
                    } else {
                        profile.setCsCutoff(null);
                    }
                }
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                return ResponseEntity.status(400).body(Map.of("message", msg != null ? msg : "File processing failed"));
            } catch (Exception e) {
                return ResponseEntity.status(400)
                        .body(Map.of("message", "File processing failed",
                                "error", e.getMessage()));
            }
        }

        // Only save profile if marksheet is valid or not provided
        if (marksheet == null || marksheet.isEmpty() || (profile.getMarksheetUrl() != null && profile.getMarksheetText() != null)) {
            try {
                StudentProfile saved = profileRepository.save(profile);
                return ResponseEntity.ok(saved);
            } catch (Exception e) {
                return ResponseEntity.status(500)
                        .body(Map.of("message", "Failed to save profile",
                                "error", e.getMessage()));
            }
        } else {
            // If marksheet was provided but not processed, do not save
            return ResponseEntity.status(400).body(Map.of("message", "Marksheet processing failed. Profile not saved."));
        }
    }

// ...existing code...
}
