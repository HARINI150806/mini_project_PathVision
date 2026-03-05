package com.pathvision.dto;

import jakarta.validation.constraints.NotBlank;

public class CollegeRecommendationRequest {

    @NotBlank(message = "Community is required")
    private String community;

    private Double studentScore;

    private Double maxAnnualFees;
    private Double latitude;
    private Double longitude;
    private Integer limit = 10;

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public Double getStudentScore() {
        return studentScore;
    }

    public void setStudentScore(Double studentScore) {
        this.studentScore = studentScore;
    }

    public Double getMaxAnnualFees() {
        return maxAnnualFees;
    }

    public void setMaxAnnualFees(Double maxAnnualFees) {
        this.maxAnnualFees = maxAnnualFees;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
