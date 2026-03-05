package com.pathvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathvision.dto.CollegeRecommendationRequest;
import com.pathvision.dto.CollegeRecommendationResponse;
import com.pathvision.dto.CollegeResponse;
import com.pathvision.dto.CollegeCutoffResponse;
import com.pathvision.dto.CollegeUploadResponse;
import com.pathvision.dto.CreateCollegeCutoffRequest;
import com.pathvision.dto.CreateCollegeRequest;
import com.pathvision.entity.College;
import com.pathvision.entity.CollegeCutoff;
import com.pathvision.entity.StudentProfile;
import com.pathvision.repository.CollegeCutoffRepository;
import com.pathvision.repository.CollegeRepository;
import com.pathvision.repository.StudentProfileRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final CollegeCutoffRepository cutoffRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CollegeService(
            CollegeRepository collegeRepository,
            CollegeCutoffRepository cutoffRepository,
            StudentProfileRepository studentProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.collegeRepository = collegeRepository;
        this.cutoffRepository = cutoffRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CollegeResponse createCollege(CreateCollegeRequest request) {
        ensureNotDuplicate(request);
        College college = buildCollegeFromRequest(request, null, null);
        return CollegeResponse.fromEntity(collegeRepository.save(college));
    }

    public List<CollegeResponse> getAllColleges() {
        return collegeRepository.findAll()
                .stream()
                .map(CollegeResponse::fromEntity)
                .toList();
    }

    public List<CollegeResponse> getAllColleges(String community) {
        List<CollegeResponse> base = getAllColleges();
        if (community == null || community.trim().isEmpty()) {
            return base;
        }

        String normalizedCommunity = normalizeLabel(community);
        for (CollegeResponse college : base) {
            cutoffRepository.findByCollegeIdAndCommunityIgnoreCase(college.getId(), normalizedCommunity)
                    .ifPresent(cutoff -> college.setCommunityCutoff(cutoff.getCutoffScore()));
        }
        return base;
    }

    public List<CollegeCutoffResponse> getAllCutoffs() {
        return cutoffRepository.findAll().stream()
                .map(CollegeCutoffResponse::fromEntity)
                .toList();
    }

    public void addOrUpdateCutoff(Long collegeId, CreateCollegeCutoffRequest request) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "College not found"));

        String community = normalizeLabel(request.getCommunity());
        CollegeCutoff cutoff = cutoffRepository.findByCollegeIdAndCommunityIgnoreCase(collegeId, community)
                .orElseGet(CollegeCutoff::new);

        cutoff.setCollege(college);
        cutoff.setCommunity(community);
        cutoff.setCutoffScore(request.getCutoffScore());
        cutoffRepository.save(cutoff);
    }

    public List<CollegeRecommendationResponse> recommendColleges(CollegeRecommendationRequest request, Long userId) {
        String community = normalizeLabel(request.getCommunity());
        double studentScore = resolveStudentScore(userId);
        Double maxFees = request.getMaxAnnualFees();
        int limit = request.getLimit() == null ? 10 : Math.max(1, Math.min(50, request.getLimit()));

        return cutoffRepository.findByCommunityIgnoreCase(community).stream()
                .map(cutoff -> toRecommendation(cutoff, studentScore, maxFees, request.getLatitude(), request.getLongitude(), community))
                .filter(item -> item != null)
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .limit(limit)
                .toList();
    }

    private double resolveStudentScore(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to identify current student.");
        }

        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Please complete student profile before getting recommendations."));

        if (profile.getCsCutoff() != null) {
            return profile.getCsCutoff();
        }
        if (profile.getAggregatePercentage() != null) {
            return profile.getAggregatePercentage();
        }

        throw new ResponseStatusException(BAD_REQUEST, "Cutoff/aggregate not found in student profile. Upload marksheet first.");
    }

    public CollegeUploadResponse uploadCollegesCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty CSV file.");
        }

        CollegeUploadResponse summary = new CollegeUploadResponse();

        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ResponseStatusException(BAD_REQUEST, "CSV file is empty.");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateRequiredHeaders(headerIndex);

            Set<String> existingKeys = collegeRepository.findAll().stream()
                    .map(this::buildNormalizedKey)
                    .collect(Collectors.toSet());

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                summary.setTotalRows(summary.getTotalRows() + 1);
                try {
                    List<String> columns = parseCsvLine(line);
                    CreateCollegeRequest request = new CreateCollegeRequest();
                    request.setName(getField(columns, headerIndex, "name"));
                    request.setType(getField(columns, headerIndex, "type"));
                    request.setDistrict(getField(columns, headerIndex, "district"));
                    request.setState(getField(columns, headerIndex, "state"));
                    request.setAddress(getField(columns, headerIndex, "address"));
                    request.setRating(parseOptionalDouble(getField(columns, headerIndex, "rating")));
                    request.setAnnualFees(parseOptionalDouble(getField(columns, headerIndex, "annualfees")));

                    Double latitude = parseOptionalDouble(getField(columns, headerIndex, "latitude"));
                    Double longitude = parseOptionalDouble(getField(columns, headerIndex, "longitude"));

                    if (isBlank(request.getName()) || isBlank(request.getType()) || isBlank(request.getDistrict())
                            || isBlank(request.getState()) || isBlank(request.getAddress())) {
                        throw new IllegalArgumentException("Missing required values.");
                    }

                    String requestKey = buildNormalizedKey(request);
                    if (existingKeys.contains(requestKey)) {
                        throw new IllegalArgumentException("Duplicate college entry.");
                    }

                    College college = buildCollegeFromRequest(request, latitude, longitude);
                    collegeRepository.save(college);
                    existingKeys.add(requestKey);
                    summary.setSuccessCount(summary.getSuccessCount() + 1);
                } catch (Exception ex) {
                    summary.setFailedCount(summary.getFailedCount() + 1);
                    if (summary.getErrors().size() < 20) {
                        summary.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                    }
                }
            }

            return summary;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse CSV. Check file format.");
        }
    }

    public CollegeUploadResponse uploadCutoffsCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty CSV file.");
        }

        CollegeUploadResponse summary = new CollegeUploadResponse();

        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ResponseStatusException(BAD_REQUEST, "CSV file is empty.");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateCutoffHeaders(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                summary.setTotalRows(summary.getTotalRows() + 1);
                try {
                    List<String> columns = parseCsvLine(line);
                    String collegeName = getField(columns, headerIndex, "college_name");
                    String community = getField(columns, headerIndex, "community");
                    String cutoffText = getField(columns, headerIndex, "cutoffscore");
                    if (isBlank(cutoffText)) {
                        cutoffText = getField(columns, headerIndex, "cutoff_mark");
                    }

                    if (isBlank(collegeName) || isBlank(community) || isBlank(cutoffText)) {
                        throw new IllegalArgumentException("Missing required values.");
                    }

                    College college = collegeRepository.findByNameIgnoreCase(collegeName.trim())
                            .orElseThrow(() -> new IllegalArgumentException("College not found: " + collegeName));

                    CreateCollegeCutoffRequest request = new CreateCollegeCutoffRequest();
                    request.setCommunity(community.trim());
                    request.setCutoffScore(Double.parseDouble(cutoffText.trim()));
                    addOrUpdateCutoff(college.getId(), request);

                    summary.setSuccessCount(summary.getSuccessCount() + 1);
                } catch (Exception ex) {
                    summary.setFailedCount(summary.getFailedCount() + 1);
                    if (summary.getErrors().size() < 25) {
                        summary.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                    }
                }
            }

            return summary;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse cutoff CSV. Check file format.");
        }
    }

    private College buildCollegeFromRequest(CreateCollegeRequest request, Double latitude, Double longitude) {
        College college = new College();
        college.setName(normalizeLabel(request.getName()));
        college.setType(request.getType().trim());
        college.setDistrict(normalizeLabel(request.getDistrict()));
        college.setState(normalizeLabel(request.getState()));
        college.setAddress(normalizeLabel(request.getAddress()));
        college.setRating(request.getRating() != null ? request.getRating() : 0.0);
        college.setAnnualFees(request.getAnnualFees());

        if (latitude != null && longitude != null) {
            college.setLatitude(latitude);
            college.setLongitude(longitude);
        } else {
            GeocodeResult geocode = tryGeocodeWithFallback(request);
            if (geocode != null) {
                college.setLatitude(geocode.latitude());
                college.setLongitude(geocode.longitude());
            } else {
                college.setLatitude(null);
                college.setLongitude(null);
            }
        }
        return college;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim().toLowerCase(), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        String[] required = {"name", "type", "district", "state", "address"};
        for (String key : required) {
            if (!headerIndex.containsKey(key)) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Missing required CSV header: " + key + ". Required: name,type,district,state,address");
            }
        }
    }

    private void validateCutoffHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("college_name")) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: college_name");
        }
        if (!headerIndex.containsKey("community")) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: community");
        }
        if (!(headerIndex.containsKey("cutoffscore") || headerIndex.containsKey("cutoff_mark"))) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: cutoffScore or cutoff_mark");
        }
    }

    private String getField(List<String> columns, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null || index < 0 || index >= columns.size()) {
            return "";
        }
        return columns.get(index).trim();
    }

    private Double parseOptionalDouble(String value) {
        if (isBlank(value)) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void ensureNotDuplicate(CreateCollegeRequest request) {
        String newKey = buildNormalizedKey(request);
        boolean exists = collegeRepository.findAll().stream()
                .map(this::buildNormalizedKey)
                .anyMatch(newKey::equals);
        if (exists) {
            throw new ResponseStatusException(CONFLICT, "Duplicate college entry already exists.");
        }
    }

    private String buildNormalizedKey(CreateCollegeRequest request) {
        return normalizedToken(request.getName()) + "|"
                + normalizedToken(request.getDistrict()) + "|"
                + normalizedToken(request.getState());
    }

    private String buildNormalizedKey(College college) {
        return normalizedToken(college.getName()) + "|"
                + normalizedToken(college.getDistrict()) + "|"
                + normalizedToken(college.getState());
    }

    private String normalizedToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^a-z0-9]", "");
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("\\s+", " ");
    }

    private CollegeRecommendationResponse toRecommendation(
            CollegeCutoff cutoff,
            double studentScore,
            Double maxFees,
            Double latitude,
            Double longitude,
            String community
    ) {
        College college = cutoff.getCollege();
        if (college == null) {
            return null;
        }

        double required = cutoff.getCutoffScore() == null ? 0 : cutoff.getCutoffScore();
        if (studentScore < required) {
            return null;
        }

        if (maxFees != null && college.getAnnualFees() != null && college.getAnnualFees() > maxFees) {
            return null;
        }

        Double distanceKm = null;
        if (latitude != null && longitude != null && college.getLatitude() != null && college.getLongitude() != null) {
            distanceKm = haversineKm(latitude, longitude, college.getLatitude(), college.getLongitude());
        }

        double cutoffScore = Math.min(1.0, 0.5 + (studentScore - required) / 100.0);
        double feeScore = 1.0;
        if (maxFees != null && maxFees > 0 && college.getAnnualFees() != null) {
            feeScore = Math.max(0.0, 1.0 - (college.getAnnualFees() / (maxFees * 1.2)));
        }
        double distanceScore = distanceKm == null ? 0.5 : (1.0 / (1.0 + (distanceKm / 25.0)));
        double finalScore = (0.45 * cutoffScore) + (0.30 * feeScore) + (0.25 * distanceScore);

        CollegeRecommendationResponse res = new CollegeRecommendationResponse();
        res.setCollegeId(college.getId());
        res.setCollegeName(college.getName());
        res.setDistrict(college.getDistrict());
        res.setState(college.getState());
        res.setType(college.getType());
        res.setRating(college.getRating());
        res.setAnnualFees(college.getAnnualFees());
        res.setCommunity(community);
        res.setCommunityCutoff(required);
        res.setStudentScore(studentScore);
        res.setDistanceKm(distanceKm);
        res.setRecommendationScore(Math.round(finalScore * 1000.0) / 1000.0);
        res.setReason(buildReason(required, studentScore, college.getAnnualFees(), distanceKm));
        return res;
    }

    private String buildReason(double required, double studentScore, Double annualFees, Double distanceKm) {
        String part1 = "Cutoff match: required " + required + ", you " + studentScore;
        String part2 = annualFees == null ? "fees not available" : "fees " + String.format("%.0f", annualFees) + "/year";
        String part3 = distanceKm == null ? "distance unavailable" : String.format("distance %.1f km", distanceKm);
        return part1 + " | " + part2 + " | " + part3;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<String> parseCsvLine(String line) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
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

    private String buildQuery(CreateCollegeRequest request) {
        return String.join(", ",
                request.getName(),
                request.getAddress(),
                request.getDistrict(),
                request.getState(),
                "India");
    }

    private GeocodeResult tryGeocodeWithFallback(CreateCollegeRequest request) {
        String[] queries = new String[] {
                buildQuery(request),
                String.join(", ", request.getAddress(), request.getDistrict(), request.getState(), "India"),
                String.join(", ", request.getDistrict(), request.getState(), "India")
        };

        for (String query : queries) {
            try {
                return geocodeAddress(query);
            } catch (ResponseStatusException ignored) {
                // Try next query variation
            }
        }
        return null;
    }

    private GeocodeResult geocodeAddress(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encodedQuery);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "PathVision/1.0 (college-geocoder)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(BAD_GATEWAY, "Geocoding service failed");
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Unable to locate this college address. Try a more specific address.");
            }

            JsonNode first = root.get(0);
            double latitude = first.path("lat").asDouble(Double.NaN);
            double longitude = first.path("lon").asDouble(Double.NaN);
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                throw new ResponseStatusException(BAD_REQUEST, "Geocoding did not return coordinates for this address.");
            }

            return new GeocodeResult(latitude, longitude);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Could not geocode address right now. Please retry.");
        }
    }

    private record GeocodeResult(double latitude, double longitude) {
    }
}
