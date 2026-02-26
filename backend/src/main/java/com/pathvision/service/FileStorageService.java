
package com.pathvision.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String saveMarksheet(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID().toString() + ext;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
            Path marksheetDir = uploadPath.resolve("marksheets");
            if (!Files.exists(marksheetDir)) Files.createDirectories(marksheetDir);
            Path target = marksheetDir.resolve(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

            return filename; // return filename; controller will build URL and can obtain physical path
        }

        public Path getMarksheetPath(String filename) {
            if (filename == null) return null;
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            return uploadPath.resolve("marksheets").resolve(filename).toAbsolutePath();
        }

        public String buildMarksheetUrl(String filename) {
            if (filename == null) return null;
            return "/uploads/marksheets/" + filename;
        }

    }
