package com.pathvision.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-request-size:100MB}")
    private String maxRequestSize;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        long contentLength = request.getContentLengthLong();
        String actualSize = contentLength > 0 ? formatBytes(contentLength) : "unknown size";
        String configuredLimit = formatConfiguredLimit(maxRequestSize);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Upload too large. Received " + actualSize + "; maximum allowed is " + configuredLimit + ".");
        body.put("error", "MAX_UPLOAD_SIZE_EXCEEDED");
        body.put("actualBytes", contentLength > 0 ? contentLength : null);
        body.put("configuredLimit", configuredLimit);

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    private String formatConfiguredLimit(String rawValue) {
        try {
            return formatBytes(DataSize.parse(rawValue).toBytes());
        } catch (Exception ex) {
            return rawValue;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024d * 1024d));
        }
        if (bytes >= 1024L) {
            return String.format("%.2f KB", bytes / 1024d);
        }
        return bytes + " bytes";
    }
}
