package com.pathvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathvision.dto.CollegeRecommendationRequest;
import com.pathvision.dto.CollegeRecommendationResponse;
import com.pathvision.dto.CollegeResponse;
import com.pathvision.dto.CollegeCutoffResponse;
import com.pathvision.dto.CollegeFeeDetailResponse;
import com.pathvision.dto.CollegeUploadResponse;
import com.pathvision.dto.ConfigureCollegeFeeSourceRequest;
import com.pathvision.dto.CreateCollegeCutoffRequest;
import com.pathvision.dto.CreateCollegeRequest;
import com.pathvision.entity.College;
import com.pathvision.entity.CollegeCutoff;
import com.pathvision.entity.CollegeFeeDetail;
import com.pathvision.entity.StudentProfile;
import com.pathvision.repository.CollegeCutoffRepository;
import com.pathvision.repository.CollegeFeeDetailRepository;
import com.pathvision.repository.CollegeRepository;
import com.pathvision.repository.StudentProfileRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final CollegeCutoffRepository cutoffRepository;
    private final CollegeFeeDetailRepository collegeFeeDetailRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.college-fee-sync.enabled:true}")
    private boolean collegeFeeSyncEnabled;

    public CollegeService(
            CollegeRepository collegeRepository,
            CollegeCutoffRepository cutoffRepository,
            CollegeFeeDetailRepository collegeFeeDetailRepository,
            StudentProfileRepository studentProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.collegeRepository = collegeRepository;
        this.cutoffRepository = cutoffRepository;
        this.collegeFeeDetailRepository = collegeFeeDetailRepository;
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
            cutoffRepository.findByCollegeIdAndCommunityIgnoreCase(college.getId(), normalizedCommunity).stream()
                    .map(CollegeCutoff::getCutoffScore)
                    .filter(Objects::nonNull)
                    .min(Double::compareTo)
                    .ifPresent(college::setCommunityCutoff);
        }
        return base;
    }

    public List<CollegeCutoffResponse> getAllCutoffs() {
        return cutoffRepository.findAll().stream()
                .map(CollegeCutoffResponse::fromEntity)
                .toList();
    }

    public CollegeResponse configureFeeSource(Long collegeId, ConfigureCollegeFeeSourceRequest request) {
        College college = getCollegeOrThrow(collegeId);
        college.setFeeSourceUrl(request.getSourceUrl().trim());
        college.setFeeSourceType(normalizeFeeSourceType(request.getSourceType()));
        college.setFeeRowSelector(normalizeNullable(request.getRowSelector()));
        college.setFeeLabelSelector(normalizeNullable(request.getLabelSelector()));
        college.setFeeAmountSelector(normalizeNullable(request.getAmountSelector()));
        college.setFeeSyncStatus("CONFIGURED");
        college.setFeeSyncMessage("Fee source configured. Run sync to fetch latest fee details.");
        return CollegeResponse.fromEntity(collegeRepository.save(college));
    }

    public CollegeFeeDetailResponse getCollegeFeeDetails(Long collegeId) {
        College college = getCollegeOrThrow(collegeId);
        List<CollegeFeeDetail> details = collegeFeeDetailRepository.findByCollegeIdOrderByDisplayOrderAscIdAsc(collegeId);
        return CollegeFeeDetailResponse.fromEntity(college, details);
    }

    @Transactional
    public CollegeFeeDetailResponse syncCollegeFees(Long collegeId) {
        College college = getCollegeOrThrow(collegeId);
        if (isBlank(college.getFeeSourceUrl())) {
            throw new ResponseStatusException(BAD_REQUEST, "Fee source is not configured for this college.");
        }

        try {
            List<FeeItemExtract> extracted = scrapeCollegeFeeItems(college);
            if (extracted.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "No fee rows found. Update selectors for this college fee page.");
            }

            LocalDateTime now = LocalDateTime.now();
            collegeFeeDetailRepository.deleteByCollegeId(collegeId);

            List<CollegeFeeDetail> details = new ArrayList<>();
            int order = 0;
            for (FeeItemExtract item : extracted) {
                CollegeFeeDetail detail = new CollegeFeeDetail();
                detail.setCollege(college);
                detail.setLabel(item.label());
                detail.setCategory(item.category());
                detail.setAmount(item.amount());
                detail.setAmountText(item.amountText());
                detail.setDisplayOrder(order++);
                detail.setFetchedAt(now);
                detail.setSourceUrl(college.getFeeSourceUrl());
                details.add(detail);
            }

            collegeFeeDetailRepository.saveAll(details);
            college.setAnnualFees(resolveAnnualFees(extracted));
            college.setFeeLastSyncedAt(now);
            college.setFeeSyncStatus("SYNCED");
            college.setFeeSyncMessage("Latest fee structure fetched successfully.");
            collegeRepository.save(college);

            return CollegeFeeDetailResponse.fromEntity(college, details);
        } catch (ResponseStatusException ex) {
            markFeeSyncFailure(college, ex.getReason());
            throw ex;
        } catch (Exception ex) {
            markFeeSyncFailure(college, "Fee sync failed: " + ex.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY, "Could not fetch fee structure right now.");
        }
    }

    public void addOrUpdateCutoff(Long collegeId, CreateCollegeCutoffRequest request) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "College not found"));

        String community = normalizeLabel(request.getCommunity());
        String branchCode = normalizeLabel(request.getBranchCode());
        Integer admissionYear = request.getAdmissionYear() == null ? currentAdmissionYear() : request.getAdmissionYear();
        CollegeCutoff cutoff = cutoffRepository.findByCollegeIdAndBranchCodeIgnoreCaseAndCommunityIgnoreCaseAndAdmissionYear(
                        collegeId,
                        branchCode,
                        community,
                        admissionYear
                )
                .orElseGet(CollegeCutoff::new);

        cutoff.setCollege(college);
        cutoff.setCommunity(community);
        cutoff.setBranch(normalizeLabel(request.getBranch()));
        cutoff.setBranchCode(branchCode);
        cutoff.setAdmissionYear(admissionYear);
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
                .collect(Collectors.toMap(
                        CollegeRecommendationResponse::getCollegeId,
                        item -> item,
                        (left, right) -> left.getRecommendationScore() >= right.getRecommendationScore() ? left : right
                ))
                .values().stream()
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .limit(limit)
                .toList();
    }

    @Scheduled(
            initialDelayString = "${app.college-fee-sync.initial-delay-ms:45000}",
            fixedDelayString = "${app.college-fee-sync.fixed-delay-ms:43200000}"
    )
    public void scheduledFeeSync() {
        if (!collegeFeeSyncEnabled) {
            return;
        }
        for (College college : collegeRepository.findAll()) {
            if (isBlank(college.getFeeSourceUrl())) {
                continue;
            }
            try {
                syncCollegeFees(college.getId());
            } catch (Exception ignored) {
                // Keep auto-sync resilient per college.
            }
        }
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

    public CollegeUploadResponse uploadCollegesDataset(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty college dataset.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".pdf")) {
            return uploadCollegesPdf(file);
        }
        return uploadCollegesCsv(file);
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
            return processCutoffCsv(reader, headerLine, summary);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse cutoff CSV. Check file format.");
        }
    }

    public CollegeUploadResponse uploadCutoffsDataset(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Please upload a non-empty cutoff dataset.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".pdf")) {
            return uploadCutoffsPdf(file);
        }
        return uploadCutoffsCsv(file);
    }

    private CollegeUploadResponse processCutoffCsv(BufferedReader reader, String headerLine, CollegeUploadResponse summary) throws Exception {
        List<String> headers = parseCsvLine(headerLine);
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);
        if (isBranchWiseCutoffDataset(headerIndex)) {
            return processBranchWiseCutoffCsv(reader, headerIndex, summary, extractYearFromHeadersAndMeta(headers, headerLine));
        }

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

                College college = findCollegeByNameOrThrow(collegeName);

                CreateCollegeCutoffRequest request = new CreateCollegeCutoffRequest();
                request.setBranch("General");
                request.setBranchCode("GEN");
                request.setAdmissionYear(currentAdmissionYear());
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
    }

    private CollegeUploadResponse processBranchWiseCutoffCsv(
            BufferedReader reader,
            Map<String, Integer> headerIndex,
            CollegeUploadResponse summary,
            Integer datasetYear
    ) throws Exception {
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
                String collegeName = getField(columns, headerIndex, "college name");
                if (isBlank(collegeName)) {
                    collegeName = getField(columns, headerIndex, "college_name");
                }
                String branch = getField(columns, headerIndex, "branch");
                String branchCode = getField(columns, headerIndex, "branch code");
                if (isBlank(branchCode)) {
                    branchCode = getField(columns, headerIndex, "branch_code");
                }

                if (isBlank(collegeName) || isBlank(branch) || isBlank(branchCode)) {
                    throw new IllegalArgumentException("Missing college, branch or branch code.");
                }

                College college = findCollegeByNameOrThrow(collegeName);
                int rowSuccess = 0;
                for (String community : List.of("OC", "BC", "BCM", "MBC", "SC", "SCA", "ST")) {
                    String cutoffText = getField(columns, headerIndex, community.toLowerCase(Locale.ROOT));
                    if (isBlank(cutoffText) || "-".equals(cutoffText.trim())) {
                        continue;
                    }
                    CreateCollegeCutoffRequest request = new CreateCollegeCutoffRequest();
                    request.setBranch(branch);
                    request.setBranchCode(branchCode);
                    request.setAdmissionYear(datasetYear == null ? currentAdmissionYear() : datasetYear);
                    request.setCommunity(community);
                    request.setCutoffScore(Double.parseDouble(cutoffText.trim()));
                    addOrUpdateCutoff(college.getId(), request);
                    rowSuccess++;
                }

                if (rowSuccess == 0) {
                    throw new IllegalArgumentException("No valid community cutoffs found in row.");
                }
                summary.setSuccessCount(summary.getSuccessCount() + rowSuccess);
            } catch (Exception ex) {
                summary.setFailedCount(summary.getFailedCount() + 1);
                if (summary.getErrors().size() < 25) {
                    summary.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                }
            }
        }
        return summary;
    }

    private CollegeUploadResponse uploadCollegesPdf(MultipartFile file) {
        CollegeUploadResponse summary = new CollegeUploadResponse();
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            List<String> collegeNames = parseCollegeNamesFromPdfText(text);
            if (collegeNames.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Could not detect college rows in the PDF.");
            }

            Set<String> existingKeys = collegeRepository.findAll().stream()
                    .map(this::buildNormalizedKey)
                    .collect(Collectors.toSet());

            for (String collegeName : collegeNames) {
                summary.setTotalRows(summary.getTotalRows() + 1);
                try {
                    CreateCollegeRequest request = new CreateCollegeRequest();
                    request.setName(collegeName);
                    request.setType("Engineering");
                    request.setDistrict("Unknown");
                    request.setState("Tamil Nadu");
                    request.setAddress("Imported from college information PDF");
                    request.setRating(0.0);

                    String requestKey = buildNormalizedKey(request);
                    if (existingKeys.contains(requestKey)) {
                        throw new IllegalArgumentException("Duplicate college entry.");
                    }

                    College college = buildCollegePlaceholderFromRequest(request);
                    collegeRepository.save(college);
                    existingKeys.add(requestKey);
                    summary.setSuccessCount(summary.getSuccessCount() + 1);
                } catch (Exception ex) {
                    summary.setFailedCount(summary.getFailedCount() + 1);
                    if (summary.getErrors().size() < 20) {
                        summary.getErrors().add("Row " + summary.getTotalRows() + ": " + ex.getMessage());
                    }
                }
            }

            return summary;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse college PDF. Check file format.");
        }
    }

    private CollegeUploadResponse uploadCutoffsPdf(MultipartFile file) {
        CollegeUploadResponse summary = new CollegeUploadResponse();
        try (InputStream inputStream = file.getInputStream()) {
            byte[] pdfBytes = inputStream.readAllBytes();
            try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
                try {
                    return processBranchWiseCutoffReportText(text, summary);
                } catch (ResponseStatusException ex) {
                    if (!"Could not detect rows in the cutoff PDF.".equals(ex.getReason())) {
                        throw ex;
                    }
                }
            }

            List<String> rawTokens = extractPdfTextTokens(pdfBytes);
            return processBranchWiseCutoffReportTokens(rawTokens, summary);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not parse cutoff PDF. Check file format.");
        }
    }

    private CollegeUploadResponse processBranchWiseCutoffReportText(String text, CollegeUploadResponse summary) {
        if (isBlank(text)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cutoff PDF did not contain readable text.");
        }

        Integer year = extractYearFromText(text);
        List<CutoffReportRow> rows = parseCutoffReportRows(text, year);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not detect rows in the cutoff PDF.");
        }

        int rowNumber = 1;
        for (CutoffReportRow row : rows) {
            rowNumber++;
            summary.setTotalRows(summary.getTotalRows() + 1);
            try {
                College college = findCollegeByNameOrThrow(row.collegeName());
                int rowSuccess = 0;
                for (Map.Entry<String, Double> entry : row.communityCutoffs().entrySet()) {
                    CreateCollegeCutoffRequest request = new CreateCollegeCutoffRequest();
                    request.setBranch(row.branch());
                    request.setBranchCode(row.branchCode());
                    request.setAdmissionYear(row.admissionYear());
                    request.setCommunity(entry.getKey());
                    request.setCutoffScore(entry.getValue());
                    addOrUpdateCutoff(college.getId(), request);
                    rowSuccess++;
                }
                summary.setSuccessCount(summary.getSuccessCount() + rowSuccess);
            } catch (Exception ex) {
                summary.setFailedCount(summary.getFailedCount() + 1);
                if (summary.getErrors().size() < 25) {
                    summary.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                }
            }
        }
        return summary;
    }

    private CollegeUploadResponse processBranchWiseCutoffReportTokens(List<String> tokens, CollegeUploadResponse summary) {
        if (tokens == null || tokens.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Cutoff PDF did not contain readable text.");
        }

        Integer year = extractYearFromText(String.join(" ", tokens));
        List<CutoffReportRow> rows = parseCutoffReportRowsFromTokens(tokens, year);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not detect rows in the cutoff PDF.");
        }

        int rowNumber = 1;
        for (CutoffReportRow row : rows) {
            rowNumber++;
            summary.setTotalRows(summary.getTotalRows() + 1);
            try {
                College college = findCollegeByNameOrThrow(row.collegeName());
                int rowSuccess = 0;
                for (Map.Entry<String, Double> entry : row.communityCutoffs().entrySet()) {
                    CreateCollegeCutoffRequest request = new CreateCollegeCutoffRequest();
                    request.setBranch(row.branch());
                    request.setBranchCode(row.branchCode());
                    request.setAdmissionYear(row.admissionYear());
                    request.setCommunity(entry.getKey());
                    request.setCutoffScore(entry.getValue());
                    addOrUpdateCutoff(college.getId(), request);
                    rowSuccess++;
                }
                summary.setSuccessCount(summary.getSuccessCount() + rowSuccess);
            } catch (Exception ex) {
                summary.setFailedCount(summary.getFailedCount() + 1);
                if (summary.getErrors().size() < 25) {
                    summary.getErrors().add("Row " + rowNumber + ": " + ex.getMessage());
                }
            }
        }
        return summary;
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

    private College buildCollegePlaceholderFromRequest(CreateCollegeRequest request) {
        College college = new College();
        college.setName(normalizeLabel(request.getName()));
        college.setType(request.getType().trim());
        college.setDistrict(normalizeLabel(request.getDistrict()));
        college.setState(normalizeLabel(request.getState()));
        college.setAddress(normalizeLabel(request.getAddress()));
        college.setRating(request.getRating() != null ? request.getRating() : 0.0);
        college.setAnnualFees(request.getAnnualFees());
        college.setLatitude(null);
        college.setLongitude(null);
        return college;
    }

    private List<String> parseCollegeNamesFromPdfText(String text) {
        List<String> names = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();

        for (String rawLine : text.split("\\R")) {
            String line = normalizeLabel(rawLine);
            if (isBlank(line) || isCollegePdfNoiseLine(line)) {
                continue;
            }

            String candidate = extractCollegeNameCandidate(line);
            if (isBlank(candidate) || !looksLikeCollegeName(candidate)) {
                continue;
            }

            String normalized = normalizeSearchToken(candidate);
            if (normalized.isEmpty() || !seen.add(normalized)) {
                continue;
            }
            names.add(candidate);
        }

        return names;
    }

    private boolean isCollegePdfNoiseLine(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.startsWith("college list report")
                || normalized.startsWith("information about colleges")
                || normalized.startsWith("type:")
                || normalized.startsWith("filters:")
                || normalized.startsWith("total colleges:")
                || normalized.startsWith("data type:")
                || normalized.startsWith("year:")
                || normalized.startsWith("generated:")
                || normalized.startsWith("page ")
                || normalized.equals("college name")
                || normalized.matches("^[\\d\\s./:-]+$");
    }

    private String extractCollegeNameCandidate(String line) {
        String candidate = line.replaceFirst("^\\d+\\s+", "").trim();
        String[] parts = candidate.split("\\s{2,}|\\t+");
        if (parts.length > 0) {
            candidate = parts[0].trim();
        }

        if (candidate.contains("  ")) {
            candidate = candidate.substring(0, candidate.indexOf("  ")).trim();
        }

        return candidate;
    }

    private boolean looksLikeCollegeName(String candidate) {
        String normalized = candidate.toLowerCase(Locale.ROOT);
        return normalized.contains("college")
                || normalized.contains("institute")
                || normalized.contains("university")
                || normalized.contains("polytechnic")
                || normalized.contains("academy")
                || normalized.contains("school");
    }

    private College getCollegeOrThrow(Long collegeId) {
        return collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "College not found"));
    }

    private void markFeeSyncFailure(College college, String message) {
        college.setFeeSyncStatus("FAILED");
        college.setFeeSyncMessage(normalizeNullable(message));
        college.setFeeLastSyncedAt(LocalDateTime.now());
        collegeRepository.save(college);
    }

    private List<FeeItemExtract> scrapeCollegeFeeItems(College college) {
        try {
            Document doc = Jsoup.connect(college.getFeeSourceUrl())
                    .userAgent("PathVisionFeeBot/1.0")
                    .timeout(15000)
                    .get();

            String rowSelector = isBlank(college.getFeeRowSelector()) ? "table tr" : college.getFeeRowSelector().trim();
            String labelSelector = isBlank(college.getFeeLabelSelector()) ? "th, td:first-child" : college.getFeeLabelSelector().trim();
            String amountSelector = isBlank(college.getFeeAmountSelector()) ? "td:last-child" : college.getFeeAmountSelector().trim();

            List<FeeItemExtract> items = new ArrayList<>();
            for (Element row : doc.select(rowSelector)) {
                String label = selectText(row, labelSelector);
                String amountText = selectText(row, amountSelector);
                if (isBlank(label) || isBlank(amountText)) {
                    continue;
                }
                Double amount = parseAmount(amountText);
                if (amount == null) {
                    continue;
                }
                items.add(new FeeItemExtract(
                        normalizeLabel(label),
                        inferFeeCategory(label),
                        amount,
                        amountText.trim()
                ));
            }

            if (!items.isEmpty()) {
                return items;
            }

            return extractFeeItemsFromPageText(doc.text());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Fee source page could not be fetched.");
        }
    }

    private List<FeeItemExtract> extractFeeItemsFromPageText(String text) {
        if (isBlank(text)) {
            return List.of();
        }

        String[] chunks = text.split("(?i)(?:\\||\\u2022|,|;|\\s{2,})");
        List<FeeItemExtract> items = new ArrayList<>();
        Pattern pattern = Pattern.compile("([A-Za-z][A-Za-z /()-]{2,}?)\\s*(?:-|:)?\\s*(Rs\\.?|INR|\\u20B9)\\s*([0-9,]+(?:\\.\\d{1,2})?)");
        for (String chunk : chunks) {
            Matcher matcher = pattern.matcher(chunk.trim());
            if (!matcher.find()) {
                continue;
            }
            String label = normalizeLabel(matcher.group(1));
            String amountText = matcher.group(0).trim();
            Double amount = parseAmount(amountText);
            if (isBlank(label) || amount == null) {
                continue;
            }
            items.add(new FeeItemExtract(label, inferFeeCategory(label), amount, amountText));
        }
        return items;
    }

    private String selectText(Element root, String selector) {
        if (root == null || isBlank(selector)) {
            return "";
        }
        Element selected = root.select(selector).first();
        return selected == null ? "" : selected.text();
    }

    private Double parseAmount(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        Matcher matcher = Pattern.compile("([0-9][0-9,]*(?:\\.\\d{1,2})?)").matcher(raw.replaceAll("\\s+", ""));
        if (!matcher.find()) {
            return null;
        }
        return Double.parseDouble(matcher.group(1).replace(",", ""));
    }

    private Double resolveAnnualFees(List<FeeItemExtract> items) {
        for (FeeItemExtract item : items) {
            String label = item.label().toLowerCase(Locale.ROOT);
            if (label.contains("total") || label.contains("annual") || label.contains("year")) {
                return item.amount();
            }
        }
        return items.stream()
                .map(FeeItemExtract::amount)
                .filter(value -> value != null)
                .max(Double::compareTo)
                .orElse(null);
    }

    private String inferFeeCategory(String label) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (normalized.contains("hostel")) return "Hostel";
        if (normalized.contains("transport")) return "Transport";
        if (normalized.contains("exam")) return "Exam";
        if (normalized.contains("tuition")) return "Tuition";
        if (normalized.contains("mess")) return "Mess";
        if (normalized.contains("admission")) return "Admission";
        if (normalized.contains("misc")) return "Misc";
        if (normalized.contains("total")) return "Total";
        return "General";
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFeeSourceType(String value) {
        if (isBlank(value)) {
            return "HTML";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(normalizeHeaderKey(headers.get(i)), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        String[] required = {"name", "type", "district", "state", "address"};
        for (String key : required) {
            if (!headerIndex.containsKey(normalizeHeaderKey(key))) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Missing required CSV header: " + key + ". Required: name,type,district,state,address");
            }
        }
    }

    private void validateCutoffHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey(normalizeHeaderKey("college_name"))) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: college_name");
        }
        if (!headerIndex.containsKey(normalizeHeaderKey("community"))) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: community");
        }
        if (!(headerIndex.containsKey(normalizeHeaderKey("cutoffscore")) || headerIndex.containsKey(normalizeHeaderKey("cutoff_mark")))) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required CSV header: cutoffScore or cutoff_mark");
        }
    }

    private boolean isBranchWiseCutoffDataset(Map<String, Integer> headerIndex) {
        return headerIndex.containsKey(normalizeHeaderKey("college name"))
                && headerIndex.containsKey(normalizeHeaderKey("branch"))
                && headerIndex.containsKey(normalizeHeaderKey("branch code"))
                && headerIndex.containsKey(normalizeHeaderKey("oc"))
                && headerIndex.containsKey(normalizeHeaderKey("bc"))
                && headerIndex.containsKey(normalizeHeaderKey("sc"));
    }

    private Integer extractYearFromHeadersAndMeta(List<String> headers, String headerLine) {
        for (String value : headers) {
            Integer year = extractYearFromText(value);
            if (year != null) {
                return year;
            }
        }
        return extractYearFromText(headerLine);
    }

    private String getField(List<String> columns, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(normalizeHeaderKey(key));
        if (index == null || index < 0 || index >= columns.size()) {
            return "";
        }
        return columns.get(index).trim();
    }

    private String normalizeHeaderKey(String header) {
        if (header == null) {
            return "";
        }
        return header
                .replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[_\\s]+", " ");
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

    private Integer currentAdmissionYear() {
        return java.time.Year.now().getValue();
    }

    private College findCollegeByNameOrThrow(String collegeName) {
        String normalizedInput = normalizeSearchToken(collegeName);
        return collegeRepository.findAll().stream()
                .filter(college -> normalizeSearchToken(college.getName()).equals(normalizedInput)
                        || normalizeSearchToken(college.getName()).contains(normalizedInput)
                        || normalizedInput.contains(normalizeSearchToken(college.getName())))
                .findFirst()
                .orElseGet(() -> createPlaceholderCollegeForCutoffImport(collegeName));
    }

    private String normalizeSearchToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("...", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private College createPlaceholderCollegeForCutoffImport(String collegeName) {
        String cleanedName = collegeName == null ? "" : collegeName.replace("\uFEFF", "").trim();
        if (cleanedName.isEmpty()) {
            throw new IllegalArgumentException("College name is missing.");
        }

        String resolvedPlaceholderName = cleanedName.endsWith("...")
                ? cleanedName.substring(0, cleanedName.length() - 3).trim() + " (Imported)"
                : cleanedName;
        final String placeholderName = resolvedPlaceholderName;

        return collegeRepository.findByNameIgnoreCase(placeholderName)
                .orElseGet(() -> {
                    College college = new College();
                    college.setName(placeholderName);
                    college.setType("Engineering");
                    college.setDistrict("Unknown");
                    college.setState("Tamil Nadu");
                    college.setAddress("Imported from cutoff dataset");
                    college.setRating(null);
                    college.setAnnualFees(null);
                    college.setLatitude(null);
                    college.setLongitude(null);
                    return collegeRepository.save(college);
                });
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

    private Integer extractYearFromText(String text) {
        if (isBlank(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b(20\\d{2})\\b").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private List<String> extractPdfTextTokens(byte[] pdfBytes) {
        String raw = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        Matcher matcher = Pattern.compile("\\((.*?)(?<!\\\\)\\)\\s*Tj", Pattern.DOTALL).matcher(raw);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group(1)
                    .replace("\\(", "(")
                    .replace("\\)", ")")
                    .replace("\\\\", "\\")
                    .trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private List<CutoffReportRow> parseCutoffReportRows(String text, Integer year) {
        List<String> lines = text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<CutoffReportRow> rows = new ArrayList<>();
        for (int i = 0; i + 9 < lines.size(); i++) {
            String branchCode = lines.get(i + 2);
            if (!isLikelyBranchCode(branchCode)) {
                continue;
            }

            List<Double> values = new ArrayList<>();
            boolean numericBlock = true;
            for (int j = 3; j <= 9; j++) {
                String raw = lines.get(i + j);
                if ("-".equals(raw)) {
                    values.add(null);
                    continue;
                }
                Double parsed = parseOptionalDoubleSafe(raw);
                if (parsed == null) {
                    numericBlock = false;
                    break;
                }
                values.add(parsed);
            }
            if (!numericBlock) {
                continue;
            }

            String collegeName = lines.get(i);
            String branch = lines.get(i + 1);
            Map<String, Double> communityCutoffs = new HashMap<>();
            List<String> communities = List.of("OC", "BC", "BCM", "MBC", "SC", "SCA", "ST");
            for (int idx = 0; idx < communities.size(); idx++) {
                Double value = values.get(idx);
                if (value != null) {
                    communityCutoffs.put(communities.get(idx), value);
                }
            }
            if (communityCutoffs.isEmpty()) {
                continue;
            }

            rows.add(new CutoffReportRow(
                    normalizeLabel(collegeName),
                    normalizeLabel(branch),
                    normalizeLabel(branchCode),
                    year == null ? currentAdmissionYear() : year,
                    communityCutoffs
            ));
            i += 9;
        }
        return rows;
    }

    private List<CutoffReportRow> parseCutoffReportRowsFromTokens(List<String> tokens, Integer year) {
        int headerIndex = findCutoffHeaderIndex(tokens);
        if (headerIndex < 0) {
            return List.of();
        }

        List<CutoffReportRow> rows = new ArrayList<>();
        List<String> communities = List.of("OC", "BC", "BCM", "MBC", "SC", "SCA", "ST");
        for (int i = headerIndex + 10; i + 9 < tokens.size(); i += 10) {
            String collegeName = tokens.get(i);
            if (collegeName.toLowerCase(Locale.ROOT).startsWith("page ")) {
                break;
            }

            String branch = tokens.get(i + 1);
            String branchCode = tokens.get(i + 2);
            if (!isLikelyBranchCode(branchCode)) {
                continue;
            }

            Map<String, Double> communityCutoffs = new HashMap<>();
            for (int j = 0; j < communities.size(); j++) {
                String rawValue = tokens.get(i + 3 + j);
                if ("-".equals(rawValue.trim())) {
                    continue;
                }
                Double parsed = parseOptionalDoubleSafe(rawValue);
                if (parsed != null) {
                    communityCutoffs.put(communities.get(j), parsed);
                }
            }

            if (communityCutoffs.isEmpty()) {
                continue;
            }

            rows.add(new CutoffReportRow(
                    normalizeLabel(collegeName),
                    normalizeLabel(branch),
                    normalizeLabel(branchCode),
                    year == null ? currentAdmissionYear() : year,
                    communityCutoffs
            ));
        }

        return rows;
    }

    private int findCutoffHeaderIndex(List<String> tokens) {
        for (int i = 0; i + 9 < tokens.size(); i++) {
            if ("College Name".equalsIgnoreCase(tokens.get(i))
                    && "Branch".equalsIgnoreCase(tokens.get(i + 1))
                    && ("Branch Code".equalsIgnoreCase(tokens.get(i + 2)) || "BranchCode".equalsIgnoreCase(tokens.get(i + 2)))
                    && "OC".equalsIgnoreCase(tokens.get(i + 3))
                    && "BC".equalsIgnoreCase(tokens.get(i + 4))
                    && "BCM".equalsIgnoreCase(tokens.get(i + 5))
                    && "MBC".equalsIgnoreCase(tokens.get(i + 6))
                    && "SC".equalsIgnoreCase(tokens.get(i + 7))
                    && "SCA".equalsIgnoreCase(tokens.get(i + 8))
                    && "ST".equalsIgnoreCase(tokens.get(i + 9))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLikelyBranchCode(String value) {
        if (isBlank(value)) {
            return false;
        }
        String token = value.trim();
        return token.matches("[A-Za-z&/ -]{1,12}") && token.replaceAll("[^A-Za-z]", "").length() >= 1;
    }

    private Double parseOptionalDoubleSafe(String value) {
        try {
            return parseOptionalDouble(value);
        } catch (Exception ex) {
            return null;
        }
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
        res.setBranch(cutoff.getBranch());
        res.setBranchCode(cutoff.getBranchCode());
        res.setAdmissionYear(cutoff.getAdmissionYear());
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

    private record FeeItemExtract(String label, String category, Double amount, String amountText) {
    }

    private record CutoffReportRow(
            String collegeName,
            String branch,
            String branchCode,
            Integer admissionYear,
            Map<String, Double> communityCutoffs
    ) {
    }
}
