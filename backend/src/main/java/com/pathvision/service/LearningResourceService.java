package com.pathvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathvision.dto.CreateLearningResourceRequest;
import com.pathvision.dto.LearningResourceResponse;
import com.pathvision.dto.LearningResourceUploadResponse;
import com.pathvision.entity.LearningResource;
import com.pathvision.entity.StudentProfile;
import com.pathvision.repository.LearningResourceRepository;
import com.pathvision.repository.StudentProfileRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class LearningResourceService {

    private static final Logger log = LoggerFactory.getLogger(LearningResourceService.class);

    private static final Map<String, String> INTEREST_QUERY_MAP = Map.ofEntries(
            Map.entry("engineering_cse_it", "computer science full course nptel"),
            Map.entry("engineering_ece_eee", "electronics engineering nptel course"),
            Map.entry("engineering_mech_auto", "mechanical engineering full course"),
            Map.entry("engineering_civil_arch", "civil engineering nptel lectures"),
            Map.entry("engineering_ai_ds", "machine learning data science nptel"),
            Map.entry("engineering_biomedical_biotech", "biotechnology biomedical engineering course"),
            Map.entry("arts_science_math_physics", "mathematics physics full course"),
            Map.entry("arts_science_chem_life", "chemistry biology full course"),
            Map.entry("arts_science_computer_science", "data structures algorithms course"),
            Map.entry("arts_science_commerce_bba", "commerce accounting management course"),
            Map.entry("arts_science_humanities", "history economics humanities course"),
            Map.entry("arts_science_media_psychology", "psychology media studies course")
    );

    private final LearningResourceRepository learningResourceRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.resource-sync.enabled:true}")
    private boolean resourceSyncEnabled;

    @Value("${app.resource-sync.youtube.api-key:}")
    private String youtubeApiKey;

    @Value("${app.resource-sync.youtube.max-results:5}")
    private int youtubeMaxResults;

    @Value("${app.resource-sync.external.enabled:true}")
    private boolean externalSyncEnabled;

    @Value("${app.resource-sync.external.csv-url:}")
    private String externalCsvUrl;

    @Value("${app.resource-sync.external.json-url:}")
    private String externalJsonUrl;

    @Value("${app.resource-sync.external.default-source:Course}")
    private String externalDefaultSource;

    @Value("${app.resource-sync.external.default-level:Beginner}")
    private String externalDefaultLevel;

    @Value("${app.resource-sync.external.default-interest-key:engineering_cse_it}")
    private String externalDefaultInterestKey;

    @Value("${app.resource-sync.nptel.enabled:true}")
    private boolean nptelSyncEnabled;

    @Value("${app.resource-sync.nptel.catalog-url:https://nptel.ac.in/courses}")
    private String nptelCatalogUrl;

    @Value("${app.resource-sync.nptel.max-items:30}")
    private int nptelMaxItems;

    @Value("${app.resource-sync.coursera.enabled:true}")
    private boolean courseraSyncEnabled;

    @Value("${app.resource-sync.coursera.catalog-url:https://www.coursera.org/browse/computer-science}")
    private String courseraCatalogUrl;

    @Value("${app.resource-sync.coursera.max-items:30}")
    private int courseraMaxItems;

    public LearningResourceService(
            LearningResourceRepository learningResourceRepository,
            StudentProfileRepository studentProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.learningResourceRepository = learningResourceRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public LearningResourceResponse create(CreateLearningResourceRequest request) {
        LearningResource entity = new LearningResource();
        entity.setTitle(normalize(request.getTitle()));
        entity.setProvider(normalize(request.getProvider()));
        entity.setSource(normalize(request.getSource()));
        entity.setLevel(normalize(request.getLevel()));
        entity.setUrl(request.getUrl().trim());
        entity.setInterestKey(request.getInterestKey().trim().toLowerCase());
        entity.setActive(true);
        entity.setAutoGenerated(false);

        return LearningResourceResponse.fromEntity(learningResourceRepository.save(entity));
    }

    public List<LearningResourceResponse> getAll() {
        return learningResourceRepository.findAll().stream()
                .map(LearningResourceResponse::fromEntity)
                .toList();
    }

    public List<LearningResourceResponse> getRecommendedForStudent(Long userId, String sourceFilter) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Please complete student profile first."));

        List<String> interestKeys = parseInterests(profile.getInterestsJson());
        if (interestKeys.isEmpty()) {
            return List.of();
        }

        String normalizedSource = sourceFilter == null ? "all" : sourceFilter.trim().toLowerCase();
        List<LearningResource> resources = learningResourceRepository.findByActiveTrueAndInterestKeyIn(interestKeys);

        List<LearningResourceResponse> result = new ArrayList<>();
        int index = 0;
        for (LearningResource resource : resources) {
            if (!"all".equals(normalizedSource) &&
                    (resource.getSource() == null || !resource.getSource().trim().equalsIgnoreCase(normalizedSource))) {
                continue;
            }
            LearningResourceResponse dto = LearningResourceResponse.fromEntity(resource);
            dto.setInterestLabel(resolveInterestLabel(resource.getInterestKey()));
            dto.setCategory(resource.getInterestKey() != null && resource.getInterestKey().startsWith("engineering") ? "Degree" : "Stream");
            int match = Math.max(72, 94 - ((index % 4) * 4));
            dto.setMatchPercentage(match);
            dto.setConfidenceText(match >= 90 ? "Excellent Match" : match >= 84 ? "Great Match" : "Strong Match");
            result.add(dto);
            index++;
        }
        return result.stream().limit(20).toList();
    }

    public LearningResourceUploadResponse uploadCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty CSV file.");
        }

        LearningResourceUploadResponse summary = new LearningResourceUploadResponse();

        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ResponseStatusException(BAD_REQUEST, "CSV file is empty.");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
            validateRequiredHeaders(headerIndex);

            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                summary.setTotalRows(summary.getTotalRows() + 1);
                try {
                    List<String> columns = parseCsvLine(line);
                    CreateLearningResourceRequest request = new CreateLearningResourceRequest();
                    request.setTitle(getField(columns, headerIndex, "title"));
                    request.setProvider(getField(columns, headerIndex, "provider"));
                    request.setSource(getField(columns, headerIndex, "source"));
                    request.setLevel(getField(columns, headerIndex, "level"));
                    request.setUrl(getField(columns, headerIndex, "url"));
                    request.setInterestKey(getField(columns, headerIndex, "interest_key"));
                    create(request);
                    summary.setSuccessCount(summary.getSuccessCount() + 1);
                } catch (Exception ex) {
                    summary.setFailedCount(summary.getFailedCount() + 1);
                    if (summary.getErrors().size() < 20) {
                        summary.getErrors().add("Row " + row + ": " + ex.getMessage());
                    }
                }
            }

            return summary;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse resources CSV.");
        }
    }

    @Scheduled(
            initialDelayString = "${app.resource-sync.initial-delay-ms:20000}",
            fixedDelayString = "${app.resource-sync.fixed-delay-ms:21600000}"
    )
    public void scheduledSync() {
        if (!resourceSyncEnabled) {
            return;
        }
        try {
            int count = syncAllSources();
            if (count > 0) {
                log.info("Learning resource auto-sync completed. Added/updated: {}", count);
            }
        } catch (Exception ex) {
            log.warn("Learning resource auto-sync failed: {}", ex.getMessage());
        }
    }

    public int syncAllSources() {
        int youtubeCount = syncFromApis();
        int externalCount = syncFromExternalFeeds();
        int nptelCount = syncFromNptelCatalog();
        int courseraCount = syncFromCourseraCatalog();
        return youtubeCount + externalCount + nptelCount + courseraCount;
    }

    public int syncFromApis() {
        if (!resourceSyncEnabled) {
            return 0;
        }
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            log.warn("Skipping resource sync: YOUTUBE API key not configured.");
            return 0;
        }

        int total = 0;
        for (Map.Entry<String, String> entry : INTEREST_QUERY_MAP.entrySet()) {
            total += fetchAndUpsertYouTube(entry.getKey(), entry.getValue());
        }
        return total;
    }

    public int syncFromExternalFeeds() {
        if (!resourceSyncEnabled || !externalSyncEnabled) {
            return 0;
        }

        int total = 0;
        if (externalCsvUrl != null && !externalCsvUrl.isBlank()) {
            total += fetchAndUpsertExternalCsv(externalCsvUrl.trim());
        }
        if (externalJsonUrl != null && !externalJsonUrl.isBlank()) {
            total += fetchAndUpsertExternalJson(externalJsonUrl.trim());
        }
        return total;
    }

    public int syncFromNptelCatalog() {
        if (!resourceSyncEnabled || !nptelSyncEnabled) {
            return 0;
        }
        try {
            Document doc = Jsoup.connect(nptelCatalogUrl)
                    .userAgent("PathVisionBot/1.0")
                    .timeout(15000)
                    .get();

            List<Element> links = doc.select("a[href]");
            Set<String> seen = new java.util.HashSet<>();
            int count = 0;
            for (Element link : links) {
                String href = link.absUrl("href");
                String text = normalize(link.text());
                if (href.isBlank()) {
                    continue;
                }
                if (!href.contains("/courses/")) {
                    continue;
                }
                if (!seen.add(href)) {
                    continue;
                }
                if (text.isBlank()) {
                    text = deriveTitleFromUrl(href);
                }
                String interestKey = mapInterestFromTitle(text);
                upsertAutoResource(
                        extractTrailingToken(href),
                        "NPTEL",
                        text,
                        "NPTEL",
                        inferLevel(text),
                        href,
                        interestKey
                );
                count++;
                if (count >= Math.max(1, nptelMaxItems)) {
                    break;
                }
            }
            return count;
        } catch (Exception ex) {
            log.debug("NPTEL catalog sync failed: {}", ex.getMessage());
            return 0;
        }
    }

    public int syncFromCourseraCatalog() {
        if (!resourceSyncEnabled || !courseraSyncEnabled) {
            return 0;
        }
        try {
            Document doc = Jsoup.connect(courseraCatalogUrl)
                    .userAgent("PathVisionBot/1.0")
                    .timeout(15000)
                    .get();

            List<Element> links = doc.select("a[href]");
            Set<String> seen = new java.util.HashSet<>();
            int count = 0;
            for (Element link : links) {
                String href = link.absUrl("href");
                String text = normalize(link.text());
                if (href.isBlank() || text.isBlank()) {
                    continue;
                }
                if (!(href.contains("/learn/") || href.contains("/professional-certificates/") || href.contains("/specializations/"))) {
                    continue;
                }
                if (!href.startsWith("https://www.coursera.org/")) {
                    continue;
                }
                if (!seen.add(href)) {
                    continue;
                }
                String interestKey = mapInterestFromTitle(text);
                upsertAutoResource(
                        extractTrailingToken(href),
                        "Course",
                        text,
                        "Coursera",
                        inferLevel(text),
                        href,
                        interestKey
                );
                count++;
                if (count >= Math.max(1, courseraMaxItems)) {
                    break;
                }
            }
            return count;
        } catch (Exception ex) {
            log.debug("Coursera catalog sync failed: {}", ex.getMessage());
            return 0;
        }
    }

    private int fetchAndUpsertYouTube(String interestKey, String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create(
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&order=relevance&maxResults="
                            + Math.max(1, Math.min(10, youtubeMaxResults))
                            + "&q=" + encodedQuery
                            + "&key=" + URLEncoder.encode(youtubeApiKey, StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return 0;
            }

            int count = 0;
            for (JsonNode item : items) {
                String videoId = item.path("id").path("videoId").asText("");
                String title = item.path("snippet").path("title").asText("");
                String channel = item.path("snippet").path("channelTitle").asText("YouTube");
                if (videoId.isBlank() || title.isBlank()) {
                    continue;
                }
                String url = "https://www.youtube.com/watch?v=" + videoId;
                String source = channel.toLowerCase().contains("nptel") ? "NPTEL" : "YouTube";
                String level = inferLevel(title);

                LearningResource entity = learningResourceRepository
                        .findBySourceIgnoreCaseAndExternalId(source, videoId)
                        .or(() -> learningResourceRepository.findByUrl(url))
                        .orElseGet(LearningResource::new);

                entity.setExternalId(videoId);
                entity.setTitle(title);
                entity.setProvider(channel);
                entity.setSource(source);
                entity.setLevel(level);
                entity.setUrl(url);
                entity.setInterestKey(interestKey);
                entity.setActive(true);
                entity.setAutoGenerated(true);
                learningResourceRepository.save(entity);
                count++;
            }
            return count;
        } catch (Exception ex) {
            log.debug("YouTube sync failed for {}: {}", interestKey, ex.getMessage());
            return 0;
        }
    }

    private int fetchAndUpsertExternalCsv(String feedUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(feedUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return 0;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return 0;
            }

            List<String> lines = body.lines().toList();
            if (lines.isEmpty()) {
                return 0;
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(lines.get(0)));
            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                String title = getField(columns, headerIndex, "title");
                String url = getField(columns, headerIndex, "url");
                if (title.isBlank() || url.isBlank()) {
                    continue;
                }

                String provider = getField(columns, headerIndex, "provider");
                String source = getField(columns, headerIndex, "source");
                String level = getField(columns, headerIndex, "level");
                String interestKey = getField(columns, headerIndex, "interest_key");
                if (interestKey.isBlank()) {
                    interestKey = getField(columns, headerIndex, "interestkey");
                }
                String externalId = getField(columns, headerIndex, "external_id");
                if (externalId.isBlank()) {
                    externalId = getField(columns, headerIndex, "externalid");
                }

                upsertAutoResource(
                        externalId,
                        normalize(source).isBlank() ? externalDefaultSource : source,
                        title,
                        normalize(provider).isBlank() ? "External Feed" : provider,
                        normalize(level).isBlank() ? externalDefaultLevel : level,
                        url,
                        normalize(interestKey).isBlank() ? externalDefaultInterestKey : interestKey
                );
                count++;
            }
            return count;
        } catch (Exception ex) {
            log.debug("External CSV sync failed: {}", ex.getMessage());
            return 0;
        }
    }

    private int fetchAndUpsertExternalJson(String feedUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(feedUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = resolveItemsNode(root);
            if (items == null || !items.isArray()) {
                return 0;
            }

            int count = 0;
            for (JsonNode item : items) {
                String title = readText(item, "title", "name");
                String url = readText(item, "url", "link");
                if (title.isBlank() || url.isBlank()) {
                    continue;
                }

                String provider = readText(item, "provider", "platform", "channel");
                String source = readText(item, "source", "type");
                String level = readText(item, "level");
                String interestKey = readText(item, "interest_key", "interestKey", "interest");
                String externalId = readText(item, "external_id", "externalId", "id");

                upsertAutoResource(
                        externalId,
                        normalize(source).isBlank() ? externalDefaultSource : source,
                        title,
                        normalize(provider).isBlank() ? "External Feed" : provider,
                        normalize(level).isBlank() ? externalDefaultLevel : level,
                        url,
                        normalize(interestKey).isBlank() ? externalDefaultInterestKey : interestKey
                );
                count++;
            }
            return count;
        } catch (Exception ex) {
            log.debug("External JSON sync failed: {}", ex.getMessage());
            return 0;
        }
    }

    private void upsertAutoResource(
            String externalId,
            String source,
            String title,
            String provider,
            String level,
            String url,
            String interestKey
    ) {
        String normalizedSource = normalize(source);
        String normalizedInterestKey = interestKey.trim().toLowerCase();

        LearningResource entity = null;
        if (externalId != null && !externalId.isBlank()) {
            entity = learningResourceRepository
                    .findBySourceIgnoreCaseAndExternalId(normalizedSource, externalId.trim())
                    .orElse(null);
        }
        if (entity == null) {
            entity = learningResourceRepository.findByUrl(url.trim()).orElseGet(LearningResource::new);
        }

        entity.setExternalId(externalId == null || externalId.isBlank() ? null : externalId.trim());
        entity.setTitle(normalize(title));
        entity.setProvider(normalize(provider));
        entity.setSource(normalizedSource);
        entity.setLevel(normalize(level));
        entity.setUrl(url.trim());
        entity.setInterestKey(normalizedInterestKey);
        entity.setActive(true);
        entity.setAutoGenerated(true);
        learningResourceRepository.save(entity);
    }

    private JsonNode resolveItemsNode(JsonNode root) {
        if (root == null) return null;
        if (root.isArray()) return root;
        if (root.has("items")) return root.get("items");
        if (root.has("resources")) return root.get("resources");
        if (root.has("data")) return root.get("data");
        return null;
    }

    private String readText(JsonNode node, String... keys) {
        if (node == null) return "";
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.asText("");
                if (!text.isBlank()) return text.trim();
            }
        }
        return "";
    }

    private String inferLevel(String title) {
        String normalized = title == null ? "" : title.toLowerCase();
        if (normalized.contains("advanced") || normalized.contains("deep dive")) return "Advanced";
        if (normalized.contains("intermediate")) return "Intermediate";
        return "Beginner";
    }

    private String mapInterestFromTitle(String title) {
        String text = title == null ? "" : title.toLowerCase();

        Map<String, List<String>> keywordMap = new HashMap<>();
        keywordMap.put("engineering_ai_ds", List.of("machine learning", "ai", "data science", "deep learning", "nlp"));
        keywordMap.put("engineering_cse_it", List.of("computer science", "programming", "software", "web", "algorithm", "data structure", "java", "python"));
        keywordMap.put("engineering_ece_eee", List.of("electronics", "electrical", "signal", "embedded", "vlsi"));
        keywordMap.put("engineering_mech_auto", List.of("mechanical", "automobile", "thermodynamics", "manufacturing"));
        keywordMap.put("engineering_civil_arch", List.of("civil", "architecture", "structural", "surveying"));
        keywordMap.put("engineering_biomedical_biotech", List.of("biomedical", "biotechnology", "bioengineering"));
        keywordMap.put("arts_science_math_physics", List.of("mathematics", "math", "physics", "statistics"));
        keywordMap.put("arts_science_chem_life", List.of("chemistry", "biology", "life science", "biochemistry"));
        keywordMap.put("arts_science_computer_science", List.of("computer", "it", "coding"));
        keywordMap.put("arts_science_commerce_bba", List.of("commerce", "accounting", "finance", "management", "business"));
        keywordMap.put("arts_science_humanities", List.of("history", "economics", "humanities", "political", "english"));
        keywordMap.put("arts_science_media_psychology", List.of("psychology", "media", "communication", "journalism"));

        return keywordMap.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(),
                        entry.getValue().stream().mapToInt(k -> text.contains(k) ? 1 : 0).sum()))
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(externalDefaultInterestKey);
    }

    private String extractTrailingToken(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0) {
            clean = clean.substring(0, query);
        }
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int slash = clean.lastIndexOf('/');
        if (slash >= 0 && slash < clean.length() - 1) {
            return clean.substring(slash + 1);
        }
        return clean;
    }

    private String deriveTitleFromUrl(String url) {
        String token = extractTrailingToken(url);
        if (token == null || token.isBlank()) {
            return "Course Resource";
        }
        return token.replace('-', ' ').replace('_', ' ').trim();
    }

    private List<String> parseInterests(String interestsJson) {
        if (interestsJson == null || interestsJson.trim().isEmpty()) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(
                    interestsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return parsed.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).map(String::toLowerCase).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String resolveInterestLabel(String key) {
        if (key == null) return "";
        return switch (key) {
            case "engineering_cse_it" -> "Engineering - CSE / IT";
            case "engineering_ece_eee" -> "Engineering - ECE / EEE";
            case "engineering_mech_auto" -> "Engineering - Mechanical / Automobile";
            case "engineering_civil_arch" -> "Engineering - Civil / Architecture";
            case "engineering_ai_ds" -> "Engineering - AI / Data Science";
            case "engineering_biomedical_biotech" -> "Engineering - Biomedical / Biotech";
            case "arts_science_math_physics" -> "Arts & Science - Mathematics / Physics";
            case "arts_science_chem_life" -> "Arts & Science - Chemistry / Life Sciences";
            case "arts_science_computer_science" -> "Arts & Science - Computer Science";
            case "arts_science_commerce_bba" -> "Arts & Science - Commerce / BBA";
            case "arts_science_humanities" -> "Arts & Science - Humanities";
            case "arts_science_media_psychology" -> "Arts & Science - Media / Psychology";
            default -> key;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        String[] required = {"title", "provider", "source", "level", "url", "interest_key"};
        for (String key : required) {
            if (!headerIndex.containsKey(key)) {
                throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: " + key);
            }
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim().toLowerCase(), i);
        }
        return index;
    }

    private String getField(List<String> columns, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx >= columns.size()) return "";
        return columns.get(idx).trim();
    }

    private List<String> parseCsvLine(String line) {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }
}
