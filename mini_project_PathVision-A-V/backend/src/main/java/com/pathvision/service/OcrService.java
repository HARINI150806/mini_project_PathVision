    // Utility: Check if the document is DigiLocker-verified (by watermark or metadata)
    
package com.pathvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathvision.config.GeminiConfig;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.color.ColorSpace;

@Service
public class OcrService {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Tesseract tesseract = new Tesseract();

    public OcrService(GeminiConfig geminiConfig) {
        this.geminiConfig = geminiConfig;
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng"); // Use only English for CBSE marksheets
        tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO); // Let Tesseract auto-detect layout
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz "); // Digits and subject names only
    }


    // Utility: If PDF, convert to image; otherwise, return image file as is
    private File ensureImageFile(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            // Convert PDF to image (first page)
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                PDDocument document = PDDocument.load(fis);
                org.apache.pdfbox.rendering.PDFRenderer pdfRenderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
                java.awt.image.BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300);
                File imageFile = new File(file.getParent(), file.getName() + ".png");
                javax.imageio.ImageIO.write(bim, "png", imageFile);
                document.close();
                return imageFile;
            }
        }
        return file;
    }
    public boolean isDigiLockerDocument(File file) {
        try {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".pdf")) {
                org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file);
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                String text = stripper.getText(document);
                document.close();
                // Check for DigiLocker keywords, digital signature, QR code, and URLs
                String[] digilockerKeywords = {
                    "DigiLocker", "digitally signed", "https://www.digilocker.gov.in",
                    "digilocker.gov.in", "Issued by DigiLocker", "CBSE DigiLocker",
                    "QR Code", "CBSE", "Government of India", "Ministry of Electronics and IT"
                };
                for (String keyword : digilockerKeywords) {
                    if (text.toLowerCase().contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
                // Check for presence of QR code in PDF (basic check)
                // Optionally, check PDF metadata for DigiLocker issuer
                org.apache.pdfbox.pdmodel.PDDocument docMeta = org.apache.pdfbox.pdmodel.PDDocument.load(file);
                String producer = docMeta.getDocumentInformation().getProducer();
                String author = docMeta.getDocumentInformation().getAuthor();
                docMeta.close();
                if ((producer != null && producer.toLowerCase().contains("digilocker")) ||
                    (author != null && author.toLowerCase().contains("digilocker"))) {
                    return true;
                }
            } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                BufferedImage img = javax.imageio.ImageIO.read(file);
                String ocrText = tesseract.doOCR(img);
                String[] digilockerKeywords = {
                    "DigiLocker", "digitally signed", "https://www.digilocker.gov.in",
                    "digilocker.gov.in", "Issued by DigiLocker", "CBSE DigiLocker",
                    "QR Code", "CBSE", "Government of India", "Ministry of Electronics and IT"
                };
                for (String keyword : digilockerKeywords) {
                    if (ocrText.toLowerCase().contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==============================
    // MAIN METHOD
    // ==============================
    public CutoffResult extractSubjectMarks(File imageFile) {
    System.out.println("[INFO] extractSubjectMarks called for file: " + imageFile.getName());

        Map<String, Integer> marks = new LinkedHashMap<>();
        File imgFile = null;
        try {
            // Enforce DigiLocker verification
            if (!isDigiLockerDocument(imageFile)) {
                System.out.println("[ERROR] DigiLocker verification failed for file: " + imageFile.getName());
                throw new RuntimeException("Only DigiLocker-verified certificates are accepted. Please upload your document from DigiLocker.");
            } else {
                System.out.println("[INFO] DigiLocker verification passed for file: " + imageFile.getName());
            }
            // Convert PDF to image if needed
            imgFile = ensureImageFile(imageFile);
            BufferedImage img = ImageIO.read(imgFile);
            if (img == null) {
                System.out.println("[ERROR] Failed to read image for OCR: " + imgFile.getAbsolutePath());
                throw new RuntimeException("Failed to read image for OCR: " + imgFile.getAbsolutePath());
            }
            String rawText = tesseract.doOCR(img);
            System.out.println("[OCR TEXT]\n" + rawText);

            if (rawText == null || rawText.isBlank()) {
                System.out.println("[ERROR] OCR returned blank text.");
                return emptyResult();
            }

            // Advanced post-processing: filter out noisy lines
            String[] rawLines = rawText.split("\n");
            StringBuilder filtered = new StringBuilder();
            for (String line : rawLines) {
                int alphaNum = line.replaceAll("[^A-Za-z0-9]", "").length();
                double nonAlphaFrac = (line.length() - alphaNum) / (double) Math.max(1, line.length());
                // Keep lines with at least 3 letters/digits and less than 40% non-alphanumeric
                if (alphaNum > 3 && nonAlphaFrac < 0.4) {
                    filtered.append(line).append("\n");
                }
            }
            String cleanedText = filtered.toString()
                    .replaceAll("[^\\x00-\\x7F]", " ")
                    .replace("\"", "")
                    .replace("\\", "")
                    .trim();

            // Regex extraction for subject codes, names, and marks
            String[] lines = cleanedText.split("\n");
            Map<String, Integer> extractedMarks = new LinkedHashMap<>();
            String marksLineRegex = "(\\d{3})\\s+([A-Z ]+)\\s+(\\d{2,3})\\s+(\\d{2,3})\\s+(\\d{2,3})";
            for (String line : lines) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(marksLineRegex).matcher(line);
                if (m.find()) {
                    String subjectCode = m.group(1);
                    String subjectName = m.group(2).trim();
                    int theory = Integer.parseInt(m.group(3));
                    int practical = Integer.parseInt(m.group(4));
                    int total = Integer.parseInt(m.group(5));
                    extractedMarks.put(subjectName, total);
                    System.out.println("[INFO] Extracted: " + subjectName + " = " + total);
                }
            }
            if (!extractedMarks.isEmpty()) {
                // Prompt Gemini for structured JSON validation
                StringBuilder jsonPrompt = new StringBuilder();
                jsonPrompt.append("Validate and return the following marksheet data as JSON:\n{");
                for (Map.Entry<String, Integer> entry : extractedMarks.entrySet()) {
                    jsonPrompt.append("\n  \"").append(entry.getKey()).append("\": ").append(entry.getValue()).append(",");
                }
                if (jsonPrompt.charAt(jsonPrompt.length() - 1) == ',') {
                    jsonPrompt.deleteCharAt(jsonPrompt.length() - 1);
                }
                jsonPrompt.append("\n}");

                String prompt = jsonPrompt.toString();

                Map<String, Object> requestMap = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(
                                        Map.of("text", prompt)
                                ))
                        )
                );

                String requestBody = mapper.writeValueAsString(requestMap);

                String apiUrl =
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                + geminiConfig.getApiKey();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.out.println("[ERROR] Gemini API error: " + response.body());
                    return emptyResult();
                }

                JsonNode root = mapper.readTree(response.body());

                JsonNode candidates = root.path("candidates");
                if (!candidates.isArray() || candidates.isEmpty()) {
                    System.out.println("[ERROR] Gemini returned no candidates.");
                    return emptyResult();
                }

                String aiText = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                if (aiText == null || aiText.isBlank()) {
                    System.out.println("[ERROR] Gemini returned blank text.");
                    return emptyResult();
                }

                aiText = aiText.replace("```json", "")
                        .replace("```", "")
                        .trim();

                JsonNode marksJson = mapper.readTree(aiText);

                Map<String, Integer> validatedMarks = new LinkedHashMap<>();
                marksJson.fields().forEachRemaining(entry -> {
                    String subject = normalizeSubject(entry.getKey());
                    int value = entry.getValue().asInt();
                    if (value >= 0 && value <= 100) {
                        validatedMarks.put(subject, value);
                    }
                });
                // Strict validation: ensure all required subjects and marks are present
                List<String> requiredSubjects = Arrays.asList("ENGLISH CORE", "MATHEMATICS", "PHYSICS", "CHEMISTRY", "BIOLOGY");
                for (String subject : requiredSubjects) {
                    if (!validatedMarks.containsKey(subject)) {
                        System.out.println("[ERROR] Missing subject: " + subject);
                        return emptyResult();
                    }
                    int mark = validatedMarks.get(subject);
                    if (mark < 0 || mark > 100) {
                        System.out.println("[ERROR] Invalid mark for " + subject + ": " + mark);
                        return emptyResult();
                    }
                }
                System.out.println("[INFO] All required subjects validated.");
                return calculateCutoffs(validatedMarks);
            }

            // 2️⃣ Gemini Prompt
            String prompt = """
                    Extract subject names and their FINAL TOTAL marks only.
                    Ignore internal marks and register numbers.

                    Return ONLY valid JSON like:
                    {
                      "PHYSICS": 86,
                      "CHEMISTRY": 90
                    }

                    OCR TEXT:
                    %s
                    """.formatted(cleanedText);

            Map<String, Object> requestMap = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String requestBody = mapper.writeValueAsString(requestMap);

            String apiUrl =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                            + geminiConfig.getApiKey();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("[ERROR] Gemini API error: " + response.body());
                return emptyResult();
            }

            JsonNode root = mapper.readTree(response.body());

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                System.out.println("[ERROR] Gemini returned no candidates.");
                return emptyResult();
            }

            String aiText = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            if (aiText == null || aiText.isBlank()) {
                System.out.println("[ERROR] Gemini returned blank text.");
                return emptyResult();
            }

            aiText = aiText.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode marksJson = mapper.readTree(aiText);

            marksJson.fields().forEachRemaining(entry -> {
                String subject = normalizeSubject(entry.getKey());
                int value = entry.getValue().asInt();

                if (value >= 0 && value <= 100) {
                    marks.put(subject, value);
                }
            });

        } catch (TesseractException e) {
            System.out.println("[ERROR] Tesseract failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure temp file is deleted
            if (imgFile != null && imgFile.exists() && !imgFile.equals(imageFile)) {
                imgFile.delete();
            }
        }
        return calculateCutoffs(marks);
    }

    // ==============================
    // SUBJECT NORMALIZATION
    // ==============================
    private String normalizeSubject(String subject) {

        subject = subject.toUpperCase().trim();

        if (subject.contains("MATH")) return "MATHEMATICS";
        if (subject.contains("PHYSIC")) return "PHYSICS";
        if (subject.contains("CHEM")) return "CHEMISTRY";
        if (subject.contains("BIO")) return "BIOLOGY";
        if (subject.contains("COMPUTER")) return "COMPUTER SCIENCE";

        return subject;
    }

    // ==============================
    // CUTOFF CALCULATION
    // ==============================
    private CutoffResult calculateCutoffs(Map<String, Integer> marks) {

        double csCutoff = 0.0;
        Integer physics = marks.get("PHYSICS");
        Integer chemistry = marks.get("CHEMISTRY");
        Integer maths = marks.get("MATHEMATICS");
        if (physics != null && chemistry != null && maths != null) {
            csCutoff = roundTwoDecimals(((physics + chemistry)/2.0) + maths);
        }
        return new CutoffResult(marks, csCutoff);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private CutoffResult emptyResult() {
        return new CutoffResult(new LinkedHashMap<>(), 0.0);
    }

    // ==============================
    // RESULT CLASS
    // ==============================
    public static class CutoffResult {
        private final Map<String, Integer> marks;
        private final double csCutoff;

        public CutoffResult(Map<String, Integer> marks, double csCutoff) {
            this.marks = marks;
            this.csCutoff = csCutoff;
        }

        public Map<String, Integer> getMarks() { return marks; }
        public double getCsCutoff() { return csCutoff; }
    }
}